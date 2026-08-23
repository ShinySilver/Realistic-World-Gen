package rwg.util;

import gnu.trove.map.hash.TLongObjectHashMap;

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
    private static final double ISLAND_JITTER = 0.10D;
    private static final double VOLCANO_THRESHOLD = 0.20D;
    private static final double VOLCANO_CHANCE = 0.10D;
    private static final double NORMAL_ISLAND_CHANCE = 0.55D;
    private static final double ISLAND_MIN_RADIUS = 250D;
    private static final double ISLAND_RADIUS_VARIATION = 130D;
    private static final double ORIGIN_ISLAND_RADIUS = ISLAND_MIN_RADIUS + ISLAND_RADIUS_VARIATION;
    public static final double VOLCANO_ISLAND_RADIUS = 220D;
    public static final double VOLCANO_RADIUS = 190D;
    private static final double MEDIUM_WARP_SCALE = 675D;
    private static final double MEDIUM_WARP_STRENGTH = 700D;
    private static final int SITE_CACHE_LIMIT = 16384;

    private final long seed;
    private final NoiseGenerator warpNoiseX;
    private final NoiseGenerator warpNoiseY;
    private final float warpOriginX;
    private final float warpOriginY;
    private final TLongObjectHashMap<Site> continentSites = new TLongObjectHashMap<Site>();
    private final TLongObjectHashMap<Site> islandSites = new TLongObjectHashMap<Site>();

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
                + (warpNoiseX.noise2((float) (x / MEDIUM_WARP_SCALE), (float) (y / MEDIUM_WARP_SCALE)) - warpOriginX)
                        * MEDIUM_WARP_STRENGTH;
        double warpedY = y
                + (warpNoiseY.noise2((float) (x / MEDIUM_WARP_SCALE), (float) (y / MEDIUM_WARP_SCALE)) - warpOriginY)
                        * MEDIUM_WARP_STRENGTH;

        int cellX = floor(warpedX / CELL_SIZE);
        int cellY = floor(warpedY / CELL_SIZE);
        double best = -Double.MAX_VALUE;

        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetY = -1; offsetY <= 1; offsetY++) {
                int siteCellX = cellX + offsetX;
                int siteCellY = cellY + offsetY;
                Site site = getContinentSite(siteCellX, siteCellY);
                double dx = warpedX - site.x;
                double dy = warpedY - site.y;
                double distance = Math.sqrt(dx * dx + dy * dy);
                best = Math.max(best, site.radius - distance);
            }
        }

        best = Math.max(best, getIslandValue(warpedX, warpedY));
        best = Math.max(best, getVolcanoIslandValue(x, y));
        return (float) Math.max(best, getOriginIslandValue(warpedX, warpedY));
    }

    private double getOriginIslandValue(double x, double y) {
        double distance = Math.sqrt(x * x + y * y);
        return ORIGIN_ISLAND_RADIUS - distance;
    }

    private double getIslandValue(double x, double y) {
        int cellX = islandCell(x);
        int cellY = islandCell(y);
        double best = -Double.MAX_VALUE;

        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetY = -1; offsetY <= 1; offsetY++) {
                int siteCellX = cellX + offsetX;
                int siteCellY = cellY + offsetY;
                Site site = getIslandSite(siteCellX, siteCellY);
                if (!site.enabled || site.volcano) {
                    continue;
                }

                double dx = x - site.x;
                double dy = y - site.y;
                double distance = Math.sqrt(dx * dx + dy * dy);
                best = Math.max(best, site.radius - distance);
            }
        }

        return best;
    }

    private double getVolcanoIslandValue(double x, double y) {
        int cellX = islandCell(x);
        int cellY = islandCell(y);
        double best = -Double.MAX_VALUE;

        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetY = -1; offsetY <= 1; offsetY++) {
                Site site = getIslandSite(cellX + offsetX, cellY + offsetY);
                if (!site.volcano) {
                    continue;
                }
                double dx = x - site.x;
                double dy = y - site.y;
                best = Math.max(best, site.radius - Math.sqrt(dx * dx + dy * dy));
            }
        }

        return best;
    }

    private Site getContinentSite(int cellX, int cellY) {
        long key = cellKey(cellX, cellY);
        Site site = continentSites.get(key);
        if (site != null) {
            return site;
        }

        double originalX = initialSite(cellX, cellY, true);
        double originalY = initialSite(cellX, cellY, false);
        double neighbourAverageX = 0D;
        double neighbourAverageY = 0D;

        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetY = -1; offsetY <= 1; offsetY++) {
                neighbourAverageX += initialSite(cellX + offsetX, cellY + offsetY, true);
                neighbourAverageY += initialSite(cellX + offsetX, cellY + offsetY, false);
            }
        }

        neighbourAverageX /= 9D;
        neighbourAverageY /= 9D;
        double x = originalX + (neighbourAverageX - originalX) * RELAXATION;
        double y = originalY + (neighbourAverageY - originalY) * RELAXATION;
        double radius = MIN_RADIUS + random01(cellX, cellY, 2) * RADIUS_VARIATION;
        site = new Site(x, y, radius, true, false);
        cacheSite(continentSites, key, site);
        return site;
    }

    private Site getIslandSite(int cellX, int cellY) {
        long key = cellKey(cellX, cellY);
        Site site = islandSites.get(key);
        if (site != null) {
            return site;
        }

        double typeRoll = random01(cellX, cellY, 3);
        boolean volcano = typeRoll >= VOLCANO_THRESHOLD && typeRoll < VOLCANO_THRESHOLD + VOLCANO_CHANCE;
        boolean enabled = volcano || typeRoll >= VOLCANO_THRESHOLD + VOLCANO_CHANCE
                && typeRoll < VOLCANO_THRESHOLD + VOLCANO_CHANCE + NORMAL_ISLAND_CHANCE;
        if (cellX == 0 && cellY == 0) {
            volcano = false;
            enabled = false;
        }
        double centreX = cellX * ISLAND_CELL_SIZE;
        double centreY = cellY * ISLAND_CELL_SIZE;
        double x = centreX + (random01(cellX, cellY, 4) * 2D - 1D) * ISLAND_CELL_SIZE * ISLAND_JITTER;
        double y = centreY + (random01(cellX, cellY, 5) * 2D - 1D) * ISLAND_CELL_SIZE * ISLAND_JITTER;
        double radius = volcano ? VOLCANO_ISLAND_RADIUS
                : ISLAND_MIN_RADIUS + random01(cellX, cellY, 6) * ISLAND_RADIUS_VARIATION;
        site = new Site(x, y, radius, enabled, volcano);
        cacheSite(islandSites, key, site);
        return site;
    }

    /** Returns packed coordinates relative to an unwarped volcano site, or {@link Long#MIN_VALUE}. */
    public long getVolcanoCoordinates(int x, int y) {
        return getVolcanoCoordinates(x, y, VOLCANO_RADIUS);
    }

    public long getVolcanoVicinityCoordinates(int x, int y) {
        return getVolcanoCoordinates(x, y, VOLCANO_ISLAND_RADIUS);
    }

    private long getVolcanoCoordinates(int x, int y, double radius) {
        int cellX = islandCell(x);
        int cellY = islandCell(y);
        Site bestSite = null;
        double bestValue = -Double.MAX_VALUE;

        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetY = -1; offsetY <= 1; offsetY++) {
                Site site = getIslandSite(cellX + offsetX, cellY + offsetY);
                if (!site.volcano) {
                    continue;
                }
                double dx = x - site.x;
                double dy = y - site.y;
                double value = radius - Math.sqrt(dx * dx + dy * dy);
                if (value >= 0D && value > bestValue) {
                    bestValue = value;
                    bestSite = site;
                }
            }
        }

        if (bestSite == null) {
            return Long.MIN_VALUE;
        }
        int localX = Float.floatToRawIntBits((float) (x - bestSite.x));
        int localY = Float.floatToRawIntBits((float) (y - bestSite.y));
        return (long) localX << 32 | (long) localY & 0xffffffffL;
    }

    public static float unpackVolcanoX(long coordinates) {
        return Float.intBitsToFloat((int) (coordinates >>> 32));
    }

    public static float unpackVolcanoY(long coordinates) {
        return Float.intBitsToFloat((int) coordinates);
    }

    private double initialSite(int cellX, int cellY, boolean xAxis) {
        int axis = xAxis ? 0 : 1;
        double centre = ((xAxis ? cellX : cellY) + 0.5D) * CELL_SIZE;
        return centre + (random01(cellX, cellY, axis) * 2D - 1D) * CELL_SIZE * JITTER;
    }

    private int islandCell(double coordinate) {
        return floor((coordinate + ISLAND_CELL_SIZE * 0.5D) / ISLAND_CELL_SIZE);
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

    private long cellKey(int cellX, int cellY) {
        return (long) cellX & 0xffffffffL | (long) cellY << 32;
    }

    private void cacheSite(TLongObjectHashMap<Site> cache, long key, Site site) {
        if (cache.size() >= SITE_CACHE_LIMIT) {
            cache.clear();
        }
        cache.put(key, site);
    }

    private int floor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    private static class Site {

        private final double x;
        private final double y;
        private final double radius;
        private final boolean enabled;
        private final boolean volcano;

        private Site(double x, double y, double radius, boolean enabled, boolean volcano) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.enabled = enabled;
            this.volcano = volcano;
        }
    }
}
