package rwg.biomes.surface;

/** Immutable, height-independent material rule for one terrain column. */
public interface SurfaceRule {

    /** Deepest block below the surface that this rule may replace. */
    int maximumDepth();

    /** Selects the material at an absolute surface-relative depth without reading the world. */
    SurfaceMaterial materialAt(int worldX, int worldZ, int surfaceY, int depth, float riverStrength,
            float mountainWeight, float slope, float variation, float broadVariation);
}
