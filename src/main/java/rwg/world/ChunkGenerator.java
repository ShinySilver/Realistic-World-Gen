package rwg.world;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.SpawnerAnimals;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.MapGenCaves;
import net.minecraft.world.gen.structure.MapGenMineshaft;
import net.minecraft.world.gen.structure.MapGenStronghold;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.ChunkProviderEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.terraingen.TerrainGen;

import cpw.mods.fml.common.eventhandler.Event.Result;
import rwg.ConfigRWG;
import rwg.biomes.decorators.BiomeDecorator;
import rwg.registry.BiomeRegistration;
import rwg.support.EtFuturumCaveVines;
import rwg.support.UndergroundRiverDecorations;
import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;
import rwg.util.NoiseSelector;
import rwg.world.debug.GridSaplingGallery;
import rwg.world.debug.GridWorldgenSelector;

/** Thin Minecraft adapter around the component-based world generator. */
public final class ChunkGenerator implements IChunkProvider {

    private final World world;
    private final ChunkManager manager;
    private final WorldGenerator generator;
    private final Random random;
    private final NoiseGenerator noise;
    private final CellNoise cell;
    private final MapGenBase caves;
    private final MapGenStronghold strongholds;
    private final MapGenMineshaft mineshafts;
    private final MapGenVillage villages;
    private final EtFuturumCaveVines caveVines;
    private final UndergroundRiverDecorations undergroundRiverDecorations;
    private final Map<Long, WorldGenerator.ChunkTerrain> pendingPopulation = new HashMap<Long, WorldGenerator.ChunkTerrain>();

    private BiomeGenBase[] baseBiomesList;

    public ChunkGenerator(World world, long seed) {
        this(world, seed, false);
    }

    public ChunkGenerator(World world, long seed, boolean continental) {
        this.world = world;
        manager = (ChunkManager) world.getWorldChunkManager();
        generator = new WorldGenerator(seed, manager.worldgenSelector());
        random = new Random(seed);
        noise = NoiseSelector.createNoiseGenerator(seed);
        cell = new CellNoise(seed, (short) 0, true);
        caves = TerrainGen
                .getModdedMapGen(new MapGenCaves(), net.minecraftforge.event.terraingen.InitMapGenEvent.EventType.CAVE);
        strongholds = (MapGenStronghold) TerrainGen.getModdedMapGen(
                new MapGenStronghold(),
                net.minecraftforge.event.terraingen.InitMapGenEvent.EventType.STRONGHOLD);
        mineshafts = (MapGenMineshaft) TerrainGen.getModdedMapGen(
                new MapGenMineshaft(),
                net.minecraftforge.event.terraingen.InitMapGenEvent.EventType.MINESHAFT);
        villages = (MapGenVillage) TerrainGen.getModdedMapGen(
                new MapGenVillage(Collections.singletonMap("distance", "24")),
                net.minecraftforge.event.terraingen.InitMapGenEvent.EventType.VILLAGE);
        caveVines = EtFuturumCaveVines.create();
        undergroundRiverDecorations = UndergroundRiverDecorations.create();
    }

    @Override
    public synchronized Chunk provideChunk(int chunkX, int chunkZ) {
        if (manager.worldgenSelector().isVoidChunk(chunkX, chunkZ)) {
            Block[] blocks = new Block[16 * 16 * 256];
            for (int index = 0; index < blocks.length; index++) blocks[index] = Blocks.air;
            Chunk chunk = new Chunk(world, blocks, new byte[blocks.length], chunkX, chunkZ);
            baseBiomesList = new BiomeGenBase[16 * 16];
            for (int index = 0; index < baseBiomesList.length; index++) baseBiomesList[index] = BiomeGenBase.plains;
            ChunkBiomeArrayCompat.setBiomes(chunk, baseBiomesList);
            chunk.generateSkylightMap();
            return chunk;
        }
        WorldGenerator.ChunkTerrain terrain = generator.sampleChunk(chunkX, chunkZ);
        if (pendingPopulation.size() > 1024) pendingPopulation.clear();
        pendingPopulation.put(ChunkCoordIntPair.chunkXZ2Int(chunkX, chunkZ), terrain);
        WorldGenerator.ChunkBlocks generated = generator.fillBaseChunk(terrain);
        UndergroundRiverCarver.carve(chunkX, chunkZ, manager, terrain, generated.blocks, generated.metadata);
        ChunkProviderEvent.ReplaceBiomeBlocks event = new ChunkProviderEvent.ReplaceBiomeBlocks(
                this,
                chunkX,
                chunkZ,
                generated.blocks,
                generated.metadata,
                generated.baseBiomes,
                world);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.getResult() != Result.DENY) generator.paintSurfaces(chunkX, chunkZ, world, generated);
        if (ConfigRWG.generateCaves) caves.func_151539_a(this, world, chunkX, chunkZ, generated.blocks);
        if (ConfigRWG.generateMineshafts) mineshafts.func_151539_a(this, world, chunkX, chunkZ, generated.blocks);
        strongholds.func_151539_a(this, world, chunkX, chunkZ, generated.blocks);
        if (ConfigRWG.generateVillages) villages.func_151539_a(this, world, chunkX, chunkZ, generated.blocks);

        Chunk chunk = new Chunk(world, generated.blocks, generated.metadata, chunkX, chunkZ);
        baseBiomesList = generated.baseBiomes;
        ChunkBiomeArrayCompat.setBiomes(chunk, generated.baseBiomes);
        chunk.generateSkylightMap();
        return chunk;
    }

    @Override
    public Chunk loadChunk(int chunkX, int chunkZ) {
        return provideChunk(chunkX, chunkZ);
    }

    @Override
    public boolean chunkExists(int chunkX, int chunkZ) {
        return true;
    }

    @Override
    public void populate(IChunkProvider provider, int chunkX, int chunkZ) {
        if (manager.worldgenSelector().isVoidChunk(chunkX, chunkZ)) return;
        BlockFalling.fallInstantly = true;
        int x = chunkX * 16;
        int z = chunkZ * 16;
        random.setSeed(world.getSeed());
        long xSeed = random.nextLong() / 2L * 2L + 1L;
        long zSeed = random.nextLong() / 2L * 2L + 1L;
        random.setSeed((long) chunkX * xSeed + (long) chunkZ * zSeed ^ world.getSeed());
        MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Pre(provider, world, random, chunkX, chunkZ, false));
        if (ConfigRWG.generateMineshafts) mineshafts.generateStructuresInChunk(world, random, chunkX, chunkZ);
        strongholds.generateStructuresInChunk(world, random, chunkX, chunkZ);
        if (ConfigRWG.generateVillages) villages.generateStructuresInChunk(world, random, chunkX, chunkZ);

        WorldGenerator.ChunkTerrain terrain = pendingPopulation.remove(ChunkCoordIntPair.chunkXZ2Int(chunkX, chunkZ));
        if (terrain == null) terrain = generator.sampleChunk(chunkX, chunkZ);
        GridWorldgenSelector gridSelector = manager.worldgenSelector() instanceof GridWorldgenSelector
                ? (GridWorldgenSelector) manager.worldgenSelector()
                : null;
        GridSaplingGallery.Entry sapling = gridSelector == null ? null : gridSelector.saplingAtChunk(chunkX, chunkZ);
        boolean saplingGalleryChunk = gridSelector != null && gridSelector.isSaplingGalleryChunk(chunkX, chunkZ);
        int center = 8 * 16 + 8;
        BiomeRegistration registration = terrain.biomeBlend.primary;
        float river = terrain.riverStrengths[center];
        if (!saplingGalleryChunk) {
            for (int index = 0; index < terrain.biomeBlend.size(); index++) {
                BiomeRegistration contributor = terrain.biomeBlend.registration(index);
                float strength = terrain.biomeBlend.weight(index);
                for (BiomeDecorator decorator : contributor.decorators)
                    decorator.decorate(world, random, x, z, noise, cell, strength, river);
            }
            if (caveVines != null) caveVines.decorate(world, random, manager, x, z);
            if (undergroundRiverDecorations != null) undergroundRiverDecorations.decorate(world, manager, x, z);
        } else if (sapling != null) {
            gridSelector.generateSapling(world, random, chunkX, chunkZ, sapling);
        } else {
            gridSelector.drawSaplingCellBorder(world, chunkX, chunkZ);
        }
        if (TerrainGen
                .populate(this, world, random, chunkX, chunkZ, false, PopulateChunkEvent.Populate.EventType.ANIMALS))
            SpawnerAnimals.performWorldGenSpawning(world, registration.biome, x + 8, z + 8, 16, 16, random);
        MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Post(provider, world, random, chunkX, chunkZ, false));
        if (TerrainGen.populate(this, world, random, chunkX, chunkZ, false, PopulateChunkEvent.Populate.EventType.ICE))
            decorateSnowAndIce(x + 8, z + 8, snowScore(terrain));
        BlockFalling.fallInstantly = false;
    }

    private static float snowScore(WorldGenerator.ChunkTerrain terrain) {
        float score = 0f;
        for (int index = 0; index < terrain.biomeBlend.size(); index++) {
            float direction = terrain.biomeBlend.registration(index).biome.temperature < 0.15f ? -0.6f : 0.6f;
            score += direction * terrain.biomeBlend.weight(index);
        }
        return score;
    }

    /** Applies the legacy noise-based cold-biome finishing step using the actual biome of each column. */
    private void decorateSnowAndIce(int startX, int startZ, float snowScore) {
        if (snowScore >= 0.59f) return;
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int blockX = startX + localX;
                int blockZ = startZ + localZ;
                BiomeGenBase biome = world.getBiomeGenForCoords(blockX, blockZ);
                if (biome == null || biome.temperature >= 0.15f) continue;
                if (snowScore >= -0.59f && noise.noise2(blockX / 3f, blockZ / 3f) + snowScore >= 0f) continue;

                int precipitationY = world.getPrecipitationHeight(blockX, blockZ);
                Block surface = world.getBlock(blockX, precipitationY, blockZ);
                Block support = world.getBlock(blockX, precipitationY - 1, blockZ);
                if (support == Blocks.water || support == Blocks.flowing_water) {
                    world.setBlock(blockX, precipitationY - 1, blockZ, Blocks.ice, 0, 2);
                    support = Blocks.ice;
                }
                if (precipitationY > 62 && support != Blocks.ice
                        && support != Blocks.water
                        && support != Blocks.packed_ice
                        && surface != Blocks.snow_layer
                        && Blocks.snow_layer.canPlaceBlockAt(world, blockX, precipitationY, blockZ)) {
                    world.setBlock(blockX, precipitationY, blockZ, Blocks.snow_layer, 0, 2);
                }
            }
        }
    }

    @Override
    public boolean saveChunks(boolean saveAll, IProgressUpdate progress) {
        return true;
    }

    @Override
    public boolean unloadQueuedChunks() {
        return false;
    }

    public boolean unload100OldestChunks() {
        return false;
    }

    @Override
    public boolean canSave() {
        return true;
    }

    @Override
    public String makeString() {
        return "RealisticWorldGenerator";
    }

    @Override
    public List getPossibleCreatures(EnumCreatureType type, int x, int y, int z) {
        BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
        return biome == null ? null : biome.getSpawnableList(type);
    }

    @Override
    public ChunkPosition func_147416_a(World searchWorld, String name, int x, int y, int z) {
        return "Stronghold".equals(name) ? strongholds.func_151545_a(searchWorld, x, y, z) : null;
    }

    @Override
    public int getLoadedChunkCount() {
        return 0;
    }

    @Override
    public void saveExtraData() {}

    @Override
    public void recreateStructures(int chunkX, int chunkZ) {
        strongholds.func_151539_a(this, world, chunkX, chunkZ, (Block[]) null);
        if (ConfigRWG.generateMineshafts) mineshafts.func_151539_a(this, world, chunkX, chunkZ, (Block[]) null);
        if (ConfigRWG.generateVillages) villages.func_151539_a(this, world, chunkX, chunkZ, (Block[]) null);
    }
}
