package rwg.world.terrain;

/** Seed-bound river morphology sampler. */
public final class RiverSampler {

    private static final long RIVER_SALT = 0x29D87F0A47E5C319L;
    private final RiverNetwork rivers;

    public RiverSampler(long seed) {
        this(seed, null);
    }

    public RiverSampler(long seed, MountainDistanceSampler mountains) {
        rivers = new RiverNetwork(seed ^ RIVER_SALT, mountains);
    }

    public float riverStrength(int x, int z) {
        return riverStrength(x, z, 0L, false);
    }

    public float riverStrength(int x, int z, long riverNetworkSeed, boolean suppressSurface) {
        return rivers.strength(x, z, riverNetworkSeed, suppressSurface);
    }

    public float riverDistance(int x, int z, long riverNetworkSeed, boolean suppressSurface) {
        return rivers.distance(x, z, riverNetworkSeed, suppressSurface);
    }

    public long surfaceRiverSample(int x, int z, long riverNetworkSeed, float mountainSuppression) {
        return rivers.surfaceSample(x, z, riverNetworkSeed, mountainSuppression);
    }

    public static float surfaceRiverDistance(long sample) {
        return Float.intBitsToFloat((int) (sample >>> 32));
    }

    public static float surfaceRiverStrength(long sample) {
        return Float.intBitsToFloat((int) sample);
    }

    public float riverErosionDistance(int x, int z, long riverNetworkSeed, float mountainSuppression) {
        return rivers.erosionDistance(x, z, riverNetworkSeed, mountainSuppression);
    }

    /** Distance to the nearest river edge selected by the edge-level 50/50 mountain-carving decision. */
    public float mountainCarvingRiverDistance(int x, int z, long riverNetworkSeed) {
        return rivers.erosionDistance(x, z, riverNetworkSeed, 1f);
    }

    public float riverStrength(float distance) {
        return RiverNetwork.strength(distance);
    }

    public float junctionDistance(int x, int z, long riverNetworkSeed) {
        return rivers.junctionDistance(x, z, riverNetworkSeed);
    }

    public float junctionStrength(float distance) {
        return RiverNetwork.junctionStrength(distance);
    }

}
