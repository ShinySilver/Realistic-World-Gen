package rwg.world.population;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import com.cardinalstar.cubicchunks.world.cube.Cube;

/** Small native templates whose geometry is reproducible and clipped by the receiving cube. */
final class NativeStructurePlan implements PlannedStructure {

    enum Kind {

        VILLAGE(38),
        MINESHAFT(30),
        STRONGHOLD(42);

        final int radius;

        Kind(int radius) {
            this.radius = radius;
        }
    }

    private final Kind kind;
    private final int centerX;
    private final int centerZ;
    private final long seed;
    private final int baseY;
    private final FeatureBounds bounds;

    static NativeStructurePlan unresolved(Kind kind, int centerX, int centerZ, long seed) {
        return new NativeStructurePlan(kind, centerX, centerZ, seed, Integer.MIN_VALUE);
    }

    private NativeStructurePlan(Kind kind, int centerX, int centerZ, long seed, int baseY) {
        this.kind = kind;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.seed = seed;
        this.baseY = baseY;
        int minimumY = baseY == Integer.MIN_VALUE ? 0 : baseY - verticalBelow();
        int maximumY = baseY == Integer.MIN_VALUE ? 0 : baseY + verticalAbove();
        bounds = new FeatureBounds(
                centerX,
                centerZ,
                centerX - kind.radius,
                minimumY,
                centerZ - kind.radius,
                centerX + kind.radius,
                maximumY,
                centerZ + kind.radius);
    }

    @Override
    public FeatureBounds bounds() {
        return bounds;
    }

    @Override
    public PlannedStructure resolve(SurfaceHeightMap surface) {
        int centerSurface = surface.surfaceY(centerX, centerZ);
        if (kind == Kind.VILLAGE) {
            int minimum = Integer.MAX_VALUE;
            int maximum = Integer.MIN_VALUE;
            for (int dx = -32; dx <= 32; dx += 16) {
                for (int dz = -32; dz <= 32; dz += 16) {
                    int height = surface.surfaceY(centerX + dx, centerZ + dz);
                    minimum = Math.min(minimum, height);
                    maximum = Math.max(maximum, height);
                }
            }
            if (maximum - minimum > 8 || centerSurface < 63) return null;
            return new NativeStructurePlan(kind, centerX, centerZ, seed, maximum + 1);
        }
        return new NativeStructurePlan(kind, centerX, centerZ, seed, centerSurface - depth(kind, seed));
    }

    @Override
    public void place(CubePopulationContext context) {
        if (!bounds.intersectsCube(context.cubeX, context.cubeY, context.cubeZ)) return;
        for (int localX = 0; localX < Cube.SIZE; localX++) {
            int x = context.minimumBlockX + localX;
            if (x < bounds.minimumX || x > bounds.maximumX) continue;
            for (int localY = 0; localY < Cube.SIZE; localY++) {
                int y = context.minimumBlockY + localY;
                if (y < bounds.minimumY || y > bounds.maximumY) continue;
                for (int localZ = 0; localZ < Cube.SIZE; localZ++) {
                    int z = context.minimumBlockZ + localZ;
                    if (z < bounds.minimumZ || z > bounds.maximumZ) continue;
                    Block block = blockAt(x - centerX, y - baseY, z - centerZ);
                    if (block != null) context.blocks.setBlock(localX, localY, localZ, block, metadata(block, x, y, z));
                }
            }
        }
    }

    private Block blockAt(int x, int y, int z) {
        switch (kind) {
            case VILLAGE:
                return villageBlock(x, y, z);
            case MINESHAFT:
                return mineshaftBlock(x, y, z);
            case STRONGHOLD:
                return strongholdBlock(x, y, z);
            default:
                throw new AssertionError(kind);
        }
    }

    private Block villageBlock(int x, int y, int z) {
        if ((Math.abs(x) <= 2 && Math.abs(z) <= 34 || Math.abs(z) <= 2 && Math.abs(x) <= 34) && y == 0)
            return Blocks.gravel;
        int houseX = nearestHouseCoordinate(x);
        int houseZ = nearestHouseCoordinate(z);
        if (houseX == Integer.MIN_VALUE || houseZ == Integer.MIN_VALUE) return null;
        int dx = x - houseX;
        int dz = z - houseZ;
        if (Math.abs(dx) > 5 || Math.abs(dz) > 5 || y < -4 || y > 6) return null;
        if (y < 0) return Blocks.cobblestone;
        if (y == 0 || y == 6) return Blocks.planks;
        boolean wall = Math.abs(dx) == 5 || Math.abs(dz) == 5;
        if (!wall) return Blocks.air;
        boolean doorway = dz == (houseZ < 0 ? 5 : -5) && Math.abs(dx) <= 1 && y <= 2;
        if (doorway) return Blocks.air;
        boolean window = y == 3
                && ((Math.abs(dx) == 5 && Math.abs(dz) <= 1) || (Math.abs(dz) == 5 && Math.abs(dx) <= 1));
        return window ? Blocks.glass_pane : Blocks.planks;
    }

    private static int nearestHouseCoordinate(int value) {
        if (Math.abs(value + 20) <= 5) return -20;
        if (Math.abs(value - 20) <= 5) return 20;
        return Integer.MIN_VALUE;
    }

    private Block mineshaftBlock(int x, int y, int z) {
        boolean mainX = Math.abs(z) <= 2 && Math.abs(x) <= 30;
        boolean mainZ = Math.abs(x) <= 2 && Math.abs(z) <= 30;
        boolean branch = Math.abs(z - 18) <= 2 && x >= -24 && x <= 18;
        if (!(mainX || mainZ || branch) || y < -1 || y > 3) return null;
        int axis = mainX || branch ? x : z;
        if (y == -1) return Blocks.planks;
        if (Math.floorMod(axis, 5) == 0 && (y == 0 || y == 1)
                && (mainX || branch ? Math.abs(z - (branch ? 18 : 0)) == 2 : Math.abs(x) == 2))
            return Blocks.fence;
        if (Math.floorMod(axis, 5) == 0 && y == 3) return Blocks.planks;
        return Blocks.air;
    }

    private Block strongholdBlock(int x, int y, int z) {
        boolean outerRoom = Math.abs(x) <= 12 && Math.abs(z) <= 12;
        boolean corridorX = Math.abs(x) <= 42 && Math.abs(z) <= 2;
        boolean corridorZ = Math.abs(z) <= 42 && Math.abs(x) <= 2;
        if (!(outerRoom || corridorX || corridorZ) || y < -1 || y > 6) return null;
        boolean boundary = y == -1 || y == 6;
        if (outerRoom) boundary |= Math.abs(x) == 12 || Math.abs(z) == 12;
        if (corridorX) boundary |= Math.abs(z) == 2;
        if (corridorZ) boundary |= Math.abs(x) == 2;
        return boundary ? Blocks.stonebrick : Blocks.air;
    }

    private int metadata(Block block, int x, int y, int z) {
        if (block != Blocks.stonebrick) return 0;
        int variation = unsigned(seed ^ (long) x * 73428767L ^ (long) y * 912931L ^ (long) z * 438289L, 12);
        return variation == 0 ? 2 : variation <= 2 ? 1 : 0;
    }

    private int verticalBelow() {
        return kind == Kind.VILLAGE ? 4 : 1;
    }

    private int verticalAbove() {
        return kind == Kind.VILLAGE ? 6 : kind == Kind.MINESHAFT ? 3 : 6;
    }

    private static int unsigned(long value, int bound) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        return (int) Long.remainderUnsigned(value, bound);
    }

    static int depth(Kind kind, long seed) {
        return kind == Kind.MINESHAFT ? 28 + unsigned(seed, 36) : 42 + unsigned(seed >>> 12, 24);
    }
}
