package rwg.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeCache;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import gnu.trove.map.hash.TLongObjectHashMap;
import rwg.ConfigRWG;
import rwg.RWG;
import rwg.registry.BiomeRegistration;
import rwg.registry.BiomeRegistry;
import rwg.registry.Climate;
import rwg.registry.TerrainSubcategory;
import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;
import rwg.world.debug.GridWorldgenSelector;
import rwg.world.layout.BasicWorldgenSelector;
import rwg.world.layout.WorldgenSamplingContext;
import rwg.world.layout.WorldgenSelector;
import rwg.world.sample.ClimateSample;
import rwg.world.sample.ColumnSample;
import rwg.world.terrain.MorphologyRules;
import rwg.world.terrain.MountainDistanceSampler;
import rwg.world.terrain.RegionalTerrainSampler;
import rwg.world.terrain.RiverSampler;

/** Minecraft biome facade backed exclusively by the component registry. */
public class ChunkManager extends WorldChunkManager {

    private static final double CLIMATE_BORDER_DISTANCE_DIFFERENCE = 288D;

    private static final float COAST_CLIP_SAMPLE_SPACING = 96f;
    private static final int COAST_CLIP_REFINEMENT_STEPS = 8;
    private static final float CLIMATE_WARP_SCALE_MULTIPLIER = .4f;
    private static final float CLIMATE_WARP_STRENGTH_MULTIPLIER = .8f;
    private static final float BIOME_WARP_SCALE_MULTIPLIER = .4f;
    private static final float BIOME_WARP_STRENGTH_MULTIPLIER = .175f;
    private static final long MOUNTAIN_BORDER_SALT = 0xD1B54A32D192ED03L;
    private static final byte NATURAL_MOUNTAIN_BORDER = -1;
    private static final int[] DIRECT_NEIGHBOR_X = { 1, 0, -1, 0 };
    private static final int[] DIRECT_NEIGHBOR_Z = { 0, 1, 0, -1 };
    private final BiomeCache biomeCache = new BiomeCache(this);
    private final List biomesToSpawnIn = new ArrayList();
    private final TLongObjectHashMap<ColumnSample> selectionCache = new TLongObjectHashMap<ColumnSample>();
    private final WorldgenSamplingContext context;
    private final boolean continental;
    private final float climateWidth;
    private final NoiseGenerator perlin;
    private final CellNoise cell;
    private final WorldgenSelector selector;
    private final boolean wetClimateEnabled;
    private final Cache<Long, ClimateRegion> climateRegions = CacheBuilder.newBuilder().maximumSize(1024)
            .concurrencyLevel(4).build();
    private final Cache<Long, Byte> mountainBorderChoices = CacheBuilder.newBuilder().maximumSize(4096)
            .concurrencyLevel(4).build();
    private final ThreadLocal<ClimateRegionLookup> lastClimateRegion = new ThreadLocal<ClimateRegionLookup>() {

        @Override
        protected ClimateRegionLookup initialValue() {
            return new ClimateRegionLookup();
        }
    };

    protected ChunkManager() {
        this(0L, false);
    }

    public ChunkManager(World world) {
        this(world, false);
    }

    public ChunkManager(World world, boolean continental) {
        this(world, continental, false);
    }

    public ChunkManager(World world, boolean continental, boolean grid) {
        this(world.getSeed(), continental, 1400f, 500f, RWG.biomeRegistry(), grid);
    }

    public ChunkManager(long seed, boolean continental) {
        this(seed, continental, 1400f, 500f, RWG.biomeRegistry());
    }

    public ChunkManager(long seed, boolean continental, float climateWidth, float biomeWidth) {
        this(seed, continental, climateWidth, biomeWidth, RWG.biomeRegistry());
    }

    public ChunkManager(long seed, boolean continental, BiomeRegistry registry) {
        this(seed, continental, 1400f, 500f, registry);
    }

    public ChunkManager(long seed, boolean continental, float climateWidth, float biomeWidth, BiomeRegistry registry) {
        this(seed, continental, climateWidth, biomeWidth, registry, false);
    }

    private ChunkManager(long seed, boolean continental, float climateWidth, float biomeWidth, BiomeRegistry registry,
            boolean grid) {
        context = new WorldgenSamplingContext(
                seed,
                continental,
                climateWidth,
                biomeWidth,
                ConfigRWG.landmassOffsetX,
                ConfigRWG.landmassOffsetZ,
                registry);
        this.continental = continental;
        this.climateWidth = climateWidth;
        wetClimateEnabled = !context.biomes.categories(Climate.WET, TerrainSubcategory.CORE).isEmpty();
        perlin = context.terrainNoise;
        cell = context.terrainCellNoise;
        final RegionalTerrainSampler regionalTerrain = new RegionalTerrainSampler(context.terrainNoise);
        selector = grid ? new GridWorldgenSelector(seed, context.biomes)
                : new BasicWorldgenSelector(
                        seed,
                        this,
                        context.biomes,
                        new RiverSampler(seed, new MountainDistanceSampler() {

                            @Override
                            public float distanceAt(int x, int z) {
                                return MorphologyRules.effectiveMountainDistance(
                                        sampleClimateAt(x, z).incompatibleBoundaryDistance,
                                        getContinentValue(x, z));
                            }

                            @Override
                            public float uncarvedBaseHeightAt(int x, int z, float mountainDistance) {
                                return regionalTerrain
                                        .uncarvedLandHeight(x, z, getContinentValue(x, z), mountainDistance);
                            }
                        }),
                        regionalTerrain);
    }

    public WorldgenSelector worldgenSelector() {
        return selector;
    }

    public BiomeRegistration gridRegistrationAt(int x, int z) {
        return selector instanceof GridWorldgenSelector ? selector.registrationAt(x, z) : null;
    }

    public float getContinentValue(int x, int z) {
        return continental ? context.continentNoise.getValue(context.landmassX(x), context.landmassZ(z))
                : (perlin.noise2(x / 1200f, z / 1200f) >= 0f ? 100f : -100f);
    }

    public long getContinentSeedCoordinates(int x, int z) {
        return continental
                ? context.continentNoise.getContinentSeedCoordinates(context.landmassX(x), context.landmassZ(z))
                : 0L;
    }

    public ClimateSample sampleClimateAt(int x, int z) {
        int bx = biomeX(x);
        int bz = biomeZ(z);
        float scale = climateWidth * CLIMATE_WARP_SCALE_MULTIPLIER;
        float strength = climateWidth * CLIMATE_WARP_STRENGTH_MULTIPLIER;
        double wx = bx + context.climateWarpNoise.noise2(bx / scale, bz / scale) * strength;
        double wz = bz + context.climateWarpNoise.noise2((bx + 1731f) / scale, (bz - 2459f) / scale) * strength;
        double queryX = (wx + 4000D) / climateWidth;
        double queryZ = wz / climateWidth;
        ClimateRegion region = climateRegion(floorCell(queryX), floorCell(queryZ));
        int winner = region.winner(queryX, queryZ);
        float value = (float) region.values[winner] * .5f + .5f;
        Climate climate = region.climates[winner];
        int neighbor = region.nearestOther(queryX, queryZ, winner);
        double winnerDistance = region.distance(queryX, queryZ, winner);
        double neighborDistance = region.distance(queryX, queryZ, neighbor);
        Climate neighborClimate = region.climates[neighbor];
        TerrainSubcategory climateCategory = TerrainSubcategory.CORE;
        if (neighborClimate != climate
                && (neighborDistance - winnerDistance) * climateWidth < CLIMATE_BORDER_DISTANCE_DIFFERENCE) {
            climateCategory = neighborClimate.ordinal() < climate.ordinal() ? TerrainSubcategory.COLD_BORDER
                    : TerrainSubcategory.HOT_BORDER;
        }
        float boundaryDistance = Float.POSITIVE_INFINITY;
        float plateauBoundaryDistance = Float.POSITIVE_INFINITY;
        long boundaryNearKey = 0L;
        long boundaryFarKey = 0L;
        double maximumDistance = MorphologyRules.MOUNTAIN_INFLUENCE_RADIUS / climateWidth;
        for (ClimateEdge edge : region.edges) {
            double distance = edge.distance(queryX, queryZ);
            if (distance > maximumDistance) continue;
            float worldDistance = (float) (distance * climateWidth);
            if (plateauEligible(region.keys[edge.first]) != plateauEligible(region.keys[edge.second]))
                plateauBoundaryDistance = Math.min(plateauBoundaryDistance, worldDistance);
            if (worldDistance < boundaryDistance) {
                boundaryDistance = worldDistance;
                double firstX = queryX - region.x[edge.first];
                double firstZ = queryZ - region.z[edge.first];
                double secondX = queryX - region.x[edge.second];
                double secondZ = queryZ - region.z[edge.second];
                if (firstX * firstX + firstZ * firstZ <= secondX * secondX + secondZ * secondZ) {
                    boundaryNearKey = region.keys[edge.first];
                    boundaryFarKey = region.keys[edge.second];
                } else {
                    boundaryNearKey = region.keys[edge.second];
                    boundaryFarKey = region.keys[edge.first];
                }
            }
        }
        boolean plateauBoundary = boundaryDistance != Float.POSITIVE_INFINITY
                && plateauEligible(boundaryNearKey) != plateauEligible(boundaryFarKey);
        boolean plateauSide = plateauBoundary && plateauEligible(boundaryNearKey);
        float plateauBoundaryInfluence = MorphologyRules.plateauBoundaryInfluence(plateauBoundaryDistance);
        return new ClimateSample(
                climate,
                climateCategory,
                value,
                region.keys[winner],
                boundaryDistance,
                plateauBoundary,
                plateauBoundaryInfluence,
                plateauSide,
                Collections.emptyList());
    }

    /** Restores the pre-rewrite domain-warped Voronoi field used to choose within a biome pool. */
    public float sampleBiomeSelectorAt(int x, int z) {
        int bx = biomeX(x);
        int bz = biomeZ(z);
        float scale = context.biomeWidth * BIOME_WARP_SCALE_MULTIPLIER;
        float strength = context.biomeWidth * BIOME_WARP_STRENGTH_MULTIPLIER;
        double warpedX = bx + context.climateWarpNoise.noise2((bx - 8191f) / scale, (bz + 3137f) / scale) * strength;
        double warpedZ = bz + context.climateWarpNoise.noise2((bx + 5171f) / scale, (bz - 6971f) / scale) * strength;
        float selector = context.biomeCellNoise.noise(warpedX / context.biomeWidth, warpedZ / context.biomeWidth, 1D)
                * .5f + .5f;
        return Math.max(0f, Math.min(.9999999f, selector));
    }

    private ClimateRegion climateRegion(int cellX, int cellZ) {
        ClimateRegionLookup lookup = lastClimateRegion.get();
        if (lookup.region != null && lookup.x == cellX && lookup.z == cellZ) return lookup.region;
        long key = (long) cellX << 32 | cellZ & 0xffffffffL;
        ClimateRegion cached = climateRegions.getIfPresent(key);
        if (cached != null) {
            lookup.x = cellX;
            lookup.z = cellZ;
            lookup.region = cached;
            return cached;
        }
        try {
            ClimateRegion region = climateRegions.get(key, new Callable<ClimateRegion>() {

                @Override
                public ClimateRegion call() {
                    return createClimateRegion(cellX, cellZ);
                }
            });
            lookup.x = cellX;
            lookup.z = cellZ;
            lookup.region = region;
            return region;
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Could not create climate topology", exception.getCause());
        }
    }

    private ClimateRegion createClimateRegion(int cellX, int cellZ) {
        CellNoise.VoronoiSample2D cells = new CellNoise.VoronoiSample2D();
        context.biomeCellNoise.sampleVoronoi2D(cellX + .5D, cellZ + .5D, 1D, cells);
        Climate[] climates = new Climate[cells.count];
        for (int index = 0; index < cells.count; index++)
            climates[index] = climate((float) cells.value[index] * .5f + .5f);
        ArrayList<ClimateEdge> edges = new ArrayList<ClimateEdge>();
        for (int first = 0; first < cells.count; first++) {
            for (int second = first + 1; second < cells.count; second++) {
                if (!mountainBorder(cells.key[first], climates[first], cells.key[second], climates[second])) continue;
                ClimateEdge edge = createClimateEdge(cells, first, second);
                if (edge != null) clipClimateEdgeToLand(edge, cellX, cellZ, edges);
            }
        }
        return new ClimateRegion(
                cells.x.clone(),
                cells.z.clone(),
                cells.value.clone(),
                cells.key.clone(),
                climates,
                edges.toArray(new ClimateEdge[edges.size()]));
    }

    /** Clips one site bisector to its finite Voronoi edge, retaining junction endpoints. */
    private static ClimateEdge createClimateEdge(CellNoise.VoronoiSample2D cells, int first, int second) {
        double firstX = cells.x[first];
        double firstZ = cells.z[first];
        double secondX = cells.x[second];
        double secondZ = cells.z[second];
        double deltaX = secondX - firstX;
        double deltaZ = secondZ - firstZ;
        double length = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double middleX = (firstX + secondX) * .5D;
        double middleZ = (firstZ + secondZ) * .5D;
        double tangentX = -deltaZ / length;
        double tangentZ = deltaX / length;
        double minimum = Double.NEGATIVE_INFINITY;
        double maximum = Double.POSITIVE_INFINITY;

        for (int other = 0; other < cells.count; other++) {
            if (other == first || other == second) continue;
            double otherDeltaX = cells.x[other] - firstX;
            double otherDeltaZ = cells.z[other] - firstZ;
            double coefficient = tangentX * otherDeltaX + tangentZ * otherDeltaZ;
            double limit = (cells.x[other] * cells.x[other] + cells.z[other] * cells.z[other]
                    - firstX * firstX
                    - firstZ * firstZ) * .5D - middleX * otherDeltaX - middleZ * otherDeltaZ;
            if (Math.abs(coefficient) < 1.0E-12D) {
                if (limit < 0D) return null;
            } else if (coefficient > 0D) {
                maximum = Math.min(maximum, limit / coefficient);
            } else {
                minimum = Math.max(minimum, limit / coefficient);
            }
            if (minimum > maximum) return null;
        }
        return new ClimateEdge(first, second, middleX, middleZ, tangentX, tangentZ, minimum, maximum);
    }

    private void clipClimateEdgeToLand(ClimateEdge edge, int cellX, int cellZ, List<ClimateEdge> output) {
        if (!continental) {
            output.add(edge);
            return;
        }
        double margin = MorphologyRules.MOUNTAIN_INFLUENCE_RADIUS / climateWidth;
        double minimum = Double.POSITIVE_INFINITY;
        double maximum = Double.NEGATIVE_INFINITY;
        for (int cornerX = 0; cornerX <= 1; cornerX++) {
            for (int cornerZ = 0; cornerZ <= 1; cornerZ++) {
                double x = cellX + (cornerX == 0 ? -margin : 1D + margin);
                double z = cellZ + (cornerZ == 0 ? -margin : 1D + margin);
                double projection = (x - edge.middleX) * edge.tangentX + (z - edge.middleZ) * edge.tangentZ;
                minimum = Math.min(minimum, projection);
                maximum = Math.max(maximum, projection);
            }
        }
        minimum = Math.max(minimum, edge.minimum);
        maximum = Math.min(maximum, edge.maximum);
        if (!(minimum < maximum)) return;

        int intervals = Math.max(1, (int) Math.ceil((maximum - minimum) * climateWidth / COAST_CLIP_SAMPLE_SPACING));
        double previous = minimum;
        boolean previousLand = mountainLandAt(edge, previous);
        double landStart = previousLand ? minimum : Double.NaN;
        for (int index = 1; index <= intervals; index++) {
            double current = minimum + (maximum - minimum) * index / intervals;
            boolean currentLand = mountainLandAt(edge, current);
            if (currentLand != previousLand) {
                double crossing = coastalCrossing(edge, previous, current, previousLand);
                if (currentLand) landStart = crossing;
                else {
                    output.add(edge.slice(landStart, crossing));
                    landStart = Double.NaN;
                }
            }
            previous = current;
            previousLand = currentLand;
        }
        if (previousLand) output.add(edge.slice(landStart, maximum));
    }

    private double coastalCrossing(ClimateEdge edge, double first, double second, boolean firstLand) {
        for (int iteration = 0; iteration < COAST_CLIP_REFINEMENT_STEPS; iteration++) {
            double middle = (first + second) * .5D;
            if (mountainLandAt(edge, middle) == firstLand) first = middle;
            else second = middle;
        }
        return (first + second) * .5D;
    }

    private boolean mountainLandAt(ClimateEdge edge, double position) {
        double climateX = edge.middleX + edge.tangentX * position;
        double climateZ = edge.middleZ + edge.tangentZ * position;
        int worldX = (int) Math.round(climateX * climateWidth - 4000D - ConfigRWG.biomeOffsetX);
        int worldZ = (int) Math.round(climateZ * climateWidth - ConfigRWG.biomeOffsetZ);
        return getContinentValue(worldX, worldZ) >= MorphologyRules.COASTAL_MOUNTAIN_EDGE_INSET;
    }

    private static int floorCell(double value) {
        return value > 0D ? (int) value : (int) value - 1;
    }

    private Climate climate(float value) {
        int index = wetClimateEnabled ? value < .16875f ? 0 : value < .545f ? 1 : value < .78f ? 2 : 3
                : value < .2475f ? 0 : value < .62375f ? 1 : 2;
        return Climate.values()[index];
    }

    private static boolean incompatible(Climate first, Climate second) {
        return first == Climate.WET && (second == Climate.COLD || second == Climate.SNOW)
                || second == Climate.WET && (first == Climate.COLD || first == Climate.SNOW)
                || first == Climate.SNOW && second == Climate.HOT
                || second == Climate.SNOW && first == Climate.HOT;
    }

    private boolean mountainBorder(long firstKey, Climate firstClimate, long secondKey, Climate secondClimate) {
        if (incompatible(firstClimate, secondClimate)) return true;
        int firstX = (int) (firstKey >> 32);
        int firstZ = (int) firstKey;
        int secondX = (int) (secondKey >> 32);
        int secondZ = (int) secondKey;
        int direction = directNeighborDirection(secondX - firstX, secondZ - firstZ);
        if (direction < 0) return false;
        return mountainBorderChoice(firstKey) == direction || mountainBorderChoice(secondKey) == (direction + 2) % 4;
    }

    private byte mountainBorderChoice(long key) {
        Byte cached = mountainBorderChoices.getIfPresent(key);
        if (cached != null) return cached;
        int gridX = (int) (key >> 32);
        int gridZ = (int) key;
        Climate center = climateAtGridCell(gridX, gridZ);
        byte choice = NATURAL_MOUNTAIN_BORDER;
        for (int direction = 0; direction < 4; direction++) {
            Climate neighbor = climateAtGridCell(
                    gridX + DIRECT_NEIGHBOR_X[direction],
                    gridZ + DIRECT_NEIGHBOR_Z[direction]);
            if (incompatible(center, neighbor)) {
                mountainBorderChoices.put(key, choice);
                return choice;
            }
        }
        long random = mix64(key ^ context.seed ^ MOUNTAIN_BORDER_SALT);
        choice = (byte) (random & 3L);
        mountainBorderChoices.put(key, choice);
        return choice;
    }

    private Climate climateAtGridCell(int gridX, int gridZ) {
        return climate((float) context.biomeCellNoise.voronoiValueAtCell(gridX, gridZ) * .5f + .5f);
    }

    private static long climateSeedKey(int gridX, int gridZ) {
        return (long) gridX << 32 | gridZ & 0xffffffffL;
    }

    private static int directNeighborDirection(int deltaX, int deltaZ) {
        for (int direction = 0; direction < 4; direction++)
            if (DIRECT_NEIGHBOR_X[direction] == deltaX && DIRECT_NEIGHBOR_Z[direction] == deltaZ) return direction;
        return -1;
    }

    private static long mix64(long value) {
        value ^= value >>> 30;
        value *= 0xbf58476d1ce4e5b9L;
        value ^= value >>> 27;
        value *= 0x94d049bb133111ebL;
        return value ^ value >>> 31;
    }

    boolean hasMountainBorderAtGridCell(int gridX, int gridZ) {
        long centerKey = climateSeedKey(gridX, gridZ);
        Climate center = climate((float) context.biomeCellNoise.voronoiValueAtCell(gridX, gridZ) * .5f + .5f);
        for (int direction = 0; direction < 4; direction++) {
            int neighborX = gridX + DIRECT_NEIGHBOR_X[direction];
            int neighborZ = gridZ + DIRECT_NEIGHBOR_Z[direction];
            long neighborKey = climateSeedKey(neighborX, neighborZ);
            Climate neighbor = climate(
                    (float) context.biomeCellNoise.voronoiValueAtCell(neighborX, neighborZ) * .5f + .5f);
            if (mountainBorder(centerKey, center, neighborKey, neighbor)) return true;
        }
        return false;
    }

    private static boolean plateauEligible(long regionKey) {
        long value = regionKey;
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        return (value & 1L) == 0L;
    }

    public ColumnSample getColumnSampleAt(int x, int z) {
        long key = ChunkCoordIntPair.chunkXZ2Int(x, z);
        ColumnSample cached = selectionCache.get(key);
        if (cached != null) return cached;
        ColumnSample sample = selector.select(x, z);
        if (selectionCache.size() > 4096) selectionCache.clear();
        selectionCache.put(key, sample);
        return sample;
    }

    public BiomeRegistration getBiomeRegistrationAt(int x, int z) {
        return getColumnSampleAt(x, z).biome.registration;
    }

    public BiomeGenBase getBiomeDataAt(int x, int z) {
        return getBiomeRegistrationAt(x, z).biome;
    }

    public BiomeGenBase getCoreBiomeAt(int x, int z) {
        return selector.biomeForBlend(x, z).biome;
    }

    @Override
    public BiomeGenBase getBiomeGenAt(int x, int z) {
        return getBiomeDataAt(x, z);
    }

    public boolean isOceanBiomeAt(int x, int z) {
        return getColumnSampleAt(x, z).continent.ocean;
    }

    public int[] getBiomesGens(int x, int z, int width, int height) {
        int[] result = new int[width * height];
        for (int dx = 0; dx < width; dx++)
            for (int dz = 0; dz < height; dz++) result[dz * width + dx] = getBiomeGenAt(x + dx, z + dz).biomeID;
        return result;
    }

    public BiomeGenBase[] getBiomesGensData(int x, int z, int width, int height) {
        return getBiomeGenAt(null, x, z, width, height, false);
    }

    public float getTerrainOceanValue(int x, int z) {
        return getTerrainOceanValue(getContinentValue(x, z));
    }

    public float getTerrainOceanValue(float continent) {
        return Math.max(0f, Math.min(2f, 1f + continent / 100f));
    }

    public float getNoiseAt(int x, int z) {
        BiomeRegistration registration = getBiomeRegistrationAt(x, z);
        boolean river = getColumnSampleAt(x, z).morphology.riverStrength > 0f;
        return registration.terrain.generateNoise(perlin, cell, x, z, getTerrainOceanValue(x, z), 1f, river ? 1f : 0f);
    }

    public float getNoiseWithRiverOceanAt(int x, int z, float river, float ocean) {
        return getBiomeRegistrationAt(x, z).terrain.generateNoise(perlin, cell, x, z, ocean, 1f, river);
    }

    public float getRiverStrength(int x, int z) {
        return getColumnSampleAt(x, z).morphology.riverStrength > 0f ? -1f : 1f;
    }

    public float getRiverStrength(int x, int z, float[] sample) {
        sample[0] = x;
        sample[1] = z;
        sample[2] = Float.NaN;
        return getRiverStrength(x, z);
    }

    public float getRiverTunnelStrength(int x, int z) {
        return selector.undergroundRiverStrength(x, z);
    }

    public float getRiverJunctionStrength(int x, int z) {
        return selector.riverJunctionStrength(x, z);
    }

    public float calculateRiver(int x, int z, float strength, float height) {
        return height;
    }

    public float calculateRiver(int x, int z, float strength, float height, float[] sample) {
        return height;
    }

    public boolean isBorderlessAt(int x, int z) {
        BiomeGenBase center = getBiomeDataAt(x, z);
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) if (getBiomeDataAt(x + dx * 16, z + dz * 16) != center) return false;
        return true;
    }

    @Override
    public List getBiomesToSpawnIn() {
        return biomesToSpawnIn;
    }

    @Override
    public float[] getRainfall(float[] output, int x, int z, int width, int height) {
        if (output == null || output.length < width * height) output = new float[width * height];
        for (int dx = 0; dx < width; dx++) for (int dz = 0; dz < height; dz++)
            output[dz * width + dx] = Math.min(1f, getBiomeGenAt(x + dx, z + dz).getIntRainfall() / 65536f);
        return output;
    }

    @Override
    public float getTemperatureAtHeight(float temperature, int height) {
        return temperature;
    }

    @Override
    public BiomeGenBase[] getBiomesForGeneration(BiomeGenBase[] output, int x, int z, int width, int height) {
        return getBiomeGenAt(output, x, z, width, height, false);
    }

    @Override
    public BiomeGenBase[] loadBlockGeneratorData(BiomeGenBase[] output, int x, int z, int width, int height) {
        return getBiomeGenAt(output, x, z, width, height, true);
    }

    @Override
    public BiomeGenBase[] getBiomeGenAt(BiomeGenBase[] output, int x, int z, int width, int height, boolean cache) {
        if (output == null || output.length < width * height) output = new BiomeGenBase[width * height];
        for (int dx = 0; dx < width; dx++)
            for (int dz = 0; dz < height; dz++) output[dz * width + dx] = getBiomeGenAt(x + dx, z + dz);
        return output;
    }

    @Override
    public boolean areBiomesViable(int x, int z, int radius, List allowed) {
        for (int dx = -radius; dx <= radius; dx += 16) for (int dz = -radius; dz <= radius; dz += 16)
            if (!allowed.contains(getBiomeGenAt(x + dx, z + dz))) return false;
        return true;
    }

    @Override
    public ChunkPosition findBiomePosition(int x, int z, int radius, List allowed, Random random) {
        if (selector instanceof GridWorldgenSelector)
            return new ChunkPosition(GridWorldgenSelector.SPAWN_BLOCK_X, 0, GridWorldgenSelector.SPAWN_BLOCK_Z);
        ChunkPosition result = null;
        int seen = 0;
        for (int dx = -radius; dx <= radius; dx += 16) for (int dz = -radius; dz <= radius; dz += 16)
            if (allowed.contains(getBiomeGenAt(x + dx, z + dz)) && random.nextInt(++seen) == 0)
                result = new ChunkPosition(x + dx, 0, z + dz);
        return result;
    }

    @Override
    public void cleanupCache() {
        biomeCache.cleanupCache();
        selectionCache.clear();
    }

    private static final class ClimateRegion {

        final double[] x;
        final double[] z;
        final double[] values;
        final long[] keys;
        final Climate[] climates;
        final ClimateEdge[] edges;

        ClimateRegion(double[] x, double[] z, double[] values, long[] keys, Climate[] climates, ClimateEdge[] edges) {
            this.x = x;
            this.z = z;
            this.values = values;
            this.keys = keys;
            this.climates = climates;
            this.edges = edges;
        }

        int winner(double queryX, double queryZ) {
            int winner = 0;
            double nearest = Double.POSITIVE_INFINITY;
            for (int index = 0; index < x.length; index++) {
                double dx = x[index] - queryX;
                double dz = z[index] - queryZ;
                double distance = dx * dx + dz * dz;
                if (distance < nearest) {
                    nearest = distance;
                    winner = index;
                }
            }
            return winner;
        }

        int nearestOther(double queryX, double queryZ, int excluded) {
            int nearestIndex = -1;
            double nearestDistance = Double.POSITIVE_INFINITY;
            for (int index = 0; index < x.length; index++) {
                if (index == excluded) continue;
                double distance = distance(queryX, queryZ, index);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestIndex = index;
                }
            }
            return nearestIndex;
        }

        double distance(double queryX, double queryZ, int index) {
            double dx = x[index] - queryX;
            double dz = z[index] - queryZ;
            return Math.sqrt(dx * dx + dz * dz);
        }
    }

    private static final class ClimateRegionLookup {

        int x;
        int z;
        ClimateRegion region;
    }

    private static final class ClimateEdge {

        final int first;
        final int second;
        final double middleX;
        final double middleZ;
        final double tangentX;
        final double tangentZ;
        final double minimum;
        final double maximum;

        ClimateEdge(int first, int second, double middleX, double middleZ, double tangentX, double tangentZ,
                double minimum, double maximum) {
            this.first = first;
            this.second = second;
            this.middleX = middleX;
            this.middleZ = middleZ;
            this.tangentX = tangentX;
            this.tangentZ = tangentZ;
            this.minimum = minimum;
            this.maximum = maximum;
        }

        double distance(double queryX, double queryZ) {
            double projected = (queryX - middleX) * tangentX + (queryZ - middleZ) * tangentZ;
            double edgePosition = Math.max(minimum, Math.min(maximum, projected));
            double dx = queryX - (middleX + tangentX * edgePosition);
            double dz = queryZ - (middleZ + tangentZ * edgePosition);
            return Math.sqrt(dx * dx + dz * dz);
        }

        ClimateEdge slice(double newMinimum, double newMaximum) {
            return new ClimateEdge(first, second, middleX, middleZ, tangentX, tangentZ, newMinimum, newMaximum);
        }
    }

    private static int biomeX(int x) {
        return x + ConfigRWG.biomeOffsetX;
    }

    private static int biomeZ(int z) {
        return z + ConfigRWG.biomeOffsetZ;
    }
}
