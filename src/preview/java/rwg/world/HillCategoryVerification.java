package rwg.world;

import java.io.File;

import rwg.ConfigRWG;
import rwg.registry.BiomeRegistry;
import rwg.registry.TerrainCategory;
import rwg.world.layout.WorldgenSelector;
import rwg.world.sample.ColumnSample;
import rwg.world.terrain.MorphologyRules;

/** Headless integration check for the regional base-height gate on hill categories. */
public final class HillCategoryVerification {

    private static final long SEED = 927692613931855800L;

    private HillCategoryVerification() {}

    public static void main(String[] arguments) {
        verifySurfaceRiverHeightFade();
        ConfigRWG.initPreview(new File(System.getProperty("rwg.previewConfig", "run/client/config/RWG.cfg")));
        BiomeRegistry registry = PreviewBiomeRegistry.create();
        ChunkManager manager = new ChunkManager(SEED, true, registry);
        WorldgenSelector selector = manager.worldgenSelector();
        int hills = 0;
        int lowPlains = 0;
        for (int z = -8192; z <= 8192; z += 32) {
            for (int x = -8192; x <= 8192; x += 32) {
                ColumnSample sample = selector.select(x, z);
                float distance = sample.morphology.mountainDistance;
                float baseHeight = sample.regional.height;
                if (sample.biome.registration.category == TerrainCategory.HILLS) {
                    hills++;
                    require(baseHeight >= MorphologyRules.HILL_MINIMUM_BASE_HEIGHT, "low terrain selected hills");
                } else if (sample.biome.registration.category == TerrainCategory.PLAIN && !sample.morphology.plateauSide
                        && distance > MorphologyRules.MOUNTAIN_RADIUS
                        && distance <= MorphologyRules.MOUNTAIN_INFLUENCE_RADIUS
                        && baseHeight < MorphologyRules.HILL_MINIMUM_BASE_HEIGHT) {
                            lowPlains++;
                        }
            }
        }

        require(hills > 0, "verification area contains no hills");
        require(lowPlains > 0, "verification area contains no height-gated plains");
        System.out.println("RWG hill-category verification passed (hills=" + hills + ", gated=" + lowPlains + ")");
    }

    private static void verifySurfaceRiverHeightFade() {
        require(
                MorphologyRules.surfaceRiverMountainSuppression(75f, false) == 0f,
                "ordinary river cores must remain fully enabled through the hill height gate");
        require(
                MorphologyRules.surfaceRiverMountainSuppression(82.5f, false) == .5f,
                "ordinary river cores must be halfway faded at Y=82.5");
        require(
                MorphologyRules.surfaceRiverMountainSuppression(90f, false) == 1f,
                "only designated mountain carvers may remain at Y=90");
        require(
                MorphologyRules.surfaceRiverMountainSuppression(100f, true) == 0f,
                "valleys must retain their complete river network");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
