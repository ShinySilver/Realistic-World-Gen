package rwg.support;

import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import rwg.biomes.realistic.land.RealisticBiomeMountainChain;
import rwg.world.ChunkManagerRealistic;

/** Placement checks shared by the optional Witchery world-generation mixin. */
public final class WitcheryWorldgenCompat {

    private static final int OPENING_MARGIN = 8;
    private static final float SKYLIGHT_THRESHOLD = 0.70f;

    private WitcheryWorldgenCompat() {}

    public static boolean intersectsRiverJunctionOpening(World world, StructureBoundingBox bounds) {
        if (!(world.getWorldChunkManager() instanceof ChunkManagerRealistic)) return false;

        ChunkManagerRealistic manager = (ChunkManagerRealistic) world.getWorldChunkManager();
        for (int sampleZ = bounds.minZ - OPENING_MARGIN; sampleZ <= bounds.maxZ + OPENING_MARGIN; sampleZ++) {
            for (int sampleX = bounds.minX - OPENING_MARGIN; sampleX <= bounds.maxX + OPENING_MARGIN; sampleX++) {
                if (manager.getRiverJunctionStrength(sampleX, sampleZ) >= SKYLIGHT_THRESHOLD
                        && hasMountainChainNearby(manager, sampleX, sampleZ)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasMountainChainNearby(ChunkManagerRealistic manager, int x, int z) {
        for (int offsetZ = -32; offsetZ <= 32; offsetZ += 16) {
            for (int offsetX = -32; offsetX <= 32; offsetX += 16) {
                if (manager.getBiomeDataAt(x + offsetX, z + offsetZ) instanceof RealisticBiomeMountainChain) {
                    return true;
                }
            }
        }
        return false;
    }
}
