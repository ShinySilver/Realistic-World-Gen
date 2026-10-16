package rwg.world;

import java.util.Collections;
import java.util.List;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

/**
 * Non-generating bootstrap object required by Minecraft before CubicChunks installs the real
 * {@link CCRealisticWorldGenerator}. Any attempt to use it for terrain generation is a lifecycle error.
 */
final class CubicBootstrapChunkProvider implements IChunkProvider {

    private final World world;

    CubicBootstrapChunkProvider(World world) {
        if (world == null) throw new NullPointerException("world");
        this.world = world;
    }

    @Override
    public Chunk provideChunk(int chunkX, int chunkZ) {
        throw new IllegalStateException("RWG terrain is available only through CubicChunks");
    }

    @Override
    public Chunk loadChunk(int chunkX, int chunkZ) {
        return provideChunk(chunkX, chunkZ);
    }

    @Override
    public boolean chunkExists(int chunkX, int chunkZ) {
        return false;
    }

    @Override
    public void populate(IChunkProvider provider, int chunkX, int chunkZ) {
        throw new IllegalStateException("RWG population is available only through CubicChunks");
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
        return "RWG CubicChunks bootstrap";
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<BiomeGenBase.SpawnListEntry> getPossibleCreatures(EnumCreatureType type, int x, int y, int z) {
        BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
        return biome == null ? Collections.<BiomeGenBase.SpawnListEntry>emptyList() : biome.getSpawnableList(type);
    }

    @Override
    public ChunkPosition func_147416_a(World searchWorld, String name, int x, int y, int z) {
        return null;
    }

    @Override
    public int getLoadedChunkCount() {
        return 0;
    }

    @Override
    public void saveExtraData() {}

    @Override
    public void recreateStructures(int chunkX, int chunkZ) {}
}
