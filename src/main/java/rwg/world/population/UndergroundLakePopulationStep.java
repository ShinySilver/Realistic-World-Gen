package rwg.world.population;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import com.cardinalstar.cubicchunks.world.cube.Cube;

import rwg.ConfigRWG;

/** Sparse coordinate-owned ellipsoids clipped independently into each intersecting cube. */
final class UndergroundLakePopulationStep implements PopulationStep {

    private static final long WATER_SEED = 0x13198A2E03707344L;
    private static final long LAVA_SEED = 0xA4093822299F31D0L;

    @Override
    public void populate(CubePopulationContext context) {
        PopulationRandom random = new PopulationRandom();
        if (ConfigRWG.generateUndergroundLakes) populateFluid(context, random, Blocks.water, -160, 40, 72, WATER_SEED);
        if (ConfigRWG.generateUndergroundLavaLakes)
            populateFluid(context, random, Blocks.lava, -640, -24, 112, LAVA_SEED);
    }

    private static void populateFluid(CubePopulationContext context, PopulationRandom random, Block fluid, int minimumY,
            int maximumY, int chance, long stageSeed) {
        int targetMinimumY = context.minimumBlockY;
        int targetMaximumY = targetMinimumY + Cube.SIZE - 1;
        if (targetMaximumY < minimumY - 4 || targetMinimumY > maximumY + 4) return;

        for (int anchorCubeX = context.cubeX - 1; anchorCubeX <= context.cubeX + 1; anchorCubeX++) {
            for (int anchorCubeY = context.cubeY - 1; anchorCubeY <= context.cubeY + 1; anchorCubeY++) {
                int anchorMinimumY = anchorCubeY * Cube.SIZE;
                if (anchorMinimumY > maximumY || anchorMinimumY + Cube.SIZE - 1 < minimumY) continue;
                for (int anchorCubeZ = context.cubeZ - 1; anchorCubeZ <= context.cubeZ + 1; anchorCubeZ++) {
                    random.reset(
                            PopulationRandom
                                    .seed(context.world.getSeed(), stageSeed, anchorCubeX, anchorCubeY, anchorCubeZ));
                    if (random.nextInt(chance) != 0) continue;
                    double centerX = anchorCubeX * Cube.SIZE + random.nextInt(Cube.SIZE);
                    double centerY = anchorMinimumY + random.nextInt(Cube.SIZE);
                    double centerZ = anchorCubeZ * Cube.SIZE + random.nextInt(Cube.SIZE);
                    double radiusX = 3.5d + random.nextDouble() * 3d;
                    double radiusY = 2d + random.nextDouble() * 2d;
                    double radiusZ = 3.5d + random.nextDouble() * 3d;
                    carve(context, fluid, centerX, centerY, centerZ, radiusX, radiusY, radiusZ);
                }
            }
        }
    }

    private static void carve(CubePopulationContext context, Block fluid, double centerX, double centerY,
            double centerZ, double radiusX, double radiusY, double radiusZ) {
        int minimumX = Math.max(context.minimumBlockX, (int) Math.floor(centerX - radiusX));
        int maximumX = Math.min(context.minimumBlockX + Cube.SIZE - 1, (int) Math.ceil(centerX + radiusX));
        int minimumY = Math.max(context.minimumBlockY, (int) Math.floor(centerY - radiusY));
        int maximumY = Math.min(context.minimumBlockY + Cube.SIZE - 1, (int) Math.ceil(centerY + radiusY));
        int minimumZ = Math.max(context.minimumBlockZ, (int) Math.floor(centerZ - radiusZ));
        int maximumZ = Math.min(context.minimumBlockZ + Cube.SIZE - 1, (int) Math.ceil(centerZ + radiusZ));
        for (int x = minimumX; x <= maximumX; x++) {
            double dx = (x + .5d - centerX) / radiusX;
            for (int y = minimumY; y <= maximumY; y++) {
                double dy = (y + .5d - centerY) / radiusY;
                for (int z = minimumZ; z <= maximumZ; z++) {
                    double dz = (z + .5d - centerZ) / radiusZ;
                    if (dx * dx + dy * dy + dz * dz > 1d) continue;
                    int localX = x - context.minimumBlockX;
                    int localY = y - context.minimumBlockY;
                    int localZ = z - context.minimumBlockZ;
                    Block current = context.blocks.getBlock(localX, localY, localZ);
                    if (!isReplaceable(current)) continue;
                    context.blocks.setBlock(localX, localY, localZ, y <= centerY ? fluid : Blocks.air);
                }
            }
        }
    }

    private static boolean isReplaceable(Block block) {
        return block == Blocks.stone || block == Blocks.dirt
                || block == Blocks.gravel
                || block == Blocks.coal_ore
                || block == Blocks.iron_ore
                || block == Blocks.gold_ore
                || block == Blocks.redstone_ore
                || block == Blocks.diamond_ore
                || block == Blocks.lapis_ore
                || block == Blocks.emerald_ore;
    }
}
