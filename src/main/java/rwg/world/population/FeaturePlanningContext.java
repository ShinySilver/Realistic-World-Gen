package rwg.world.population;

import rwg.world.ColumnPlan;
import rwg.world.CubicWorldSettings;

/** Pure inputs for planning one blended biome feature. */
public final class FeaturePlanningContext {

    private final long seed;
    private final int columnX;
    private final int columnZ;
    private final int featureIndex;
    private final ColumnPlan column;
    private final float strength;
    private final float river;
    private final int surfaceY;

    public FeaturePlanningContext(long seed, int columnX, int columnZ, int featureIndex, ColumnPlan column,
            float strength, float river) {
        if (column == null) throw new NullPointerException("column");
        this.seed = seed;
        this.columnX = columnX;
        this.columnZ = columnZ;
        this.featureIndex = featureIndex;
        this.column = column;
        this.strength = strength;
        this.river = river;
        surfaceY = (int) Math.floor(column.height(8, 8));
    }

    public float strength() {
        return strength;
    }

    public float river() {
        return river;
    }

    public int ownerCubeY() {
        return Math.floorDiv(surfaceY, 16);
    }

    public long featureSeed() {
        long value = seed ^ 0x9E3779B97F4A7C15L;
        value = mix(value ^ columnX * 0x4F9939F508L);
        value = mix(value ^ columnZ * 0x1EF1565BD5L);
        return mix(value ^ featureIndex * 0xD1B54A32D192ED03L);
    }

    public FeatureBounds surfaceFeatureBounds(int horizontalReach, int heightAboveSurface) {
        if (horizontalReach < 0 || heightAboveSurface < 0) throw new IllegalArgumentException("negative feature reach");
        int originX = columnX << 4;
        int originZ = columnZ << 4;
        int minimumSurface = column.minimumSurfaceY();
        int maximumSurface = column.maximumSurfaceY();
        return new FeatureBounds(
                originX,
                originZ,
                originX + 8 - horizontalReach,
                Math.max(CubicWorldSettings.MIN_GENERATION_Y, minimumSurface - 8),
                originZ + 8 - horizontalReach,
                originX + 23 + horizontalReach,
                Math.min(CubicWorldSettings.MAX_GENERATION_Y - 1, maximumSurface + heightAboveSurface),
                originZ + 23 + horizontalReach);
    }

    private static long mix(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }
}
