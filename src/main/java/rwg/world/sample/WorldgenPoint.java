package rwg.world.sample;

/** Immutable identity of one horizontal world-generation column. */
public final class WorldgenPoint {

    public final long seed;
    public final int x;
    public final int z;

    public WorldgenPoint(long seed, int x, int z) {
        this.seed = seed;
        this.x = x;
        this.z = z;
    }
}
