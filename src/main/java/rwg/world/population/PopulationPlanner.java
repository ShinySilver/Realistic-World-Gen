package rwg.world.population;

import java.util.ArrayList;
import java.util.List;

import rwg.biomes.decorators.CubicFeature;
import rwg.registry.BiomeRegistration;
import rwg.world.ColumnPlan;
import rwg.world.sample.BiomeBlendSample;

/** Pure, off-thread-safe conversion from biome blend metadata to immutable feature invocations. */
public final class PopulationPlanner {

    private final long seed;
    private final StructurePlanner structures;

    public PopulationPlanner(long seed) {
        this.seed = seed;
        structures = new StructurePlanner(seed);
    }

    public PopulationPlan plan(int columnX, int columnZ, ColumnPlan column) {
        BiomeBlendSample blend = column.biomeBlend();
        float river = column.riverStrength(8, 8);
        List<PlannedFeature> planned = new ArrayList<PlannedFeature>();
        int featureIndex = 0;
        for (int blendIndex = 0; blendIndex < blend.size(); blendIndex++) {
            BiomeRegistration registration = blend.registration(blendIndex);
            float strength = blend.weight(blendIndex);
            for (CubicFeature feature : registration.features) {
                FeaturePlanningContext context = new FeaturePlanningContext(
                        seed,
                        columnX,
                        columnZ,
                        featureIndex++,
                        column,
                        strength,
                        river);
                PlannedFeature result = feature.plan(context);
                if (result != null) planned.add(result);
            }
        }
        return new PopulationPlan(planned, structures.plan(columnX, columnZ));
    }
}
