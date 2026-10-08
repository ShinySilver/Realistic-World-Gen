package rwg.world;

import java.io.File;
import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import rwg.ConfigRWG;
import rwg.registry.BiomeRegistration;
import rwg.registry.BiomeRegistry;
import rwg.registry.BiomeRegistryBootstrap;

/** Headless checks for post-composition surface carving and restored underground junction chambers. */
public final class RiverCarverVerification {

    private RiverCarverVerification() {}

    public static void main(String[] args) {
        ConfigRWG.initPreview(new File(System.getProperty("rwg.previewConfig", "run/client/config/RWG.cfg")));
        BiomeRegistry registry = BiomeRegistryBootstrap.create(false);
        ChunkManager manager = new ChunkManager(0L, true, registry);
        verifyFinalHeightCarving(manager);
        verifyRaisedRiverContinuesUnderground(manager);
        verifyUndergroundJunction(manager);
        System.out.println("RWG river carver verification passed");
    }

    private static void verifyRaisedRiverContinuesUnderground(ChunkManager manager) {
        int riverX = 0;
        int riverZ = 0;
        boolean found = false;
        for (int x = -3000; x <= 3000 && !found; x += 4) {
            for (int z = -3000; z <= 3000; z += 4) {
                rwg.world.sample.ColumnSample sample = manager.worldgenSelector().select(x, z);
                if (sample.morphology.mountainStrength <= .10f || sample.morphology.riverStrength > 0f
                        || manager.getRiverTunnelStrength(x, z) < .90f)
                    continue;
                riverX = x;
                riverZ = z;
                found = true;
                break;
            }
        }
        require(found, "no raised underground continuation of a suppressed surface river found");

        int chunkX = Math.floorDiv(riverX, 16);
        int chunkZ = Math.floorDiv(riverZ, 16);
        int localX = riverX - chunkX * 16;
        int localZ = riverZ - chunkZ * 16;
        int sampleIndex = localX * 16 + localZ;
        WorldGenerator generator = new WorldGenerator(0L, manager.worldgenSelector());
        WorldGenerator.ChunkTerrain terrain = generator.sampleChunk(chunkX, chunkZ);
        require(terrain.mountainWeights[sampleIndex] > .10f, "raised hill did not host an underground river");
        WorldGenerator.ChunkBlocks generated = generator.fillBaseChunk(terrain);
        UndergroundRiverCarver.carve(chunkX, chunkZ, manager, terrain, generated.blocks, generated.metadata);
        int column = sampleIndex * 256;
        require(generated.blocks[column + 62] == Blocks.water, "suppressed surface river did not continue underground");
        require(generated.blocks[column + 65] == Blocks.air, "underground continuation has no carved ceiling");
    }

    private static void verifyFinalHeightCarving(ChunkManager manager) {
        WorldGenerator generator = new WorldGenerator(0L, manager.worldgenSelector());
        for (int x = -3000; x <= 3000; x += 4) {
            for (int z = -3000; z <= 3000; z += 4) {
                WorldGenerator.PreviewColumn column = generator.samplePreviewPoint(x, z);
                if (column.sample.morphology.riverStrength < .95f || column.baseHeight < 68f) continue;
                require(column.height < column.baseHeight, "core river did not cut the composed terrain height");
                require(column.height <= 62f, "core river center remained above its intended bed");
                return;
            }
        }
        throw new AssertionError("no strong surface-river sample found");
    }

    private static void verifyUndergroundJunction(ChunkManager manager) {
        int junctionX = 0;
        int junctionZ = 0;
        boolean found = false;
        for (int x = -3000; x <= 3000 && !found; x += 4) {
            for (int z = -3000; z <= 3000; z += 4) {
                if (manager.getRiverJunctionStrength(x, z) < .90f) continue;
                junctionX = x;
                junctionZ = z;
                found = true;
                break;
            }
        }
        require(found, "no underground river junction found");

        int chunkX = Math.floorDiv(junctionX, 16);
        int chunkZ = Math.floorDiv(junctionZ, 16);
        float[] heights = new float[256];
        float[] baseHeights = new float[256];
        float[] riverStrengths = new float[256];
        float[] mountainWeights = new float[256];
        Arrays.fill(heights, 120f);
        Arrays.fill(baseHeights, 120f);
        Arrays.fill(mountainWeights, 1f);
        WorldGenerator.ChunkTerrain terrain = new WorldGenerator.ChunkTerrain(
                heights,
                baseHeights,
                new BiomeRegistration[256],
                riverStrengths,
                mountainWeights,
                null);
        Block[] blocks = new Block[16 * 16 * 256];
        byte[] metadata = new byte[blocks.length];
        Arrays.fill(blocks, Blocks.stone);
        UndergroundRiverCarver.carve(chunkX, chunkZ, manager, terrain, blocks, metadata);

        int localX = junctionX - chunkX * 16;
        int localZ = junctionZ - chunkZ * 16;
        int column = (localX * 16 + localZ) * 256;
        require(blocks[column + 62] == Blocks.water, "underground river was not flooded through sea level");
        require(blocks[column + 65] == Blocks.air, "underground river ceiling was not carved");
        require(blocks[column + 45] == Blocks.air, "junction chamber did not expand downward");
        require(blocks[column + 90] == Blocks.air, "junction chamber did not expand upward");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
