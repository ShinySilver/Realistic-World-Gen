package rwg.biomes.terrain;

import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;
import rwg.world.sample.TerrainContext;

/** General-purpose mesa-like plateau using the legacy border threshold for its upper shelf. */
public class TerrainGenericPlateau extends TerrainBase {

    @Override
    public float generateNoise(NoiseGenerator perlin, CellNoise cell, int x, int z, TerrainContext context,
            float border) {
        return generateFromBase(perlin, cell, x, z, context.baseHeight, border, context.riverTerrainFactor);
    }

    @Override
    public float generateNoise(NoiseGenerator perlin, CellNoise cell, int x, int z, float ocean, float border,
            float river) {
        return generateFromBase(perlin, cell, x, z, 74f, border, river);
    }

    private float generateFromBase(NoiseGenerator perlin, CellNoise cell, int x, int z, float baseHeight, float border,
            float river) {
        float mass = perlin.noise2(x / 170f, z / 170f) * 42f;
        mass *= mass / 40f;
        float thresholdNoise = perlin.noise2(x / 12f, z / 12f);
        float shelf = 0f;
        if (mass > 5.5f && border > .95f + thresholdNoise * .09f) {
            shelf = Math.min(.5f, mass - 5.5f) * 34f;
        }
        float lowerTerrace = mass > 1f ? Math.min(4.5f, mass - 1f) * 2.5f : 0f;
        float detail = perlin.noise2((x + 1907f) / 70f, (z - 3371f) / 70f) * 2f + thresholdNoise * 1.2f;
        return baseHeight + (mass + lowerTerrace + shelf + detail) * river;
    }
}
