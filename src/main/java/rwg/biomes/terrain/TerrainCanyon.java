package rwg.biomes.terrain;

import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;
import rwg.world.sample.TerrainContext;

public class TerrainCanyon extends TerrainBase {

    static final float STEP_DATUM = 69f;
    private static final float MAXIMUM_SCULPTED_HEIGHT = 245f;
    private static final float[] CORE_STEPS = { 5.0f, 0.5f, 12.5f, 0.5f, 18.0f, 0.5f, 40.0f, 0.5f, 70.0f, 0.5f, 105.0f,
            0.5f, 145.0f, 0.5f, 175.0f, 0.5f };

    private boolean smallRiver;
    private float strength;
    private float cWidth;
    private float cHeigth;
    private float cStrength;
    private float base;

    /*
     * Example parameters: allowed to generate rivers? riverGen = true canyon jump heights heightArray = new
     * float[]{2.0f, 0.5f, 6.5f, 0.5f, 14.0f, 0.5f, 19.0f, 0.5f} strength of canyon jump heights heightStrength = 35f
     * canyon width (cliff to cliff) canyonWidth = 160f canyon heigth (total heigth) canyonHeight = 60f canyon strength
     * canyonStrength = 40f
     */
    public TerrainCanyon(boolean riverGen, float heightStrength, float canyonWidth, float canyonHeight,
            float canyonStrength, float baseHeight) {
        smallRiver = riverGen;
        strength = heightStrength;
        cWidth = canyonWidth;
        cHeigth = canyonHeight;
        cStrength = canyonStrength;
        base = baseHeight;
    }

    @Override
    public float generateNoise(NoiseGenerator perlin, CellNoise cell, int x, int y, TerrainContext context,
            float border) {
        return generateFromBase(perlin, x, y, border, context.riverTerrainFactor, context.baseHeight);
    }

    @Override
    public float generateNoise(NoiseGenerator perlin, CellNoise cell, int x, int y, float ocean, float border,
            float river) {
        return generateFromBase(perlin, x, y, border, river, base);
    }

    private float generateFromBase(NoiseGenerator perlin, int x, int y, float border, float river, float baseHeight) {
        // float b = perlin.noise2(x / cWidth, y / cWidth) * cHeigth * river;
        // b *= b / cStrength;
        river *= 1.3f;
        river = river > 1f ? 1f : river;
        float r = perlin.noise2(x / 100f, y / 100f) * 50f;
        r = r < -7.4f ? -7.4f : r > 7.4f ? 7.4f : r;
        float b = (17f + r) * river;

        float hn = perlin.noise2(x / 12f, y / 12f) * 0.5f;
        float sb = 0f;
        if (b > 0f) {
            sb = b;
            sb = sb < 0f ? 0f : sb > 7f ? 7f : sb;
            sb = hn * sb;
        }
        b += sb;

        float cTotal = coreStepHeight(baseHeight - STEP_DATUM + b, border, hn, strength);

        float bn = 0f;
        if (smallRiver) {
            if (b < 5f) {
                bn = 5f - b;
                for (int i = 0; i < 3; i++) {
                    bn *= bn / 4.5f;
                }
            }
        } else if (b < 5f) {
            bn = (perlin.noise2(x / 7f, y / 7f) * 1.3f + perlin.noise2(x / 15f, y / 15f) * 2f) * (5f - b) * 0.2f;
        }

        b += cTotal - bn;

        return reboundHeight(baseHeight + b);
    }

    static float coreStepHeight(float terrainHeight, float border, float stepNoise, float stepStrength) {
        float result = 0f;
        for (int i = 0; i < CORE_STEPS.length; i += 2) {
            float borderThreshold = 0.6f + Math.min(CORE_STEPS[i], 18f) * 0.015f + stepNoise * 0.2f;
            if (terrainHeight > CORE_STEPS[i] && border > borderThreshold) {
                float rise = Math.min(CORE_STEPS[i + 1], terrainHeight - CORE_STEPS[i]);
                result += rise * stepStrength;
            }
        }
        return result;
    }

    static float reboundHeight(float height) {
        return height > MAXIMUM_SCULPTED_HEIGHT ? MAXIMUM_SCULPTED_HEIGHT * 2f - height : height;
    }
}
