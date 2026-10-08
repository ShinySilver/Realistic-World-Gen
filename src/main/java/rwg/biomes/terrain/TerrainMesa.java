package rwg.biomes.terrain;

import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;
import rwg.world.sample.TerrainContext;

public class TerrainMesa extends TerrainBase {

    public TerrainMesa() {}

    @Override
    public float generateNoise(NoiseGenerator perlin, CellNoise cell, int x, int y, TerrainContext context,
            float border) {
        float base = terracedBase(perlin, x, y, context.baseHeight);
        return base + mesaRelief(perlin, cell, x, y, border, context.riverTerrainFactor);
    }

    @Override
    public float generateNoise(NoiseGenerator perlin, CellNoise cell, int x, int y, float ocean, float border,
            float river) {
        return 74f + mesaRelief(perlin, cell, x, y, border, river);
    }

    private float mesaRelief(NoiseGenerator perlin, CellNoise cell, int x, int y, float border, float river) {
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

        float c1 = 0f;
        if (b > 1f) {
            c1 = b > 5.5f ? 4.5f : b - 1f;
            c1 *= 3;
        }

        float c2 = 0f;
        if (b > 5.5f && border > 0.95f + hn * 0.09f) {
            c2 = b > 6f ? 0.5f : b - 5.5f;
            c2 *= 35;
        }

        float bn = 0f;
        if (b < 7f) {
            float bnh = 5f - b;
            bn += perlin.noise2(x / 70f, y / 70f) * (bnh * 0.4f);
            bn += perlin.noise2(x / 20f, y / 20f) * (bnh * 0.3f);
        }

        float w = perlin.noise2(x / 80f, y / 80f) * 25f;
        w *= w / 25f;

        b += c1 + c2 + bn - w;

        return b;
    }

    private static float terracedBase(NoiseGenerator perlin, int x, int y, float baseHeight) {
        float step = 4f;
        float offset = perlin.noise2((x + 1877f) / 95f, (y - 2917f) / 95f) * 1.1f;
        float shifted = baseHeight - 62f + offset;
        float level = (float) Math.floor(shifted / step);
        float fraction = shifted / step - level;
        float stair = 62f + level * step - offset;
        // Preserve short ramps at each riser so surface painting does not produce one-block numerical chatter.
        if (fraction > .82f) stair += step * smoothstep((fraction - .82f) / .18f);
        return stair;
    }

    private static float smoothstep(float value) {
        value = Math.max(0f, Math.min(1f, value));
        return value * value * (3f - 2f * value);
    }
}
