package rwg.world;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.World;

import com.cardinalstar.cubicchunks.util.DependencyGraph;
import com.cardinalstar.cubicchunks.world.cube.Cube;
import com.cardinalstar.cubicchunks.world.cube.blockview.UniformBlockView;
import com.gtnewhorizon.gtnhlib.util.data.BlockMeta;

import rwg.biomes.surface.SurfaceRule;
import rwg.biomes.surface.SurfaceMaterial;
import rwg.ConfigRWG;
import rwg.util.NoiseGenerator;
import rwg.util.NoiseSelector;

/** Ordered, cube-local terrain mutation with allocation-free classification before detailed work. */
final class CubeTerrainPipeline {

    private static final UniformBlockView STONE = new UniformBlockView(new BlockMeta(Blocks.stone, 0));
    private static final UniformBlockView WATER = new UniformBlockView(new BlockMeta(Blocks.water, 0));
    private static final SurfaceMaterial SAND_SURFACE = new SurfaceMaterial(Blocks.sand);
    private static final SurfaceMaterial GRASS_SURFACE = new SurfaceMaterial(Blocks.grass);
    private static final SurfaceMaterial DIRT_SURFACE = new SurfaceMaterial(Blocks.dirt);

    private final World world;
    private final DependencyGraph<TerrainStage> stages = new DependencyGraph<TerrainStage>();

    CubeTerrainPipeline(World world) {
        if (world == null) throw new NullPointerException("world");
        this.world = world;
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
        if (ConfigRWG.generateCaves) stages.addObject(
                "rwg:caves",
                new CaveStage(),
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
        return ConfigRWG.generateCaves && RWGCubicCaveGenerator.mayIntersect(cubeY)
                || UndergroundRiverCarver.mayIntersect(plan, cubeY)
                || GeologyStage.mayIntersect(cubeY);
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
            for (int localX = 0; localX < Cube.SIZE; localX++) {
                int worldX = context.cubeX * Cube.SIZE + localX;
                for (int localZ = 0; localZ < Cube.SIZE; localZ++) {
                    int worldZ = context.cubeZ * Cube.SIZE + localZ;
                    int surfaceY = (int) Math.floor(context.column.height(localX, localZ));
                    for (int localY = 0; localY < Cube.SIZE; localY++) {
                        int worldY = context.minimumBlockY + localY;
                        if (worldY < MINIMUM_Y || worldY > MAXIMUM_Y || surfaceY - worldY < 8) continue;
                        if (context.blocks.getBlock(localX, localY, localZ) != Blocks.stone) continue;
                        double broad = noise.noise3(worldX / 38f, worldY / 26f, worldZ / 38f);
                        double detail = noise.noise3(worldX / 11f, worldY / 9f, worldZ / 11f);
                        if (broad > .68d && detail > .05d)
                            context.blocks.setBlock(localX, localY, localZ, worldY < 8 ? Blocks.gravel : Blocks.dirt);
                    }
                }
            }
        }
    }

    private static final class UndergroundRiverStage implements TerrainStage {

        @Override
        public void generate(CubeGenerationContext context) {
            UndergroundRiverCarver.carve(context);
        }
    }

    private final class CaveStage implements TerrainStage {

        private final ThreadLocal<RWGCubicCaveGenerator> generators = new ThreadLocal<RWGCubicCaveGenerator>() {

            @Override
            protected RWGCubicCaveGenerator initialValue() {
                return new RWGCubicCaveGenerator();
            }
        };

        @Override
        public void generate(CubeGenerationContext context) {
            if (RWGCubicCaveGenerator.mayIntersect(context.cubeY)) generators.get().generate(world, context.cube);
        }
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
                        SurfaceMaterial material = surfaceY < CubicWorldSettings.SEA_LEVEL - 1
                                ? SAND_SURFACE
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
                        context.blocks.setBlock(
                                localX,
                                blockY - context.minimumBlockY,
                                localZ,
                                material.block,
                                material.metadata);
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
