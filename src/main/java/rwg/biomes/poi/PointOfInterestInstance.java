package rwg.biomes.poi;

/** Immutable point-local view of one point-of-interest instance. */
public final class PointOfInterestInstance {

    public final PointOfInterestDefinition definition;
    public final PointOfInterestStage discoveredAt;
    public final long instanceKey;
    public final int centerX;
    public final int centerZ;
    public final float localX;
    public final float localZ;
    public final float strength;

    public PointOfInterestInstance(PointOfInterestDefinition definition, PointOfInterestStage discoveredAt,
            long instanceKey, int centerX, int centerZ, float localX, float localZ, float strength) {
        if (definition == null || discoveredAt == null) throw new NullPointerException("point-of-interest identity");
        this.definition = definition;
        this.discoveredAt = discoveredAt;
        this.instanceKey = instanceKey;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.localX = localX;
        this.localZ = localZ;
        this.strength = strength;
    }

    public float distanceSquared() {
        return localX * localX + localZ * localZ;
    }
}
