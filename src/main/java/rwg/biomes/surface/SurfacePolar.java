package rwg.biomes.surface;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

/** Deep snow cover with exposed rocky channels along strong rivers. */
public final class SurfacePolar extends SurfaceBase {

    private static final SurfaceMaterial SNOW = new SurfaceMaterial(Blocks.snow);
    private static final SurfaceMaterial STONE = new SurfaceMaterial(Blocks.stone);
    private static final SurfaceMaterial COBBLESTONE = new SurfaceMaterial(Blocks.cobblestone);

    public SurfacePolar(Block top, Block fill) {
        super(top, fill);
    }

    @Override
    public int maximumDepth() {
        return 8;
    }

    @Override
    public SurfaceMaterial materialAt(int worldX, int worldZ, int surfaceY, int depth, float riverStrength,
            float mountainWeight, float slope, float variation, float broadVariation) {
        boolean river = riverStrength > .05f && riverStrength + variation * .1f > .86f;
        if (!river) return SNOW;
        if (variation > .25f && depth < 4) return fillerMaterial;
        return ((worldX * 73428767 ^ worldZ * 912931) & 1) == 0 ? STONE : COBBLESTONE;
    }
}
