package rwg.world;

import java.util.List;
import java.util.Random;

import com.cardinalstar.cubicchunks.world.worldgen.MapGenCavesCubic;

/** CubicChunks' cached cave-feature generator, bounded to RWG's useful underground band. */
final class RWGCubicCaveGenerator extends MapGenCavesCubic {

    static final int MINIMUM_SEED_CUBE_Y = -8;
    static final int MAXIMUM_SEED_CUBE_Y = 12;
    static final int FEATURE_RANGE_CUBES = 8;

    static boolean mayIntersect(int cubeY) {
        return cubeY >= MINIMUM_SEED_CUBE_Y - FEATURE_RANGE_CUBES
                && cubeY <= MAXIMUM_SEED_CUBE_Y + FEATURE_RANGE_CUBES;
    }

    @Override
    protected void getSeedsImpl(Random rng, int cubeX, int cubeY, int cubeZ, List<CaveSeed> seeds) {
        if (cubeY < MINIMUM_SEED_CUBE_Y || cubeY > MAXIMUM_SEED_CUBE_Y) return;
        super.getSeedsImpl(rng, cubeX, cubeY, cubeZ, seeds);
    }
}
