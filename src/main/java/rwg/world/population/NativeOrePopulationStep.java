package rwg.world.population;

/**
 * Stable dependency slot for native ores.
 *
 * <p>
 * Intentionally empty until RWG has region-owned large-vein plans. Do not fill this with independent vanilla-sized cube
 * deposits: large veins must be planned once from world seed and region coordinates, expose immutable bounds, and be
 * clipped into each intersecting cube so request order and cache eviction cannot change their shape.
 */
final class NativeOrePopulationStep implements PopulationStep {

    @Override
    public void populate(CubePopulationContext context) {}
}
