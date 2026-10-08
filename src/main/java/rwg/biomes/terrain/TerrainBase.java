package rwg.biomes.terrain;

import rwg.registry.TerrainCategory;
import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;
import rwg.world.sample.TerrainContext;

public abstract class TerrainBase {

    /** Context-aware entry point used by current world generation and preview sampling. */
    public float generateNoise(NoiseGenerator perlin, CellNoise cell, int x, int y, TerrainContext context,
            float border) {
        // Existing terrain implementations use this argument as a terrain-amplitude multiplier. Preserve the
        // narrow legacy behavior for plains, but let raised/category terrain begin yielding across the old broad
        // river corridor before the final channel carver runs.
        float river = context.category == TerrainCategory.PLAIN ? 1f - context.riverStrength
                : context.riverTerrainFactor;
        return generateNoise(perlin, cell, x, y, context.ocean, border, river);
    }

    /** Legacy implementation hook retained for existing terrain implementations and integrations. */
    public float generateNoise(NoiseGenerator perlin, CellNoise cell, int x, int y, float ocean, float border,
            float river) {
        return 70f;
    }
}
