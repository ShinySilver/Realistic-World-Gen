package rwg.world.population;

import net.minecraft.world.World;

import com.cardinalstar.cubicchunks.api.CCAPI;
import com.cardinalstar.cubicchunks.util.DependencyGraph;
import com.cardinalstar.cubicchunks.world.api.ICubeProviderServer.Requirement;
import com.cardinalstar.cubicchunks.world.cube.Cube;

import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;
import rwg.util.NoiseSelector;

/** Ordered cube-local population plus placement of already-planned spanning features. */
public final class CubePopulationPipeline {

    private final World world;
    private final NoiseGenerator noise;
    private final CellNoise cell;
    private final DependencyGraph<PopulationStep> stages = new DependencyGraph<PopulationStep>();

    public CubePopulationPipeline(World world) {
        if (world == null) throw new NullPointerException("world");
        this.world = world;
        noise = NoiseSelector.createNoiseGenerator(world.getSeed());
        cell = new CellNoise(world.getSeed(), (short) 0, true);
        stages.addTarget("rwg:ores-complete");
        stages.addTarget("rwg:underground-decoration-complete", "requires:rwg:ores-complete");
        stages.addTarget("rwg:structures-complete", "requires:rwg:underground-decoration-complete");
        stages.addTarget("rwg:surface-features-complete", "requires:rwg:structures-complete");
        stages.addTarget("rwg:surface-finish-complete", "requires:rwg:surface-features-complete");
        stages.addTarget("rwg:population-complete", "requires:rwg:surface-finish-complete");
        stages.addObject("rwg:ores", new NativeOrePopulationStep(), "required-by:rwg:ores-complete");
        stages.addObject(
                "rwg:underground-lakes",
                new UndergroundLakePopulationStep(),
                "requires:rwg:ores-complete",
                "required-by:rwg:underground-decoration-complete");
        stages.addObject(
                "rwg:native-structures",
                new NativeStructurePopulationStep(),
                "requires:rwg:underground-decoration-complete",
                "required-by:rwg:structures-complete");
        stages.addObject(
                "rwg:surface-features",
                new PlannedFeaturePopulationStep(),
                "requires:rwg:structures-complete",
                "required-by:rwg:surface-features-complete");
        stages.addObject(
                "rwg:snow-and-ice",
                new SnowAndIcePopulationStep(),
                "requires:rwg:surface-features-complete",
                "required-by:rwg:surface-finish-complete");
        stages.sorted();
    }

    public void populate(Cube cube, PopulationPlan plan) {
        prepareAffectedTerrain(cube, plan);
        CubePopulationContext context = new CubePopulationContext(world, cube, plan, noise, cell);
        for (PopulationStep stage : stages.sorted()) stage.populate(context);
    }

    private void prepareAffectedTerrain(Cube owner, PopulationPlan plan) {
        int minimumCubeX = Integer.MAX_VALUE;
        int minimumCubeY = Integer.MAX_VALUE;
        int minimumCubeZ = Integer.MAX_VALUE;
        int maximumCubeX = Integer.MIN_VALUE;
        int maximumCubeY = Integer.MIN_VALUE;
        int maximumCubeZ = Integer.MIN_VALUE;
        for (PlannedFeature feature : plan.features()) {
            if (feature.ownerCubeY() != owner.getY()) continue;
            FeatureBounds bounds = feature.bounds();
            minimumCubeX = Math.min(minimumCubeX, Math.floorDiv(bounds.minimumX, 16));
            minimumCubeY = Math.min(minimumCubeY, Math.floorDiv(bounds.minimumY, 16));
            minimumCubeZ = Math.min(minimumCubeZ, Math.floorDiv(bounds.minimumZ, 16));
            maximumCubeX = Math.max(maximumCubeX, Math.floorDiv(bounds.maximumX, 16));
            maximumCubeY = Math.max(maximumCubeY, Math.floorDiv(bounds.maximumY, 16));
            maximumCubeZ = Math.max(maximumCubeZ, Math.floorDiv(bounds.maximumZ, 16));
        }
        if (minimumCubeX == Integer.MAX_VALUE) return;
        for (int cubeX = minimumCubeX; cubeX <= maximumCubeX; cubeX++) {
            for (int cubeY = minimumCubeY; cubeY <= maximumCubeY; cubeY++) {
                for (int cubeZ = minimumCubeZ; cubeZ <= maximumCubeZ; cubeZ++) {
                    if (cubeX == owner.getX() && cubeY == owner.getY() && cubeZ == owner.getZ()) continue;
                    CCAPI.getCube(world, cubeX, cubeY, cubeZ, Requirement.GENERATE);
                }
            }
        }
    }

    private static final class PlannedFeaturePopulationStep implements PopulationStep {

        @Override
        public void populate(CubePopulationContext context) {
            for (PlannedFeature feature : context.plan.features()) {
                if (feature.stage() != PopulationStage.SURFACE_FEATURES || feature.ownerCubeY() != context.cubeY)
                    continue;
                feature.feature().place(
                        context.world,
                        feature,
                        context.surface,
                        context.noise,
                        context.cell,
                        feature.strength(),
                        feature.river());
            }
        }
    }

    private static final class NativeStructurePopulationStep implements PopulationStep {

        @Override
        public void populate(CubePopulationContext context) {
            for (PlannedStructure structure : context.plan.structures()) structure.place(context);
        }
    }
}
