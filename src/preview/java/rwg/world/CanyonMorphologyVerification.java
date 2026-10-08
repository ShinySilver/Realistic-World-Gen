package rwg.world;

import java.io.File;

import rwg.ConfigRWG;
import rwg.registry.BiomeRegistry;
import rwg.registry.TerrainCategory;
import rwg.world.layout.WorldgenSelector;
import rwg.world.sample.ColumnSample;
import rwg.world.terrain.MorphologyRules;

/** Headless integration check for plateau-side river canyons. */
public final class CanyonMorphologyVerification {

    private static final long SEED = 927692613931855800L;

    private CanyonMorphologyVerification() {}

    public static void main(String[] arguments) {
        ConfigRWG.initPreview(new File(System.getProperty("rwg.previewConfig", "run/client/config/RWG.cfg")));
        BiomeRegistry registry = PreviewBiomeRegistry.create();
        ChunkManager manager = new ChunkManager(SEED, true, registry);
        verifyMountainBorderCoverage(manager, registry);
        WorldgenSelector selector = manager.worldgenSelector();

        ColumnSample plateauCanyon = null;
        ColumnSample oppositeCanyon = null;
        int plateauX = 0;
        int plateauZ = 0;
        int oppositeX = 0;
        int oppositeZ = 0;
        search: for (int z = -8192; z <= 8192; z += 16) {
            for (int x = -8192; x <= 8192; x += 16) {
                ColumnSample sample = selector.select(x, z);
                if (sample.morphology.category != TerrainCategory.CANYON) continue;
                if (sample.morphology.plateauSide && plateauCanyon == null) {
                    plateauCanyon = sample;
                    plateauX = x;
                    plateauZ = z;
                } else if (!sample.morphology.plateauSide && oppositeCanyon == null) {
                    oppositeCanyon = sample;
                    oppositeX = x;
                    oppositeZ = z;
                }
                if (plateauCanyon != null && oppositeCanyon != null) break search;
            }
        }

        require(plateauCanyon != null, "no plateau-side river canyon found in verification area");
        require(oppositeCanyon != null, "no opposite-side river canyon found in verification area");
        verifyCanyon(plateauCanyon);
        verifyCanyon(oppositeCanyon);
        require(
                MorphologyRules.canyonErosionStrength(MorphologyRules.MOUNTAIN_RADIUS) == 0f,
                "canyon erosion must begin at normal width");
        require(
                MorphologyRules.canyonErosionStrength(MorphologyRules.CANYON_EROSION_BLEND_START) == 1f,
                "canyon erosion did not reach its narrow width");
        verifyClimateJunctionContinuity(selector);
        System.out.println(
                "RWG canyon verification passed on both sides at plateau=" + plateauX
                        + ","
                        + plateauZ
                        + " opposite="
                        + oppositeX
                        + ","
                        + oppositeZ);
    }

    private static void verifyMountainBorderCoverage(ChunkManager manager, BiomeRegistry registry) {
        ChunkManager duplicate = new ChunkManager(SEED, true, registry);
        for (int gridX = -32; gridX <= 32; gridX++) {
            for (int gridZ = -32; gridZ <= 32; gridZ++) {
                require(manager.hasMountainBorderAtGridCell(gridX, gridZ), "climate seed has no mountain border");
                require(
                        manager.hasMountainBorderAtGridCell(gridX, gridZ)
                                == duplicate.hasMountainBorderAtGridCell(gridX, gridZ),
                        "fallback mountain border is not deterministic");
            }
        }
    }

    /** Regression guard for a river crossing where the nearest climate edge changes at a three-climate junction. */
    private static void verifyClimateJunctionContinuity(WorldgenSelector selector) {
        WorldGenerator generator = new WorldGenerator(SEED, selector);
        WorldGenerator.PreviewColumn plateauEdge = generator.samplePreviewPoint(-5191, 3913);
        WorldGenerator.PreviewColumn neighboringEdge = generator.samplePreviewPoint(-5155, 3941);
        require(
                plateauEdge.sample.morphology.category == TerrainCategory.CANYON
                        && neighboringEdge.sample.morphology.category == TerrainCategory.CANYON,
                "canyon ended at a three-climate junction");
        require(
                Math.abs(plateauEdge.baseHeight - neighboringEdge.baseHeight) < 10f,
                "canyon erosion has a seam at a three-climate junction");
    }

    private static void verifyCanyon(ColumnSample canyon) {
        require(canyon.biome.registration.category == TerrainCategory.CANYON, "canyon did not select a canyon biome");
        require(canyon.climate.plateauBoundaryInfluence > 0f, "canyon is outside plateau-boundary influence");
        require(
                canyon.morphology.rawMountainDistance <= MorphologyRules.MOUNTAIN_RADIUS,
                "canyon escaped the mountain core");
        require(canyon.morphology.riverStrength == 0f, "river channel should retain the river category");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
