package rwg.world.terrain;

import rwg.registry.TerrainCategory;

/** Shared conversion from climate/coast distances to terrain morphology. */
public final class MorphologyRules {

    public static final float MOUNTAIN_RADIUS = 250f;
    public static final float SURFACE_RIVER_MOUNTAIN_FADE_START = 175f;
    public static final float SURFACE_RIVER_MOUNTAIN_FADE_END = 325f;
    public static final float MOUNTAIN_INFLUENCE_RADIUS = 750f;
    public static final float HILL_MINIMUM_BASE_HEIGHT = 75f;
    public static final float FULL_SURFACE_RIVER_HEIGHT = HILL_MINIMUM_BASE_HEIGHT;
    public static final float MOUNTAIN_CARVER_ONLY_HEIGHT = 90f;
    public static final float MAXIMUM_SURFACE_RIVER_WIDTH = 30f;
    public static final float CANYON_RADIUS = 208f;
    public static final float CANYON_EROSION_WIDTH = 112f;
    public static final float CANYON_EROSION_BLEND_START = 150f;
    public static final float PLATEAU_BOUNDARY_INFLUENCE_START = MOUNTAIN_RADIUS;
    public static final float PLATEAU_BOUNDARY_INFLUENCE_END = MOUNTAIN_INFLUENCE_RADIUS;
    public static final float VALLEY_MOUNTAIN_DISTANCE_OFFSET = 650f;
    public static final float VALLEY_CATEGORY_STRENGTH = .35f;
    public static final float COASTAL_FADE_DISTANCE = 432f;
    public static final float CLIFF_WIDTH = COASTAL_FADE_DISTANCE * .25f;
    public static final float COASTAL_MOUNTAIN_EDGE_INSET = (COASTAL_FADE_DISTANCE + MOUNTAIN_RADIUS) * 1.3f;

    private MorphologyRules() {}

    public static float effectiveMountainDistance(float climateDistance, float continentDistance) {
        if (continentDistance < 0f) return Float.POSITIVE_INFINITY;
        return climateDistance;
    }

    public static TerrainCategory category(float mountainDistance, boolean plateauSide) {
        if (mountainDistance <= MOUNTAIN_RADIUS) return TerrainCategory.MOUNTAIN;
        if (mountainDistance <= MOUNTAIN_INFLUENCE_RADIUS)
            return plateauSide ? TerrainCategory.PLATEAU : TerrainCategory.HILLS;
        return TerrainCategory.PLAIN;
    }

    public static float mountainStrength(float mountainDistance) {
        return mountainDistance < MOUNTAIN_INFLUENCE_RADIUS ? 1f - mountainDistance / MOUNTAIN_INFLUENCE_RADIUS : 0f;
    }

    public static float valleyAdjustedMountainDistance(float rawMountainDistance, float valleyStrength) {
        if (!Float.isFinite(rawMountainDistance)) return rawMountainDistance;
        return rawMountainDistance + valleyStrength * VALLEY_MOUNTAIN_DISTANCE_OFFSET;
    }

    public static boolean isRaised(float mountainDistance) {
        return mountainDistance <= MOUNTAIN_INFLUENCE_RADIUS;
    }

    public static boolean isValley(float rawMountainDistance, float valleyStrength) {
        return isRaised(rawMountainDistance) && valleyStrength >= VALLEY_CATEGORY_STRENGTH;
    }

    public static boolean isCanyon(float mountainCarvingRiverDistance) {
        return mountainCarvingRiverDistance < CANYON_RADIUS;
    }

    /** Smoothly narrows broad river erosion after a canyon enters the mountain core. */
    public static float canyonErosionStrength(float rawMountainDistance) {
        float normalized = Math.max(
                0f,
                Math.min(1f, (MOUNTAIN_RADIUS - rawMountainDistance) / (MOUNTAIN_RADIUS - CANYON_EROSION_BLEND_START)));
        return normalized * normalized * (3f - 2f * normalized);
    }

    /** Extends plateau-bearing canyon behavior smoothly through neighboring climate-edge junctions. */
    public static float plateauBoundaryInfluence(float plateauBoundaryDistance) {
        float normalized = Math.max(
                0f,
                Math.min(
                        1f,
                        (PLATEAU_BOUNDARY_INFLUENCE_END - plateauBoundaryDistance)
                                / (PLATEAU_BOUNDARY_INFLUENCE_END - PLATEAU_BOUNDARY_INFLUENCE_START)));
        return normalized * normalized * (3f - 2f * normalized);
    }

    public static float surfaceRiverErosionSuppression(float rawMountainDistance, boolean valley) {
        if (valley) return 0f;
        float normalized = Math.max(
                0f,
                Math.min(
                        1f,
                        (rawMountainDistance - SURFACE_RIVER_MOUNTAIN_FADE_START)
                                / (SURFACE_RIVER_MOUNTAIN_FADE_END - SURFACE_RIVER_MOUNTAIN_FADE_START)));
        float smooth = normalized * normalized * (3f - 2f * normalized);
        return 1f - smooth;
    }

    /** Fades ordinary river cores out with altitude while retaining designated mountain carvers. */
    public static float surfaceRiverMountainSuppression(float uncarvedHeight, boolean valley) {
        if (valley || uncarvedHeight <= FULL_SURFACE_RIVER_HEIGHT) return 0f;
        if (uncarvedHeight >= MOUNTAIN_CARVER_ONLY_HEIGHT) return 1f;
        float normalized = (uncarvedHeight - FULL_SURFACE_RIVER_HEIGHT)
                / (MOUNTAIN_CARVER_ONLY_HEIGHT - FULL_SURFACE_RIVER_HEIGHT);
        return normalized * normalized * (3f - 2f * normalized);
    }
}
