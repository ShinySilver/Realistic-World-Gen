package rwg.registry;

import java.io.File;

import net.minecraft.world.biome.BiomeGenBase;

import rwg.ConfigRWG;
import rwg.biomes.decorators.NativeBiomeDecorator;
import rwg.biomes.terrain.TerrainBase;
import rwg.biomes.terrain.TerrainGenericCanyon;
import rwg.biomes.terrain.TerrainGenericCliff;
import rwg.biomes.terrain.TerrainGenericHills;
import rwg.biomes.terrain.TerrainGenericMountain;
import rwg.biomes.terrain.TerrainGenericPlateau;
import rwg.biomes.terrain.TerrainMesa;
import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;
import rwg.util.NoiseSelector;
import rwg.world.debug.GridWorldgenSelector;
import rwg.world.sample.MorphologySample;
import rwg.world.sample.TerrainContext;

/** Executable structural guard for the legacy-to-registry built-in migration. */
public final class BuiltinBiomeRegistrationVerification {

    private BuiltinBiomeRegistrationVerification() {}

    public static void main(String[] args) {
        ConfigRWG.initPreview(new File(System.getProperty("rwg.previewConfig", "run/client/config/RWG.cfg")));
        BiomeRegistry registry = BiomeRegistryBootstrap.create(false);

        int selectable = 0;
        int nativeDecorated = 0;
        int woodHills = 0;
        int woodMountains = 0;
        int categoryPlaceholders = 0;
        int canyonPlaceholders = 0;
        int cliffPlaceholders = 0;
        int plainCore = 0;
        int valleyCore = 0;
        int mesaPlateaus = 0;
        int mesaCanyons = 0;
        int biomeHillVariants = 0;
        int biomeMountainVariants = 0;
        for (BiomeRegistration registration : registry.registrations()) {
            if (registration.selectable) selectable++;
            int nativeCount = 0;
            for (int index = 0; index < registration.decorators.length; index++) {
                if (!(registration.decorators[index] instanceof NativeBiomeDecorator)) continue;
                nativeCount++;
                NativeBiomeDecorator decorator = (NativeBiomeDecorator) registration.decorators[index];
                require(decorator.biome() == registration.biome, "native decorator wraps a different biome");
                require(
                        decorator.strengthThreshold() == NativeBiomeDecorator.LEGACY_STRENGTH_THRESHOLD,
                        "native decorator threshold differs from legacy support behavior");
                require(index == 0, "native decorator must run before compatibility edits");
            }
            require(nativeCount <= 1, "registration has duplicate native decorators");
            nativeDecorated += nativeCount;

            boolean placeholder = registration.biome == BiomeGenBase.plains
                    && (registration.terrain instanceof TerrainGenericCanyon
                    || registration.terrain instanceof TerrainGenericCliff
                    || registration.terrain instanceof TerrainGenericHills
                    || registration.terrain instanceof TerrainGenericMountain
                    || registration.terrain instanceof TerrainGenericPlateau);
            if (placeholder) {
                categoryPlaceholders++;
                require(registration.biome == BiomeGenBase.plains, "category placeholder must use vanilla Plains");
                require(registration.subcategory == TerrainSubcategory.CORE, "category placeholder must be core");
                require(
                        registration.terrain.getClass() == placeholderTerrain(registration.category),
                        "category placeholder must use its dedicated generic terrain: " + registration.category);
                int categoryEntries = registry
                        .entries(registration.climate, registration.category, TerrainSubcategory.CORE)
                        .size();
                require(categoryEntries == 1, "placeholder must only exist for an otherwise empty climate/category");
                if (registration.category == TerrainCategory.CANYON) {
                    canyonPlaceholders++;
                    require(
                            registration.terrain instanceof TerrainGenericCanyon,
                            "canyon category must use its dedicated placeholder terrain");
                }
                if (registration.category == TerrainCategory.CLIFF) {
                    cliffPlaceholders++;
                    require(
                            registration.terrain instanceof TerrainGenericCliff,
                            "cliff category must use its dedicated placeholder terrain");
                }
            } else if (registration.terrain instanceof TerrainGenericMountain) {
                require(
                        registration.category == TerrainCategory.MOUNTAIN,
                        "generic mountains must use the mountain category");
                biomeMountainVariants++;
            } else if (registration.terrain instanceof TerrainGenericHills) {
                require(registration.category == TerrainCategory.HILLS, "generic hills must use the hill category");
                biomeHillVariants++;
            } else if (registration.terrain instanceof TerrainMesa) {
                require(
                        registration.category == TerrainCategory.PLATEAU
                                || registration.category == TerrainCategory.CANYON,
                        "mesa terrain must be assigned to plateau or canyon morphology");
                if (registration.category == TerrainCategory.PLATEAU) mesaPlateaus++;
                if (registration.category == TerrainCategory.CANYON) mesaCanyons++;
            } else if (registration.category == TerrainCategory.SMALL_ISLAND
                    || registration.category == TerrainCategory.MEDIUM_ISLAND
                    || registration.category == TerrainCategory.LARGE_ISLAND) {
                        require(
                                registration.subcategory == TerrainSubcategory.CORE,
                                "island size must not be a subcategory");
                    } else
                if (registration.category != TerrainCategory.RIVER
                        && registration.category != TerrainCategory.SHALLOW_OCEAN
                        && registration.category != TerrainCategory.DEEP_OCEAN
                        && registration.category != TerrainCategory.CANYON) {
                            require(
                                    registration.category == TerrainCategory.PLAIN,
                                    "legacy land entry must be plain-only");
                        }

            if (registration.subcategory == TerrainSubcategory.CORE) {
                if (registration.category == TerrainCategory.PLAIN) plainCore++;
                if (registration.category == TerrainCategory.VALLEY) valleyCore++;
            }

            if (registration.biome.biomeID == ConfigRWG.biomeIDs[24]
                    && registration.terrain.getClass().getSimpleName().equals("TerrainHilly")) {
                woodHills++;
                require(registration.weight == 2, "Wood Hills must retain its legacy double weight");
            }
            if (registration.biome.biomeID == ConfigRWG.biomeIDs[24]
                    && registration.terrain.getClass().getSimpleName().equals("TerrainMountainRiver")) {
                woodMountains++;
                require(registration.weight == 2, "Wood Mountains must retain its legacy double weight");
            }
        }

        require(selectable == 109, "unexpected selectable built-in registration count: " + selectable);
        require(nativeDecorated == 16, "unexpected native decorator count: " + nativeDecorated);
        require(
                categoryPlaceholders == 8,
                "expected placeholders only for empty climate/category combinations: " + categoryPlaceholders);
        require(canyonPlaceholders == 2, "expected canyon placeholders only for unpopulated climates");
        require(cliffPlaceholders == 4, "expected one cliff placeholder per climate");
        require(plainCore > 0 && valleyCore == 0, "legacy registrations must no longer populate valleys");
        require(mesaPlateaus == 3, "expected hot mesa biomes and snow forest on plateaus");
        require(mesaCanyons == 1, "expected a mesa-shaped snow forest canyon");
        require(biomeHillVariants == 27, "expected one explicit hill variant per non-marsh plain variant");
        require(biomeMountainVariants == 27, "expected one explicit mountain variant per non-marsh plain variant");
        require(woodHills == 1, "Wood Hills should be registered for plain terrain only");
        require(woodMountains == 1, "Wood Mountains should be registered for plain terrain only");
        verifyGenericTerrainBaseHeight();

        System.setProperty("rwg.skipSaplingGalleryDiscovery", "true");
        GridWorldgenSelector grid = new GridWorldgenSelector(0L, registry);
        require(!grid.usesReferenceBiomeGeneration(), "grid must paint registered RWG surfaces");
        require(grid.registrationAt(0, 0) == null, "grid axis must begin empty");
        require(grid.registrationAt(15, 15) == null, "grid axis must be exactly one chunk wide");
        require(grid.registrationAt(-1, -1) != null, "negative quadrants must border the grid axis");
        require(grid.registrationAt(16, 16) != null, "positive quadrants must border the grid axis");
        require(
                GridWorldgenSelector.SPAWN_BLOCK_X == 48 && GridWorldgenSelector.SPAWN_BLOCK_Z == 48,
                "grid spawn must follow the shifted positive quadrant");
        for (BiomeRegistration registration : registry.registrations()) {
            if (!registration.selectable) continue;
            require(grid.terrainFor(registration) != null, "grid terrain is missing");
        }

        // Ensure the vanilla support objects used by the four wrappers are available in this environment.
        require(BiomeGenBase.mushroomIsland != null && BiomeGenBase.jungle != null, "vanilla support biomes missing");
        System.out.println("RWG built-in biome registry verification passed (" + selectable + " selectable entries)");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static Class<? extends TerrainBase> placeholderTerrain(TerrainCategory category) {
        if (category == TerrainCategory.HILLS) return TerrainGenericHills.class;
        if (category == TerrainCategory.MOUNTAIN) return TerrainGenericMountain.class;
        if (category == TerrainCategory.PLATEAU) return TerrainGenericPlateau.class;
        if (category == TerrainCategory.CANYON) return TerrainGenericCanyon.class;
        if (category == TerrainCategory.CLIFF) return TerrainGenericCliff.class;
        throw new AssertionError("unexpected placeholder category: " + category);
    }

    private static void verifyGenericTerrainBaseHeight() {
        NoiseGenerator noise = NoiseSelector.createNoiseGenerator(0L);
        CellNoise cell = new CellNoise(0L, (short) 0, true);
        MorphologySample morphology = new MorphologySample(TerrainCategory.PLAIN, 0f, 0f, 0f,
                Float.POSITIVE_INFINITY, false);
        TerrainContext low = new TerrainContext(0f, 80f, morphology);
        TerrainContext high = new TerrainContext(0f, 123f, morphology);
        TerrainBase[] terrains = { new TerrainGenericHills(), new TerrainGenericMountain(),
                new TerrainGenericPlateau(), new TerrainGenericCanyon(), new TerrainGenericCliff() };
        for (TerrainBase terrain : terrains) {
            float lowHeight = terrain.generateNoise(noise, cell, 137, -211, low, 1f);
            float highHeight = terrain.generateNoise(noise, cell, 137, -211, high, 1f);
            require(
                    Math.abs((highHeight - lowHeight) - 43f) < .001f,
                    terrain.getClass().getSimpleName() + " must preserve the regional base-height delta");
        }
        float hill = terrains[0].generateNoise(noise, cell, 137, -211, high, 1f);
        float mountain = terrains[1].generateNoise(noise, cell, 137, -211, high, 1f);
        require(hill == mountain, "generic mountain terrain must exactly match generic hill terrain");
    }
}
