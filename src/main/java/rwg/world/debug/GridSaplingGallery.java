package rwg.world.debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.IGrowable;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.oredict.OreDictionary;

import com.falsepattern.endlessids.mixin.helpers.ChunkBiomeHook;

/** Discovers registered saplings and grows deterministic samples in the RWG grid. */
public final class GridSaplingGallery {

    private static final int GROWTH_ATTEMPTS = 16;
    private static final int GROUND_Y = 66;
    private static final int PLANT_Y = 67;
    public static final int PLOT_CHUNKS = 1;

    private final List<Entry> entries;
    private final int firstChunkX;
    private final int firstChunkZ;
    private final int chunksPerLine;

    public GridSaplingGallery(int firstChunkX, int firstChunkZ) {
        this.firstChunkX = firstChunkX;
        this.firstChunkZ = firstChunkZ;
        entries = discover();
        chunksPerLine = Math.max(1, (int) Math.ceil(Math.sqrt(entries.size())));
    }

    public Entry entryAtChunk(int chunkX, int chunkZ) {
        int localChunkX = chunkX - firstChunkX;
        int localChunkZ = chunkZ - firstChunkZ;
        if (localChunkX < 0 || localChunkZ < 0) return null;
        int line = localChunkX / PLOT_CHUNKS;
        int offset = localChunkZ / PLOT_CHUNKS;
        if (line >= chunksPerLine || offset >= chunksPerLine) return null;
        int index = line * chunksPerLine + offset;
        return index < entries.size() ? entries.get(index) : null;
    }

    public boolean containsChunk(int chunkX, int chunkZ) {
        int localChunkX = chunkX - firstChunkX;
        int localChunkZ = chunkZ - firstChunkZ;
        int width = chunksPerLine * PLOT_CHUNKS;
        return localChunkX >= 0 && localChunkZ >= 0 && localChunkX < width && localChunkZ < width;
    }

    public String cellKey(int chunkX, int chunkZ) {
        return "Tree gallery:" + (chunkX - firstChunkX) + ':' + (chunkZ - firstChunkZ);
    }

    public int eastBlock() {
        return (firstChunkX + chunksPerLine * PLOT_CHUNKS) * 16;
    }

    public void generate(World world, Random random, int chunkX, int chunkZ, Entry entry) {
        int localChunkX = chunkX - firstChunkX;
        int localChunkZ = chunkZ - firstChunkZ;
        if (localChunkX % PLOT_CHUNKS != 0 || localChunkZ % PLOT_CHUNKS != 0) return;
        // Establish the ground-level boundary before growing anything. Drawing it afterward can replace low leaves or
        // leaning trunks from generators whose canopy reaches the edge of its chunk.
        drawCellBorder(world, chunkX, chunkZ);
        int x = chunkX * 16 + 8;
        int z = chunkZ * 16 + 8;
        int y = PLANT_Y;
        Block soil = chooseSoil(world, x, y, z, entry);
        if (soil == Blocks.netherrack || soil == Blocks.soul_sand) setNetherBiome(world, chunkX, chunkZ);
        if (!prepareSoil(world, x, y, z, soil)) return;

        if (grow(world, random, x, y, z, entry, soil)) return;

        // Saplings such as dark oak require a square. Only try this fallback when the single sapling stayed in place.
        clearSaplings(world, x, y, z, entry.block);
        for (int dx = 0; dx < 2; dx++) {
            for (int dz = 0; dz < 2; dz++) {
                prepareSoil(world, x + dx, y, z + dz, soil);
                world.setBlock(x + dx, y, z + dz, entry.block, entry.metadata, 2);
            }
        }
        for (int attempt = 0; attempt < GROWTH_ATTEMPTS; attempt++) {
            for (int dx = 0; dx < 2; dx++) {
                for (int dz = 0; dz < 2; dz++) {
                    if (world.getBlock(x + dx, y, z + dz) != entry.block)
                        world.setBlock(x + dx, y, z + dz, entry.block, entry.metadata, 2);
                    if (growOnce(world, random, x + dx, y, z + dz, entry.block) && hasWood(world, x, y, z)) return;
                }
            }
        }
    }

    public void drawCellBorder(World world, int chunkX, int chunkZ) {
        int startX = chunkX * 16;
        int startZ = chunkZ * 16;
        for (int offset = 0; offset < 16; offset++) {
            markSurface(world, startX + offset, startZ);
            markSurface(world, startX + offset, startZ + 15);
            markSurface(world, startX, startZ + offset);
            markSurface(world, startX + 15, startZ + offset);
        }
    }

    private static void markSurface(World world, int x, int z) {
        world.setBlock(x, GROUND_Y, z, Blocks.stonebrick, 0, 2);
    }

    private static boolean grow(World world, Random random, int x, int y, int z, Entry entry, Block soil) {
        prepareSoil(world, x, y, z, soil);
        world.setBlock(x, y, z, entry.block, entry.metadata, 2);
        for (int attempt = 0; attempt < GROWTH_ATTEMPTS; attempt++) {
            if (world.getBlock(x, y, z) != entry.block) world.setBlock(x, y, z, entry.block, entry.metadata, 2);
            if (growOnce(world, random, x, y, z, entry.block) && hasWood(world, x, y, z)) return true;
        }
        clearSaplings(world, x, y, z, entry.block);
        return false;
    }

    private static boolean growOnce(World world, Random random, int x, int y, int z, Block sapling) {
        if (world.getBlock(x, y, z) != sapling) return false;
        if (sapling instanceof BlockSapling) {
            ((BlockSapling) sapling).func_149878_d(world, x, y, z, random);
        } else if (sapling instanceof IGrowable) {
            ((IGrowable) sapling).func_149853_b(world, random, x, y, z);
        } else {
            return false;
        }
        return world.getBlock(x, y, z) != sapling;
    }

    private static boolean hasWood(World world, int x, int y, int z) {
        Block trunk = world.getBlock(x, y, z);
        if (trunk != Blocks.air && trunk != Blocks.grass
                && trunk != Blocks.sand
                && trunk != Blocks.netherrack
                && trunk != Blocks.soul_sand
                && trunk != Blocks.stonebrick)
            return true;
        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = -12; dz <= 12; dz++) {
                for (int dy = 0; dy <= 48 && y + dy < 256; dy++) {
                    Block block = world.getBlock(x + dx, y + dy, z + dz);
                    if (block != Blocks.air && block != Blocks.leaves
                            && block != Blocks.leaves2
                            && block != Blocks.grass
                            && block != Blocks.dirt
                            && block != Blocks.sand
                            && block != Blocks.netherrack
                            && block != Blocks.soul_sand
                            && block != Blocks.stonebrick)
                        return true;
                }
            }
        }
        return false;
    }

    private static Block chooseSoil(World world, int x, int y, int z, Entry entry) {
        Block[] candidates = { Blocks.grass, Blocks.sand, Blocks.netherrack, Blocks.soul_sand };
        if (y <= 1 || y >= 255) return Blocks.grass;
        for (Block candidate : candidates) {
            world.setBlock(x, y - 1, z, candidate, 0, 2);
            world.setBlock(x, y, z, entry.block, entry.metadata, 2);
            boolean stays = entry.block.canBlockStay(world, x, y, z);
            world.setBlock(x, y, z, Blocks.air, 0, 2);
            if (stays) return candidate;
        }
        return Blocks.grass;
    }

    private static void setNetherBiome(World world, int chunkX, int chunkZ) {
        setBiome(world.getChunkFromChunkCoords(chunkX, chunkZ), BiomeGenBase.hell);
    }

    private static void setBiome(Chunk chunk, BiomeGenBase biome) {
        short[] ids = ((ChunkBiomeHook) chunk).getBiomeShortArray();
        short id = (short) biome.biomeID;
        for (int index = 0; index < ids.length; index++) ids[index] = id;
    }

    private static boolean prepareSoil(World world, int x, int y, int z, Block soil) {
        if (y <= 1 || y >= 255) return false;
        world.setBlock(x, y - 1, z, soil, 0, 2);
        world.setBlock(x, y, z, Blocks.air, 0, 2);
        return true;
    }

    private static void clearSaplings(World world, int x, int y, int z, Block sapling) {
        for (int dx = 0; dx < 2; dx++) {
            for (int dz = 0; dz < 2; dz++) {
                if (world.getBlock(x + dx, y, z + dz) == sapling) world.setBlock(x + dx, y, z + dz, Blocks.air, 0, 2);
            }
        }
    }

    private static List<Entry> discover() {
        ArrayList<Entry> result = new ArrayList<Entry>();
        if (Boolean.getBoolean("rwg.skipSaplingGalleryDiscovery")) return Collections.unmodifiableList(result);
        Set<String> represented = new HashSet<String>();
        for (ItemStack stack : OreDictionary.getOres("treeSapling")) {
            if (stack == null || stack.getItem() == null) continue;
            if (stack.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
                ArrayList<ItemStack> variants = new ArrayList<ItemStack>();
                Item item = stack.getItem();
                item.getSubItems(item, item.getCreativeTab(), variants);
                for (ItemStack variant : variants) addEntry(result, represented, variant);
            } else {
                addEntry(result, represented, stack);
            }
        }
        Collections.sort(result, new Comparator<Entry>() {

            @Override
            public int compare(Entry left, Entry right) {
                return left.name.compareToIgnoreCase(right.name);
            }
        });
        return Collections.unmodifiableList(result);
    }

    private static void addEntry(List<Entry> result, Set<String> represented, ItemStack stack) {
        Block block = Block.getBlockFromItem(stack.getItem());
        if (block == null || block == Blocks.air || !(block instanceof IGrowable)) return;
        int metadata = stack.getItemDamage();
        String key = Block.blockRegistry.getNameForObject(block) + ":" + metadata;
        if (!represented.add(key)) return;
        result.add(new Entry(block, metadata, stack.getDisplayName() + " [" + key + "]"));
    }

    public static final class Entry {

        public final Block block;
        public final int metadata;
        public final String name;

        Entry(Block block, int metadata, String name) {
            this.block = block;
            this.metadata = metadata;
            this.name = name;
        }
    }
}
