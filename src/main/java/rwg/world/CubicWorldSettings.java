package rwg.world;

import com.cardinalstar.cubicchunks.api.IntRange;

/** Vertical constants shared by the cubic world type and terrain pipeline. */
public final class CubicWorldSettings {

    public static final int MIN_GENERATION_Y = -1024;
    public static final int MAX_GENERATION_Y = 2048;
    public static final int SEA_LEVEL = 63;
    static final IntRange GENERATION_RANGE = new IntRange(MIN_GENERATION_Y, MAX_GENERATION_Y);

    private CubicWorldSettings() {}
}
