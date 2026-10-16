package rwg.world;

import net.minecraft.world.biome.BiomeGenBase;

import rwg.biomes.surface.SurfaceRule;
import rwg.registry.BiomeRegistration;
import rwg.world.sample.BiomeBlendSample;

/** Immutable result of all expensive X/Z-only work for one 16x16 column. */
public final class ColumnPlan {

    public static final int SIZE = 16;
    public static final int COLUMN_COUNT = SIZE * SIZE;

    private final float[] heights;
    private final float[] baseHeights;
    private final BiomeRegistration[] registrations;
    private final float[] riverStrengths;
    private final float[] mountainWeights;
    private final float[] undergroundRiverStrengths;
    private final float[] riverJunctionStrengths;
    private final float[] surfaceSlopes;
    private final float[] surfaceVariations;
    private final float[] broadSurfaceVariations;
    private final BiomeGenBase[] biomes;
    private final SurfaceRule[] surfaceRules;
    private final BiomeBlendSample biomeBlend;
    private final int minimumSurfaceY;
    private final int maximumSurfaceY;
    private final int maximumSurfaceDepth;
    private final boolean undergroundRiver;

    ColumnPlan(float[] heights, float[] baseHeights, BiomeRegistration[] registrations, float[] riverStrengths,
            float[] mountainWeights, float[] undergroundRiverStrengths, float[] riverJunctionStrengths,
            float[] surfaceSlopes, float[] surfaceVariations, float[] broadSurfaceVariations,
            BiomeBlendSample biomeBlend) {
        requireLength(heights, "heights");
        requireLength(baseHeights, "base heights");
        requireLength(registrations, "registrations");
        requireLength(riverStrengths, "river strengths");
        requireLength(mountainWeights, "mountain weights");
        requireLength(undergroundRiverStrengths, "underground river strengths");
        requireLength(riverJunctionStrengths, "river junction strengths");
        requireLength(surfaceSlopes, "surface slopes");
        requireLength(surfaceVariations, "surface variations");
        requireLength(broadSurfaceVariations, "broad surface variations");
        if (biomeBlend == null) throw new NullPointerException("biome blend");
        this.heights = heights.clone();
        this.baseHeights = baseHeights.clone();
        this.registrations = registrations.clone();
        this.riverStrengths = riverStrengths.clone();
        this.mountainWeights = mountainWeights.clone();
        this.undergroundRiverStrengths = undergroundRiverStrengths.clone();
        this.riverJunctionStrengths = riverJunctionStrengths.clone();
        this.surfaceSlopes = surfaceSlopes.clone();
        this.surfaceVariations = surfaceVariations.clone();
        this.broadSurfaceVariations = broadSurfaceVariations.clone();
        this.biomeBlend = biomeBlend;
        biomes = new BiomeGenBase[COLUMN_COUNT];
        surfaceRules = new SurfaceRule[COLUMN_COUNT];
        int minimum = Integer.MAX_VALUE;
        int maximum = Integer.MIN_VALUE;
        int maximumDepth = 0;
        boolean hasUndergroundRiver = false;
        for (int localX = 0; localX < SIZE; localX++) {
            for (int localZ = 0; localZ < SIZE; localZ++) {
                int sampleIndex = index(localX, localZ);
                BiomeRegistration registration = this.registrations[sampleIndex];
                if (registration == null) throw new NullPointerException("registration at " + sampleIndex);
                biomes[localZ * SIZE + localX] = this.riverStrengths[sampleIndex] > 0f ? registration.riverBiome
                        : registration.biome;
                // Resolve once during X/Z planning. Cube generation performs no registry lookup or rule selection.
                SurfaceRule surfaceRule = registration.surfaceRule;
                if (surfaceRule.maximumDepth() < 0)
                    throw new IllegalArgumentException("negative surface depth for registration " + registration.id);
                surfaceRules[localZ * SIZE + localX] = surfaceRule;
                maximumDepth = Math.max(maximumDepth, surfaceRule.maximumDepth());
                hasUndergroundRiver |= this.undergroundRiverStrengths[sampleIndex] > 0f
                        || this.riverJunctionStrengths[sampleIndex] > 0f;
                int surfaceY = (int) Math.floor(this.heights[sampleIndex]);
                minimum = Math.min(minimum, surfaceY);
                maximum = Math.max(maximum, surfaceY);
            }
        }
        minimumSurfaceY = minimum;
        maximumSurfaceY = maximum;
        maximumSurfaceDepth = maximumDepth;
        undergroundRiver = hasUndergroundRiver;
    }

    public float height(int localX, int localZ) {
        return heights[index(localX, localZ)];
    }

    public float baseHeight(int localX, int localZ) {
        return baseHeights[index(localX, localZ)];
    }

    public BiomeRegistration registration(int localX, int localZ) {
        return registrations[index(localX, localZ)];
    }

    public float riverStrength(int localX, int localZ) {
        return riverStrengths[index(localX, localZ)];
    }

    public float mountainWeight(int localX, int localZ) {
        return mountainWeights[index(localX, localZ)];
    }

    public float surfaceSlope(int localX, int localZ) {
        return surfaceSlopes[index(localX, localZ)];
    }

    public float surfaceVariation(int localX, int localZ) {
        return surfaceVariations[index(localX, localZ)];
    }

    public float broadSurfaceVariation(int localX, int localZ) {
        return broadSurfaceVariations[index(localX, localZ)];
    }

    public float undergroundRiverStrength(int localX, int localZ) {
        return undergroundRiverStrengths[index(localX, localZ)];
    }

    public float riverJunctionStrength(int localX, int localZ) {
        return riverJunctionStrengths[index(localX, localZ)];
    }

    public boolean hasUndergroundRiver() {
        return undergroundRiver;
    }

    public BiomeBlendSample biomeBlend() {
        return biomeBlend;
    }

    public BiomeGenBase biome(int localX, int localZ) {
        index(localX, localZ);
        return biomes[localZ * SIZE + localX];
    }

    public SurfaceRule surfaceRule(int localX, int localZ) {
        index(localX, localZ);
        return surfaceRules[localZ * SIZE + localX];
    }

    public int minimumSurfaceY() {
        return minimumSurfaceY;
    }

    public int maximumSurfaceY() {
        return maximumSurfaceY;
    }

    public int maximumSurfaceDepth() {
        return maximumSurfaceDepth;
    }

    public BiomeGenBase[] copyBiomes() {
        return biomes.clone();
    }

    private static int index(int localX, int localZ) {
        if ((localX | localZ) < 0 || localX >= SIZE || localZ >= SIZE)
            throw new IndexOutOfBoundsException("column coordinates " + localX + "," + localZ);
        return localX * SIZE + localZ;
    }

    private static void requireLength(Object[] values, String name) {
        if (values == null || values.length != COLUMN_COUNT) throw new IllegalArgumentException(name);
    }

    private static void requireLength(float[] values, String name) {
        if (values == null || values.length != COLUMN_COUNT) throw new IllegalArgumentException(name);
    }
}
