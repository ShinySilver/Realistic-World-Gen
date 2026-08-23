package rwg.util;

import java.util.ArrayList;

import gnu.trove.map.hash.TLongObjectHashMap;
import rwg.config.ConfigRWG;

/**
 * An infinite, deterministic field of continent sites. Each grid cell owns one jittered site. A single relaxation pass
 * pulls that site toward the average of its eight neighbours, avoiding both a rigid grid and tightly clustered sites.
 */
public class ContinentalNoise {

    private static final double BASE_CELL_SIZE = 3600D;
    private static final double BASE_JITTER = 0.38D;
    private static final double RELAXATION = 0.5D;
    private static final double MIN_RADIUS = 1120D;
    private static final double RADIUS_VARIATION = 360D;
    private static final double BASE_ISLAND_CELL_SIZE = 1800D;
    private static final double BASE_ISLAND_JITTER = 0.10D;
    private static final double VOLCANO_THRESHOLD = 0.20D;
    private static final double VOLCANO_CHANCE = 0.10D;
    private static final double NORMAL_ISLAND_CHANCE = 0.55D;
    private static final double ISLAND_MIN_RADIUS = 250D;
    private static final double ISLAND_RADIUS_VARIATION = 130D;
    private static final double ORIGIN_ISLAND_RADIUS = ISLAND_MIN_RADIUS + ISLAND_RADIUS_VARIATION;
    public static final double VOLCANO_ISLAND_RADIUS = 110D;
    public static final double VOLCANO_RADIUS = 95D;
    private static final double VOLCANO_CONTINENT_CLEARANCE = 200D;
    private static final double MEDIUM_WARP_SCALE = 675D;
    private static final double MEDIUM_WARP_STRENGTH = 700D;
    private static final int SITE_CACHE_LIMIT = 16384;

    private final long seed;
    private final double cellSize;
    private final double jitter;
    private final double continentGridOffsetX;
    private final double continentGridOffsetZ;
    private final double continentMinRadius;
    private final double continentRadiusVariation;
    private final double islandCellSize;
    private final double islandJitter;
    private final double islandMinRadius;
    private final double islandRadiusVariation;
    private final double originIslandRadius;
    private final double volcanoChance;
    private final int relaxationSteps;
    private final int continentSearchRadius;
    private final int islandSearchRadius;
    private final NoiseGenerator warpNoiseX;
    private final NoiseGenerator warpNoiseY;
    private final float warpOriginX;
    private final float warpOriginY;
    private final TLongObjectHashMap<Site> continentSites = new TLongObjectHashMap<Site>();
    private final TLongObjectHashMap<Site> islandSites = new TLongObjectHashMap<Site>();
    private final ArrayList<TLongObjectHashMap<Position>> relaxedContinentSites = new ArrayList<>();

    public ContinentalNoise(long seed) {
        this.seed = seed;
        cellSize = BASE_CELL_SIZE * ConfigRWG.continentGridScale;
        jitter = BASE_JITTER * ConfigRWG.continentOffsetMultiplier;
        continentGridOffsetX = ConfigRWG.continentGridOffsetX;
        continentGridOffsetZ = ConfigRWG.continentGridOffsetZ;
        continentMinRadius = MIN_RADIUS * ConfigRWG.continentScale;
        continentRadiusVariation = RADIUS_VARIATION * ConfigRWG.continentScale;
        islandCellSize = BASE_ISLAND_CELL_SIZE * ConfigRWG.islandGridScale;
        islandJitter = BASE_ISLAND_JITTER * ConfigRWG.islandOffsetMultiplier;
        islandMinRadius = ISLAND_MIN_RADIUS * ConfigRWG.islandScale;
        islandRadiusVariation = ISLAND_RADIUS_VARIATION * ConfigRWG.islandScale;
        originIslandRadius = ORIGIN_ISLAND_RADIUS * ConfigRWG.islandScale;
        volcanoChance = VOLCANO_CHANCE / ConfigRWG.volcanoRarity;
        relaxationSteps = ConfigRWG.continentRelaxationSteps;
        continentSearchRadius = Math
                .max(1, (int) Math.ceil(jitter + (continentMinRadius + continentRadiusVariation) / cellSize));
        islandSearchRadius = Math
                .max(1, (int) Math.ceil((islandMinRadius + islandRadiusVariation) / islandCellSize + islandJitter));
        for (int step = 0; step <= relaxationSteps; step++) {
            relaxedContinentSites.add(new TLongObjectHashMap<Position>());
        }
        warpNoiseX = NoiseSelector.createNoiseGenerator(seed ^ 0xBB67AE8584CAA73BL);
        warpNoiseY = NoiseSelector.createNoiseGenerator(seed ^ 0x3C6EF372FE94F82BL);
        warpOriginX = warpNoiseX.noise2(0f, 0f);
        warpOriginY = warpNoiseY.noise2(0f, 0f);
    }

    /**
     * Returns distance from the nearest continental shore in blocks. Positive values are land, negative values are sea.
     */
    public float getValue(int x, int y) {
        double warpedX = warpX(x, y);
        double warpedY = warpY(x, y);
        double best = getContinentalValueAt(warpedX, warpedY);

        best = Math.max(best, getIslandValue(warpedX, warpedY));
        best = Math.max(best, getVolcanoIslandValue(x, y));
        return (float) Math.max(best, getOriginIslandValue(warpedX, warpedY));
    }

    private double getContinentalValue(double x, double y) {
        return getContinentalValueAt(warpX(x, y), warpY(x, y));
    }

    private double getContinentalValueAt(double warpedX, double warpedY) {
        int cellX = floor(warpedX / cellSize + continentGridOffsetX);
        int cellY = floor(warpedY / cellSize + continentGridOffsetZ);
        double best = -Double.MAX_VALUE;

        for (int offsetX = -continentSearchRadius; offsetX <= continentSearchRadius; offsetX++) {
            for (int offsetY = -continentSearchRadius; offsetY <= continentSearchRadius; offsetY++) {
                Site site = getContinentSite(cellX + offsetX, cellY + offsetY);
                double dx = warpedX - site.x;
                double dy = warpedY - site.y;
                best = Math.max(best, site.radius - Math.sqrt(dx * dx + dy * dy));
            }
        }
        return best;
    }

    private double warpX(double x, double y) {
        return x + (warpNoiseX.noise2((float) (x / MEDIUM_WARP_SCALE), (float) (y / MEDIUM_WARP_SCALE)) - warpOriginX)
                * MEDIUM_WARP_STRENGTH;
    }

    private double warpY(double x, double y) {
        return y + (warpNoiseY.noise2((float) (x / MEDIUM_WARP_SCALE), (float) (y / MEDIUM_WARP_SCALE)) - warpOriginY)
                * MEDIUM_WARP_STRENGTH;
    }

    private double getOriginIslandValue(double x, double y) {
        double distance = Math.sqrt(x * x + y * y);
        return originIslandRadius - distance;
    }

    private double getIslandValue(double x, double y) {
        int cellX = islandCell(x);
        int cellY = islandCell(y);
        double best = -Double.MAX_VALUE;

        for (int offsetX = -islandSearchRadius; offsetX <= islandSearchRadius; offsetX++) {
            for (int offsetY = -islandSearchRadius; offsetY <= islandSearchRadius; offsetY++) {
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

        Position position = getRelaxedContinentSite(cellX, cellY, relaxationSteps);
        double radius = continentMinRadius + random01(cellX, cellY, 2) * continentRadiusVariation;
        site = new Site(position.x, position.y, radius, true, false);
        cacheSite(continentSites, key, site);
        return site;
    }

    private Position getRelaxedContinentSite(int cellX, int cellY, int step) {
        long key = cellKey(cellX, cellY);
        TLongObjectHashMap<Position> cache = relaxedContinentSites.get(step);
        Position position = cache.get(key);
        if (position != null) {
            return position;
        }

        if (step == 0) {
            position = new Position(initialSite(cellX, cellY, true), initialSite(cellX, cellY, false));
        } else {
            Position previous = getRelaxedContinentSite(cellX, cellY, step - 1);
            double neighbourAverageX = 0D;
            double neighbourAverageY = 0D;
            for (int offsetX = -1; offsetX <= 1; offsetX++) {
                for (int offsetY = -1; offsetY <= 1; offsetY++) {
                    Position neighbour = getRelaxedContinentSite(cellX + offsetX, cellY + offsetY, step - 1);
                    neighbourAverageX += neighbour.x;
                    neighbourAverageY += neighbour.y;
                }
            }
            neighbourAverageX /= 9D;
            neighbourAverageY /= 9D;
            position = new Position(
                    previous.x + (neighbourAverageX - previous.x) * RELAXATION,
                    previous.y + (neighbourAverageY - previous.y) * RELAXATION);
        }
        cacheSite(cache, key, position);
        return position;
    }

    private Site getIslandSite(int cellX, int cellY) {
        long key = cellKey(cellX, cellY);
        Site site = islandSites.get(key);
        if (site != null) {
            return site;
        }

        double typeRoll = random01(cellX, cellY, 3);
        boolean volcano = typeRoll >= VOLCANO_THRESHOLD && typeRoll < VOLCANO_THRESHOLD + volcanoChance;
        boolean enabled = volcano || typeRoll >= VOLCANO_THRESHOLD + VOLCANO_CHANCE
                && typeRoll < VOLCANO_THRESHOLD + VOLCANO_CHANCE + NORMAL_ISLAND_CHANCE;
        // Keep the staggered odd/odd positions reserved for the continent grid.
        boolean continentCentre = (cellX & 1) != 0 && (cellY & 1) != 0;
        if (continentCentre || cellX == 0 && cellY == 0) {
            volcano = false;
            enabled = false;
        }
        double centreX = cellX * islandCellSize;
        double centreY = cellY * islandCellSize;
        double x = centreX + (random01(cellX, cellY, 4) * 2D - 1D) * islandCellSize * islandJitter;
        double y = centreY + (random01(cellX, cellY, 5) * 2D - 1D) * islandCellSize * islandJitter;
        if (volcano && getContinentalValue(x, y) > -(VOLCANO_ISLAND_RADIUS + VOLCANO_CONTINENT_CLEARANCE)) {
            volcano = false;
            enabled = false;
        }
        double radius = volcano ? VOLCANO_ISLAND_RADIUS
                : islandMinRadius + random01(cellX, cellY, 6) * islandRadiusVariation;
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
        double gridOffset = xAxis ? continentGridOffsetX : continentGridOffsetZ;
        double centre = ((xAxis ? cellX : cellY) + 0.5D - gridOffset) * cellSize;
        double offset = (random01(cellX, cellY, axis) * 2D - 1D) * cellSize * jitter;
        return centre - offset;
    }

    private int islandCell(double coordinate) {
        return floor((coordinate + islandCellSize * 0.5D) / islandCellSize);
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

    private <T> void cacheSite(TLongObjectHashMap<T> cache, long key, T site) {
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

    private static class Position {

        private final double x;
        private final double y;

        private Position(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}
