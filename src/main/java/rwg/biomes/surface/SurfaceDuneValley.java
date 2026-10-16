package rwg.biomes.surface;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.biome.BiomeGenBase;

/** Dune/valley transition rule; biome identity remains a planner responsibility. */
public final class SurfaceDuneValley extends SurfaceBase {

    private static final SurfaceMaterial SAND = new SurfaceMaterial(Blocks.sand);
    private static final SurfaceMaterial SANDSTONE = new SurfaceMaterial(Blocks.sandstone);
    private static final SurfaceMaterial COARSE_DIRT = new SurfaceMaterial(Blocks.dirt, 1);
    private final boolean dirt;
    private final boolean mix;

    public SurfaceDuneValley(Block top, Block fill, float valleySize, boolean dirt, boolean mix,
            BiomeGenBase sandBiome) {
        super(top, fill);
        this.dirt = dirt;
        this.mix = mix;
    }

    @Override
    public int maximumDepth() {
        return 5;
    }

    @Override
    public SurfaceMaterial materialAt(int worldX, int worldZ, int surfaceY, int depth, float riverStrength,
            float mountainWeight, float slope, float variation, float broadVariation) {
        float valleyRelief = Math.max(1f, (broadVariation + .25f) * 65f);
        boolean sandy = surfaceY > 90f + variation * 10f - valleyRelief || mix && variation < -.28f;
        if (sandy) return depth < 4 ? SAND : SANDSTONE;
        if (dirt && variation < .22f || surfaceY < 62) return COARSE_DIRT;
        return depth == 0 ? topMaterial : fillerMaterial;
    }
}
