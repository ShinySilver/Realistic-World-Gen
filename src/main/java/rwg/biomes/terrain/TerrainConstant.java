package rwg.biomes.terrain;

import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;

/** Constant diagnostic height for the new sampling path; legacy biome terrain remains unchanged. */
public final class TerrainConstant extends TerrainBase {

    private final float height;

    public TerrainConstant(float height) {
        this.height = height;
    }

    @Override
    public float generateNoise(NoiseGenerator perlin, CellNoise cell, int x, int z, float ocean, float border,
            float river) {
        return height;
    }
}
