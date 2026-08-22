package rwg.util;

/**
 * An infinite, deterministic field of continent sites. Each grid cell owns one jittered site. A single relaxation pass
 * pulls that site toward the average of its eight neighbours, avoiding both a rigid grid and tightly clustered sites.
 */
public class ContinentalNoise {

    private static final double CELL_SIZE = 3600D;
    private static final double JITTER = 0.38D;
    private static final double RELAXATION = 0.5D;
    private static final double MIN_RADIUS = 1120D;
    private static final double RADIUS_VARIATION = 360D;
    private static final double ISLAND_CELL_SIZE = CELL_SIZE;
    private static final double ISLAND_JITTER = 0.08D;
    private static final double ISLAND_CHANCE = 0.5D;
    private static final double ISLAND_MIN_RADIUS = 140D;
    private static final double ISLAND_RADIUS_VARIATION = 240D;
    private static final double LARGE_WARP_SCALE = 750D;
    private static final double LARGE_WARP_STRENGTH = 700D;

    private final long seed;
    private final NoiseGenerator warpNoiseX;
    private final NoiseGenerator warpNoiseY;
    private final float warpOriginX;
    private final float warpOriginY;

    public ContinentalNoise(long seed) {
        this.seed = seed;
        warpNoiseX = NoiseSelector.createNoiseGenerator(seed ^ 0xBB67AE8584CAA73BL);
        warpNoiseY = NoiseSelector.createNoiseGenerator(seed ^ 0x3C6EF372FE94F82BL);
        warpOriginX = warpNoiseX.noise2(0f, 0f);
        warpOriginY = warpNoiseY.noise2(0f, 0f);
    }

    /**
     * Returns distance from the nearest continental shore in blocks. Positive values are land, negative values are sea.
     */
    public float getValue(int x, int y) {
        double warpedX = x
                + (warpNoiseX.noise2((float) (x / LARGE_WARP_SCALE), (float) (y / LARGE_WARP_SCALE)) - warpOriginX)
                        * LARGE_WARP_STRENGTH;
        double warpedY = y
                + (warpNoiseY.noise2((float) (x / LARGE_WARP_SCALE), (float) (y / LARGE_WARP_SCALE)) - warpOriginY)
                        * LARGE_WARP_STRENGTH;

        int cellX = floor(warpedX / CELL_SIZE);
        int cellY = floor(warpedY / CELL_SIZE);
        double best = -Double.MAX_VALUE;

        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetY = -1; offsetY <= 1; offsetY++) {
                int siteCellX = cellX + offsetX;
                int siteCellY = cellY + offsetY;
                double siteX = relaxedSite(siteCellX, siteCellY, true);
                double siteY = relaxedSite(siteCellX, siteCellY, false);
                double dx = warpedX - siteX;
                double dy = warpedY - siteY;
                double distance = Math.sqrt(dx * dx + dy * dy);
                double radius = MIN_RADIUS + random01(siteCellX, siteCellY, 2) * RADIUS_VARIATION;
                best = Math.max(best, radius - distance);
            }
        }

        best = Math.max(best, getIslandValue(warpedX, warpedY));
        return (float) Math.max(best, getOriginIslandValue(warpedX, warpedY));
    }

    private double getOriginIslandValue(double x, double y) {
        double distance = Math.sqrt(x * x + y * y);
        return ISLAND_MIN_RADIUS + ISLAND_RADIUS_VARIATION - distance;
    }

    private double getIslandValue(double x, double y) {
        int cellX = floor(x / ISLAND_CELL_SIZE);
        int cellY = floor(y / ISLAND_CELL_SIZE);
        double best = -Double.MAX_VALUE;

        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetY = -1; offsetY <= 1; offsetY++) {
                int siteCellX = cellX + offsetX;
                int siteCellY = cellY + offsetY;
                if (random01(siteCellX, siteCellY, 3) >= ISLAND_CHANCE) {
                    continue;
                }

                double siteX = initialIslandSite(siteCellX, siteCellY, true);
                double siteY = initialIslandSite(siteCellX, siteCellY, false);
                double dx = x - siteX;
                double dy = y - siteY;
                double distance = Math.sqrt(dx * dx + dy * dy);
                double radius = ISLAND_MIN_RADIUS + random01(siteCellX, siteCellY, 6) * ISLAND_RADIUS_VARIATION;
                best = Math.max(best, radius - distance);
            }
        }

        return best;
    }

    private double initialIslandSite(int cellX, int cellY, boolean xAxis) {
        int axis = xAxis ? 4 : 5;
        double centre = ((xAxis ? cellX : cellY) + 0.5D) * ISLAND_CELL_SIZE;
        return centre + (random01(cellX, cellY, axis) * 2D - 1D) * ISLAND_CELL_SIZE * ISLAND_JITTER;
    }

    private double relaxedSite(int cellX, int cellY, boolean xAxis) {
        double original = initialSite(cellX, cellY, xAxis);
        double neighbourAverage = 0D;

        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetY = -1; offsetY <= 1; offsetY++) {
                neighbourAverage += initialSite(cellX + offsetX, cellY + offsetY, xAxis);
            }
        }

        neighbourAverage /= 9D;
        return original + (neighbourAverage - original) * RELAXATION;
    }

    private double initialSite(int cellX, int cellY, boolean xAxis) {
        int axis = xAxis ? 0 : 1;
        double centre = ((xAxis ? cellX : cellY) + 0.5D) * CELL_SIZE;
        return centre + (random01(cellX, cellY, axis) * 2D - 1D) * CELL_SIZE * JITTER;
    }

    private double random01(int cellX, int cellY, int salt) {
        long value = seed;
        value ^= (long) cellX * 341873128712L;
        value ^= (long) cellY * 132897987541L;
        value ^= (long) salt * 42317861L;
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return (value >>> 11) * 0x1.0p-53;
    }

    private int floor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }
}
