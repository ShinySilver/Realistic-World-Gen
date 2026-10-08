package rwg.support;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.common.Loader;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.world.generate.decorate.WorldGenCaveVines;
import rwg.registry.TerrainCategory;
import rwg.world.ChunkManager;

/** Optional Et Futurum Requiem cave-vine decoration for RWG's mountain river caves. */
public final class EtFuturumCaveVines {

    private final WorldGenerator generator;

    private EtFuturumCaveVines(WorldGenerator generator) {
        this.generator = generator;
    }

    public static EtFuturumCaveVines create() {
        if (!Loader.isModLoaded("etfuturum")) return null;
        return new EtFuturumCaveVines(new WorldGenCaveVines(ModBlocks.CAVE_VINE.get()));
    }

    public void decorate(World world, Random random, ChunkManager manager, int chunkX, int chunkZ) {
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int x = chunkX + localX;
                int z = chunkZ + localZ;
                if (!hasMountainChainNearby(manager, x, z)) continue;

                float junction = manager.getRiverJunctionStrength(x, z);
                float tunnel = manager.getRiverTunnelStrength(x, z);
                float chance = junction > 0f ? 0.42f * junction : 0.06f * tunnel;
                if (chance <= 0f || random.nextFloat() >= chance) continue;

                int ceilingAir = findCeilingAir(world, x, z);
                if (ceilingAir >= 0) generator.generate(world, random, x, ceilingAir, z);
            }
        }
    }

    private static boolean hasMountainChainNearby(ChunkManager manager, int x, int z) {
        for (int offsetX = -8; offsetX <= 8; offsetX += 8) {
            for (int offsetZ = -8; offsetZ <= 8; offsetZ += 8) {
                if (manager.getBiomeRegistrationAt(x + offsetX, z + offsetZ).category == TerrainCategory.MOUNTAIN)
                    return true;
            }
        }
        return false;
    }

    private static int findCeilingAir(World world, int x, int z) {
        int top = Math.min(115, world.getHeightValue(x, z) - 1);
        for (int y = top; y >= 63; y--) {
            if (world.isAirBlock(x, y, z)
                    && world.getBlock(x, y + 1, z).isSideSolid(world, x, y + 1, z, ForgeDirection.DOWN)) {
                return y;
            }
        }
        return -1;
    }
}
