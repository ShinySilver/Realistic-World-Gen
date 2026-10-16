package rwg.world.population;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Fast structural guard for deterministic region ownership and surface resolution. */
public final class StructurePlanningVerification {

    private StructurePlanningVerification() {}

    public static void main(String[] arguments) {
        StructurePlanner first = new StructurePlanner(0x1234ABCDL);
        StructurePlanner second = new StructurePlanner(0x1234ABCDL);
        verifySame(first.plan(-17, 29), second.plan(-17, 29));
        verifyNoDuplicateOwners(first.plan(0, 0));
        verifySurfaceResolution(first.plan(0, 0));
        if (!same(first.nearestStronghold(-2000, 4500), second.nearestStronghold(-2000, 4500)))
            throw new AssertionError("stronghold lookup is not deterministic");
        System.out.println("Native cubic structure planning verification passed");
    }

    private static void verifySame(List<PlannedStructure> first, List<PlannedStructure> second) {
        if (first.size() != second.size()) throw new AssertionError("plan size changed");
        for (int index = 0; index < first.size(); index++) {
            FeatureBounds a = first.get(index).bounds();
            FeatureBounds b = second.get(index).bounds();
            if (a.minimumX != b.minimumX || a.minimumZ != b.minimumZ
                    || a.maximumX != b.maximumX
                    || a.maximumZ != b.maximumZ)
                throw new AssertionError("region layout changed");
        }
    }

    private static void verifyNoDuplicateOwners(List<PlannedStructure> plans) {
        Set<String> owners = new HashSet<String>();
        for (PlannedStructure plan : plans) {
            FeatureBounds bounds = plan.bounds();
            String owner = bounds.originBlockX() + ":"
                    + bounds.originBlockZ()
                    + ":"
                    + (bounds.maximumX - bounds.minimumX);
            if (!owners.add(owner)) throw new AssertionError("duplicate structure owner " + owner);
        }
    }

    private static void verifySurfaceResolution(List<PlannedStructure> plans) {
        for (PlannedStructure plan : plans) {
            FeatureBounds bounds = plan.bounds();
            int width = bounds.maximumX - bounds.minimumX + 1;
            int depth = bounds.maximumZ - bounds.minimumZ + 1;
            int[] heights = new int[width * depth];
            java.util.Arrays.fill(heights, 320);
            PlannedStructure resolved = plan
                    .resolve(new SurfaceHeightMap(bounds.minimumX, bounds.minimumZ, width, depth, heights));
            if (resolved == null || resolved.bounds().minimumY <= 0)
                throw new AssertionError("flat high-altitude site did not resolve");
        }
    }

    private static boolean same(net.minecraft.world.ChunkPosition first, net.minecraft.world.ChunkPosition second) {
        return first.chunkPosX == second.chunkPosX && first.chunkPosY == second.chunkPosY
                && first.chunkPosZ == second.chunkPosZ;
    }
}
