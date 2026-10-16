package rwg.biomes.surface;

import net.minecraft.init.Blocks;

/** Red sand with sandstone subsoil and hardened-clay cliff faces. */
public final class SurfaceRedDesert extends SurfaceBase {

    private static final SurfaceMaterial RED_SAND = new SurfaceMaterial(Blocks.sand, 1);
    private static final SurfaceMaterial RED_CLAY = new SurfaceMaterial(Blocks.stained_hardened_clay, 14);
    private static final SurfaceMaterial SANDSTONE = new SurfaceMaterial(Blocks.sandstone);

    public SurfaceRedDesert() {
        super(Blocks.sand, Blocks.sand);
    }

    @Override
    public int maximumDepth() {
        return 5;
    }

    @Override
    public SurfaceMaterial materialAt(int worldX, int worldZ, int surfaceY, int depth, float riverStrength,
            float mountainWeight, float slope, float variation, float broadVariation) {
        if (slope > 1.4f) return RED_CLAY;
        return depth < 4 ? RED_SAND : SANDSTONE;
    }
}
