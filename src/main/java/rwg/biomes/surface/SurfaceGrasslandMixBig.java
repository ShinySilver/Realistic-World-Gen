package rwg.biomes.surface;

import net.minecraft.block.Block;

/** Broad alternate grassland patches with matching top and filler materials. */
public final class SurfaceGrasslandMixBig extends SurfaceGrassland {

    private final SurfaceMaterial mixTop;
    private final SurfaceMaterial mixFiller;
    private final float threshold;
    private final float detailStrength;

    public SurfaceGrasslandMixBig(Block top, Block filler, Block mixTop, Block mixFill, Block cliff1, Block cliff2,
            float mixWidth, float mixHeight, float smallWidth, float smallStrength) {
        super(top, filler, cliff1, cliff2);
        this.mixTop = new SurfaceMaterial(mixTop);
        mixFiller = new SurfaceMaterial(mixFill);
        threshold = mixHeight;
        detailStrength = smallStrength;
    }

    @Override
    public SurfaceMaterial materialAt(int worldX, int worldZ, int surfaceY, int depth, float riverStrength,
            float mountainWeight, float slope, float variation, float broadVariation) {
        if (slope > 1.4f) return super.materialAt(
                worldX,
                worldZ,
                surfaceY,
                depth,
                riverStrength,
                mountainWeight,
                slope,
                variation,
                broadVariation);
        boolean mixed = broadVariation + variation * detailStrength > threshold;
        if (mixed) return depth == 0 ? mixTop : mixFiller;
        return depth == 0 ? topMaterial : fillerMaterial;
    }
}
