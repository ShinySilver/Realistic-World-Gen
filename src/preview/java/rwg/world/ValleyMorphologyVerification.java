package rwg.world;

import java.io.File;

import rwg.ConfigRWG;
import rwg.registry.BiomeRegistry;
import rwg.registry.TerrainCategory;
import rwg.world.layout.WorldgenSelector;
import rwg.world.sample.ColumnSample;
import rwg.world.terrain.MorphologyRules;

/** Headless integration check for junction valleys and their resolved biome category. */
public final class ValleyMorphologyVerification {

    private static final long SEED = 927692613931855800L;

    private ValleyMorphologyVerification() {}

    public static void main(String[] arguments) {
        ConfigRWG.initPreview(new File(System.getProperty("rwg.previewConfig", "run/client/config/RWG.cfg")));
        BiomeRegistry registry = PreviewBiomeRegistry.create();
        ChunkManager manager = new ChunkManager(SEED, true, registry);
        WorldgenSelector selector = manager.worldgenSelector();

        ColumnSample valley = null;
        int valleyX = 0;
        int valleyZ = 0;
        search: for (int z = -8192; z <= 8192; z += 32) {
            for (int x = -8192; x <= 8192; x += 32) {
                ColumnSample sample = selector.select(x, z);
                if (sample.morphology.category != TerrainCategory.VALLEY) continue;
                valley = sample;
                valleyX = x;
                valleyZ = z;
                break search;
            }
        }

        require(valley != null, "no junction valley found in verification area");
        require(valley.biome.registration.category == TerrainCategory.VALLEY, "valley did not select a valley biome");
        require(
                valley.morphology.valleyStrength >= MorphologyRules.VALLEY_CATEGORY_STRENGTH,
                "valley category strength is below its threshold");
        require(
                MorphologyRules.isRaised(valley.morphology.rawMountainDistance),
                "valley escaped the raw raised-terrain field");
        require(
                valley.morphology.mountainDistance > valley.morphology.rawMountainDistance,
                "valley did not suppress mountain uplift");
        System.out.println(
                "RWG valley verification passed at " + valleyX
                        + ","
                        + valleyZ
                        + " strength="
                        + valley.morphology.valleyStrength
                        + " mountainDistance="
                        + valley.morphology.rawMountainDistance
                        + "->"
                        + valley.morphology.mountainDistance);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
