package rwg.world.population;

import net.minecraft.world.biome.BiomeGenBase;

import rwg.world.ColumnPlan;
import rwg.world.CubicWorldSettings;

/** Immutable surface height and cold mask for the final cube-local snow/ice pass. */
final class SurfaceFinishPlan {

    private final int[] surfaceY;
    private final boolean[] cold;

    SurfaceFinishPlan(ColumnPlan column) {
        surfaceY = new int[ColumnPlan.COLUMN_COUNT];
        cold = new boolean[ColumnPlan.COLUMN_COUNT];
        for (int localX = 0; localX < ColumnPlan.SIZE; localX++) {
            for (int localZ = 0; localZ < ColumnPlan.SIZE; localZ++) {
                int index = index(localX, localZ);
                int terrainY = (int) Math.floor(column.height(localX, localZ));
                surfaceY[index] = Math.max(terrainY, CubicWorldSettings.SEA_LEVEL - 1);
                BiomeGenBase biome = column.biome(localX, localZ);
                cold[index] = biome != null && biome.temperature < 0.15f;
            }
        }
    }

    int surfaceY(int localX, int localZ) {
        return surfaceY[index(localX, localZ)];
    }

    boolean isCold(int localX, int localZ) {
        return cold[index(localX, localZ)];
    }

    private static int index(int localX, int localZ) {
        if ((localX | localZ) < 0 || localX >= ColumnPlan.SIZE || localZ >= ColumnPlan.SIZE)
            throw new IndexOutOfBoundsException("column coordinates " + localX + "," + localZ);
        return localX * ColumnPlan.SIZE + localZ;
    }
}
