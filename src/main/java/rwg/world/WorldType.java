package rwg.world;

import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.chunk.IChunkProvider;

import com.cardinalstar.cubicchunks.api.IntRange;
import com.cardinalstar.cubicchunks.api.world.ICubicWorldType;
import com.cardinalstar.cubicchunks.api.worldgen.IWorldGenerator;

public class WorldType extends net.minecraft.world.WorldType implements ICubicWorldType {

    public WorldType(String name) {
        super(name);
    }

    public WorldChunkManager getChunkManager(World world) {
        return new ChunkManager(world, true);
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
        // Secondary dimensions inherit the overworld's WorldType. Let their providers (or CubicChunks' vanilla
        // adapter) supply their own generator and height range.
        return world.provider.dimensionId == 0 && world.getWorldChunkManager() instanceof ChunkManager;
    }

    public float getCloudHeight() {
        return 256F;
    }

}
