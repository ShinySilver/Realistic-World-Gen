package rwg.world;

import com.cardinalstar.cubicchunks.world.cube.Cube;
import com.cardinalstar.cubicchunks.world.cube.blockview.CubeBlockView;

/** All inputs used by ordered terrain stages for one requested cube. */
final class CubeGenerationContext {

    final Cube cube;
    final CubeBlockView blocks;
    final ColumnPlan column;
    final int cubeX;
    final int cubeY;
    final int cubeZ;
    final int minimumBlockY;
    final int maximumBlockY;

    CubeGenerationContext(Cube cube, ColumnPlan column) {
        if (cube == null || column == null) throw new NullPointerException("cube generation inputs");
        this.cube = cube;
        blocks = new CubeBlockView(cube);
        this.column = column;
        cubeX = cube.getX();
        cubeY = cube.getY();
        cubeZ = cube.getZ();
        minimumBlockY = cubeY * Cube.SIZE;
        maximumBlockY = minimumBlockY + Cube.SIZE - 1;
    }
}
