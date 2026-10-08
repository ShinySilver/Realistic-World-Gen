package rwg.world.sample;

import rwg.registry.TerrainCategory;

/** Shared environmental inputs supplied to terrain implementations for one horizontal column. */
public final class TerrainContext {

    public final float ocean;
    public final float baseHeight;
    public final float riverStrength;
    /** Legacy-style broad terrain fade: zero on the river axis and one outside its land-shaping corridor. */
    public final float riverTerrainFactor;
    public final float mountainDistance;
    public final float mountainStrength;
    public final boolean plateauSide;
    public final TerrainCategory category;

    public TerrainContext(float ocean, float baseHeight, MorphologySample morphology) {
        this(ocean, baseHeight, morphology, morphology.category);
    }

    public TerrainContext(float ocean, float baseHeight, MorphologySample morphology, TerrainCategory category) {
        if (morphology == null) throw new NullPointerException("morphology");
        if (category == null) throw new NullPointerException("terrain category");
        this.ocean = ocean;
        this.baseHeight = baseHeight;
        riverStrength = morphology.riverStrength;
        float riverFade = Math.max(0f, Math.min(1f, morphology.riverDistance / 50f));
        riverTerrainFactor = riverFade * riverFade * (3f - 2f * riverFade);
        mountainDistance = morphology.mountainDistance;
        mountainStrength = morphology.mountainStrength;
        plateauSide = morphology.plateauSide;
        this.category = category;
    }
}
