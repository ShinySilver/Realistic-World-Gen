package rwg.util;

/** Construction boundary for the fixed world-generation noise backend. */
public final class NoiseSelector {

    private NoiseSelector() {}

    public static NoiseGenerator createNoiseGenerator(long seed) {
        return new PerlinNoise(seed);
    }
}
