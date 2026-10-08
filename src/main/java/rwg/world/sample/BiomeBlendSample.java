package rwg.world.sample;

import rwg.registry.BiomeRegistration;

/** Immutable chunk-level biome composition produced by the terrain blending kernel. */
public final class BiomeBlendSample {

    private static final float MINIMUM_TOTAL_WEIGHT = 0.999f;
    private static final float MAXIMUM_TOTAL_WEIGHT = 1.001f;

    private final BiomeRegistration[] registrations;
    private final float[] weights;
    public final BiomeRegistration primary;

    public BiomeBlendSample(BiomeRegistration[] registrations, float[] weights) {
        if (registrations == null || weights == null
                || registrations.length != weights.length
                || registrations.length == 0)
            throw new IllegalArgumentException("biome blend entries");
        this.registrations = registrations.clone();
        this.weights = weights.clone();
        float total = 0f;
        float primaryWeight = -1f;
        BiomeRegistration primaryRegistration = null;
        for (int index = 0; index < registrations.length; index++) {
            if (registrations[index] == null || weights[index] <= 0f)
                throw new IllegalArgumentException("invalid biome blend contribution at " + index);
            total += weights[index];
            if (weights[index] > primaryWeight) {
                primaryWeight = weights[index];
                primaryRegistration = registrations[index];
            }
        }
        if (total < MINIMUM_TOTAL_WEIGHT || total > MAXIMUM_TOTAL_WEIGHT)
            throw new IllegalArgumentException("biome blend weights total " + total);
        primary = primaryRegistration;
    }

    public int size() {
        return registrations.length;
    }

    public BiomeRegistration registration(int index) {
        return registrations[index];
    }

    public float weight(int index) {
        return weights[index];
    }
}
