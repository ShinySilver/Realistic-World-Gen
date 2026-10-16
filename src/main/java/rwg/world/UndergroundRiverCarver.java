package rwg.world;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import com.cardinalstar.cubicchunks.world.cube.Cube;

/** Reproduces RWG's flooded mountain tunnels using only immutable column signals and the requested cube. */
final class UndergroundRiverCarver {

    private UndergroundRiverCarver() {}

    static boolean mayIntersect(ColumnPlan plan, int cubeY) {
        if (!plan.hasUndergroundRiver()) return false;
        int minimumY = cubeY * Cube.SIZE;
        int maximumY = minimumY + Cube.SIZE - 1;
        return maximumY >= 39 && minimumY <= plan.maximumSurfaceY();
    }

    static void carve(CubeGenerationContext context) {
        if (!mayIntersect(context.column, context.cubeY)) return;
        for (int localX = 0; localX < Cube.SIZE; localX++) {
            for (int localZ = 0; localZ < Cube.SIZE; localZ++) {
                float mountainWeight = context.column.mountainWeight(localX, localZ);
                if (mountainWeight <= .10f) continue;
                float tunnel = context.column.undergroundRiverStrength(localX, localZ);
                float junction = context.column.riverJunctionStrength(localX, localZ);
                if (tunnel <= 0f && junction <= 0f) continue;

                int surfaceY = (int) Math.floor(context.column.height(localX, localZ));
                float tunnelCurve = (float) Math.sqrt(Math.max(0f, tunnel));
                int floor = 62 - Math.round(tunnelCurve * 4f);
                int ceiling = 62 + Math.round(tunnelCurve * 11f);
                float chamberStrength = junction * smoothstep((mountainWeight - .10f) / .40f)
                        * smoothstep((surfaceY - 76f) / 24f);
                if (chamberStrength > 0f) {
                    float chamberCurve = (float) Math.sqrt(chamberStrength);
                    floor = Math.min(floor, 63 - Math.round(chamberCurve * 23f));
                    ceiling = Math.max(ceiling, 63 + Math.round(chamberCurve * 42f));
                    ceiling = Math.max(62 + Math.round(tunnelCurve * 11f), Math.min(ceiling, surfaceY - 10));
                }
                carveRange(context, localX, localZ, floor, ceiling);
                if (chamberStrength > .70f && surfaceY > ceiling) {
                    int openingBottom = Math.max(ceiling + 1, CubicWorldSettings.SEA_LEVEL);
                    for (int y = Math.max(openingBottom, context.minimumBlockY); y
                            <= Math.min(surfaceY, context.maximumBlockY); y++) {
                        float fraction = (y - openingBottom) / (float) Math.max(1, surfaceY - openingBottom);
                        if (chamberStrength >= .70f + fraction * .15f) carveBlock(context, localX, localZ, y);
                    }
                }
            }
        }
    }

    private static void carveRange(CubeGenerationContext context, int x, int z, int floor, int ceiling) {
        for (int y = Math.max(floor, context.minimumBlockY); y <= Math.min(ceiling, context.maximumBlockY); y++)
            carveBlock(context, x, z, y);
    }

    private static void carveBlock(CubeGenerationContext context, int x, int z, int blockY) {
        int localY = blockY - context.minimumBlockY;
        Block current = context.blocks.getBlock(x, localY, z);
        if (current != Blocks.stone && current != Blocks.dirt && current != Blocks.gravel) return;
        context.blocks.setBlock(x, localY, z, blockY < CubicWorldSettings.SEA_LEVEL ? Blocks.water : Blocks.air);
    }

    private static float smoothstep(float value) {
        value = Math.max(0f, Math.min(1f, value));
        return value * value * (3f - 2f * value);
    }
}
