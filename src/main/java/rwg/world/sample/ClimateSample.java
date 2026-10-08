package rwg.world.sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rwg.registry.Climate;
import rwg.registry.TerrainSubcategory;

/** Climate decision and the signals that produced it. */
public final class ClimateSample {

    public final Climate climate;
    /** Core climate or the directional border band selected by the climate topology. */
    public final TerrainSubcategory category;
    public final float selector;
    public final long regionKey;
    public final float incompatibleBoundaryDistance;
    /** True on either side when exactly one side of this mountain chain is plateau terrain. */
    public final boolean plateauBoundary;
    /** Smooth proximity to the nearest plateau-bearing mountain boundary. */
    public final float plateauBoundaryInfluence;
    public final boolean plateauSide;
    public final List<Neighbor> neighbors;

    public ClimateSample(Climate climate, float selector, long regionKey, float incompatibleBoundaryDistance,
            boolean plateauSide, List<Neighbor> neighbors) {
        this(
                climate,
                TerrainSubcategory.CORE,
                selector,
                regionKey,
                incompatibleBoundaryDistance,
                plateauSide,
                plateauSide ? 1f : 0f,
                plateauSide,
                neighbors);
    }

    public ClimateSample(Climate climate, float selector, long regionKey, float incompatibleBoundaryDistance,
            boolean plateauBoundary, boolean plateauSide, List<Neighbor> neighbors) {
        this(
                climate,
                TerrainSubcategory.CORE,
                selector,
                regionKey,
                incompatibleBoundaryDistance,
                plateauBoundary,
                plateauBoundary ? 1f : 0f,
                plateauSide,
                neighbors);
    }

    public ClimateSample(Climate climate, TerrainSubcategory category, float selector, long regionKey,
            float incompatibleBoundaryDistance, boolean plateauBoundary, float plateauBoundaryInfluence,
            boolean plateauSide, List<Neighbor> neighbors) {
        if (climate == null) throw new NullPointerException("climate");
        if (category == null) throw new NullPointerException("category");
        this.climate = climate;
        this.category = category;
        this.selector = selector;
        this.regionKey = regionKey;
        this.incompatibleBoundaryDistance = incompatibleBoundaryDistance;
        this.plateauBoundary = plateauBoundary;
        this.plateauBoundaryInfluence = plateauBoundaryInfluence;
        this.plateauSide = plateauSide;
        this.neighbors = neighbors.isEmpty() ? Collections.<Neighbor>emptyList()
                : Collections.unmodifiableList(new ArrayList<Neighbor>(neighbors));
    }

    public ClimateSample withClimate(Climate replacement) {
        return new ClimateSample(
                replacement,
                category,
                selector,
                regionKey,
                incompatibleBoundaryDistance,
                plateauBoundary,
                plateauBoundaryInfluence,
                plateauSide,
                neighbors);
    }

    /** A ranked neighboring climate candidate; the baseline selector initially leaves this list empty. */
    public static final class Neighbor {

        public final Climate climate;
        public final double distance;

        public Neighbor(Climate climate, double distance) {
            if (climate == null) throw new NullPointerException("climate");
            this.climate = climate;
            this.distance = distance;
        }
    }
}
