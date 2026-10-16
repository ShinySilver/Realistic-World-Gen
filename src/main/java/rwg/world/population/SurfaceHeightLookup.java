package rwg.world.population;

/** Resolves immutable terrain heights for feature placement without querying mutable world blocks. */
public final class SurfaceHeightLookup {

    private final SurfaceHeightMap heights;

    SurfaceHeightLookup(SurfaceHeightMap heights) {
        if (heights == null) throw new NullPointerException("heights");
        this.heights = heights;
    }

    public int surfaceY(int blockX, int blockZ) {
        return heights.surfaceY(blockX, blockZ);
    }
}
