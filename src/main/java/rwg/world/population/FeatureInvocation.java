package rwg.world.population;

import rwg.biomes.decorators.CubicFeature;

/** Immutable default plan for a whole biome-feature recipe. */
public final class FeatureInvocation implements PlannedFeature {

    private final CubicFeature feature;
    private final FeatureBounds bounds;
    private final int ownerCubeY;
    private final PopulationStage stage;
    private final long seed;
    private final float strength;
    private final float river;

    public FeatureInvocation(CubicFeature feature, FeatureBounds bounds, int ownerCubeY, PopulationStage stage,
            long seed, float strength, float river) {
        if (feature == null || bounds == null || stage == null) throw new NullPointerException("planned feature");
        this.feature = feature;
        this.bounds = bounds;
        this.ownerCubeY = ownerCubeY;
        this.stage = stage;
        this.seed = seed;
        this.strength = strength;
        this.river = river;
    }

    @Override
    public CubicFeature feature() {
        return feature;
    }

    @Override
    public FeatureBounds bounds() {
        return bounds;
    }

    @Override
    public int ownerCubeY() {
        return ownerCubeY;
    }

    @Override
    public PopulationStage stage() {
        return stage;
    }

    @Override
    public long seed() {
        return seed;
    }

    @Override
    public float strength() {
        return strength;
    }

    @Override
    public float river() {
        return river;
    }

    FeatureInvocation withBounds(FeatureBounds replacement) {
        return new FeatureInvocation(feature, replacement, ownerCubeY, stage, seed, strength, river);
    }
}
