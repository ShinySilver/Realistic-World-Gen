package rwg.biomes.surface;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import rwg.world.CubicWorldSettings;

/** Wind-scoured tundra with gravel beaches, rocky cliffs, podzol patches, and high snow. */
public final class SurfaceTundra extends SurfaceBase {

    private static final SurfaceMaterial GRASS = new SurfaceMaterial(Blocks.grass);
    private static final SurfaceMaterial DIRT = new SurfaceMaterial(Blocks.dirt);
    private static final SurfaceMaterial PODZOL = new SurfaceMaterial(Blocks.dirt, 2);
    private static final SurfaceMaterial GRAVEL = new SurfaceMaterial(Blocks.gravel);
    private static final SurfaceMaterial STONE = new SurfaceMaterial(Blocks.stone);
    private static final SurfaceMaterial CLAY = new SurfaceMaterial(Blocks.stained_hardened_clay, 9);
    private static final SurfaceMaterial SNOW = new SurfaceMaterial(Blocks.snow);

    public SurfaceTundra(Block top, Block fill) {
        super(top, fill);
    }

    @Override
    public int maximumDepth() {
        return 5;
    }

    @Override
    public SurfaceMaterial materialAt(int worldX, int worldZ, int surfaceY, int depth, float riverStrength,
            float mountainWeight, float slope, float variation, float broadVariation) {
        if (surfaceY > 110 + variation * 4f && slope < .3f + (surfaceY - 100f) / 50f + variation * .5f) return SNOW;
        if (slope > 1.5f) return CLAY;
        if (slope > Math.max(.45f, 1.5f - (surfaceY - 60f) / 65f + variation * .5f)) return STONE;
        if (surfaceY < CubicWorldSettings.SEA_LEVEL) return GRAVEL;
        if (depth == 0 && broadVariation + variation * .3f > .24f) return PODZOL;
        return depth == 0 ? GRASS : DIRT;
    }
}
