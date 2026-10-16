package rwg.world.population;

/** Immutable surface heights covering every X/Z position a population plan may address. */
final class SurfaceHeightMap {

    private static final SurfaceHeightMap EMPTY = new SurfaceHeightMap(0, 0, 0, 0, new int[0]);

    private final int minimumX;
    private final int minimumZ;
    private final int width;
    private final int depth;
    private final int[] heights;

    static SurfaceHeightMap empty() {
        return EMPTY;
    }

    SurfaceHeightMap(int minimumX, int minimumZ, int width, int depth, int[] heights) {
        if (width < 0 || depth < 0 || heights == null || heights.length != width * depth)
            throw new IllegalArgumentException("surface height map");
        this.minimumX = minimumX;
        this.minimumZ = minimumZ;
        this.width = width;
        this.depth = depth;
        this.heights = heights.clone();
    }

    int surfaceY(int blockX, int blockZ) {
        int localX = blockX - minimumX;
        int localZ = blockZ - minimumZ;
        if (localX < 0 || localZ < 0 || localX >= width || localZ >= depth) throw new IllegalArgumentException(
                "feature accessed height outside its planned bounds: " + blockX + "," + blockZ);
        return heights[localX * depth + localZ];
    }

    int minimum(FeatureBounds bounds) {
        int result = Integer.MAX_VALUE;
        for (int x = bounds.minimumX; x <= bounds.maximumX; x++)
            for (int z = bounds.minimumZ; z <= bounds.maximumZ; z++) result = Math.min(result, surfaceY(x, z));
        return result;
    }

    int maximum(FeatureBounds bounds) {
        int result = Integer.MIN_VALUE;
        for (int x = bounds.minimumX; x <= bounds.maximumX; x++)
            for (int z = bounds.minimumZ; z <= bounds.maximumZ; z++) result = Math.max(result, surfaceY(x, z));
        return result;
    }
}
