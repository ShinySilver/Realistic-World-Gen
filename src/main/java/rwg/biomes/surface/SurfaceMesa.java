package rwg.biomes.surface;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import rwg.util.NoiseGenerator;
import rwg.util.NoiseSelector;

/** Layered mesa clay with a configurable cap on gentle terrain. */
public class SurfaceMesa extends SurfaceBase {

    private static final SurfaceMaterial DIRT = new SurfaceMaterial(Blocks.dirt);
    private final SurfaceMaterial[] clayBands = new SurfaceMaterial[100];
    private final SurfaceMaterial capTop;
    private final SurfaceMaterial capFiller;

    public SurfaceMesa(Block top, Block fill, byte metadata) {
        super(top, fill);
        capTop = new SurfaceMaterial(top, metadata);
        capFiller = new SurfaceMaterial(fill, metadata);
        int[] colors = { 1, 8, 0 };
        NoiseGenerator bands = NoiseSelector.createNoiseGenerator(2L);
        for (int index = 0; index < clayBands.length; index++) {
            float value = bands.noise1(index / 3f) * 3f + bands.noise1(index) * .3f + 1.5f;
            value = Math.max(0f, Math.min(2.9f, value));
            clayBands[index] = new SurfaceMaterial(Blocks.stained_hardened_clay, colors[(int) value]);
        }
    }

    @Override
    public int maximumDepth() {
        return 11;
    }

    protected SurfaceMaterial clayAt(int y) {
        return clayBands[Math.max(0, Math.min(99, y - 60))];
    }

    @Override
    public SurfaceMaterial materialAt(int worldX, int worldZ, int surfaceY, int depth, float riverStrength,
            float mountainWeight, float slope, float variation, float broadVariation) {
        if (slope > 1.3f || depth > 4) return clayAt(surfaceY - depth);
        if (surfaceY < 69) return DIRT;
        return depth == 0 ? capTop : capFiller;
    }
}
