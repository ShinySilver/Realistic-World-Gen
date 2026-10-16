package rwg.world;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;

import com.cardinalstar.cubicchunks.util.DependencyGraph;
import com.cardinalstar.cubicchunks.world.cube.Cube;
import com.cardinalstar.cubicchunks.world.cube.blockview.UniformBlockView;
import com.gtnewhorizon.gtnhlib.util.data.BlockMeta;

/** Ordered, cube-local terrain mutation with allocation-free classification before detailed work. */
final class CubeTerrainPipeline {

    private static final int SURFACE_DEPTH = 3;
    private static final UniformBlockView STONE = new UniformBlockView(new BlockMeta(Blocks.stone, 0));
    private static final UniformBlockView WATER = new UniformBlockView(new BlockMeta(Blocks.water, 0));

    private final DependencyGraph<TerrainStage> stages = new DependencyGraph<TerrainStage>();

    CubeTerrainPipeline() {
        stages.addTarget("rwg:base-complete");
        stages.addTarget("rwg:carvers-complete", "requires:rwg:base-complete");
        stages.addTarget("rwg:terrain-complete");
        stages.addObject("rwg:base", new BaseTerrainStage(), "required-by:rwg:base-complete");
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
        if (maximumY < plan.minimumSurfaceY() - SURFACE_DEPTH && minimumY > CubicWorldSettings.MIN_GENERATION_Y)
            return GenerationKind.SOLID;
        return GenerationKind.DETAILED;
    }

    static Block baseBlock(int surfaceY, int blockY) {
        if (blockY <= surfaceY) return Blocks.stone;
        return blockY < CubicWorldSettings.SEA_LEVEL ? Blocks.water : Blocks.air;
    }

    static Block surfaceBlock(BiomeGenBase biome, int surfaceY, int depth) {
        if (surfaceY < CubicWorldSettings.SEA_LEVEL - 1) return Blocks.sand;
        Block block = depth == 0 ? biome.topBlock : biome.fillerBlock;
        if (block != null) return block;
        return depth == 0 ? Blocks.grass : Blocks.dirt;
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

    private static final class SimpleSurfaceStage implements TerrainStage {

        @Override
        public void generate(CubeGenerationContext context) {
            if (context.maximumBlockY < context.column.minimumSurfaceY() - SURFACE_DEPTH
                    || context.minimumBlockY > context.column.maximumSurfaceY())
                return;

            for (int localX = 0; localX < Cube.SIZE; localX++) {
                for (int localZ = 0; localZ < Cube.SIZE; localZ++) {
                    int surfaceY = (int) Math.floor(context.column.height(localX, localZ));
                    BiomeGenBase biome = context.column.biome(localX, localZ);
                    for (int depth = 0; depth <= SURFACE_DEPTH; depth++) {
                        int blockY = surfaceY - depth;
                        if (blockY < context.minimumBlockY || blockY > context.maximumBlockY) continue;
                        context.blocks.setBlock(
                                localX,
                                blockY - context.minimumBlockY,
                                localZ,
                                CubeTerrainPipeline.surfaceBlock(biome, surfaceY, depth));
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
