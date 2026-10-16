package rwg.world.population;

import net.minecraft.world.World;

import com.cardinalstar.cubicchunks.world.cube.Cube;
import com.cardinalstar.cubicchunks.world.cube.blockview.CubeBlockView;

import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;

/** Explicit inputs shared by ordered population stages for one cube. */
final class CubePopulationContext {

    final World world;
    final Cube cube;
    final CubeBlockView blocks;
    final PopulationPlan plan;
    final SurfaceHeightLookup surface;
    final NoiseGenerator noise;
    final CellNoise cell;
    final int cubeX;
    final int cubeY;
    final int cubeZ;
    final int minimumBlockX;
    final int minimumBlockY;
    final int minimumBlockZ;

    CubePopulationContext(World world, Cube cube, PopulationPlan plan, NoiseGenerator noise, CellNoise cell) {
        if (world == null || cube == null || plan == null || noise == null || cell == null)
            throw new NullPointerException("population context");
        this.world = world;
        this.cube = cube;
        blocks = new CubeBlockView(cube);
        this.plan = plan;
        surface = plan.surfaceHeights();
        this.noise = noise;
        this.cell = cell;
        cubeX = cube.getX();
        cubeY = cube.getY();
        cubeZ = cube.getZ();
        minimumBlockX = cubeX * Cube.SIZE;
        minimumBlockY = cubeY * Cube.SIZE;
        minimumBlockZ = cubeZ * Cube.SIZE;
    }
}
