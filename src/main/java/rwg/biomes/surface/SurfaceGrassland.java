package rwg.biomes.surface;

import net.minecraft.block.Block;

/** Grass/filler surface with deterministic rocky cliff faces. */
public class SurfaceGrassland extends SurfaceBase {

    protected final SurfaceMaterial cliff;
    protected final SurfaceMaterial cliffAccent;

    public SurfaceGrassland(Block top, Block filler, Block cliff1, Block cliff2) {
        super(top, filler);
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
        if (slope > 1.4f) return depth < 2 && ((worldX * 73428767 ^ worldZ * 912931) & 3) == 0 ? cliffAccent : cliff;
        return depth == 0 ? topMaterial : fillerMaterial;
    }
}
