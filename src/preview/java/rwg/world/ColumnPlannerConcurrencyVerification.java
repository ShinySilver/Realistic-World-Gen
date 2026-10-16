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

    private static long mix(long value, int part) {
        return (value ^ part) * 0x100000001b3L;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
