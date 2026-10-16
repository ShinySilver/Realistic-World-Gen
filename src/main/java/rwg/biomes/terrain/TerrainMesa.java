package rwg.biomes.terrain;

import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;
import rwg.world.sample.TerrainContext;

public class TerrainMesa extends TerrainBase {

    public TerrainMesa() {}

    @Override
    public float generateNoise(NoiseGenerator perlin, CellNoise cell, int x, int y, TerrainContext context,
            float border) {
        float height = context.baseHeight + mesaRelief(
                perlin,
                cell,
                x,
                y,
                border,
                context.riverTerrainFactor,
                context.baseHeight - TerrainCanyon.STEP_DATUM);
        return TerrainCanyon.reboundHeight(height);
    }

    @Override
    public float generateNoise(NoiseGenerator perlin, CellNoise cell, int x, int y, float ocean, float border,
            float river) {
        return TerrainCanyon.reboundHeight(74f + mesaRelief(perlin, cell, x, y, border, river, 5f));
    }

    private float mesaRelief(NoiseGenerator perlin, CellNoise cell, int x, int y, float border, float river,
            float baseRelief) {
        float b = perlin.noise2(x / 130f, y / 130f) * 50f * river;
        b *= b / 40f;

        float hn = perlin.noise2(x / 12f, y / 12f);

        float sb = 0f;
        if (b > 2f) {
            sb = (b - 2f) / 2f;
            sb = sb < 0f ? 0f : sb > 5.5f ? 5.5f : sb;
            sb = hn * sb;
        }
        b += sb;

        b = b < 0.1f ? 0.1f : b;

        float steps = TerrainCanyon.coreStepHeight(baseRelief + b, border, hn * 0.5f, 35f);

        float bn = 0f;
        if (b < 7f) {
            float bnh = 5f - b;
            bn += perlin.noise2(x / 70f, y / 70f) * (bnh * 0.4f);
            bn += perlin.noise2(x / 20f, y / 20f) * (bnh * 0.3f);
        }

        float w = perlin.noise2(x / 80f, y / 80f) * 25f;
        w *= w / 25f;

        b += steps + bn - w;

        return b;
    }

}
