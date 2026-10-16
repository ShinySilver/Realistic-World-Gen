package rwg.world;

import java.io.File;
import java.util.ArrayDeque;

import rwg.ConfigRWG;
import rwg.registry.BiomeRegistry;

/** Deterministic classification/frontier estimate matching the default 33x33x33 profiler volume. */
public final class CubeCullingEstimate {

    private static final long SEED = 927692613931855800L;
    private static final int SIDE = 33;
    private static final int MIN_Y = -12;
    private static final int CENTER_Y = 4;
    private static final int[] DX = { -1, 1, 0, 0, 0, 0 };
    private static final int[] DY = { 0, 0, -1, 1, 0, 0 };
    private static final int[] DZ = { 0, 0, 0, 0, -1, 1 };

    private CubeCullingEstimate() {}

    public static void main(String[] args) {
        ConfigRWG.initPreview(new File(System.getProperty("rwg.previewConfig", "run/client/config/RWG.cfg")));
        BiomeRegistry registry = PreviewBiomeRegistry.create();
        ColumnPlanner planner = new ColumnPlanner(SEED, new ChunkManager(SEED, true, registry).worldgenSelector());
        CubeTerrainPipeline.GenerationKind[] kinds = new CubeTerrainPipeline.GenerationKind[SIDE * SIDE * SIDE];
        int[] counts = new int[CubeTerrainPipeline.GenerationKind.values().length];
        for (int x = 0; x < SIDE; x++) {
            for (int z = 0; z < SIDE; z++) {
                ColumnPlan plan = planner.plan(x - SIDE / 2, z - SIDE / 2);
                for (int y = 0; y < SIDE; y++) {
                    CubeTerrainPipeline.GenerationKind kind = CubeTerrainPipeline.classify(plan, MIN_Y + y);
                    kinds[index(x, y, z)] = kind;
                    counts[kind.ordinal()]++;
                }
            }
        }

        boolean[] reached = new boolean[kinds.length];
        boolean[] materialized = new boolean[kinds.length];
        ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
        int center = SIDE / 2;
        for (int x = center - 1; x <= center + 1; x++)
            for (int y = CENTER_Y - MIN_Y - 1; y <= CENTER_Y - MIN_Y + 1; y++)
                for (int z = center - 1; z <= center + 1; z++) activate(x, y, z, kinds, reached, materialized, queue);
        while (!queue.isEmpty()) {
            int current = queue.removeFirst();
            int x = current % SIDE;
            int z = current / SIDE % SIDE;
            int y = current / (SIDE * SIDE);
            for (int direction = 0; direction < 6; direction++)
                activate(x + DX[direction], y + DY[direction], z + DZ[direction], kinds, reached, materialized, queue);
        }
        int generated = 0;
        for (boolean value : materialized) if (value) generated++;
        System.out.printf(
                "CULLING_ESTIMATE total=%d empty=%d solid=%d water=%d detailed=%d materialized=%d skipped=%d%n",
                kinds.length,
                counts[CubeTerrainPipeline.GenerationKind.EMPTY.ordinal()],
                counts[CubeTerrainPipeline.GenerationKind.SOLID.ordinal()],
                counts[CubeTerrainPipeline.GenerationKind.WATER.ordinal()],
                counts[CubeTerrainPipeline.GenerationKind.DETAILED.ordinal()],
                generated,
                kinds.length - generated);
    }

    private static void activate(int x, int y, int z, CubeTerrainPipeline.GenerationKind[] kinds, boolean[] reached,
            boolean[] materialized, ArrayDeque<Integer> queue) {
        if (x < 0 || x >= SIDE || y < 0 || y >= SIDE || z < 0 || z >= SIDE) return;
        int index = index(x, y, z);
        CubeTerrainPipeline.GenerationKind kind = kinds[index];
        if (kind != CubeTerrainPipeline.GenerationKind.EMPTY) materialized[index] = true;
        if (kind == CubeTerrainPipeline.GenerationKind.SOLID || reached[index]) return;
        reached[index] = true;
        queue.add(index);
    }

    private static int index(int x, int y, int z) {
        return (y * SIDE + z) * SIDE + x;
    }
}
