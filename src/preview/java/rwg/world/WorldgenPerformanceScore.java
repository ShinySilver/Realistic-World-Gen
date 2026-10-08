package rwg.world;

import java.io.File;
import java.util.Arrays;

import one.profiler.AsyncProfiler;
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
        boolean profileChunks = Arrays.asList(arguments).contains("--profile-chunks");
        if (profileChunks) profileChunks(registry);
        else score(registry);
    }

    private static void score(BiomeRegistry registry) {
        runPreviewPoints(registry, 5_000, SEED ^ 0x51A7E5L);
        runChunks(registry, 8, SEED ^ 0xC4A11L);

        long[] previewTimes = new long[ROUNDS];
        long[] chunkTimes = new long[ROUNDS];
        long checksum = 0L;
        for (int round = 0; round < ROUNDS; round++) {
            TimedResult preview = runPreviewPoints(registry, 20_000, SEED);
            TimedResult chunks = runChunks(registry, 32, SEED);
            previewTimes[round] = preview.nanoseconds;
            chunkTimes[round] = chunks.nanoseconds;
            checksum ^= preview.checksum + Long.rotateLeft(chunks.checksum, round + 1);
        }
        Arrays.sort(previewTimes);
        Arrays.sort(chunkTimes);
        double previewScore = perSecond(20_000, previewTimes[ROUNDS / 2]);
        double chunkScore = perSecond(32, chunkTimes[ROUNDS / 2]);
        System.out.println(
                String.format(
                        "PERF_SCORE preview_points_per_second=%.1f base_chunks_per_second=%.2f checksum=0x%016X",
                        previewScore,
                        chunkScore,
                        checksum));
        System.out.println(
                "base_chunks includes sampleChunk + fillBaseChunk; surfaces, structures, Forge events and "
                        + "skylight require the live Minecraft provideChunk adapter and are intentionally excluded");
    }

    private static void profileChunks(BiomeRegistry registry) {
        runChunks(registry, 64, SEED ^ 0xC4A11L);
        String output = System.getProperty("rwg.asyncProfilerOutput");
        if (output == null) throw new IllegalStateException("Missing async-profiler output path");
        try {
            AsyncProfiler.getInstance().execute("stop");
            AsyncProfiler.getInstance().execute("start,event=cpu,interval=1ms");
            TimedResult result = runChunks(registry, 256, SEED);
            AsyncProfiler.getInstance().execute("stop,file=" + output + ",title=RWG base chunk generation");
            System.out.println(
                    String.format(
                            "Profiled 256 base chunks at %.2f chunks/s; checksum=0x%016X",
                            perSecond(256, result.nanoseconds),
                            result.checksum));
        } catch (Exception exception) {
            throw new RuntimeException("Could not profile base chunk generation", exception);
        }
    }

    private static TimedResult runPreviewPoints(BiomeRegistry registry, int count, long seed) {
        ChunkManager manager = new ChunkManager(seed, true, registry);
        WorldGenerator generator = new WorldGenerator(seed, manager.worldgenSelector());
        long checksum = 0xcbf29ce484222325L;
        int side = (int) Math.ceil(Math.sqrt(count));
        long started = System.nanoTime();
        for (int index = 0; index < count; index++) {
            int x = (index % side - side / 2) * 16;
            int z = (index / side - side / 2) * 16;
            WorldGenerator.PreviewColumn column = generator.samplePreviewPoint(x, z);
            checksum = mix(checksum, Float.floatToIntBits(column.baseHeight));
            checksum = mix(checksum, Float.floatToIntBits(column.height));
            checksum = mix(checksum, column.sample.biome.registration.id);
        }
        long elapsed = System.nanoTime() - started;
        sink ^= checksum;
        return new TimedResult(elapsed, checksum);
    }

    private static TimedResult runChunks(BiomeRegistry registry, int count, long seed) {
        ChunkManager manager = new ChunkManager(seed, true, registry);
        WorldGenerator generator = new WorldGenerator(seed, manager.worldgenSelector());
        long checksum = 0xcbf29ce484222325L;
        int side = (int) Math.ceil(Math.sqrt(count));
        long started = System.nanoTime();
        for (int index = 0; index < count; index++) {
            int chunkX = index % side - side / 2;
            int chunkZ = index / side - side / 2;
            WorldGenerator.ChunkTerrain terrain = generator.sampleChunk(chunkX, chunkZ);
            WorldGenerator.ChunkBlocks blocks = generator.fillBaseChunk(terrain);
            for (int column = 0; column < terrain.heights.length; column += 17) {
                checksum = mix(checksum, Float.floatToIntBits(terrain.baseHeights[column]));
                checksum = mix(checksum, Float.floatToIntBits(terrain.heights[column]));
                checksum = mix(checksum, terrain.registrations[column].id);
            }
            for (int block = 0; block < blocks.blocks.length; block += 4093) {
                checksum = mix(
                        checksum,
                        blocks.blocks[block] == null ? 0
                                : net.minecraft.block.Block.getIdFromBlock(blocks.blocks[block]));
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
