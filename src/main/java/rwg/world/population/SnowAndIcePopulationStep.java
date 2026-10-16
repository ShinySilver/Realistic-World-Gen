package rwg.world.population;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;

import com.cardinalstar.cubicchunks.world.cube.Cube;

import rwg.world.CubicWorldSettings;

/** Final surface pass using only immutable height/climate inputs and the current cube. */
final class SnowAndIcePopulationStep implements PopulationStep {

    @Override
    public void populate(CubePopulationContext context) {
        SurfaceFinishPlan finish = context.plan.surfaceFinish();
        for (int localX = 0; localX < Cube.SIZE; localX++) {
            for (int localZ = 0; localZ < Cube.SIZE; localZ++) {
                if (!finish.isCold(localX, localZ)) continue;
                int surfaceY = finish.surfaceY(localX, localZ);
                freezeSurfaceWater(context, localX, surfaceY, localZ);
                placeSnow(context, localX, surfaceY + 1, localZ);
            }
        }
    }

    private static void freezeSurfaceWater(CubePopulationContext context, int localX, int y, int localZ) {
        int localY = y - context.minimumBlockY;
        if (localY < 0 || localY >= Cube.SIZE) return;
        Block surface = context.blocks.getBlock(localX, localY, localZ);
        if (surface == Blocks.water || surface == Blocks.flowing_water)
            context.blocks.setBlock(localX, localY, localZ, Blocks.ice);
    }

    private static void placeSnow(CubePopulationContext context, int localX, int y, int localZ) {
        if (y < CubicWorldSettings.SEA_LEVEL || y < context.minimumBlockY || y >= context.minimumBlockY + Cube.SIZE)
            return;
        int blockX = context.minimumBlockX + localX;
        int blockZ = context.minimumBlockZ + localZ;
        if (insideStructure(context.plan, blockX, y, blockZ)) return;
        int localY = y - context.minimumBlockY;
        if (context.blocks.getBlock(localX, localY, localZ) != Blocks.air) return;

        // At a cube boundary the support belongs to the cube below. Terrain planning guarantees a surface block there;
        // remaining exclusions are checked whenever the support is locally addressable.
        if (localY > 0) {
            Block support = context.blocks.getBlock(localX, localY - 1, localZ);
            Material material = support.getMaterial();
            if (support == Blocks.air || support == Blocks.ice
                    || support == Blocks.packed_ice
                    || material == Material.water
                    || material == Material.lava)
                return;
        }
        context.blocks.setBlock(localX, localY, localZ, Blocks.snow_layer);
    }

    private static boolean insideStructure(PopulationPlan plan, int x, int y, int z) {
        for (PlannedStructure structure : plan.structures()) {
            FeatureBounds bounds = structure.bounds();
            if (x >= bounds.minimumX && x <= bounds.maximumX
                    && y >= bounds.minimumY
                    && y <= bounds.maximumY
                    && z >= bounds.minimumZ
                    && z <= bounds.maximumZ)
                return true;
        }
        return false;
    }
}
