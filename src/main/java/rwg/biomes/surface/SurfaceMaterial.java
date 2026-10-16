package rwg.biomes.surface;

import net.minecraft.block.Block;

/** Immutable block state shared by surface rules; instances are created at registry construction, never per block. */
public final class SurfaceMaterial {

    public final Block block;
    public final int metadata;

    public SurfaceMaterial(Block block) {
        this(block, 0);
    }

    public SurfaceMaterial(Block block, int metadata) {
        if (metadata < 0 || metadata > 15) throw new IllegalArgumentException("surface metadata");
        this.block = block;
        this.metadata = metadata;
    }
}
