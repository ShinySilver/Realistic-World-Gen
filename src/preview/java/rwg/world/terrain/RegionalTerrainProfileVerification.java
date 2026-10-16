package rwg.world.terrain;

import rwg.biomes.terrain.TerrainBase;
import rwg.biomes.terrain.TerrainGenericHills;
import rwg.biomes.terrain.TerrainGenericMountain;
import rwg.registry.TerrainCategory;
import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;
import rwg.util.NoiseSelector;
import rwg.world.sample.MorphologySample;
import rwg.world.sample.TerrainContext;

/** Headless checks for the shared hill/mountain detail and coreward-steepening regional profile. */
public final class RegionalTerrainProfileVerification {

    private RegionalTerrainProfileVerification() {}

    public static void main(String[] args) {
        require(RegionalTerrainSampler.mountainEnvelope(0f) == 0f, "mountain foot must start flat");
        require(RegionalTerrainSampler.mountainEnvelope(1f) == 1f, "mountain core must retain its full height");
        float shelfStart = 1f - 325f / MorphologyRules.MOUNTAIN_INFLUENCE_RADIUS;
        float mountainStart = 1f - MorphologyRules.MOUNTAIN_RADIUS / MorphologyRules.MOUNTAIN_INFLUENCE_RADIUS;
        require(
                RegionalTerrainSampler.mountainEnvelope(shelfStart)
                        == RegionalTerrainSampler.mountainEnvelope(mountainStart),
                "inner hills must form a flat shelf before the mountain rise");
        float hillRise = RegionalTerrainSampler.mountainEnvelope(shelfStart)
                - RegionalTerrainSampler.mountainEnvelope(shelfStart - .05f);
        float mountainRise = RegionalTerrainSampler.mountainEnvelope(1f)
                - RegionalTerrainSampler.mountainEnvelope(.95f);
        require(mountainRise > hillRise, "mountain rise must be steeper than the hill rise");

        NoiseGenerator noise = NoiseSelector.createNoiseGenerator(0L);
        CellNoise cell = new CellNoise(0L, (short) 0, true);
        MorphologySample morphology = new MorphologySample(TerrainCategory.HILLS, 0f, .5f, 0f, 500f, false);
        TerrainContext context = new TerrainContext(0f, 112f, morphology);
        TerrainBase hills = new TerrainGenericHills();
        TerrainBase mountains = new TerrainGenericMountain();
        for (int x = -256; x <= 256; x += 32) {
            for (int z = -256; z <= 256; z += 32) {
                require(
                        hills.generateNoise(noise, cell, x, z, context, 1f)
                                == mountains.generateNoise(noise, cell, x, z, context, 1f),
                        "generic hill and mountain terrain diverged");
            }
        }
        System.out.println("RWG regional terrain profile verification passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
