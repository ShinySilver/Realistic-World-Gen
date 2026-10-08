package rwg.world.sample;

import rwg.registry.TerrainCategory;

/** Minimal coupled result of baseline river and terrain-category selection. */
public final class MorphologySample {

    public final TerrainCategory category;
    public final float categorySelector;
    public final float mountainStrength;
    public final float riverStrength;
    public final float riverDistance;
    public final float riverErosionDistance;
    /** Mountain distance before localized points of interest alter the regional silhouette. */
    public final float rawMountainDistance;
    /** Mountain distance after valley suppression; used by base-height and category morphology. */
    public final float mountainDistance;
    public final float valleyDistance;
    public final float valleyStrength;
    public final boolean plateauSide;

    public MorphologySample(TerrainCategory category, float categorySelector, float mountainStrength,
            float riverStrength, float mountainDistance, boolean plateauSide) {
        this(
                category,
                categorySelector,
                mountainStrength,
                riverStrength,
                riverStrength > 0f ? 0f : Float.POSITIVE_INFINITY,
                riverStrength > 0f ? 0f : Float.POSITIVE_INFINITY,
                mountainDistance,
                mountainDistance,
                Float.POSITIVE_INFINITY,
                0f,
                plateauSide);
    }

    public MorphologySample(TerrainCategory category, float categorySelector, float mountainStrength,
            float riverStrength, float riverDistance, float mountainDistance, boolean plateauSide) {
        this(
                category,
                categorySelector,
                mountainStrength,
                riverStrength,
                riverDistance,
                riverDistance,
                mountainDistance,
                mountainDistance,
                Float.POSITIVE_INFINITY,
                0f,
                plateauSide);
    }

    public MorphologySample(TerrainCategory category, float categorySelector, float mountainStrength,
            float riverStrength, float riverDistance, float riverErosionDistance, float rawMountainDistance,
            float mountainDistance, float valleyDistance, float valleyStrength, boolean plateauSide) {
        if (category == null) throw new NullPointerException("terrain category");
        this.category = category;
        this.categorySelector = categorySelector;
        this.mountainStrength = mountainStrength;
        this.riverStrength = riverStrength;
        this.riverDistance = riverDistance;
        this.riverErosionDistance = riverErosionDistance;
        this.rawMountainDistance = rawMountainDistance;
        this.mountainDistance = mountainDistance;
        this.valleyDistance = valleyDistance;
        this.valleyStrength = valleyStrength;
        this.plateauSide = plateauSide;
    }
}
