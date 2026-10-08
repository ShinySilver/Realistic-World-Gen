package rwg.biomes.terrain;

import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;
import rwg.world.sample.TerrainContext;

/** Irregular, domain-warped hills with broad, lightly offset shelves. */
public class TerrainGenericHills extends TerrainBase {

    private static final float TERRACE_HEIGHT = 3f;

    @Override
    public float generateNoise(NoiseGenerator perlin, CellNoise cell, int x, int z, TerrainContext context,
            float border) {
        return generateFromBase(perlin, cell, x, z, context.baseHeight, border, context.riverTerrainFactor);
    }

    @Override
    public float generateNoise(NoiseGenerator perlin, CellNoise cell, int x, int z, float ocean, float border,
            float river) {
        return generateFromBase(perlin, cell, x, z, 70f, border, river);
    }

    private float generateFromBase(NoiseGenerator perlin, CellNoise cell, int x, int z, float baseHeight, float border,
            float river) {
        float warpX = perlin.noise2(x / 310f, z / 310f) * 55f;
        float warpZ = perlin.noise2((x + 1709f) / 310f, (z - 919f) / 310f) * 55f;
        float shape = smoothstep(-.25f, .65f, perlin.noise2((x + warpX) / 135f, (z + warpZ) / 135f));
        shape *= shape;

        float uplift = shape * 12f * river;
        float thresholdOffset = cell.noise(x / 340D, z / 340D, 1D) * 1.25f * river;
        float terrace = softTerrace(uplift + thresholdOffset, TERRACE_HEIGHT) - thresholdOffset;
        float height = lerp(uplift, terrace, .3f);
        float detail = perlin.noise2((x + 919f) / 32f, (z - 613f) / 32f) * 1.2f
                + perlin.noise2(x / 17f, z / 17f) * .35f;
        return baseHeight + height + detail * shape * river;
    }

    private static float softTerrace(float height, float step) {
        float level = (float) Math.floor(height / step);
        float fraction = height / step - level;
        return (level + smoothstep(.22f, .78f, fraction)) * step;
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float amount = Math.max(0f, Math.min(1f, (value - edge0) / (edge1 - edge0)));
        return amount * amount * (3f - 2f * amount);
    }

    private static float lerp(float first, float second, float amount) {
        return first + (second - first) * amount;
    }
}
