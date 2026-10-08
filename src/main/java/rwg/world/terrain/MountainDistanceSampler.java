package rwg.world.terrain;

/** Narrow view of climate topology used while constructing cached points of interest. */
public interface MountainDistanceSampler {

    float distanceAt(int x, int z);

    float uncarvedBaseHeightAt(int x, int z, float mountainDistance);
}
