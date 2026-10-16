package rwg.biomes.surface;

import net.minecraft.block.Block;

/** Mountain surface with the same cliff model as stone mountains and arid configured materials. */
public final class SurfaceDesertMountain extends SurfaceMountainStone {

    public SurfaceDesertMountain(Block top, Block fill, boolean beach, Block beachBlock, float minimumCliff) {
        super(top, fill, beach, beachBlock, minimumCliff);
    }

    public SurfaceDesertMountain(Block top, Block fill, boolean beach, Block beachBlock, float minimumCliff,
            float stoneCliff, float stoneHeight, float stoneStrength, float clayCliff) {
        super(top, fill, beach, beachBlock, minimumCliff, stoneCliff, stoneHeight, stoneStrength, clayCliff);
    }
}
