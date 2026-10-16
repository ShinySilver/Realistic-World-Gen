package rwg.biomes.surface;

/** Immutable registration-time composition; later rules override only when they return a material. */
public final class SurfaceRuleSequence implements SurfaceRule {

    private final SurfaceRule[] rules;
    private final int maximumDepth;

    public SurfaceRuleSequence(SurfaceRule[] rules) {
        if (rules == null || rules.length == 0) throw new IllegalArgumentException("surface rules");
        this.rules = rules.clone();
        int depth = 0;
        for (SurfaceRule rule : this.rules) {
            if (rule == null) throw new NullPointerException("surface rule");
            depth = Math.max(depth, rule.maximumDepth());
        }
        maximumDepth = depth;
    }

    @Override
    public int maximumDepth() {
        return maximumDepth;
    }

    @Override
    public SurfaceMaterial materialAt(int worldX, int worldZ, int surfaceY, int depth, float riverStrength,
            float mountainWeight, float slope, float variation, float broadVariation) {
        SurfaceMaterial selected = null;
        for (SurfaceRule rule : rules) {
            SurfaceMaterial candidate = rule.materialAt(
                    worldX,
                    worldZ,
                    surfaceY,
                    depth,
                    riverStrength,
                    mountainWeight,
                    slope,
                    variation,
                    broadVariation);
            if (candidate != null) selected = candidate;
        }
        return selected;
    }
}
