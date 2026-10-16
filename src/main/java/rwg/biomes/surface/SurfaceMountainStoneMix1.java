package rwg.biomes.surface;

import net.minecraft.block.Block;

/** Stone-mountain rule with a noise-selected alternate cap. */
public final class SurfaceMountainStoneMix1 extends SurfaceMountainStone {

    private final SurfaceMaterial mix;
    private final float mixThreshold;

    public SurfaceMountainStoneMix1(Block top, Block fill, boolean beach, Block beachBlock, float minimumCliff,
            float stoneCliff, float stoneHeight, float stoneStrength, float clayCliff, Block mixBlock, float mixSize) {
        super(top, fill, beach, beachBlock, minimumCliff, stoneCliff, stoneHeight, stoneStrength, clayCliff);
        mix = new SurfaceMaterial(mixBlock);
        mixThreshold = mixSize;
    }

    @Override
    public SurfaceMaterial materialAt(int worldX, int worldZ, int surfaceY, int depth, float riverStrength,
            float mountainWeight, float slope, float variation, float broadVariation) {
        SurfaceMaterial base = super.materialAt(
                worldX,
                worldZ,
                surfaceY,
                depth,
                riverStrength,
                mountainWeight,
                slope,
                variation,
                broadVariation);
        if (slope <= 1.4f && surfaceY >= 63 && variation > mixThreshold) return mix;
        return base;
    }
}
