package rwg.world.sample;

import rwg.biomes.poi.PointOfInterestSet;

/** Complete horizontal selection result shared by world generation, biome queries, and the preview. */
public final class ColumnSample {

    public final WorldgenPoint point;
    public final ContinentalSample continent;
    public final ClimateSample climate;
    public final PointOfInterestSet pointsOfInterest;
    public final MorphologySample morphology;
    public final RegionalSample regional;
    public final BiomeSample biome;

    public ColumnSample(WorldgenPoint point, ContinentalSample continent, ClimateSample climate,
            PointOfInterestSet pointsOfInterest, MorphologySample morphology, RegionalSample regional,
            BiomeSample biome) {
        if (point == null || continent == null
                || climate == null
                || pointsOfInterest == null
                || morphology == null
                || regional == null
                || biome == null)
            throw new NullPointerException("column sample stages");
        this.point = point;
        this.continent = continent;
        this.climate = climate;
        this.pointsOfInterest = pointsOfInterest;
        this.morphology = morphology;
        this.regional = regional;
        this.biome = biome;
    }
}
