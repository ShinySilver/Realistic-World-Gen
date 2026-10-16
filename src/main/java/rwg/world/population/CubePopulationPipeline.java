package rwg.world.population;

import net.minecraft.world.World;

import com.cardinalstar.cubicchunks.api.CCAPI;
import com.cardinalstar.cubicchunks.util.DependencyGraph;
import com.cardinalstar.cubicchunks.world.api.ICubeProviderServer.Requirement;
import com.cardinalstar.cubicchunks.world.cube.Cube;

import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;
import rwg.util.NoiseSelector;

/** Ordered placement of already-planned features. */
public final class CubePopulationPipeline {

    private final World world;
    private final NoiseGenerator noise;
    private final CellNoise cell;
    private final DependencyGraph<PopulationStage> stages = new DependencyGraph<PopulationStage>();

    public CubePopulationPipeline(World world) {
        if (world == null) throw new NullPointerException("world");
        this.world = world;
        noise = NoiseSelector.createNoiseGenerator(world.getSeed());
        cell = new CellNoise(world.getSeed(), (short) 0, true);
        stages.addObject("rwg:ores", PopulationStage.ORES);
        stages.addObject("rwg:underground-decoration", PopulationStage.UNDERGROUND_DECORATION, "after:rwg:ores");
        stages.addObject("rwg:structures", PopulationStage.STRUCTURES, "after:rwg:underground-decoration");
        stages.addObject("rwg:surface-features", PopulationStage.SURFACE_FEATURES, "after:rwg:structures");
        stages.addObject("rwg:snow-and-ice", PopulationStage.SNOW_AND_ICE, "after:rwg:surface-features");
        stages.sorted();
    }

    public void populate(Cube cube, PopulationPlan plan) {
        prepareAffectedTerrain(cube, plan);
        SurfaceHeightLookup surface = plan.surfaceHeights();
        for (PopulationStage stage : stages.sorted()) {
            for (PlannedFeature feature : plan.features()) {
                if (feature.stage() != stage || feature.ownerCubeY() != cube.getY()) continue;
                feature.feature().place(world, feature, surface, noise, cell, feature.strength(), feature.river());
            }
        }
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
}
