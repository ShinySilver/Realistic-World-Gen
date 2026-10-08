package rwg.world;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import rwg.biomes.surface.SurfaceBase;
import rwg.registry.BiomeRegistration;
import rwg.registry.TerrainCategory;
import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;
import rwg.util.NoiseSelector;
import rwg.world.layout.WorldgenSelector;
import rwg.world.sample.BiomeBlendSample;
import rwg.world.sample.ColumnSample;
import rwg.world.sample.MorphologySample;
import rwg.world.sample.TerrainContext;
import rwg.world.terrain.MorphologyRules;

/** Chunk-aligned selection, biome blending, and base-terrain generation for Minecraft. */
public final class WorldGenerator {

    private static final int CHUNK_SIZE = 16;
    private static final int BIOME_STEP = 8;
    private static final int KERNEL_RADIUS = 8;
    private static final int SOURCE_SIZE = 19; // -8..10 at eight-block spacing
    private static final int ANCHOR_SIZE = 3; // x/z = 0, 8, 16
    private static final float[][] KERNEL = createKernel();

    private final WorldgenSelector selector;
    private final NoiseGenerator terrainNoise;
    private final CellNoise cellNoise;
    private final long seed;

    public WorldGenerator(long seed, WorldgenSelector selector) {
        if (selector == null) throw new NullPointerException("worldgen selector");
        this.seed = seed;
        this.selector = selector;
        terrainNoise = NoiseSelector.createNoiseGenerator(seed);
        cellNoise = new CellNoise(seed, (short) 0, true);
    }

    /** Approximate one-column LOD; exact chunk previews must use {@link #sampleChunk(int, int)}. */
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

        PreviewColumn(ColumnSample sample, float baseHeight, float height) {
            this.sample = sample;
            this.baseHeight = baseHeight;
            this.height = height;
        }
    }

    /** Evaluates all 256 columns, blending biome heights across chunk boundaries. */
    public ChunkTerrain sampleChunk(int chunkX, int chunkZ) {
        int originX = chunkX * CHUNK_SIZE;
        int originZ = chunkZ * CHUNK_SIZE;
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
        for (int id = 0; id < registrationCount; id++) {
            if (active[id]) activeIds[activeCount++] = id;
        }

        float[] heights = new float[CHUNK_SIZE * CHUNK_SIZE];
        float[] baseHeights = new float[CHUNK_SIZE * CHUNK_SIZE];
        BiomeRegistration[] registrations = new BiomeRegistration[CHUNK_SIZE * CHUNK_SIZE];
        float[] riverStrengths = new float[CHUNK_SIZE * CHUNK_SIZE];
        float[] mountainWeights = new float[CHUNK_SIZE * CHUNK_SIZE];
        float[] chunkWeights = new float[registrationCount];
        for (int localX = 0; localX < CHUNK_SIZE; localX++) {
            int ax = localX / BIOME_STEP;
            float tx = (localX % BIOME_STEP) / (float) BIOME_STEP;
            for (int localZ = 0; localZ < CHUNK_SIZE; localZ++) {
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
                // Rounding can leave the cumulative kernel infinitesimally below one at the last contributor.
                if (selectedRegistration == null) selectedRegistration = selector.registration(activeIds[activeCount - 1]);
                float riverStrength = morphology.riverStrength;
                height = carveRiver(
                        worldX,
                        worldZ,
                        height,
                        columnSample.regional.erodedMountainDistance,
                        morphology.riverDistance);
                int column = localX * CHUNK_SIZE + localZ;
                heights[column] = height;
                baseHeights[column] = columnSample.regional.height;
                registrations[column] = selectedRegistration;
                riverStrengths[column] = riverStrength;
                mountainWeights[column] = morphology.mountainStrength;
            }
        }
        BiomeRegistration[] blendRegistrations = new BiomeRegistration[activeCount];
        float[] blendWeights = new float[activeCount];
        float totalWeight = 0f;
        for (int index = 0; index < activeCount; index++) totalWeight += chunkWeights[activeIds[index]];
        for (int index = 0; index < activeCount; index++) {
            int id = activeIds[index];
            blendRegistrations[index] = selector.registration(id);
            blendWeights[index] = chunkWeights[id] / totalWeight;
        }
        return new ChunkTerrain(
                heights,
                baseHeights,
                registrations,
                riverStrengths,
                mountainWeights,
                new BiomeBlendSample(blendRegistrations, blendWeights));
    }

    /** Fills raw stone, water and air; biome mapgen and Forge replacement hooks may run before surface painting. */
    public ChunkBlocks fillBaseChunk(ChunkTerrain terrain) {
        if (terrain == null) throw new NullPointerException("chunk terrain");
        Block[] blocks = new Block[CHUNK_SIZE * CHUNK_SIZE * 256];
        byte[] metadata = new byte[blocks.length];
        BiomeGenBase[] baseBiomes = new BiomeGenBase[CHUNK_SIZE * CHUNK_SIZE];
        float[] surfaceHeights = new float[terrain.heights.length];
        for (int localX = 0; localX < CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < CHUNK_SIZE; localZ++) {
                int sampleIndex = localX * CHUNK_SIZE + localZ;
                int biomeIndex = localZ * CHUNK_SIZE + localX;
                int column = sampleIndex * 256;
                int top = (int) terrain.heights[sampleIndex];
                surfaceHeights[biomeIndex] = terrain.heights[sampleIndex];
                BiomeRegistration registration = terrain.registrations[sampleIndex];
                baseBiomes[biomeIndex] = terrain.riverStrengths[sampleIndex] > 0f ? registration.riverBiome
                        : registration.biome;
                for (int level = 0; level < 256; level++) {
                    blocks[column + level] = level <= top ? Blocks.stone : level < 63 ? Blocks.water : Blocks.air;
                }
            }
        }

        return new ChunkBlocks(terrain, blocks, metadata, baseBiomes, surfaceHeights);
    }

    /** Calls the existing biome surface implementations, then places bedrock. */
    public void paintSurfaces(int chunkX, int chunkZ, World world, ChunkBlocks chunk) {
        if (world == null || chunk == null) throw new NullPointerException("surface painting inputs");
        ChunkTerrain terrain = chunk.terrain;
        Block[] blocks = chunk.blocks;
        byte[] metadata = chunk.metadata;
        BiomeGenBase[] baseBiomes = chunk.baseBiomes;
        float[] surfaceHeights = chunk.surfaceHeights;
        Random random = new Random(seed ^ ((long) chunkX * 0x4F9939F508L) ^ ((long) chunkZ * 0x1EF1565BD5L));

        for (int localX = 0; localX < CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < CHUNK_SIZE; localZ++) {
                int sampleIndex = localX * CHUNK_SIZE + localZ;
                BiomeRegistration registration = terrain.registrations[sampleIndex];
                int worldX = chunkX * CHUNK_SIZE + localX;
                int worldZ = chunkZ * CHUNK_SIZE + localZ;
                if (selector.usesReferenceBiomeGeneration()) {
                    // Vanilla's 1.7 terrain hook addresses the raw array as (secondCoord * 16 + firstCoord),
                    // opposite to Chunk's X-major constructor and RWG's legacy surface convention. Swap only the
                    // coordinates supplied to that hook so the selected atlas column is painted in place.
                    registration.biome.genTerrainBlocks(
                            world,
                            random,
                            blocks,
                            metadata,
                            worldZ,
                            worldX,
                            terrainNoise.noise2(worldX / 32f, worldZ / 32f) * 4d);
                } else {
                    for (SurfaceBase surface : registration.surfaces) {
                        surface.paintTerrain(
                                blocks,
                                metadata,
                                worldX,
                                worldZ,
                                localZ,
                                localX,
                                -1,
                                world,
                                random,
                                terrainNoise,
                                cellNoise,
                                surfaceHeights,
                                terrain.riverStrengths[sampleIndex],
                                baseBiomes);
                    }
                    int column = sampleIndex * 256;
                    blocks[column] = Blocks.bedrock;
                    for (int depth = 2; depth <= 5; depth++) blocks[column + random.nextInt(depth)] = Blocks.bedrock;
                }
            }
        }
    }

    /** Chunk-owned arrays in localX * 16 + localZ order, ready for a Minecraft block adapter. */
    public static final class ChunkTerrain {

        public final float[] heights;
        public final float[] baseHeights;
        public final BiomeRegistration[] registrations;
        public final float[] riverStrengths;
        /** Blended mountain-category contribution used to host underground river caves. */
        public final float[] mountainWeights;
        public final BiomeBlendSample biomeBlend;

        ChunkTerrain(float[] heights, float[] baseHeights, BiomeRegistration[] registrations, float[] riverStrengths,
                float[] mountainWeights, BiomeBlendSample biomeBlend) {
            this.heights = heights;
            this.baseHeights = baseHeights;
            this.registrations = registrations;
            this.riverStrengths = riverStrengths;
            this.mountainWeights = mountainWeights;
            this.biomeBlend = biomeBlend;
        }
    }

    /** Blocks are X-major; biome and surface-height arrays use Minecraft's Z-major biome-map order. */
    public static final class ChunkBlocks {

        public final ChunkTerrain terrain;
        public final Block[] blocks;
        public final byte[] metadata;
        public final BiomeGenBase[] baseBiomes;
        public final float[] surfaceHeights;

        ChunkBlocks(ChunkTerrain terrain, Block[] blocks, byte[] metadata, BiomeGenBase[] baseBiomes,
                float[] surfaceHeights) {
            this.terrain = terrain;
            this.blocks = blocks;
            this.metadata = metadata;
            this.baseBiomes = baseBiomes;
            this.surfaceHeights = surfaceHeights;
        }
    }

    private static float bilinear(float[][][] anchors, int ax, int az, int id, float tx, float tz) {
        float near = anchors[ax][az][id] * (1f - tx) + anchors[ax + 1][az][id] * tx;
        float far = anchors[ax][az + 1][id] * (1f - tx) + anchors[ax + 1][az + 1][id] * tx;
        return near * (1f - tz) + far * tz;
    }

    /** Cuts the core channel only after all biome terrain contributions have produced the final height. */
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
        for (float[] row : kernel) {
            for (int index = 0; index < row.length; index++) row[index] /= total;
        }
        return kernel;
    }
}
