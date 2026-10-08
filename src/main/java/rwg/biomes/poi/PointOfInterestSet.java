package rwg.biomes.poi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable, append-only collection carried through selection until final composition. */
public final class PointOfInterestSet {

    public static final PointOfInterestSet EMPTY = new PointOfInterestSet(
            Collections.<PointOfInterestInstance>emptyList());

    public final List<PointOfInterestInstance> instances;

    private PointOfInterestSet(List<PointOfInterestInstance> instances) {
        this.instances = Collections.unmodifiableList(instances);
    }

    public PointOfInterestSet with(PointOfInterestInstance pointOfInterest) {
        if (pointOfInterest == null) throw new NullPointerException("point of interest");
        for (PointOfInterestInstance existing : instances) {
            if (existing.definition.id().equals(pointOfInterest.definition.id())
                    && existing.instanceKey == pointOfInterest.instanceKey)
                return this;
        }
        ArrayList<PointOfInterestInstance> result = new ArrayList<PointOfInterestInstance>(instances.size() + 1);
        result.addAll(instances);
        result.add(pointOfInterest);
        return new PointOfInterestSet(result);
    }

    public PointOfInterestInstance find(PointOfInterestDefinition definition) {
        for (PointOfInterestInstance pointOfInterest : instances)
            if (pointOfInterest.definition.id().equals(definition.id())) return pointOfInterest;
        return null;
    }
}
