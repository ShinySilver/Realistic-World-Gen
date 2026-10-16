package rwg.world;

import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.chunk.IChunkProvider;

import com.cardinalstar.cubicchunks.api.IntRange;
import com.cardinalstar.cubicchunks.api.world.ICubicWorldType;
import com.cardinalstar.cubicchunks.api.worldgen.IWorldGenerator;

public class WorldType extends net.minecraft.world.WorldType implements ICubicWorldType {

    private final boolean continents;
    private final boolean grid;

    public WorldType(String name, boolean continents) {
        this(name, continents, false);
    }

    public WorldType(String name, boolean continents, boolean grid) {
        super(name);
        this.continents = continents;
        this.grid = grid;
    }

    public WorldChunkManager getChunkManager(World world) {
        return new ChunkManager(world, continents, grid);
    }

    public IChunkProvider getChunkGenerator(World world, String generatorOptions) {
        return new CubicBootstrapChunkProvider(world);
    }

    @Override
    public IWorldGenerator createCubeGenerator(WorldServer world, IChunkProvider chunkGenerator) {
        return new CCRealisticWorldGenerator(world);
    }

    @Override
    public IntRange getGenerationRange(World world) {
        return CubicWorldSettings.GENERATION_RANGE;
    }

    @Override
    public boolean hasCubicGeneratorForWorld(World world) {
        return true;
    }

    public float getCloudHeight() {
        return 256F;
    }

    @Override
    public int getSpawnFuzz() {
        return grid ? 1 : super.getSpawnFuzz();
    }
}
