package rwg.world.population;

import rwg.biomes.decorators.CubicFeature;

/** Minimal scheduling contract; complex features may provide their own immutable implementation. */
public interface PlannedFeature {

    CubicFeature feature();

    FeatureBounds bounds();

    int ownerCubeY();

    PopulationStage stage();

    long seed();

    float strength();

    float river();
}
