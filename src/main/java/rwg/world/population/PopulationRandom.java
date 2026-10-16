package rwg.world.population;

/** Small allocation-free SplitMix stream for coordinate-owned population candidates. */
final class PopulationRandom {

    private long state;

    void reset(long seed) {
        state = seed;
    }

    long nextLong() {
        long value = state += 0x9E3779B97F4A7C15L;
        value = (value ^ value >>> 30) * 0xBF58476D1CE4E5B9L;
        value = (value ^ value >>> 27) * 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }

    int nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("bound");
        return (int) Long.remainderUnsigned(nextLong(), bound);
    }

    double nextDouble() {
        return (nextLong() >>> 11) * 0x1.0p-53;
    }

    static long seed(long worldSeed, long stageSeed, int cubeX, int cubeY, int cubeZ) {
        long value = worldSeed ^ stageSeed;
        value = mix(value ^ (long) cubeX * 0x4F9939F508L);
        value = mix(value ^ (long) cubeY * 0x6A09E667F3BCC909L);
        return mix(value ^ (long) cubeZ * 0x1EF1565BD5L);
    }

    private static long mix(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }
}
