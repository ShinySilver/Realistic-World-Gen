package rwg.biomes.surface;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

/** Patchy oasis soil and sand with configurable canyon cliff material. */
public final class SurfaceDesertOasis extends SurfaceBase {

    private final SurfaceMaterial sand;
    private final SurfaceMaterial sandstone = new SurfaceMaterial(Blocks.sandstone);
    private final SurfaceMaterial cliff;
    private final SurfaceMaterial cliffAccent;
    private final int cliffType;

    public SurfaceDesertOasis(Block top, Block filler, Block cliff1, Block cliff2, byte metadata, int cliffType) {
        super(top, filler);
        sand = new SurfaceMaterial(Blocks.sand, metadata);
        cliff = new SurfaceMaterial(cliff1, cliffType == 1 ? 14 : 0);
        cliffAccent = new SurfaceMaterial(cliff2 == null ? cliff1 : cliff2);
        this.cliffType = cliffType;
    }

    @Override
    public int maximumDepth() {
        return cliffType == 1 ? 5 : 9;
    }

    @Override
    public SurfaceMaterial materialAt(int worldX, int worldZ, int surfaceY, int depth, float riverStrength,
            float mountainWeight, float slope, float variation, float broadVariation) {
        if (slope > 1.3f) {
            if (cliffType != 1 && depth < 2 && ((worldX * 73428767 ^ worldZ * 912931) & 3) == 0)
                return cliffAccent;
            return cliff;
        }
        boolean soil = surfaceY > 61 && variation > -.3f + (surfaceY - 61f) / 15f;
        if (soil) return depth == 0 ? topMaterial : fillerMaterial;
        return depth < 4 ? sand : sandstone;
    }
}
