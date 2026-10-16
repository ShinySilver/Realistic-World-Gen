package rwg.world;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;

import com.cardinalstar.cubicchunks.api.ICube;
import com.cardinalstar.cubicchunks.api.worldgen.GenerationResult;
import com.cardinalstar.cubicchunks.api.worldgen.IWorldGenerator;
import com.cardinalstar.cubicchunks.server.chunkio.CubeInitLevel;
import com.cardinalstar.cubicchunks.server.chunkio.IPreloadFailureDelegate;
import com.cardinalstar.cubicchunks.util.CubePos;
import com.cardinalstar.cubicchunks.world.core.IColumnInternal;
import com.cardinalstar.cubicchunks.world.cube.Cube;

import rwg.world.population.CubePopulationPipeline;
import rwg.world.population.PopulationPlanCache;
import rwg.world.population.PopulationPlanner;

/**
 * CubicChunks world-generator entry point.
 *
 * <p>
 * The lifecycle, concurrency invariants, stage order, and migration milestones are specified in
 * {@code CUBIC_WORLDGEN.md}. Keep this adapter thin: pure planning and bounded in-progress caches belong behind
 * dedicated abstractions, while cube mutation and world-mutating population remain separate ordered stages.
 */
public final class CCRealisticWorldGenerator implements IWorldGenerator, IPreloadFailureDelegate {

    private final World world;
    private final ColumnPlanCache columnPlans;
    private final CubeTerrainPipeline terrain;
    private final PopulationPlanCache populationPlans;
    private final CubePopulationPipeline population;

    public CCRealisticWorldGenerator(World world) {
        if (world == null) throw new NullPointerException("world");
        this.world = world;
        terrain = new CubeTerrainPipeline(world);
        if (!(world.getWorldChunkManager() instanceof ChunkManager))
            throw new IllegalStateException("RWG cubic worlds require the RWG chunk manager");
        ChunkManager manager = (ChunkManager) world.getWorldChunkManager();
        columnPlans = new ColumnPlanCache(new ColumnPlanner(world.getSeed(), manager.worldgenSelector()));
        populationPlans = new PopulationPlanCache(new PopulationPlanner(world.getSeed()), columnPlans);
        population = new CubePopulationPipeline(world);
    }

    @Override
    public GenerationResult<Chunk> provideColumn(World requestedWorld, int columnX, int columnZ) {
        if (requestedWorld != world) throw new IllegalArgumentException("column requested for a different world");

        ColumnPlan plan = columnPlans.get(columnX, columnZ);
        return new GenerationResult<Chunk>(createColumn(columnX, columnZ, plan));
    }

    @Override
    public GenerationResult<Cube> provideCube(@Nullable Chunk column, int cubeX, int cubeY, int cubeZ) {
        ColumnPlan plan = columnPlans.get(cubeX, cubeZ);
        if (column == null) {
            column = createColumn(cubeX, cubeZ, plan);
            Cube cube = terrain.generate(column, cubeY, plan);
            return new GenerationResult<Cube>(cube, java.util.Collections.singletonList(column), null);
        }
        if (column.xPosition != cubeX || column.zPosition != cubeZ)
            throw new IllegalArgumentException("cube coordinates do not match the supplied column");
        return new GenerationResult<Cube>(terrain.generate(column, cubeY, plan));
    }

    @Override
    public void onColumnPreloadFailed(ChunkCoordIntPair pos) {
        columnPlans.precalculate(pos.chunkXPos, pos.chunkZPos);
    }

    @Override
    public void onCubePreloadFailed(CubePos pos, CubeInitLevel actual, CubeInitLevel wanted) {
        if (actual.ordinal() < CubeInitLevel.Generated.ordinal()
                && wanted.ordinal() >= CubeInitLevel.Generated.ordinal())
            columnPlans.precalculate(pos.getX(), pos.getZ());
        if (actual.ordinal() < CubeInitLevel.Populated.ordinal()
                && wanted.ordinal() >= CubeInitLevel.Populated.ordinal())
            populationPlans.precalculate(pos.getX(), pos.getZ());
    }

    @Override
    public void populate(Cube cube) {
        population.populate(cube, populationPlans.get(cube.getX(), cube.getZ()));
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

    private Chunk createColumn(int columnX, int columnZ, ColumnPlan plan) {
        Chunk column = new Chunk(world, columnX, columnZ);
        ChunkBiomeArrayCompat.setBiomes(column, plan.copyBiomes());
        ((IColumnInternal) column).setColumn(true);
        return column;
    }
}
