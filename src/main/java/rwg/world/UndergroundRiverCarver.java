package rwg.world;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

/** Carves the legacy flooded mountain tunnels and their enlarged river-junction chambers. */
final class UndergroundRiverCarver {

    private UndergroundRiverCarver() {}

    static void carve(int chunkX, int chunkZ, ChunkManager manager, WorldGenerator.ChunkTerrain terrain, Block[] blocks,
            byte[] metadata) {
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int column = localX * 16 + localZ;
                float mountainWeight = terrain.mountainWeights[column];
                if (mountainWeight <= .10f) continue;

                int worldX = chunkX * 16 + localX;
                int worldZ = chunkZ * 16 + localZ;
                float tunnel = manager.getRiverTunnelStrength(worldX, worldZ);
                float junction = manager.getRiverJunctionStrength(worldX, worldZ);
                if (tunnel <= 0f && junction <= 0f) continue;

                int surface = Math.min(255, (int) terrain.heights[column]);
                float tunnelCurve = (float) Math.sqrt(Math.max(0f, tunnel));
                int tunnelFloor = 62 - Math.round(tunnelCurve * 4f);
                int tunnelCeiling = 62 + Math.round(tunnelCurve * 11f);
                int caveFloor = tunnelFloor;
                int caveCeiling = tunnelCeiling;

                float mountainHost = smoothstep((mountainWeight - .10f) / .40f);
                float overheadHost = smoothstep((surface - 76f) / 24f);
                float chamberStrength = junction * mountainHost * overheadHost;
                if (chamberStrength > 0f) {
                    float chamberCurve = (float) Math.sqrt(chamberStrength);
                    caveFloor = Math.min(caveFloor, 63 - Math.round(chamberCurve * 23f));
                    caveCeiling = Math.max(caveCeiling, 63 + Math.round(chamberCurve * 42f));
                    caveCeiling = Math.min(caveCeiling, surface - 10);
                    caveCeiling = Math.max(caveCeiling, tunnelCeiling);
                }

                carveColumn(blocks, metadata, localX, localZ, caveFloor, caveCeiling);
                if (chamberStrength > .70f && surface > caveCeiling) {
                    int openingBottom = Math.max(caveCeiling + 1, 63);
                    for (int y = openingBottom; y <= surface; y++) {
                        float heightFraction = (y - openingBottom) / (float) Math.max(1, surface - openingBottom);
                        if (chamberStrength >= .70f + heightFraction * .15f)
                            setCaveBlock(blocks, metadata, localX, localZ, y);
                    }
                }
            }
        }
    }

    private static float smoothstep(float value) {
        value = Math.max(0f, Math.min(1f, value));
        return value * value * (3f - 2f * value);
    }

    private static void carveColumn(Block[] blocks, byte[] metadata, int localX, int localZ, int floor, int ceiling) {
        for (int y = Math.max(1, floor); y <= Math.min(255, ceiling); y++)
            setCaveBlock(blocks, metadata, localX, localZ, y);
    }

    private static void setCaveBlock(Block[] blocks, byte[] metadata, int localX, int localZ, int y) {
        int index = (localX * 16 + localZ) * 256 + y;
        blocks[index] = y <= 62 ? Blocks.water : Blocks.air;
        metadata[index] = 0;
    }
}
