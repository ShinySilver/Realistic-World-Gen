package rwg.support;

import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import rwg.world.ChunkManager;
import rwg.world.layout.WorldgenSelector;

/** Placement checks shared by the optional Witchery world-generation mixin. */
public final class WitcheryWorldgenCompat {

    private static final int SAMPLE_SPACING = 2;
    private static final int MAX_HEIGHT_VARIATION = 12;

    private WitcheryWorldgenCompat() {}

    public static boolean hasUnsuitableFoundation(World world, StructureBoundingBox bounds) {
        if (!(world.getWorldChunkManager() instanceof ChunkManager)) return false;
        WorldgenSelector selector = ((ChunkManager) world.getWorldChunkManager()).worldgenSelector();
        int minimumChunkX = Math.floorDiv(bounds.minX, 16);
        int maximumChunkX = Math.floorDiv(bounds.maxX, 16);
        int minimumChunkZ = Math.floorDiv(bounds.minZ, 16);
        int maximumChunkZ = Math.floorDiv(bounds.maxZ, 16);
        for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; chunkZ++) {
            for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; chunkX++) {
                if (selector.isVoidChunk(chunkX, chunkZ)) return true;
            }
        }
        int minimumHeight = Integer.MAX_VALUE;
        int maximumHeight = Integer.MIN_VALUE;
        for (int z = bounds.minZ; z <= bounds.maxZ; z += SAMPLE_SPACING) {
            for (int x = bounds.minX; x <= bounds.maxX; x += SAMPLE_SPACING) {
                int height = world.getTopSolidOrLiquidBlock(x, z);
                if (height <= 0) return true;
                minimumHeight = Math.min(minimumHeight, height);
                maximumHeight = Math.max(maximumHeight, height);
                if (maximumHeight - minimumHeight > MAX_HEIGHT_VARIATION) return true;
            }
        }
        return false;
    }
}
