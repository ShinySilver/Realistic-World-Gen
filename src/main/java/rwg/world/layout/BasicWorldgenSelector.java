package rwg.world.layout;

import rwg.ConfigRWG;
import rwg.biomes.poi.PointOfInterestSet;
import rwg.biomes.terrain.TerrainBase;
import rwg.registry.BiomeRegistration;
import rwg.registry.BiomeRegistry;
import rwg.registry.Climate;
import rwg.registry.TerrainCategory;
import rwg.registry.TerrainSubcategory;
import rwg.world.ChunkManager;
import rwg.world.sample.BiomeSample;
import rwg.world.sample.ClimateSample;
import rwg.world.sample.ColumnSample;
import rwg.world.sample.ContinentalSample;
import rwg.world.sample.MorphologySample;
import rwg.world.sample.RegionalSample;
import rwg.world.sample.WorldgenPoint;
import rwg.world.terrain.MorphologyRules;
import rwg.world.terrain.RegionalTerrainSampler;
import rwg.world.terrain.RiverSampler;

/** Basic category-first selection policy; points of interest are deliberately excluded for now. */
public final class BasicWorldgenSelector implements WorldgenSelector {

    private final ChunkManager manager;
    private final RiverSampler terrain;
    private final RegionalTerrainSampler regionalTerrain;
    private final long seed;
    private final BiomeRegistration[][][][] biomePools = new BiomeRegistration[Climate.values().length][TerrainCategory
            .values().length][TerrainSubcategory.values().length][];
    private final BiomeRegistry registry;
    private final BiomeRegistration[] riverRegistrations;

    public BasicWorldgenSelector(long seed, ChunkManager manager, BiomeRegistry registry, RiverSampler terrain,
            RegionalTerrainSampler regionalTerrain) {
        if (manager == null || registry == null || terrain == null || regionalTerrain == null)
            throw new NullPointerException("selector inputs");
        this.seed = seed;
        this.manager = manager;
        this.registry = registry;
        this.terrain = terrain;
        this.regionalTerrain = regionalTerrain;
        riverRegistrations = new BiomeRegistration[registry.registrations().size()];
        for (Climate climate : Climate.values()) {
            for (TerrainCategory category : TerrainCategory.values()) {
                for (TerrainSubcategory subcategory : TerrainSubcategory.values()) {
                    java.util.List<BiomeRegistration> weighted = registry.entries(climate, category, subcategory);
                    biomePools[climate.ordinal()][category.ordinal()][subcategory.ordinal()] = weighted
                            .toArray(new BiomeRegistration[weighted.size()]);
                }
            }
            if (pool(climate, TerrainCategory.PLAIN, TerrainSubcategory.CORE).length == 0)
                throw new IllegalStateException("No plain core biomes for " + climate);
        }
        for (BiomeRegistration registration : registry.registrations()) {
            riverRegistrations[registration.id] = registry.registrationOf(
                    registration.riverBiome,
                    registration.climate,
                    TerrainCategory.RIVER,
                    TerrainSubcategory.CORE);
        }
    }

    @Override
    public ColumnSample select(int x, int z) {
        float continent = manager.getContinentValue(x, z);
        boolean ocean = continent < 0f;
        ClimateSample climate = manager.sampleClimateAt(x, z);
        long riverNetworkSeed = ConfigRWG.separateRiverNetworksPerContinent ? manager.getContinentSeedCoordinates(x, z)
                : 0L;
        float rawMountainDistance = MorphologyRules
                .effectiveMountainDistance(climate.incompatibleBoundaryDistance, continent);
        float valleyDistance = ocean || !MorphologyRules.isRaised(rawMountainDistance) ? Float.POSITIVE_INFINITY
                : terrain.junctionDistance(x, z, riverNetworkSeed);
        float valleyStrength = terrain.junctionStrength(valleyDistance);
        float mountainDistance = MorphologyRules.valleyAdjustedMountainDistance(rawMountainDistance, valleyStrength);
        boolean valley = MorphologyRules.isValley(rawMountainDistance, valleyStrength);
        float uncarvedHeight = ocean ? Float.NaN
                : regionalTerrain.uncarvedLandHeight(x, z, continent, mountainDistance);
        float surfaceRiverSuppression = ocean ? 1f
                : MorphologyRules.surfaceRiverMountainSuppression(uncarvedHeight, valley);
        float erosionSuppression = MorphologyRules.surfaceRiverErosionSuppression(rawMountainDistance, valley);
        long surfaceRiver = ocean ? 0L : terrain.surfaceRiverSample(x, z, riverNetworkSeed, surfaceRiverSuppression);
        float riverDistance = ocean ? Float.POSITIVE_INFINITY : RiverSampler.surfaceRiverDistance(surfaceRiver);
        float riverStrength = ocean ? 0f : RiverSampler.surfaceRiverStrength(surfaceRiver);
        float riverErosionDistance = ocean ? Float.POSITIVE_INFINITY
                : terrain.riverErosionDistance(x, z, riverNetworkSeed, erosionSuppression);
        boolean hasRiver = riverStrength > 0f;
        boolean canyon = !valley && climate.plateauBoundaryInfluence > 0f
                && rawMountainDistance <= MorphologyRules.MOUNTAIN_RADIUS
                && MorphologyRules.isCanyon(terrain.mountainCarvingRiverDistance(x, z, riverNetworkSeed));
        boolean littoral = !ocean && continent < MorphologyRules.COASTAL_FADE_DISTANCE;
        boolean swampMouth = littoral && hasRiver
                && climate.climate == Climate.WET
                && pool(Climate.WET, TerrainCategory.SWAMP, TerrainSubcategory.LITTORAL).length > 0;
        float categorySignal = MorphologyRules.mountainStrength(mountainDistance);
        TerrainCategory morphology = MorphologyRules.category(mountainDistance, climate.plateauSide);
        MorphologySample provisionalMorphology = morphologySample(
                morphology,
                categorySignal,
                riverStrength,
                riverDistance,
                riverErosionDistance,
                rawMountainDistance,
                mountainDistance,
                valleyDistance,
                valleyStrength,
                climate.plateauSide);
        RegionalSample regional = null;
        if (morphology == TerrainCategory.HILLS) {
            regional = regionalTerrain.sample(
                    x,
                    z,
                    continent,
                    ocean,
                    climate,
                    provisionalMorphology,
                    TerrainCategory.HILLS,
                    uncarvedHeight);
            if (regional.height < MorphologyRules.HILL_MINIMUM_BASE_HEIGHT) morphology = TerrainCategory.PLAIN;
        }
        boolean raised = MorphologyRules.isRaised(mountainDistance);
        boolean cliff = continent < MorphologyRules.CLIFF_WIDTH
                && (morphology == TerrainCategory.HILLS || morphology == TerrainCategory.MOUNTAIN);
        TerrainCategory requestedCategory = ocean ? oceanCategory(continent)
                : swampMouth ? TerrainCategory.SWAMP
                        : canyon ? TerrainCategory.CANYON
                                : cliff ? TerrainCategory.CLIFF
                                        : valley ? TerrainCategory.VALLEY
                                                : raised ? morphology
                                                        : littoral ? TerrainCategory.WETLANDS : morphology;
        TerrainSubcategory requestedSubcategory = requestedCategory == TerrainCategory.WETLANDS
                || requestedCategory == TerrainCategory.SWAMP ? TerrainSubcategory.LITTORAL : TerrainSubcategory.CORE;
        float biomeSelector = manager.sampleBiomeSelectorAt(x, z);
        BiomeRegistration registration = chooseBiome(climate, requestedCategory, requestedSubcategory, biomeSelector);
        boolean climateBorder = isClimateBorder(registration.subcategory);
        TerrainCategory finalCategory = swampMouth ? TerrainCategory.SWAMP
                : hasRiver ? TerrainCategory.RIVER : climateBorder ? requestedCategory : registration.category;
        MorphologySample finalMorphology = morphologySample(
                finalCategory,
                categorySignal,
                riverStrength,
                riverDistance,
                riverErosionDistance,
                rawMountainDistance,
                mountainDistance,
                valleyDistance,
                valleyStrength,
                climate.plateauSide);
        TerrainCategory regionalCategory = climateBorder ? requestedCategory : registration.category;
        if (regional == null || hasRegionalCategoryAdjustment(regionalCategory)) regional = regionalTerrain
                .sample(x, z, continent, ocean, climate, finalMorphology, regionalCategory, uncarvedHeight);
        WorldgenPoint point = new WorldgenPoint(seed, x, z);
        ContinentalSample continentalSample = new ContinentalSample(continent, ocean, -1);
        return new ColumnSample(
                point,
                continentalSample,
                climate,
                PointOfInterestSet.EMPTY,
                finalMorphology,
                regional,
                new BiomeSample(registration, biomeSelector));
    }

    @Override
    public MorphologySample morphologyAt(int x, int z) {
        float continent = manager.getContinentValue(x, z);
        if (continent < 0f)
            return new MorphologySample(oceanCategory(continent), 0f, 0f, 0f, Float.POSITIVE_INFINITY, false);
        ClimateSample climate = manager.sampleClimateAt(x, z);
        long riverNetworkSeed = ConfigRWG.separateRiverNetworksPerContinent ? manager.getContinentSeedCoordinates(x, z)
                : 0L;
        float rawMountainDistance = MorphologyRules
                .effectiveMountainDistance(climate.incompatibleBoundaryDistance, continent);
        float valleyDistance = MorphologyRules.isRaised(rawMountainDistance)
                ? terrain.junctionDistance(x, z, riverNetworkSeed)
                : Float.POSITIVE_INFINITY;
        float valleyStrength = terrain.junctionStrength(valleyDistance);
        float mountainDistance = MorphologyRules.valleyAdjustedMountainDistance(rawMountainDistance, valleyStrength);
        boolean valley = MorphologyRules.isValley(rawMountainDistance, valleyStrength);
        float uncarvedHeight = regionalTerrain.uncarvedLandHeight(x, z, continent, mountainDistance);
        float surfaceRiverSuppression = MorphologyRules.surfaceRiverMountainSuppression(uncarvedHeight, valley);
        float erosionSuppression = MorphologyRules.surfaceRiverErosionSuppression(rawMountainDistance, valley);
        long surfaceRiver = terrain.surfaceRiverSample(x, z, riverNetworkSeed, surfaceRiverSuppression);
        float riverDistance = RiverSampler.surfaceRiverDistance(surfaceRiver);
        float riverStrength = RiverSampler.surfaceRiverStrength(surfaceRiver);
        float riverErosionDistance = terrain.riverErosionDistance(x, z, riverNetworkSeed, erosionSuppression);
        float mountainStrength = MorphologyRules.mountainStrength(mountainDistance);
        boolean canyon = !valley && climate.plateauBoundaryInfluence > 0f
                && rawMountainDistance <= MorphologyRules.MOUNTAIN_RADIUS
                && MorphologyRules.isCanyon(terrain.mountainCarvingRiverDistance(x, z, riverNetworkSeed));
        TerrainCategory landCategory = MorphologyRules.category(mountainDistance, climate.plateauSide);
        MorphologySample provisional = morphologySample(
                landCategory,
                mountainStrength,
                riverStrength,
                riverDistance,
                riverErosionDistance,
                rawMountainDistance,
                mountainDistance,
                valleyDistance,
                valleyStrength,
                climate.plateauSide);
        if (landCategory == TerrainCategory.HILLS && regionalTerrain
                .sample(x, z, continent, false, climate, provisional, TerrainCategory.HILLS, uncarvedHeight).height
                < MorphologyRules.HILL_MINIMUM_BASE_HEIGHT)
            landCategory = TerrainCategory.PLAIN;
        TerrainCategory category = riverStrength > 0f ? TerrainCategory.RIVER
                : canyon ? TerrainCategory.CANYON : valley ? TerrainCategory.VALLEY : landCategory;
        return new MorphologySample(
                category,
                mountainStrength,
                mountainStrength,
                riverStrength,
                riverDistance,
                riverErosionDistance,
                rawMountainDistance,
                mountainDistance,
                valleyDistance,
                valleyStrength,
                climate.plateauSide);
    }

    /** Coarse-grid hot path: category first, then biome, without river or full column-sample allocation. */
    public BiomeRegistration biomeForBlend(int x, int z) {
        float continent = manager.getContinentValue(x, z);
        ClimateSample climate = manager.sampleClimateAt(x, z);
        TerrainCategory category;
        TerrainSubcategory subcategory = TerrainSubcategory.CORE;
        if (continent < 0f) {
            category = oceanCategory(continent);
        } else {
            float rawMountainDistance = MorphologyRules
                    .effectiveMountainDistance(climate.incompatibleBoundaryDistance, continent);
            long riverNetworkSeed = ConfigRWG.separateRiverNetworksPerContinent
                    ? manager.getContinentSeedCoordinates(x, z)
                    : 0L;
            float valleyStrength = MorphologyRules.isRaised(rawMountainDistance)
                    ? terrain.junctionStrength(terrain.junctionDistance(x, z, riverNetworkSeed))
                    : 0f;
            float mountainDistance = MorphologyRules
                    .valleyAdjustedMountainDistance(rawMountainDistance, valleyStrength);
            boolean valley = MorphologyRules.isValley(rawMountainDistance, valleyStrength);
            TerrainCategory morphology = MorphologyRules.category(mountainDistance, climate.plateauSide);
            if (morphology == TerrainCategory.HILLS) {
                float erosionSuppression = MorphologyRules.surfaceRiverErosionSuppression(rawMountainDistance, valley);
                float riverDistance = terrain.riverDistance(x, z, riverNetworkSeed, false);
                float riverStrength = terrain.riverStrength(riverDistance);
                float riverErosionDistance = terrain.riverErosionDistance(x, z, riverNetworkSeed, erosionSuppression);
                float mountainStrength = MorphologyRules.mountainStrength(mountainDistance);
                MorphologySample provisional = morphologySample(
                        morphology,
                        mountainStrength,
                        riverStrength,
                        riverDistance,
                        riverErosionDistance,
                        rawMountainDistance,
                        mountainDistance,
                        Float.POSITIVE_INFINITY,
                        valleyStrength,
                        climate.plateauSide);
                if (regionalTerrain.sample(x, z, continent, false, climate, provisional, TerrainCategory.HILLS).height
                        < MorphologyRules.HILL_MINIMUM_BASE_HEIGHT)
                    morphology = TerrainCategory.PLAIN;
            }
            boolean raised = MorphologyRules.isRaised(mountainDistance);
            boolean canyon = !valley && climate.plateauBoundaryInfluence > 0f
                    && rawMountainDistance <= MorphologyRules.MOUNTAIN_RADIUS
                    && MorphologyRules.isCanyon(terrain.mountainCarvingRiverDistance(x, z, riverNetworkSeed));
            if (continent >= MorphologyRules.COASTAL_FADE_DISTANCE || raised) {
                category = canyon ? TerrainCategory.CANYON
                        : continent < MorphologyRules.CLIFF_WIDTH
                                && (morphology == TerrainCategory.HILLS || morphology == TerrainCategory.MOUNTAIN)
                                        ? TerrainCategory.CLIFF
                                        : valley ? TerrainCategory.VALLEY : morphology;
            } else {
                subcategory = TerrainSubcategory.LITTORAL;
                boolean swampMouth = climate.climate == Climate.WET
                        && pool(Climate.WET, TerrainCategory.SWAMP, TerrainSubcategory.LITTORAL).length > 0
                        && terrain.riverStrength(x, z, riverNetworkSeed, false) > 0f;
                category = swampMouth ? TerrainCategory.SWAMP : TerrainCategory.WETLANDS;
            }
        }
        float biomeSelector = manager.sampleBiomeSelectorAt(x, z);
        return chooseBiome(climate, category, subcategory, biomeSelector);
    }

    @Override
    public BiomeRegistration registrationAt(int x, int z) {
        return biomeForBlend(x, z);
    }

    @Override
    public boolean isVoidChunk(int chunkX, int chunkZ) {
        return false;
    }

    @Override
    public TerrainBase terrainFor(BiomeRegistration registration) {
        return registration.terrain;
    }

    @Override
    public boolean usesReferenceBiomeGeneration() {
        return false;
    }

    private BiomeRegistration chooseBiome(ClimateSample climate, TerrainCategory category,
            TerrainSubcategory subcategory, float biomeSelector) {
        TerrainSubcategory requestedSubcategory = subcategory == TerrainSubcategory.CORE
                && category != TerrainCategory.SHALLOW_OCEAN
                && category != TerrainCategory.DEEP_OCEAN
                        && climate.category != TerrainSubcategory.CORE
                                ? climate.category
                                : subcategory;
        BiomeRegistration[] choices = pool(climate.climate, category, requestedSubcategory);
        if (choices.length == 0 && requestedSubcategory != subcategory)
            choices = pool(climate.climate, category, subcategory);
        if (choices.length == 0 && subcategory != TerrainSubcategory.CORE)
            choices = pool(climate.climate, category, TerrainSubcategory.CORE);
        if (choices.length == 0) choices = pool(climate.climate, TerrainCategory.PLAIN, TerrainSubcategory.CORE);
        return choices[(int) (biomeSelector * choices.length)];
    }

    public BiomeRegistration registration(int id) {
        return registry.registration(id);
    }

    public int registrationCount() {
        return registry.registrations().size();
    }

    public BiomeRegistration riverRegistration(BiomeRegistration land) {
        return riverRegistrations[land.id];
    }

    public float riverStrength(int x, int z) {
        float continent = manager.getContinentValue(x, z);
        if (continent < 0f) return 0f;
        long riverNetworkSeed = ConfigRWG.separateRiverNetworksPerContinent ? manager.getContinentSeedCoordinates(x, z)
                : 0L;
        ClimateSample climate = manager.sampleClimateAt(x, z);
        float rawMountainDistance = MorphologyRules
                .effectiveMountainDistance(climate.incompatibleBoundaryDistance, continent);
        float valleyDistance = MorphologyRules.isRaised(rawMountainDistance)
                ? terrain.junctionDistance(x, z, riverNetworkSeed)
                : Float.POSITIVE_INFINITY;
        float valleyStrength = terrain.junctionStrength(valleyDistance);
        float mountainDistance = MorphologyRules.valleyAdjustedMountainDistance(rawMountainDistance, valleyStrength);
        boolean valley = MorphologyRules.isValley(rawMountainDistance, valleyStrength);
        float uncarvedHeight = regionalTerrain.uncarvedLandHeight(x, z, continent, mountainDistance);
        float surfaceRiverSuppression = MorphologyRules.surfaceRiverMountainSuppression(uncarvedHeight, valley);
        long surfaceRiver = terrain.surfaceRiverSample(x, z, riverNetworkSeed, surfaceRiverSuppression);
        return RiverSampler.surfaceRiverStrength(surfaceRiver);
    }

    @Override
    public float undergroundRiverStrength(int x, int z) {
        if (manager.getContinentValue(x, z) < 0f) return 0f;
        return terrain.riverStrength(x, z, riverNetworkSeed(x, z), false);
    }

    @Override
    public float riverJunctionStrength(int x, int z) {
        if (manager.getContinentValue(x, z) < 0f) return 0f;
        return terrain.junctionStrength(terrain.junctionDistance(x, z, riverNetworkSeed(x, z)));
    }

    private long riverNetworkSeed(int x, int z) {
        return ConfigRWG.separateRiverNetworksPerContinent ? manager.getContinentSeedCoordinates(x, z) : 0L;
    }

    private BiomeRegistration[] pool(Climate climate, TerrainCategory category, TerrainSubcategory subcategory) {
        return biomePools[climate.ordinal()][category.ordinal()][subcategory.ordinal()];
    }

    private static MorphologySample morphologySample(TerrainCategory category, float mountainStrength,
            float riverStrength, float riverDistance, float riverErosionDistance, float rawMountainDistance,
            float mountainDistance, float valleyDistance, float valleyStrength, boolean plateauSide) {
        return new MorphologySample(
                category,
                mountainStrength,
                mountainStrength,
                riverStrength,
                riverDistance,
                riverErosionDistance,
                rawMountainDistance,
                mountainDistance,
                valleyDistance,
                valleyStrength,
                plateauSide);
    }

    private static boolean hasRegionalCategoryAdjustment(TerrainCategory category) {
        return category == TerrainCategory.WETLANDS || category == TerrainCategory.SWAMP
                || category == TerrainCategory.SMALL_ISLAND
                || category == TerrainCategory.MEDIUM_ISLAND
                || category == TerrainCategory.LARGE_ISLAND;
    }

    private static boolean isClimateBorder(TerrainSubcategory subcategory) {
        return subcategory == TerrainSubcategory.COLD_BORDER || subcategory == TerrainSubcategory.HOT_BORDER;
    }

    private static TerrainCategory oceanCategory(float continent) {
        return continent < -300f ? TerrainCategory.DEEP_OCEAN : TerrainCategory.SHALLOW_OCEAN;
    }

}
