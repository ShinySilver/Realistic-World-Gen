package rwg.world;

import java.io.File;
import java.util.Arrays;

import rwg.ConfigRWG;
import rwg.registry.BiomeRegistry;

/** Deterministic headless throughput score for the preview and base-chunk pipelines. */
public final class WorldgenPerformanceScore {

    private static final long SEED = 927692613931855800L;
    private static final int ROUNDS = 3;
    private static volatile long sink;

    private WorldgenPerformanceScore() {}

    public static void main(String[] arguments) {
        ConfigRWG.initPreview(new File(System.getProperty("rwg.previewConfig", "run/client/config/RWG.cfg")));
        BiomeRegistry registry = PreviewBiomeRegistry.create();
        score(registry);
    }

    private static void score(BiomeRegistry registry) {
        runPreviewPoints(registry, 5_000, SEED ^ 0x51A7E5L);
        runPlans(registry, 8, SEED ^ 0xC4A11L);

        long[] previewTimes = new long[ROUNDS];
        long[] planTimes = new long[ROUNDS];
        long checksum = 0L;
        for (int round = 0; round < ROUNDS; round++) {
            TimedResult preview = runPreviewPoints(registry, 20_000, SEED);
            TimedResult plans = runPlans(registry, 32, SEED);
            previewTimes[round] = preview.nanoseconds;
            planTimes[round] = plans.nanoseconds;
            checksum ^= preview.checksum + Long.rotateLeft(plans.checksum, round + 1);
        }
        Arrays.sort(previewTimes);
        Arrays.sort(planTimes);
        double previewScore = perSecond(20_000, previewTimes[ROUNDS / 2]);
        double planScore = perSecond(32, planTimes[ROUNDS / 2]);
        System.out.println(
                String.format(
                        "PERF_SCORE preview_points_per_second=%.1f column_plans_per_second=%.2f checksum=0x%016X",
                        previewScore,
                        planScore,
                        checksum));
        System.out.println(
                "column_plans includes biome blending and terrain heights; cube filling and population are "
                        + "intentionally measured by the cubic pipeline");
    }

    private static TimedResult runPreviewPoints(BiomeRegistry registry, int count, long seed) {
        ChunkManager manager = new ChunkManager(seed, true, registry);
        ColumnPlanner generator = new ColumnPlanner(seed, manager.worldgenSelector());
        long checksum = 0xcbf29ce484222325L;
        int side = (int) Math.ceil(Math.sqrt(count));
        long started = System.nanoTime();
        for (int index = 0; index < count; index++) {
            int x = (index % side - side / 2) * 16;
            int z = (index / side - side / 2) * 16;
            ColumnPlanner.PreviewColumn column = generator.samplePreviewPoint(x, z);
            checksum = mix(checksum, Float.floatToIntBits(column.baseHeight));
            checksum = mix(checksum, Float.floatToIntBits(column.height));
            checksum = mix(checksum, column.sample.biome.registration.id);
        }
        long elapsed = System.nanoTime() - started;
        sink ^= checksum;
        return new TimedResult(elapsed, checksum);
    }

    private static TimedResult runPlans(BiomeRegistry registry, int count, long seed) {
        ChunkManager manager = new ChunkManager(seed, true, registry);
        ColumnPlanner generator = new ColumnPlanner(seed, manager.worldgenSelector());
        long checksum = 0xcbf29ce484222325L;
        int side = (int) Math.ceil(Math.sqrt(count));
        long started = System.nanoTime();
        for (int index = 0; index < count; index++) {
            int chunkX = index % side - side / 2;
            int chunkZ = index / side - side / 2;
            ColumnPlan terrain = generator.plan(chunkX, chunkZ);
            for (int localX = 0; localX < ColumnPlan.SIZE; localX += 4) {
                for (int localZ = 0; localZ < ColumnPlan.SIZE; localZ += 4) {
                    checksum = mix(checksum, Float.floatToIntBits(terrain.baseHeight(localX, localZ)));
                    checksum = mix(checksum, Float.floatToIntBits(terrain.height(localX, localZ)));
                    checksum = mix(checksum, terrain.registration(localX, localZ).id);
                }
            }
        }
        long elapsed = System.nanoTime() - started;
        sink ^= checksum;
        return new TimedResult(elapsed, checksum);
    }

    private static long mix(long checksum, int value) {
        return (checksum ^ value) * 0x100000001b3L;
    }

    private static double perSecond(int operations, long nanoseconds) {
        return operations * 1_000_000_000d / nanoseconds;
    }

    private static final class TimedResult {

        final long nanoseconds;
        final long checksum;

        TimedResult(long nanoseconds, long checksum) {
            this.nanoseconds = nanoseconds;
            this.checksum = checksum;
        }
    }
}
