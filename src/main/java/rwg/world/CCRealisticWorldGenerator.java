package rwg.world;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;

import com.cardinalstar.cubicchunks.api.ICube;
import com.cardinalstar.cubicchunks.api.worldgen.GenerationResult;
import com.cardinalstar.cubicchunks.api.worldgen.IWorldGenerator;
import com.cardinalstar.cubicchunks.world.core.IColumnInternal;
import com.cardinalstar.cubicchunks.world.cube.Cube;

/**
 * CubicChunks world-generator entry point.
 *
 * <p>The lifecycle, concurrency invariants, stage order, and migration milestones are specified in
 * {@code CUBIC_WORLDGEN.md}. Keep this adapter thin: pure planning and bounded in-progress caches belong
 * behind dedicated abstractions, while cube mutation and world-mutating population remain separate ordered stages.
 */
public final class CCRealisticWorldGenerator implements IWorldGenerator {

    private final World world;

    public CCRealisticWorldGenerator(World world) {
        if (world == null) throw new NullPointerException("world");
        this.world = world;
    }

    @Override
    public GenerationResult<Chunk> provideColumn(World requestedWorld, int columnX, int columnZ) {
        if (requestedWorld != world) throw new IllegalArgumentException("column requested for a different world");

        // Milestone 2 replaces this direct biome query with the shared immutable ColumnPlan. Never generate cubes here.
        Chunk column = new Chunk(world, columnX, columnZ);
        BiomeGenBase[] biomes = world.getWorldChunkManager()
                .loadBlockGeneratorData(null, columnX * 16, columnZ * 16, 16, 16);
        ChunkBiomeArrayCompat.setBiomes(column, biomes);
        ((IColumnInternal) column).setColumn(true);
        return new GenerationResult<Chunk>(column);
    }

    @Override
    public GenerationResult<Cube> provideCube(@Nullable Chunk column, int cubeX, int cubeY, int cubeZ) {
        // Generate only this cube. The future terrain path must share one in-progress plan per X/Z column and retain
        // empty/solid fast paths instead of materializing a full vertical column as side effects.
        if (column == null) {
            column = provideColumn(world, cubeX, cubeZ).object;
            Cube cube = new Cube(column, cubeY);
            return new GenerationResult<Cube>(cube, java.util.Collections.singletonList(column), null);
        }
        if (column.xPosition != cubeX || column.zPosition != cubeZ)
            throw new IllegalArgumentException("cube coordinates do not match the supplied column");
        return new GenerationResult<Cube>(new Cube(column, cubeY));
    }

    @Override
    public void populate(Cube cube) {
        // Population will join an immutable prepopulation plan and run the documented dependency graph. Planning may
        // run off-thread; block placement must remain here and must not trigger cascading generation.
        cube.markPopulated(Cube.POP_ALL);
    }

    @Override
    public void recreateStructures(ICube cube) {}

    @Override
    public void recreateStructures(Chunk column) {}

    @Override
    @SuppressWarnings("unchecked")
    public List<BiomeGenBase.SpawnListEntry> getPossibleCreatures(EnumCreatureType type, int x, int y, int z) {
        BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
        return biome == null ? java.util.Collections.<BiomeGenBase.SpawnListEntry>emptyList()
                : biome.getSpawnableList(type);
    }

    @Override
    @Nullable
    public ChunkPosition getNearestStructure(String name, int x, int y, int z) {
        return null;
    }
}
