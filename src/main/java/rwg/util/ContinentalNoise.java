package rwg.util;

import rwg.config.ConfigRWG;

/**
 * A continent distance field built from priority-sampled Voronoi cells. Land occupies the area around a cell's feature
 * point and ocean occupies its edge. The lookup coordinates are strongly domain-warped to keep the cell boundaries from
 * looking geometric.
 */
public class ContinentalNoise {

    /** Retained for callers which describe volcano exclusion zones. Islands are currently disabled. */
    public static final double VOLCANO_ISLAND_RADIUS = 110D;
    public static final double VOLCANO_RADIUS = 95D;

    private final long seed;
    private final PoissonPointNoise points;
    private final IslandPointNoise islands;
    private final double minimumContinentWidth;
    private final double continentWidthRange;
    private final double voronoiRadius;
    private final NoiseGenerator warpX;
    private final NoiseGenerator warpY;
    private final double warpScale;
    private final double warpStrength;
    private final double offsetX;
    private final double offsetY;
    private final ThreadLocal<double[]> samples = new ThreadLocal<double[]>() {

        @Override
        protected double[] initialValue() {
            return new double[5];
        }
    };

    public ContinentalNoise(long seed) {
        this.seed = seed;
        minimumContinentWidth = Math.min(ConfigRWG.minimumContinentWidth, ConfigRWG.maximumContinentWidth);
        double maximumContinentWidth = Math.max(ConfigRWG.minimumContinentWidth, ConfigRWG.maximumContinentWidth);
        continentWidthRange = maximumContinentWidth - minimumContinentWidth;
        double averageContinentWidth = (minimumContinentWidth + maximumContinentWidth) * 0.5D;
        voronoiRadius = averageContinentWidth + ConfigRWG.averageOceanWidth;
        warpScale = averageContinentWidth * 2D;
        warpStrength = averageContinentWidth;
        warpX = new PerlinNoise(seed ^ 0x243F6A8885A308D3L);
        warpY = new PerlinNoise(seed ^ 0x13198A2E03707344L);
        // Prevent the largest two continents from overlapping; later rounds fill the remaining holes.
        points = new PoissonPointNoise(seed, maximumContinentWidth * 2D + ConfigRWG.minimumOceanWidth, 6);
        islands = new IslandPointNoise(
                seed ^ 0xA4093822299F31D0L,
                points,
                maximumContinentWidth,
                ConfigRWG.minimumIslandWidth,
                ConfigRWG.maximumIslandWidth,
                ConfigRWG.minimumOceanWidth,
                ConfigRWG.islandPlacementChance);

        double[] origin = new double[5];
        points.sample(0D, 0D, origin);
        offsetX = origin[3];
        offsetY = origin[4];
    }

    /** Returns distance from the shore in blocks: positive on land and negative at sea. */
    public float getValue(int x, int y) {
        double[] sample = samples.get();
        double noiseX = x / warpScale;
        double noiseY = y / warpScale;
        double warpedX = x + warpX.noise2((float) noiseX, (float) noiseY) * warpStrength
                + warpX.noise2((float) (noiseX * 2D), (float) (noiseY * 2D)) * warpStrength;
        double warpedY = y + warpY.noise2((float) noiseX, (float) noiseY) * warpStrength
                + warpY.noise2((float) (noiseX * 2D), (float) (noiseY * 2D)) * warpStrength;
        points.sample(warpedX + offsetX, warpedY + offsetY, sample);
        int cellX = (int) sample[1];
        int cellY = (int) sample[2];

        double width = minimumContinentWidth + random01(cellX, cellY, 0) * continentWidthRange;
        width = Math.min(voronoiRadius, width);
        double distanceFromCentre = sample[0];
        double continent = width - distanceFromCentre;
        return (float) Math.max(continent, islands.getValue(warpedX + offsetX, warpedY + offsetY));
    }

    /** Islands, including volcano islands, are disabled. */
    public long getVolcanoCoordinates(int x, int y) {
        return Long.MIN_VALUE;
    }

    /** Islands, including volcano islands, are disabled. */
    public long getVolcanoVicinityCoordinates(int x, int y) {
        return Long.MIN_VALUE;
    }

    public static float unpackVolcanoX(long coordinates) {
        return Float.intBitsToFloat((int) (coordinates >>> 32));
    }

    public static float unpackVolcanoY(long coordinates) {
        return Float.intBitsToFloat((int) coordinates);
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
}
