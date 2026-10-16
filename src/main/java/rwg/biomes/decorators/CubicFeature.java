package rwg.biomes.decorators;

import java.util.Random;

import net.minecraft.world.World;

import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;
import rwg.world.population.FeatureBounds;
import rwg.world.population.FeatureInvocation;
import rwg.world.population.FeaturePlanningContext;
import rwg.world.population.PlannedFeature;
import rwg.world.population.PopulationStage;
import rwg.world.population.SurfaceHeightLookup;

/**
 * A biome feature split into immutable planning and world-mutating placement.
 *
 * <p>
 * The default plan is one deterministic invocation owned by the column's surface cube. Implementations only describe
 * their legacy-visible placement recipe; scheduling, bounds, and random seeding are centralized.
 */
public abstract class CubicFeature {

    /** Returns {@code null} when this blended biome contribution has no work. */
    public PlannedFeature plan(FeaturePlanningContext context) {
        if (context.strength() <= 0f) return null;
        FeatureBounds bounds = context.surfaceFeatureBounds(maximumHorizontalReach(), maximumHeightAboveSurface());
        return new FeatureInvocation(
                this,
                bounds,
                context.ownerCubeY(),
                stage(),
                context.featureSeed(),
                context.strength(),
                context.river());
    }

    public PopulationStage stage() {
        return PopulationStage.SURFACE_FEATURES;
    }

    /** Conservative reach from the vanilla +8 population window, including tree crowns and leaning logs. */
    protected int maximumHorizontalReach() {
        return 12;
    }

    protected int maximumHeightAboveSurface() {
        return 40;
    }

    public void place(World world, PlannedFeature feature, SurfaceHeightLookup surface, NoiseGenerator noise,
            CellNoise cell, float strength, float river) {
        Random random = new Random(feature.seed());
        place(
                world,
                random,
                feature.bounds().originBlockX(),
                feature.bounds().originBlockZ(),
                surface,
                noise,
                cell,
                strength,
                river);
    }

    protected abstract void place(World world, Random random, int chunkX, int chunkZ, SurfaceHeightLookup surface,
            NoiseGenerator noise, CellNoise cell, float strength, float river);
}
