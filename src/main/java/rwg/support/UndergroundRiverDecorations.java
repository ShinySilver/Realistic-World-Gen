package rwg.support;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import cpw.mods.fml.common.Loader;
import eu.usrv.legacylootgames.blocks.DungeonBrick;
import ganymedes01.etfuturum.ModBlocks;
import ru.timeconqueror.lootgames.registry.LGBlocks;
import rwg.registry.TerrainCategory;
import rwg.world.ChunkManager;
import twilightforest.block.TFBlocks;

/** Optional mod-backed centerpieces for underground river junction chambers. */
public final class UndergroundRiverDecorations {

    private static final int PILLAR_BOTTOM = 40;
    private static final int PILLAR_TOP = 64;

    private final Block deepslate;
    private final Block puzzleMaster;
    private final Block twilightPortal;

    private UndergroundRiverDecorations(Block deepslate, Block puzzleMaster, Block twilightPortal) {
        this.deepslate = deepslate;
        this.puzzleMaster = puzzleMaster;
        this.twilightPortal = twilightPortal;
    }

    public static UndergroundRiverDecorations create() {
        if (!Loader.isModLoaded("etfuturum")) return null;
        Block deep = ModBlocks.DEEPSLATE.get();
        return new UndergroundRiverDecorations(
                deep == null ? Blocks.stone : deep,
                Loader.isModLoaded("lootgames") ? LGBlocks.PUZZLE_MASTER : null,
                Loader.isModLoaded("TwilightForest") ? TFBlocks.portal : null);
    }

    public void decorate(World world, ChunkManager manager, int chunkX, int chunkZ) {
        int[] center = findJunctionCenter(manager, chunkX, chunkZ);
        if (center == null || Math.floorDiv(center[0], 16) * 16 != chunkX
                || Math.floorDiv(center[1], 16) * 16 != chunkZ)
            return;
        if (world.getBlock(center[0], 62, center[1]).getMaterial() != Material.water
                || !world.isAirBlock(center[0], 65, center[1]))
            return;

        long key = (long) center[0] << 32 ^ center[1] & 0xffffffffL;
        long choice = mix(world.getSeed() ^ key ^ 0x94D049BB133111EBL);
        if ((choice & 1L) != 0L) return;
        if ((choice >>> 1 & 3L) == 0L && twilightPortal != null) {
            placeSquarePillar(world, center[0], center[1], 8, deepslate);
            placeTwilightPortal(world, center[0], PILLAR_TOP, center[1]);
        } else if (puzzleMaster != null) {
            placeRoundedPillar(world, center[0], center[1], 2);
            world.setBlock(center[0], PILLAR_TOP + 2, center[1], puzzleMaster, 0, 2);
        }
    }

    private static int[] findJunctionCenter(ChunkManager manager, int chunkX, int chunkZ) {
        int bestX = chunkX + 8;
        int bestZ = chunkZ + 8;
        float best = -1f;
        for (int x = chunkX; x < chunkX + 16; x += 2) {
            for (int z = chunkZ; z < chunkZ + 16; z += 2) {
                float strength = manager.getRiverJunctionStrength(x, z);
                if (strength > best) {
                    best = strength;
                    bestX = x;
                    bestZ = z;
                }
            }
        }
        if (best < .35f) return null;
        for (int step = 4; step >= 1; step /= 2) {
            boolean moved;
            do {
                moved = false;
                for (int dx = -step; dx <= step; dx += step) {
                    for (int dz = -step; dz <= step; dz += step) {
                        int x = bestX + dx;
                        int z = bestZ + dz;
                        float strength = manager.getRiverJunctionStrength(x, z);
                        if (strength > best) {
                            best = strength;
                            bestX = x;
                            bestZ = z;
                            moved = true;
                        }
                    }
                }
            } while (moved);
        }
        return best >= .82f && hasMountainNearby(manager, bestX, bestZ) ? new int[] { bestX, bestZ } : null;
    }

    private static boolean hasMountainNearby(ChunkManager manager, int x, int z) {
        for (int dx = -24; dx <= 24; dx += 12) for (int dz = -24; dz <= 24; dz += 12)
            if (manager.getBiomeRegistrationAt(x + dx, z + dz).category == TerrainCategory.MOUNTAIN) return true;
        return false;
    }

    private void placeRoundedPillar(World world, int centerX, int centerZ, int radius) {
        int radiusSquared = radius * radius + radius / 2;
        int shieldMetadata = DungeonBrick.Type.FLOOR_SHIELDED.ordinal();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radiusSquared) continue;
                for (int y = PILLAR_BOTTOM; y <= PILLAR_TOP; y++) {
                    boolean shield = y == PILLAR_TOP;
                    world.setBlock(
                            centerX + dx,
                            y,
                            centerZ + dz,
                            shield ? LGBlocks.DUNGEON_WALL : deepslate,
                            shield ? shieldMetadata : 0,
                            2);
                }
            }
        }
    }

    private static void placeSquarePillar(World world, int centerX, int centerZ, int width, Block block) {
        int minimumOffset = -width / 2;
        int maximumOffset = minimumOffset + width - 1;
        for (int dx = minimumOffset; dx <= maximumOffset; dx++)
            for (int dz = minimumOffset; dz <= maximumOffset; dz++) for (int y = PILLAR_BOTTOM; y <= PILLAR_TOP; y++)
                world.setBlock(centerX + dx, y, centerZ + dz, block, 0, 2);
    }

    private void placeTwilightPortal(World world, int centerX, int y, int centerZ) {
        for (int dx = -1; dx <= 2; dx++) {
            for (int dz = -1; dz <= 2; dz++) {
                boolean portal = dx >= 0 && dx <= 1 && dz >= 0 && dz <= 1;
                world.setBlock(centerX + dx, y, centerZ + dz, portal ? twilightPortal : Blocks.grass, 0, 2);
            }
        }
    }

    private static long mix(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }
}
