package rwg.world.population;

import net.minecraft.world.ChunkPosition;

import rwg.world.ColumnPlanCache;

/** Public query facade; structure layout remains owned by the population package. */
public final class StructureLocator {

    private final StructurePlanner planner;
    private final ColumnPlanCache columns;

    public StructureLocator(long seed, ColumnPlanCache columns) {
        if (columns == null) throw new NullPointerException("columns");
        planner = new StructurePlanner(seed);
        this.columns = columns;
    }

    public ChunkPosition nearestStronghold(int blockX, int blockZ) {
        ChunkPosition horizontal = planner.nearestStronghold(blockX, blockZ);
        int columnX = Math.floorDiv(horizontal.chunkPosX, 16);
        int columnZ = Math.floorDiv(horizontal.chunkPosZ, 16);
        int localX = Math.floorMod(horizontal.chunkPosX, 16);
        int localZ = Math.floorMod(horizontal.chunkPosZ, 16);
        int y = (int) Math.floor(columns.get(columnX, columnZ).height(localX, localZ))
                - planner.strongholdDepth(horizontal.chunkPosX, horizontal.chunkPosZ);
        return new ChunkPosition(horizontal.chunkPosX, y, horizontal.chunkPosZ);
    }
}
