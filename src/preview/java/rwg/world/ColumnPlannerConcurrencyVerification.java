package rwg.world;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import rwg.ConfigRWG;
import rwg.registry.BiomeRegistry;
import rwg.registry.BiomeRegistryBootstrap;
import rwg.world.population.FeatureBounds;
import rwg.world.population.PlannedFeature;
import rwg.world.population.PopulationPlan;
import rwg.world.population.PopulationPlanCache;
import rwg.world.population.PopulationPlanner;

/** Verifies order-independent planning and exact deduplication of concurrent requests for one column. */
public final class ColumnPlannerConcurrencyVerification {

    private static final long SEED = 0x5EEDC0DEL;
    private static final int[][] COORDINATES = { { 0, 0 }, { 7, -11 }, { -37, 19 }, { 128, -96 }, { -401, 233 } };

    private ColumnPlannerConcurrencyVerification() {}

    public static void main(String[] arguments) throws Exception {
        ConfigRWG.initPreview(new File(System.getProperty("rwg.previewConfig", "run/client/config/RWG.cfg")));
        BiomeRegistry registry = BiomeRegistryBootstrap.create();
        verifyParallelDeterminism(registry);
        verifyConcurrentDeduplication(registry);
        verifyPopulationPlanning(registry);
        System.out.println("RWG column planner concurrency verification passed");
    }

    private static void verifyParallelDeterminism(BiomeRegistry registry) throws Exception {
        ColumnPlanner sequential = planner(registry);
        long[] expected = new long[COORDINATES.length];
        for (int index = 0; index < COORDINATES.length; index++)
            expected[index] = checksum(sequential.plan(COORDINATES[index][0], COORDINATES[index][1]));

        ColumnPlanner parallel = planner(registry);
        ExecutorService workers = Executors.newFixedThreadPool(4);
        try {
            List<Future<Long>> results = new ArrayList<Future<Long>>();
            for (int pass = 0; pass < 4; pass++) {
                for (int index = COORDINATES.length - 1; index >= 0; index--) {
                    final int coordinate = index;
                    results.add(workers.submit(new Callable<Long>() {

                        @Override
                        public Long call() {
                            return checksum(parallel.plan(COORDINATES[coordinate][0], COORDINATES[coordinate][1]));
                        }
                    }));
                }
            }
            int result = 0;
            for (int pass = 0; pass < 4; pass++) {
                for (int index = COORDINATES.length - 1; index >= 0; index--)
                    require(results.get(result++).get().longValue() == expected[index], "parallel plan changed output");
            }
        } finally {
            workers.shutdownNow();
        }
    }

    private static void verifyConcurrentDeduplication(BiomeRegistry registry) throws Exception {
        final ColumnPlanCache cache = new ColumnPlanCache(planner(registry), 8);
        ExecutorService workers = Executors.newFixedThreadPool(8);
        try {
            List<Future<ColumnPlan>> results = new ArrayList<Future<ColumnPlan>>();
            for (int index = 0; index < 32; index++) results.add(workers.submit(new Callable<ColumnPlan>() {

                @Override
                public ColumnPlan call() {
                    return cache.get(91, -73);
                }
            }));
            ColumnPlan first = results.get(0).get();
            for (Future<ColumnPlan> result : results)
                require(result.get() == first, "cache did not publish one shared immutable plan");
            require(cache.computationCount() == 1L, "column was computed more than once");
        } finally {
            workers.shutdownNow();
        }
    }

    private static void verifyPopulationPlanning(BiomeRegistry registry) throws Exception {
        ColumnPlanCache columns = new ColumnPlanCache(planner(registry), 8);
        final PopulationPlanCache population = new PopulationPlanCache(new PopulationPlanner(SEED), columns);
        PopulationPlan expected = population.get(7, -11);
        long checksum = populationChecksum(expected);
        require(
                checksum == populationChecksum(new PopulationPlanner(SEED).plan(7, -11, columns.get(7, -11))),
                "population plan changed when recomputed");

        ExecutorService workers = Executors.newFixedThreadPool(8);
        try {
            List<Future<PopulationPlan>> results = new ArrayList<Future<PopulationPlan>>();
            for (int index = 0; index < 32; index++) results.add(workers.submit(new Callable<PopulationPlan>() {

                @Override
                public PopulationPlan call() {
                    return population.get(7, -11);
                }
            }));
            for (Future<PopulationPlan> result : results)
                require(result.get() == expected, "population cache did not publish one immutable plan");
        } finally {
            workers.shutdownNow();
        }
    }

    private static ColumnPlanner planner(BiomeRegistry registry) {
        ChunkManager manager = new ChunkManager(SEED, true, registry);
        return new ColumnPlanner(SEED, manager.worldgenSelector());
    }

    private static long checksum(ColumnPlan plan) {
        long value = 0xcbf29ce484222325L;
        for (int x = 0; x < ColumnPlan.SIZE; x++) {
            for (int z = 0; z < ColumnPlan.SIZE; z++) {
                value = mix(value, Float.floatToIntBits(plan.height(x, z)));
                value = mix(value, Float.floatToIntBits(plan.baseHeight(x, z)));
                value = mix(value, Float.floatToIntBits(plan.riverStrength(x, z)));
                value = mix(value, Float.floatToIntBits(plan.mountainWeight(x, z)));
                value = mix(value, plan.registration(x, z).id);
            }
        }
        value = mix(value, plan.minimumSurfaceY());
        return mix(value, plan.maximumSurfaceY());
    }

    private static long populationChecksum(PopulationPlan plan) {
        long value = 0xcbf29ce484222325L;
        for (PlannedFeature feature : plan.features()) {
            FeatureBounds bounds = feature.bounds();
            value = mix(value, feature.feature().getClass().getName().hashCode());
            value = mix(value, (int) feature.seed());
            value = mix(value, (int) (feature.seed() >>> 32));
            value = mix(value, feature.ownerCubeY());
            value = mix(value, feature.stage().ordinal());
            value = mix(value, Float.floatToIntBits(feature.strength()));
            value = mix(value, Float.floatToIntBits(feature.river()));
            value = mix(value, bounds.minimumX);
            value = mix(value, bounds.minimumY);
            value = mix(value, bounds.minimumZ);
            value = mix(value, bounds.maximumX);
            value = mix(value, bounds.maximumY);
            value = mix(value, bounds.maximumZ);
        }
        return value;
    }

    private static long mix(long value, int part) {
        return (value ^ part) * 0x100000001b3L;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
