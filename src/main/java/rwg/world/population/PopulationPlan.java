package rwg.world.population;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rwg.world.ColumnPlan;
import rwg.world.ColumnPlanCache;
import rwg.world.CubicWorldSettings;

/** Immutable feature work owned by one X/Z column. */
public final class PopulationPlan {

    private final List<PlannedFeature> features;
    private final List<PlannedStructure> structures;
    private final SurfaceHeightMap surfaceHeights;

    PopulationPlan(List<PlannedFeature> features, List<PlannedStructure> structures) {
        this(features, structures, SurfaceHeightMap.empty());
    }

    private PopulationPlan(List<PlannedFeature> features, List<PlannedStructure> structures,
            SurfaceHeightMap surfaceHeights) {
        this.features = Collections.unmodifiableList(new ArrayList<PlannedFeature>(features));
        this.structures = Collections.unmodifiableList(new ArrayList<PlannedStructure>(structures));
        this.surfaceHeights = surfaceHeights;
    }

    public List<PlannedFeature> features() {
        return features;
    }

    List<PlannedStructure> structures() {
        return structures;
    }

    SurfaceHeightLookup surfaceHeights() {
        return new SurfaceHeightLookup(surfaceHeights);
    }

    PopulationPlan resolveSurfaceHeights(ColumnPlanCache columns) {
        if (features.isEmpty() && structures.isEmpty()) return this;
        int minimumX = Integer.MAX_VALUE;
        int minimumZ = Integer.MAX_VALUE;
        int maximumX = Integer.MIN_VALUE;
        int maximumZ = Integer.MIN_VALUE;
        for (PlannedFeature feature : features) {
            minimumX = Math.min(minimumX, feature.bounds().minimumX);
            minimumZ = Math.min(minimumZ, feature.bounds().minimumZ);
            maximumX = Math.max(maximumX, feature.bounds().maximumX);
            maximumZ = Math.max(maximumZ, feature.bounds().maximumZ);
        }
        for (PlannedStructure structure : structures) {
            minimumX = Math.min(minimumX, structure.bounds().minimumX);
            minimumZ = Math.min(minimumZ, structure.bounds().minimumZ);
            maximumX = Math.max(maximumX, structure.bounds().maximumX);
            maximumZ = Math.max(maximumZ, structure.bounds().maximumZ);
        }
        int width = maximumX - minimumX + 1;
        int depth = maximumZ - minimumZ + 1;
        int[] heights = new int[width * depth];
        for (int blockX = minimumX; blockX <= maximumX; blockX++) {
            int columnX = Math.floorDiv(blockX, ColumnPlan.SIZE);
            int localX = Math.floorMod(blockX, ColumnPlan.SIZE);
            for (int blockZ = minimumZ; blockZ <= maximumZ; blockZ++) {
                int columnZ = Math.floorDiv(blockZ, ColumnPlan.SIZE);
                int localZ = Math.floorMod(blockZ, ColumnPlan.SIZE);
                ColumnPlan column = columns.get(columnX, columnZ);
                heights[(blockX - minimumX) * depth + blockZ - minimumZ] = (int) Math
                        .floor(column.height(localX, localZ));
            }
        }
        SurfaceHeightMap surfaceMap = new SurfaceHeightMap(minimumX, minimumZ, width, depth, heights);
        List<PlannedFeature> resolved = new ArrayList<PlannedFeature>(features.size());
        for (PlannedFeature feature : features) {
            if (!(feature instanceof FeatureInvocation)) {
                resolved.add(feature);
                continue;
            }
            FeatureBounds bounds = feature.bounds();
            int resolvedMinimum = Math.max(CubicWorldSettings.MIN_GENERATION_Y, surfaceMap.minimum(bounds) - 8);
            int resolvedMaximum = Math.min(CubicWorldSettings.MAX_GENERATION_Y - 1, surfaceMap.maximum(bounds) + 40);
            resolved.add(
                    ((FeatureInvocation) feature)
                            .withBounds(bounds.withVerticalRange(resolvedMinimum, resolvedMaximum)));
        }
        List<PlannedStructure> resolvedStructures = new ArrayList<PlannedStructure>(structures.size());
        for (PlannedStructure structure : structures) {
            PlannedStructure resolvedStructure = structure.resolve(surfaceMap);
            if (resolvedStructure != null) resolvedStructures.add(resolvedStructure);
        }
        return new PopulationPlan(resolved, resolvedStructures, surfaceMap);
    }
}
