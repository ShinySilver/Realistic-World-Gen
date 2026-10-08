package rwg.world.sample;

/** Continental-scale inputs resolved before climate and terrain-category selection. */
public final class ContinentalSample {

    public final float distance;
    public final boolean ocean;
    public final int islandTier;

    public ContinentalSample(float distance, boolean ocean, int islandTier) {
        if (ocean && islandTier >= 0) throw new IllegalArgumentException("ocean sample cannot be an island");
        this.distance = distance;
        this.ocean = ocean;
        this.islandTier = islandTier;
    }

    public boolean hasIsland() {
        return islandTier >= 0;
    }
}
