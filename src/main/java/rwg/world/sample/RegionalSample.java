package rwg.world.sample;

/** Reusable regional terrain result resolved between morphology and biome-detail composition. */
public final class RegionalSample {

    public final float uncarvedHeight;
    public final float erodedMountainDistance;
    public final float height;

    public RegionalSample(float uncarvedHeight, float erodedMountainDistance, float height) {
        this.uncarvedHeight = uncarvedHeight;
        this.erodedMountainDistance = erodedMountainDistance;
        this.height = height;
    }
}
