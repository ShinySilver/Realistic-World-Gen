package rwg.biomes.terrain;

import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;
import rwg.world.sample.TerrainContext;

/** General-purpose sharp escarpment with a narrow noisy transition between its lower and upper shelves. */
public final class TerrainGenericCliff extends TerrainBase {

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
        float face = perlin.noise2(x / 185f, z / 185f) + perlin.noise2((x + 1907f) / 48f, (z - 3371f) / 48f) * .18f;
        float transition = clamp(face * 7f + .5f);
        transition = transition * transition * (3f - 2f * transition);
        float upperShelf = 38f * transition * river;
        float detail = perlin.noise2(x / 31f, z / 31f) * (1.5f + transition * 2.5f);
        return baseHeight + upperShelf + detail * river;
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
