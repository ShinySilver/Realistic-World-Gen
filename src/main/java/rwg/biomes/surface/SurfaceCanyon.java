package rwg.biomes.surface;

import net.minecraft.block.Block;

/** Mesa-like canyon surface whose cap transition can be raised for different canyon variants. */
public final class SurfaceCanyon extends SurfaceMesa {

    private final int capRaise;

    public SurfaceCanyon(Block top, Block fill, byte metadata, int grassHeight) {
        super(top, fill, metadata);
        capRaise = grassHeight;
    }

    @Override
    public SurfaceMaterial materialAt(int worldX, int worldZ, int surfaceY, int depth, float riverStrength,
            float mountainWeight, float slope, float variation, float broadVariation) {
        if (slope > 1.3f || depth > 4 || surfaceY < 62) return clayAt(surfaceY - depth);
        if (surfaceY < 62 + capRaise) return super.materialAt(
                worldX,
                worldZ,
                78,
                depth,
                riverStrength,
                mountainWeight,
                0f,
                variation,
                broadVariation);
        return super.materialAt(
                worldX,
                worldZ,
                surfaceY,
                depth,
                riverStrength,
                mountainWeight,
                slope,
                variation,
                broadVariation);
    }
}
