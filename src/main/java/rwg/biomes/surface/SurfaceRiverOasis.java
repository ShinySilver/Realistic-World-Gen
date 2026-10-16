package rwg.biomes.surface;

import net.minecraft.init.Blocks;

/** Conditional riverbank overlay used after arid base rules. */
public final class SurfaceRiverOasis extends SurfaceBase {

    public SurfaceRiverOasis() {
        super(Blocks.grass, Blocks.dirt);
    }

    @Override
    public SurfaceMaterial materialAt(int worldX, int worldZ, int surfaceY, int depth, float riverStrength,
            float mountainWeight, float slope, float variation, float broadVariation) {
        if (riverStrength <= .05f || riverStrength + variation * .15f <= .8f || surfaceY <= 61) return null;
        return depth == 0 ? topMaterial : fillerMaterial;
    }
}
