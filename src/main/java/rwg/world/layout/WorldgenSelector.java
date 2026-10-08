package rwg.world.layout;

import rwg.biomes.terrain.TerrainBase;
import rwg.registry.BiomeRegistration;
import rwg.world.sample.ColumnSample;
import rwg.world.sample.MorphologySample;

/** Selects the complete world-generation identity for one horizontal column. */
public interface WorldgenSelector {

    ColumnSample select(int x, int z);

    MorphologySample morphologyAt(int x, int z);

    BiomeRegistration biomeForBlend(int x, int z);

    BiomeRegistration registrationAt(int x, int z);

    boolean isVoidChunk(int chunkX, int chunkZ);

    TerrainBase terrainFor(BiomeRegistration registration);

    boolean usesReferenceBiomeGeneration();

    BiomeRegistration registration(int id);

    int registrationCount();

    BiomeRegistration riverRegistration(BiomeRegistration land);

    float riverStrength(int x, int z);

    float undergroundRiverStrength(int x, int z);

    float riverJunctionStrength(int x, int z);

}
