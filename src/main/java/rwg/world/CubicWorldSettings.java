package rwg.world;

import com.cardinalstar.cubicchunks.api.IntRange;

/** Vertical constants shared by the cubic world type and terrain pipeline. */
final class CubicWorldSettings {

    static final int MIN_GENERATION_Y = -1024;
    static final int MAX_GENERATION_Y = 2048;
    static final int SEA_LEVEL = 63;
    static final IntRange GENERATION_RANGE = new IntRange(MIN_GENERATION_Y, MAX_GENERATION_Y);

    private CubicWorldSettings() {}
}
