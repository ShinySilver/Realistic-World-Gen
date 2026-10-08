package rwg.world.terrain;

import rwg.registry.TerrainCategory;
import rwg.util.NoiseGenerator;
import rwg.world.sample.ClimateSample;
import rwg.world.sample.MorphologySample;
import rwg.world.sample.RegionalSample;

/** Broad category-owned terrain silhouette, independent from biome-specific detail terrain. */
public final class RegionalTerrainSampler {

    private final NoiseGenerator noise;

    public RegionalTerrainSampler(NoiseGenerator noise) {
        if (noise == null) throw new NullPointerException("regional terrain noise");
        this.noise = noise;
    }

    public RegionalSample sample(int x, int z, float continent, boolean ocean, ClimateSample climate,
            MorphologySample morphology, TerrainCategory host) {
        return sample(x, z, continent, ocean, climate, morphology, host, Float.NaN);
    }

    public RegionalSample sample(int x, int z, float continent, boolean ocean, ClimateSample climate,
            MorphologySample morphology, TerrainCategory host, float knownUncarvedHeight) {
        if (ocean) {
            float height = oceanHeight(x, z, continent);
            return new RegionalSample(height, morphology.mountainDistance, height);
        }

        float canyonStrength = host == TerrainCategory.CANYON
                ? MorphologyRules.canyonErosionStrength(morphology.rawMountainDistance)
                        * climate.plateauBoundaryInfluence
                : 0f;
        float riverErodedMountainDistance = riverErodedMountainDistance(
                morphology.mountainDistance,
                morphology.riverErosionDistance,
                canyonStrength);
        float uncarvedHeight = Float.isNaN(knownUncarvedHeight)
                ? uncarvedLandHeight(x, z, continent, morphology.mountainDistance)
                : knownUncarvedHeight;
        float base = riverErodedMountainDistance == morphology.mountainDistance ? uncarvedHeight
                : uncarvedLandHeight(x, z, continent, riverErodedMountainDistance);
        float broad = noise.noise2(x / 310f, z / 310f);
        float medium = noise.noise2((x + 1907f) / 115f, (z - 3371f) / 115f);

        switch (host) {
            case HILLS:
            case MOUNTAIN:
            case CLIFF:
            case PLATEAU:
            case CANYON:
                break;
            case WETLANDS:
            case SWAMP:
                base = 63f + broad * 1.2f + medium * .6f;
                break;
            case SMALL_ISLAND:
            case MEDIUM_ISLAND:
            case LARGE_ISLAND:
                base += 3f + broad * 5f;
                break;
            default:
                break;
        }

        return new RegionalSample(uncarvedHeight, riverErodedMountainDistance, base);
    }

    /** Regional land silhouette before category-specific adjustments, valleys, or river carving. */
    public float uncarvedLandHeight(int x, int z, float continent, float mountainDistance) {
        float coast = smooth(clamp(continent / 180f));
        float broad = noise.noise2(x / 310f, z / 310f);
        float medium = noise.noise2((x + 1907f) / 115f, (z - 3371f) / 115f);
        float base = 62f + coast * 8f + broad * (1.5f + coast * 1.5f);
        float mountain = clamp(MorphologyRules.mountainStrength(mountainDistance));
        if (mountain > 0f) {
            float envelope = mountainEnvelope(mountain);
            base += (250f - base) * envelope;
            // Confine detail to the flanks so it cannot blunt the regional ridge line.
            float transition = (float) Math.sin(Math.PI * mountain);
            transition *= transition;
            float ridge = 1f - Math.abs(noise.noise2((x - 811f) / 165f, (z + 1291f) / 165f));
            base += transition * ((ridge * ridge - .35f) * 8f + medium * 2f);
        }
        return base;
    }

    /** Low-gradient outer foot that reaches the same ridge height with increasing slope toward the core. */
    static float mountainEnvelope(float strength) {
        return strength * strength;
    }

    private float oceanHeight(int x, int z, float continent) {
        float depth = clamp(-continent / 300f);
        float height = 62f + (34f - 62f) * smooth(depth);
        return height + noise.noise2(x / 220f, z / 220f) * 2.5f + noise.noise2(x / 55f, z / 55f);
    }

    /** Broadly removes regional uplift around rivers before the narrow channel itself is cut. */
    private static float riverErodedMountainDistance(float mountainDistance, float riverDistance,
            float canyonStrength) {
        float hillBlend = 1f - smoothstep(650f, MorphologyRules.MOUNTAIN_INFLUENCE_RADIUS, mountainDistance);
        float mountainBlend = 1f - smoothstep(175f, 325f, mountainDistance);
        float width = lerp(208f, 240f, hillBlend);
        width = lerp(width, 208f, mountainBlend);
        width = lerp(width, MorphologyRules.CANYON_EROSION_WIDTH, canyonStrength);
        if (riverDistance >= width) return mountainDistance;
        float influence = smoother(1f - riverDistance / width);
        return mountainDistance + influence * MorphologyRules.VALLEY_MOUNTAIN_DISTANCE_OFFSET;
    }

    private static float smooth(float value) {
        return value * value * (3f - 2f * value);
    }

    private static float smoother(float value) {
        return value * value * value * (value * (value * 6f - 15f) + 10f);
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        return smooth(clamp((value - edge0) / (edge1 - edge0)));
    }

    private static float lerp(float first, float second, float amount) {
        return first + (second - first) * amount;
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
