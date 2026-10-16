package rwg.world.population;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.ChunkPosition;

import rwg.ConfigRWG;
import rwg.world.ColumnPlan;

/** Stateless region layout for native cubic structures. */
final class StructurePlanner {

    private static final long VILLAGE_SEED = 0x243F6A8885A308D3L;
    private static final long MINESHAFT_SEED = 0x452821E638D01377L;
    private static final long STRONGHOLD_SEED = 0xBE5466CF34E90C6CL;

    private final long worldSeed;

    StructurePlanner(long worldSeed) {
        this.worldSeed = worldSeed;
    }

    List<PlannedStructure> plan(int columnX, int columnZ) {
        List<PlannedStructure> result = new ArrayList<PlannedStructure>();
        if (ConfigRWG.generateVillages)
            collect(result, NativeStructurePlan.Kind.VILLAGE, columnX, columnZ, 24, 38, VILLAGE_SEED);
        if (ConfigRWG.generateMineshafts)
            collect(result, NativeStructurePlan.Kind.MINESHAFT, columnX, columnZ, 12, 30, MINESHAFT_SEED);
        collect(result, NativeStructurePlan.Kind.STRONGHOLD, columnX, columnZ, 64, 42, STRONGHOLD_SEED);
        return result;
    }

    private void collect(List<PlannedStructure> result, NativeStructurePlan.Kind kind, int columnX, int columnZ,
            int regionSize, int radius, long stageSeed) {
        int blockMinimumX = columnX * ColumnPlan.SIZE;
        int blockMinimumZ = columnZ * ColumnPlan.SIZE;
        int blockMaximumX = blockMinimumX + ColumnPlan.SIZE - 1;
        int blockMaximumZ = blockMinimumZ + ColumnPlan.SIZE - 1;
        int reach = Math.floorDiv(radius + ColumnPlan.SIZE - 1, regionSize * ColumnPlan.SIZE) + 1;
        int regionX = Math.floorDiv(columnX, regionSize);
        int regionZ = Math.floorDiv(columnZ, regionSize);
        PopulationRandom random = new PopulationRandom();
        for (int rx = regionX - reach; rx <= regionX + reach; rx++) {
            for (int rz = regionZ - reach; rz <= regionZ + reach; rz++) {
                random.reset(PopulationRandom.seed(worldSeed, stageSeed, rx, 0, rz));
                int anchorColumnX = rx * regionSize + random.nextInt(regionSize);
                int anchorColumnZ = rz * regionSize + random.nextInt(regionSize);
                int centerX = anchorColumnX * ColumnPlan.SIZE + 8;
                int centerZ = anchorColumnZ * ColumnPlan.SIZE + 8;
                if (centerX + radius < blockMinimumX || centerX - radius > blockMaximumX
                        || centerZ + radius < blockMinimumZ
                        || centerZ - radius > blockMaximumZ)
                    continue;
                result.add(NativeStructurePlan.unresolved(kind, centerX, centerZ, random.nextLong()));
            }
        }
    }

    ChunkPosition nearestStronghold(int blockX, int blockZ) {
        int regionSize = 64;
        int originRegionX = Math.floorDiv(Math.floorDiv(blockX, ColumnPlan.SIZE), regionSize);
        int originRegionZ = Math.floorDiv(Math.floorDiv(blockZ, ColumnPlan.SIZE), regionSize);
        PopulationRandom random = new PopulationRandom();
        long bestDistance = Long.MAX_VALUE;
        ChunkPosition best = null;
        for (int radius = 0; radius <= 8; radius++) {
            for (int rx = originRegionX - radius; rx <= originRegionX + radius; rx++) {
                for (int rz = originRegionZ - radius; rz <= originRegionZ + radius; rz++) {
                    if (radius != 0 && Math.abs(rx - originRegionX) != radius && Math.abs(rz - originRegionZ) != radius)
                        continue;
                    random.reset(PopulationRandom.seed(worldSeed, STRONGHOLD_SEED, rx, 0, rz));
                    int x = (rx * regionSize + random.nextInt(regionSize)) * ColumnPlan.SIZE + 8;
                    int z = (rz * regionSize + random.nextInt(regionSize)) * ColumnPlan.SIZE + 8;
                    long dx = (long) x - blockX;
                    long dz = (long) z - blockZ;
                    long distance = dx * dx + dz * dz;
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = new ChunkPosition(x, 0, z);
                    }
                }
            }
            if (best != null && radius >= 2) break;
        }
        return best;
    }

    int strongholdDepth(int centerBlockX, int centerBlockZ) {
        int regionSize = 64;
        int regionX = Math.floorDiv(Math.floorDiv(centerBlockX, ColumnPlan.SIZE), regionSize);
        int regionZ = Math.floorDiv(Math.floorDiv(centerBlockZ, ColumnPlan.SIZE), regionSize);
        PopulationRandom random = new PopulationRandom();
        random.reset(PopulationRandom.seed(worldSeed, STRONGHOLD_SEED, regionX, 0, regionZ));
        random.nextInt(regionSize);
        random.nextInt(regionSize);
        return NativeStructurePlan.depth(NativeStructurePlan.Kind.STRONGHOLD, random.nextLong());
    }
}
