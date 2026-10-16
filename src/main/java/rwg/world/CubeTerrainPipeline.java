package rwg.world;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import com.cardinalstar.cubicchunks.util.DependencyGraph;
import com.cardinalstar.cubicchunks.world.cube.Cube;
import com.cardinalstar.cubicchunks.world.cube.blockview.UniformBlockView;
import com.gtnewhorizon.gtnhlib.util.data.BlockMeta;

import rwg.biomes.surface.SurfaceMaterial;
import rwg.biomes.surface.SurfaceRule;
import rwg.util.NoiseGenerator;
import rwg.util.NoiseSelector;

/** Ordered, cube-local terrain mutation with allocation-free classification before detailed work. */
final class CubeTerrainPipeline {

    private static final UniformBlockView STONE = new UniformBlockView(new BlockMeta(Blocks.stone, 0));
    private static final UniformBlockView WATER = new UniformBlockView(new BlockMeta(Blocks.water, 0));
    private static final SurfaceMaterial SAND_SURFACE = new SurfaceMaterial(Blocks.sand);
    private static final SurfaceMaterial GRASS_SURFACE = new SurfaceMaterial(Blocks.grass);
    private static final SurfaceMaterial DIRT_SURFACE = new SurfaceMaterial(Blocks.dirt);

    private final DependencyGraph<TerrainStage> stages = new DependencyGraph<TerrainStage>();

    CubeTerrainPipeline(World world) {
        if (world == null) throw new NullPointerException("world");
        stages.addTarget("rwg:base-complete");
        stages.addTarget("rwg:geology-complete", "requires:rwg:base-complete");
        stages.addTarget("rwg:carvers-complete", "requires:rwg:geology-complete");
        stages.addTarget("rwg:terrain-complete");
        stages.addObject("rwg:base", new BaseTerrainStage(), "required-by:rwg:base-complete");
        stages.addObject(
                "rwg:geology",
                new GeologyStage(world.getSeed()),
                "requires:rwg:base-complete",
                "required-by:rwg:geology-complete");
        stages.addObject(
                "rwg:underground-rivers",
                new UndergroundRiverStage(),
                "requires:rwg:geology-complete",
                "required-by:rwg:carvers-complete");
        stages.addObject(
                "rwg:caves",
                new NativeCaveStage(),
                "requires:rwg:geology-complete",
                "required-by:rwg:carvers-complete");
        stages.addObject(
                "rwg:surface",
                new SimpleSurfaceStage(),
                "requires:rwg:carvers-complete",
                "required-by:rwg:terrain-complete");
        stages.addObject("rwg:bedrock", new BedrockStage(), "after:rwg:surface", "required-by:rwg:terrain-complete");
        // Resolve and validate the immutable graph during construction, before generation can become concurrent.
        stages.sorted();
    }

    Cube generate(Chunk column, int cubeY, ColumnPlan plan) {
        GenerationKind kind = classify(plan, cubeY);
        if (kind == GenerationKind.EMPTY) return new Cube(column, cubeY);
        if (kind == GenerationKind.SOLID) return new Cube(column, cubeY, STONE);
        if (kind == GenerationKind.WATER) return new Cube(column, cubeY, WATER);

        Cube cube = new Cube(column, cubeY);
        CubeGenerationContext context = new CubeGenerationContext(cube, plan);
        for (TerrainStage stage : stages.sorted()) stage.generate(context);
        return cube;
    }

    static GenerationKind classify(ColumnPlan plan, int cubeY) {
        int minimumY = cubeY * Cube.SIZE;
        int maximumY = minimumY + Cube.SIZE - 1;
        if (maximumY < CubicWorldSettings.MIN_GENERATION_Y || minimumY >= CubicWorldSettings.MAX_GENERATION_Y)
            return GenerationKind.EMPTY;
        if (minimumY > plan.maximumSurfaceY() && minimumY >= CubicWorldSettings.SEA_LEVEL) return GenerationKind.EMPTY;
        if (minimumY > plan.maximumSurfaceY() && maximumY < CubicWorldSettings.SEA_LEVEL) return GenerationKind.WATER;
        if (maximumY < plan.minimumSurfaceY() - plan.maximumSurfaceDepth()
                && minimumY > CubicWorldSettings.MIN_GENERATION_Y
                && !carversMayIntersect(plan, cubeY))
            return GenerationKind.SOLID;
        return GenerationKind.DETAILED;
    }

    private static boolean carversMayIntersect(ColumnPlan plan, int cubeY) {
        return UndergroundRiverCarver.mayIntersect(plan, cubeY) || GeologyStage.mayIntersect(cubeY);
    }

    static Block baseBlock(int surfaceY, int blockY) {
        if (blockY <= surfaceY) return Blocks.stone;
        return blockY < CubicWorldSettings.SEA_LEVEL ? Blocks.water : Blocks.air;
    }

    static boolean isBedrockY(int blockY) {
        return blockY == CubicWorldSettings.MIN_GENERATION_Y;
    }

    enum GenerationKind {
        EMPTY,
        SOLID,
        WATER,
        DETAILED
    }

    private interface TerrainStage {

        void generate(CubeGenerationContext context);
    }

    private static final class BaseTerrainStage implements TerrainStage {

        @Override
        public void generate(CubeGenerationContext context) {
            for (int localX = 0; localX < Cube.SIZE; localX++) {
                for (int localZ = 0; localZ < Cube.SIZE; localZ++) {
                    int surfaceY = (int) Math.floor(context.column.height(localX, localZ));
                    for (int localY = 0; localY < Cube.SIZE; localY++) {
                        int blockY = context.minimumBlockY + localY;
                        Block block = CubeTerrainPipeline.baseBlock(surfaceY, blockY);
                        if (block != Blocks.air) context.blocks.setBlock(localX, localY, localZ, block);
                    }
                }
            }
        }

    }

    private static final class GeologyStage implements TerrainStage {

        private static final int MINIMUM_Y = -64;
        private static final int MAXIMUM_Y = 48;
        private static final int BROAD_SAMPLE_STEP = 4;
        private static final int BROAD_SAMPLE_SIZE = Cube.SIZE / BROAD_SAMPLE_STEP + 1;
        private final NoiseGenerator noise;

        GeologyStage(long seed) {
            noise = NoiseSelector.createNoiseGenerator(seed ^ 0x6A09E667F3BCC909L);
        }

        static boolean mayIntersect(int cubeY) {
            int minimumY = cubeY * Cube.SIZE;
            int maximumY = minimumY + Cube.SIZE - 1;
            return maximumY >= MINIMUM_Y && minimumY <= MAXIMUM_Y;
        }

        @Override
        public void generate(CubeGenerationContext context) {
            if (!mayIntersect(context.cubeY)) return;
            int firstLocalY = Math.max(0, MINIMUM_Y - context.minimumBlockY);
            int maximumGeologyY = Math.min(MAXIMUM_Y, context.column.maximumSurfaceY() - 8);
            if (firstLocalY > Math.min(Cube.SIZE - 1, maximumGeologyY - context.minimumBlockY)) return;
            float[] broadSamples = sampleBroadNoise(context);
            for (int localX = 0; localX < Cube.SIZE; localX++) {
                int worldX = context.cubeX * Cube.SIZE + localX;
                for (int localZ = 0; localZ < Cube.SIZE; localZ++) {
                    int worldZ = context.cubeZ * Cube.SIZE + localZ;
                    int surfaceY = (int) Math.floor(context.column.height(localX, localZ));
                    int lastLocalY = Math.min(Cube.SIZE - 1, Math.min(MAXIMUM_Y, surfaceY - 8) - context.minimumBlockY);
                    for (int localY = firstLocalY; localY <= lastLocalY; localY++) {
                        int worldY = context.minimumBlockY + localY;
                        if (interpolateBroadNoise(broadSamples, localX, localY, localZ) <= .68f) continue;
                        if (noise.noise3(worldX / 11f, worldY / 9f, worldZ / 11f) <= .05f) continue;
                        // Base terrain is columnar and geology runs before carvers, so every block in this depth range
                        // is known natural stone. Avoid a storage lookup for information already present in the plan.
                        context.blocks.setBlock(localX, localY, localZ, worldY < 8 ? Blocks.gravel : Blocks.dirt);
                    }
                }
            }
        }

        private float[] sampleBroadNoise(CubeGenerationContext context) {
            float[] samples = new float[BROAD_SAMPLE_SIZE * BROAD_SAMPLE_SIZE * BROAD_SAMPLE_SIZE];
            int minimumX = context.cubeX * Cube.SIZE;
            int minimumZ = context.cubeZ * Cube.SIZE;
            for (int sampleX = 0; sampleX < BROAD_SAMPLE_SIZE; sampleX++) {
                float x = (minimumX + sampleX * BROAD_SAMPLE_STEP) / 38f;
                for (int sampleY = 0; sampleY < BROAD_SAMPLE_SIZE; sampleY++) {
                    float y = (context.minimumBlockY + sampleY * BROAD_SAMPLE_STEP) / 26f;
                    for (int sampleZ = 0; sampleZ < BROAD_SAMPLE_SIZE; sampleZ++) {
                        float z = (minimumZ + sampleZ * BROAD_SAMPLE_STEP) / 38f;
                        samples[broadIndex(sampleX, sampleY, sampleZ)] = noise.noise3(x, y, z);
                    }
                }
            }
            return samples;
        }

        private static float interpolateBroadNoise(float[] samples, int localX, int localY, int localZ) {
            int sampleX = localX / BROAD_SAMPLE_STEP;
            int sampleY = localY / BROAD_SAMPLE_STEP;
            int sampleZ = localZ / BROAD_SAMPLE_STEP;
            float tx = (localX % BROAD_SAMPLE_STEP) / (float) BROAD_SAMPLE_STEP;
            float ty = (localY % BROAD_SAMPLE_STEP) / (float) BROAD_SAMPLE_STEP;
            float tz = (localZ % BROAD_SAMPLE_STEP) / (float) BROAD_SAMPLE_STEP;
            float x00 = lerp(
                    samples[broadIndex(sampleX, sampleY, sampleZ)],
                    samples[broadIndex(sampleX + 1, sampleY, sampleZ)],
                    tx);
            float x10 = lerp(
                    samples[broadIndex(sampleX, sampleY + 1, sampleZ)],
                    samples[broadIndex(sampleX + 1, sampleY + 1, sampleZ)],
                    tx);
            float x01 = lerp(
                    samples[broadIndex(sampleX, sampleY, sampleZ + 1)],
                    samples[broadIndex(sampleX + 1, sampleY, sampleZ + 1)],
                    tx);
            float x11 = lerp(
                    samples[broadIndex(sampleX, sampleY + 1, sampleZ + 1)],
                    samples[broadIndex(sampleX + 1, sampleY + 1, sampleZ + 1)],
                    tx);
            return lerp(lerp(x00, x10, ty), lerp(x01, x11, ty), tz);
        }

        private static int broadIndex(int x, int y, int z) {
            return (x * BROAD_SAMPLE_SIZE + y) * BROAD_SAMPLE_SIZE + z;
        }

        private static float lerp(float first, float second, float amount) {
            return first + (second - first) * amount;
        }
    }

    private static final class UndergroundRiverStage implements TerrainStage {

        @Override
        public void generate(CubeGenerationContext context) {
            UndergroundRiverCarver.carve(context);
        }
    }

    /**
     * Stable dependency slot for native caves.
     *
     * <p>
     * Intentionally empty until caves are planned as immutable, bounded features and clipped into intersecting cubes.
     * Do not restore a per-cube neighborhood scan here: it repeats seed discovery for every requested cube and makes
     * population's affected-terrain preparation pay the same cost again.
     */
    private static final class NativeCaveStage implements TerrainStage {

        @Override
        public void generate(CubeGenerationContext context) {}
    }

    private static final class SimpleSurfaceStage implements TerrainStage {

        @Override
        public void generate(CubeGenerationContext context) {
            if (context.maximumBlockY < context.column.minimumSurfaceY() - context.column.maximumSurfaceDepth()
                    || context.minimumBlockY > context.column.maximumSurfaceY())
                return;

            for (int localX = 0; localX < Cube.SIZE; localX++) {
                for (int localZ = 0; localZ < Cube.SIZE; localZ++) {
                    int surfaceY = (int) Math.floor(context.column.height(localX, localZ));
                    SurfaceRule rule = context.column.surfaceRule(localX, localZ);
                    int worldX = context.cubeX * Cube.SIZE + localX;
                    int worldZ = context.cubeZ * Cube.SIZE + localZ;
                    for (int depth = 0; depth <= rule.maximumDepth(); depth++) {
                        int blockY = surfaceY - depth;
                        if (blockY < context.minimumBlockY || blockY > context.maximumBlockY) continue;
                        int localY = blockY - context.minimumBlockY;
                        Block current = context.blocks.getBlock(localX, localY, localZ);
                        if (current == Blocks.air || current == Blocks.water || current == Blocks.flowing_water)
                            continue;
                        SurfaceMaterial material = surfaceY < CubicWorldSettings.SEA_LEVEL - 1 ? SAND_SURFACE
                                : rule.materialAt(
                                        worldX,
                                        worldZ,
                                        surfaceY,
                                        depth,
                                        context.column.riverStrength(localX, localZ),
                                        context.column.mountainWeight(localX, localZ),
                                        context.column.surfaceSlope(localX, localZ),
                                        context.column.surfaceVariation(localX, localZ),
                                        context.column.broadSurfaceVariation(localX, localZ));
                        if (material == null || material.block == null)
                            material = depth == 0 ? GRASS_SURFACE : DIRT_SURFACE;
                        context.blocks.setBlock(localX, localY, localZ, material.block, material.metadata);
                    }
                }
            }
        }

    }

    private static final class BedrockStage implements TerrainStage {

        @Override
        public void generate(CubeGenerationContext context) {
            if (CubicWorldSettings.MIN_GENERATION_Y < context.minimumBlockY
                    || CubicWorldSettings.MIN_GENERATION_Y > context.maximumBlockY)
                return;
            int localY = CubicWorldSettings.MIN_GENERATION_Y - context.minimumBlockY;
            for (int localX = 0; localX < Cube.SIZE; localX++) for (int localZ = 0; localZ < Cube.SIZE; localZ++)
                context.blocks.setBlock(localX, localY, localZ, Blocks.bedrock);
        }
    }
}
