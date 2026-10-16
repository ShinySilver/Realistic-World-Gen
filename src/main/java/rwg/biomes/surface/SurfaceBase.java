package rwg.biomes.surface;

import net.minecraft.block.Block;

public class SurfaceBase implements SurfaceRule {

    private static final int DEFAULT_MAXIMUM_DEPTH = 3;

    protected final Block topBlock;
    protected final Block fillerBlock;
    protected final SurfaceMaterial topMaterial;
    protected final SurfaceMaterial fillerMaterial;

    public SurfaceBase(Block top, Block fill) {
        topBlock = top;
        fillerBlock = fill;
        topMaterial = new SurfaceMaterial(top);
        fillerMaterial = new SurfaceMaterial(fill);
    }

    @Override
    public int maximumDepth() {
        return DEFAULT_MAXIMUM_DEPTH;
    }

    @Override
    public SurfaceMaterial materialAt(int worldX, int worldZ, int surfaceY, int depth, float riverStrength,
            float mountainWeight, float slope, float variation, float broadVariation) {
        return depth == 0 ? topMaterial : fillerMaterial;
    }
}
