package rwg.biomes.surface;

import net.minecraft.block.Block;

/** Grassland with a noise-selected alternate top material. */
public final class SurfaceGrasslandMix1 extends SurfaceGrassland {

    private final SurfaceMaterial mix;
    private final float threshold;

    public SurfaceGrasslandMix1(Block top, Block filler, Block mix, Block cliff1, Block cliff2, float mixWidth,
            float mixHeight) {
        super(top, filler, cliff1, cliff2);
        this.mix = new SurfaceMaterial(mix);
        threshold = mixHeight;
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
        if (depth == 0 && variation > threshold) return mix;
        return depth == 0 ? topMaterial : fillerMaterial;
    }
}
