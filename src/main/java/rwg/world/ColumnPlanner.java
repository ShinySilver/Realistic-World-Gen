package rwg.world;

import rwg.registry.BiomeRegistration;
import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;
import rwg.util.NoiseSelector;
import rwg.world.layout.WorldgenSelector;
import rwg.world.sample.BiomeBlendSample;
import rwg.world.sample.ColumnSample;
import rwg.world.sample.MorphologySample;
import rwg.world.sample.TerrainContext;
import rwg.world.terrain.MorphologyRules;

/** Pure, thread-safe planner for all expensive data shared by the vertical cubes in one X/Z column. */
public final class ColumnPlanner {

    private static final int BIOME_STEP = 8;
    private static final int KERNEL_RADIUS = 8;
    private static final int SOURCE_SIZE = 20;
    private static final int ANCHOR_SIZE = 4;
    private static final float[][] KERNEL = createKernel();

    private final WorldgenSelector selector;
    private final NoiseGenerator terrainNoise;
    private final CellNoise cellNoise;

    public ColumnPlanner(long seed, WorldgenSelector selector) {
        if (selector == null) throw new NullPointerException("worldgen selector");
        this.selector = selector;
        terrainNoise = NoiseSelector.createNoiseGenerator(seed);
        cellNoise = new CellNoise(seed, (short) 0, true);
    }

    /** Approximate one-point view for diagnostics; exact generation uses {@link #plan(int, int)}. */
    public PreviewColumn samplePreviewPoint(int x, int z) {
        ColumnSample sample = selector.select(x, z);
        BiomeRegistration registration = sample.biome.registration;
        TerrainContext terrainContext = new TerrainContext(
                0f,
                sample.regional.height,
                sample.morphology,
                registration.category);
        float height = selector.terrainFor(registration)
                .generateNoise(terrainNoise, cellNoise, x, z, terrainContext, 1f);
        height = carveRiver(x, z, height, sample.regional.erodedMountainDistance, sample.morphology.riverDistance);
        return new PreviewColumn(sample, sample.regional.height, height);
    }

    public static final class PreviewColumn {

        public final ColumnSample sample;
        public final float baseHeight;
        public final float height;

        private PreviewColumn(ColumnSample sample, float baseHeight, float height) {
            this.sample = sample;
            this.baseHeight = baseHeight;
            this.height = height;
        }
    }

    public ColumnPlan plan(int chunkX, int chunkZ) {
        int originX = chunkX * ColumnPlan.SIZE;
        int originZ = chunkZ * ColumnPlan.SIZE;
        int registrationCount = selector.registrationCount();
        int[][] sourceIds = new int[SOURCE_SIZE][SOURCE_SIZE];
        boolean[] active = new boolean[registrationCount];
        for (int sx = 0; sx < SOURCE_SIZE; sx++) {
            int worldX = originX + (sx - KERNEL_RADIUS) * BIOME_STEP;
            for (int sz = 0; sz < SOURCE_SIZE; sz++) {
                int worldZ = originZ + (sz - KERNEL_RADIUS) * BIOME_STEP;
                int id = selector.biomeForBlend(worldX, worldZ).id;
                sourceIds[sx][sz] = id;
                active[id] = true;
            }
        }

        float[][][] anchors = new float[ANCHOR_SIZE][ANCHOR_SIZE][registrationCount];
        for (int ax = 0; ax < ANCHOR_SIZE; ax++) {
            for (int az = 0; az < ANCHOR_SIZE; az++) {
                float[] weights = anchors[ax][az];
                for (int dx = -KERNEL_RADIUS; dx <= KERNEL_RADIUS; dx++) {
                    for (int dz = -KERNEL_RADIUS; dz <= KERNEL_RADIUS; dz++) {
                        weights[sourceIds[ax + dx + KERNEL_RADIUS][az + dz + KERNEL_RADIUS]] += KERNEL[dx
                                + KERNEL_RADIUS][dz + KERNEL_RADIUS];
                    }
                }
            }
        }

        int[] activeIds = new int[registrationCount];
        int activeCount = 0;
        for (int id = 0; id < registrationCount; id++) if (active[id]) activeIds[activeCount++] = id;

        float[] heights = new float[ColumnPlan.COLUMN_COUNT];
        float[] baseHeights = new float[ColumnPlan.COLUMN_COUNT];
        BiomeRegistration[] registrations = new BiomeRegistration[ColumnPlan.COLUMN_COUNT];
        float[] riverStrengths = new float[ColumnPlan.COLUMN_COUNT];
        float[] mountainWeights = new float[ColumnPlan.COLUMN_COUNT];
        float[] undergroundRiverStrengths = new float[ColumnPlan.COLUMN_COUNT];
        float[] riverJunctionStrengths = new float[ColumnPlan.COLUMN_COUNT];
        float[][] heightHalo = new float[ColumnPlan.SIZE + 2][ColumnPlan.SIZE + 2];
        float[] chunkWeights = new float[registrationCount];
        for (int localX = -1; localX <= ColumnPlan.SIZE; localX++) {
            int ax = localX / BIOME_STEP;
            float tx = (localX % BIOME_STEP) / (float) BIOME_STEP;
            for (int localZ = -1; localZ <= ColumnPlan.SIZE; localZ++) {
                int az = localZ / BIOME_STEP;
                float tz = (localZ % BIOME_STEP) / (float) BIOME_STEP;
                int worldX = originX + localX;
                int worldZ = originZ + localZ;
                ColumnSample columnSample = selector.select(worldX, worldZ);
                MorphologySample morphology = columnSample.morphology;
                float height = 0f;
                float biomeChoice = clamp(.5f + terrainNoise.noise2(worldX / 15f, worldZ / 15f));
                float cumulativeWeight = 0f;
                BiomeRegistration selectedRegistration = null;
                for (int index = 0; index < activeCount; index++) {
                    int id = activeIds[index];
                    float weight = bilinear(anchors, ax, az, id, tx, tz);
                    if (weight == 0f) continue;
                    if (localX >= 0 && localX < ColumnPlan.SIZE && localZ >= 0 && localZ < ColumnPlan.SIZE)
                        chunkWeights[id] += weight;
                    BiomeRegistration registration = selector.registration(id);
                    TerrainContext terrainContext = new TerrainContext(
                            0f,
                            columnSample.regional.height,
                            morphology,
                            registration.category);
                    cumulativeWeight += weight;
                    if (selectedRegistration == null && cumulativeWeight > biomeChoice)
                        selectedRegistration = registration;
                    height += selector.terrainFor(registration)
                            .generateNoise(terrainNoise, cellNoise, worldX, worldZ, terrainContext, weight) * weight;
                }
                if (selectedRegistration == null)
                    selectedRegistration = selector.registration(activeIds[activeCount - 1]);
                float riverStrength = morphology.riverStrength;
                height = carveRiver(
                        worldX,
                        worldZ,
                        height,
                        columnSample.regional.erodedMountainDistance,
                        morphology.riverDistance);
                heightHalo[localX + 1][localZ + 1] = height;
                if (localX < 0 || localX >= ColumnPlan.SIZE || localZ < 0 || localZ >= ColumnPlan.SIZE) continue;
                int column = localX * ColumnPlan.SIZE + localZ;
                heights[column] = height;
                baseHeights[column] = columnSample.regional.height;
                registrations[column] = selectedRegistration;
                riverStrengths[column] = riverStrength;
                mountainWeights[column] = morphology.mountainStrength;
                undergroundRiverStrengths[column] = selector.undergroundRiverStrength(worldX, worldZ);
                riverJunctionStrengths[column] = selector.riverJunctionStrength(worldX, worldZ);
            }
        }

        int blendCount = 0;
        for (int index = 0; index < activeCount; index++) if (chunkWeights[activeIds[index]] > 0f) blendCount++;
        BiomeRegistration[] blendRegistrations = new BiomeRegistration[blendCount];
        float[] blendWeights = new float[blendCount];
        float[] surfaceSlopes = new float[ColumnPlan.COLUMN_COUNT];
        float[] surfaceVariations = new float[ColumnPlan.COLUMN_COUNT];
        float[] broadSurfaceVariations = new float[ColumnPlan.COLUMN_COUNT];
        for (int localX = 0; localX < ColumnPlan.SIZE; localX++) {
            for (int localZ = 0; localZ < ColumnPlan.SIZE; localZ++) {
                float center = heightHalo[localX + 1][localZ + 1];
                float slope = Math.max(
                        Math.max(
                                Math.abs(center - heightHalo[localX][localZ + 1]),
                                Math.abs(center - heightHalo[localX + 2][localZ + 1])),
                        Math.max(
                                Math.abs(center - heightHalo[localX + 1][localZ]),
                                Math.abs(center - heightHalo[localX + 1][localZ + 2])));
                surfaceSlopes[localX * ColumnPlan.SIZE + localZ] = slope;
                int column = localX * ColumnPlan.SIZE + localZ;
                int worldX = originX + localX;
                int worldZ = originZ + localZ;
                surfaceVariations[column] = terrainNoise.noise2(worldX / 12f, worldZ / 12f);
                broadSurfaceVariations[column] = terrainNoise.noise2(worldX / 50f, worldZ / 50f);
            }
        }
        float totalWeight = 0f;
        for (int index = 0; index < activeCount; index++) totalWeight += chunkWeights[activeIds[index]];
        int blendIndex = 0;
        for (int index = 0; index < activeCount; index++) {
            int id = activeIds[index];
            if (chunkWeights[id] <= 0f) continue;
            blendRegistrations[blendIndex] = selector.registration(id);
            blendWeights[blendIndex++] = chunkWeights[id] / totalWeight;
        }
        return new ColumnPlan(
                heights,
                baseHeights,
                registrations,
                riverStrengths,
                mountainWeights,
                undergroundRiverStrengths,
                riverJunctionStrengths,
                surfaceSlopes,
                surfaceVariations,
                broadSurfaceVariations,
                new BiomeBlendSample(blendRegistrations, blendWeights));
    }

    private float carveRiver(int x, int z, float height, float mountainDistance, float distance) {
        float hillBlend = 1f - smoothstep(650f, MorphologyRules.MOUNTAIN_INFLUENCE_RADIUS, mountainDistance);
        float mountainBlend = 1f - smoothstep(
                MorphologyRules.SURFACE_RIVER_MOUNTAIN_FADE_START,
                MorphologyRules.SURFACE_RIVER_MOUNTAIN_FADE_END,
                mountainDistance);
        float width = lerp(26f, 30f, hillBlend);
        float bed = lerp(61f, 59.5f, hillBlend);
        width = lerp(width, 20f, mountainBlend);
        bed = lerp(bed, 57.5f, mountainBlend);
        if (distance >= width) return height;
        float influence = smooth(1f - distance / width);
        float channel = bed + terrainNoise.noise2(x / 18f, z / 18f) * .8f;
        return height + (channel - height) * influence;
    }

    private static float bilinear(float[][][] anchors, int ax, int az, int id, float tx, float tz) {
        float near = anchors[ax][az][id] * (1f - tx) + anchors[ax + 1][az][id] * tx;
        float far = anchors[ax][az + 1][id] * (1f - tx) + anchors[ax + 1][az + 1][id] * tx;
        return near * (1f - tz) + far * tz;
    }

    private static float lerp(float start, float end, float amount) {
        return start + (end - start) * amount;
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(.99999f, value));
    }

    private static float smooth(float value) {
        return value * value * (3f - 2f * value);
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        return smooth(Math.max(0f, Math.min(1f, (value - edge0) / (edge1 - edge0))));
    }

    private static float[][] createKernel() {
        float[][] kernel = new float[KERNEL_RADIUS * 2 + 1][KERNEL_RADIUS * 2 + 1];
        float total = 0f;
        for (int dx = -KERNEL_RADIUS; dx <= KERNEL_RADIUS; dx++) {
            for (int dz = -KERNEL_RADIUS; dz <= KERNEL_RADIUS; dz++) {
                float value = 0.445f / (float) Math.sqrt(dx * dx + dz * dz + 0.3f);
                kernel[dx + KERNEL_RADIUS][dz + KERNEL_RADIUS] = value;
                total += value;
            }
        }
        for (float[] row : kernel) for (int index = 0; index < row.length; index++) row[index] /= total;
        return kernel;
    }
}
