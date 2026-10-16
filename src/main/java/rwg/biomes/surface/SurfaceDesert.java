package rwg.biomes.surface;

import net.minecraft.block.Block;

/** Sand cap with a sandstone base and configurable rocky cliff materials. */
public final class SurfaceDesert extends SurfaceBase {

    private final SurfaceMaterial bottom;
    private final SurfaceMaterial cliff;
    private final SurfaceMaterial cliffAccent;

    public SurfaceDesert(Block top, Block filler, Block bottom, Block cliff1, Block cliff2) {
        super(top, filler);
        this.bottom = new SurfaceMaterial(bottom);
        cliff = new SurfaceMaterial(cliff1);
        cliffAccent = new SurfaceMaterial(cliff2 == null ? cliff1 : cliff2);
    }

    @Override
    public int maximumDepth() {
        return 9;
    }

    @Override
    public SurfaceMaterial materialAt(int worldX, int worldZ, int surfaceY, int depth, float riverStrength,
            float mountainWeight, float slope, float variation, float broadVariation) {
        if (slope > 2.8f) {
            if (depth < 2 && ((worldX * 73428767 ^ worldZ * 912931) & 3) == 0) return cliffAccent;
            return cliff;
        }
        if (depth == 0) return topMaterial;
        return depth < 4 ? fillerMaterial : bottom;
    }
}
