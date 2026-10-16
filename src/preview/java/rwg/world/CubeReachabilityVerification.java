package rwg.world;

import com.cardinalstar.cubicchunks.server.CubeReachabilityRegion;

/** Focused bit-layout and conservative-barrier checks for the 4x4x4 reachability primitive. */
public final class CubeReachabilityVerification {

    private CubeReachabilityVerification() {}

    public static void main(String[] args) {
        long all = -1L;
        require(close(1L, all, 0, 0) == all, "open region must be fully reachable");

        long lowHalf = 0x3333333333333333L;
        long positiveXBarrier = 0x2222222222222222L;
        require(close(1L, all, 0, positiveXBarrier) == lowHalf, "a sealed positive-X plane must stop propagation");
        require(
                close(1L, all, 0x4444444444444444L, 0) == lowHalf,
                "the destination negative-X face must also stop propagation");

        long sparseCandidates = 0xFL;
        require(close(1L, sparseCandidates, 0, 0) == sparseCandidates, "propagation must remain inside candidates");
        require(
                CubeReachabilityRegion.seedsAcrossPosX(1L << 3, 0) == 1L,
                "positive-X spill must map to the neighboring region's negative edge");
        require(
                CubeReachabilityRegion.seedsAcrossPosY(1L << 63, 0) == 1L << 15,
                "positive-Y spill must preserve local X/Z");

        System.out.println("Cube reachability verification passed");
    }

    private static long close(long seeds, long candidates, long sealedNegX, long sealedPosX) {
        return CubeReachabilityRegion.close(seeds, candidates, sealedNegX, sealedPosX, 0, 0, 0, 0);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
