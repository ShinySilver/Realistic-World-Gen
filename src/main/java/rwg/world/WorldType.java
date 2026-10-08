package rwg.world;

import net.minecraft.world.World;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.chunk.IChunkProvider;

public class WorldType extends net.minecraft.world.WorldType {

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
        return new ChunkGenerator(world, world.getSeed(), continents);
    }

    public float getCloudHeight() {
        return 256F;
    }

    @Override
    public int getSpawnFuzz() {
        return grid ? 1 : super.getSpawnFuzz();
    }
}
