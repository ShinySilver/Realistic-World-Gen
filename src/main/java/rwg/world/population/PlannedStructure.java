package rwg.world.population;

/** Immutable region-owned structure geometry, resolved entirely before world mutation. */
interface PlannedStructure {

    FeatureBounds bounds();

    /** Resolves surface-dependent geometry; {@code null} rejects an unsuitable site. */
    PlannedStructure resolve(SurfaceHeightMap surface);

    void place(CubePopulationContext context);
}
