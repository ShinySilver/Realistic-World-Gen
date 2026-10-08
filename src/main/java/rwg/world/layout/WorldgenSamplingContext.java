package rwg.world.layout;

import rwg.registry.BiomeRegistry;
import rwg.util.CellNoise;
import rwg.util.ContinentalNoise;
import rwg.util.NoiseGenerator;
import rwg.util.NoiseSelector;
import rwg.util.PoissonPointNoise;

/**
 * Immutable, seed-bound inputs shared by uncached world-generation stages. Scratch buffers and result caches must not
 * be stored here. Noise instances are fully configured during construction and treated as read-only afterward; their
 * internal thread safety must still be established by the planned noise audit.
 */
public final class WorldgenSamplingContext {

    private static final long CLIMATE_WARP_SALT = 0xBB67AE8584CAA73BL;
    private static final long SMALL_BIOME_SALT = 0x510E527FADE682D1L;
    private static final long CONTINENT_SALT = 0x6A09E667F3BCC909L;

    public final long seed;
    public final boolean continental;
    public final float climateWidth;
    public final float biomeWidth;
    public final int landmassOffsetX;
    public final int landmassOffsetZ;
    public final BiomeRegistry biomes;

    public final NoiseGenerator terrainNoise;
    public final CellNoise terrainCellNoise;
    public final CellNoise biomeCellNoise;
    public final NoiseGenerator climateWarpNoise;
    public final PoissonPointNoise smallBiomePointNoise;
    public final ContinentalNoise continentNoise;

    public WorldgenSamplingContext(long seed, boolean continental, float climateWidth, float biomeWidth,
            int landmassOffsetX, int landmassOffsetZ, BiomeRegistry biomes) {
        if (climateWidth <= 0f || biomeWidth <= 0f) throw new IllegalArgumentException("noise widths must be positive");
        if (biomes == null) throw new NullPointerException("biome registry");
        this.seed = seed;
        this.continental = continental;
        this.climateWidth = climateWidth;
        this.biomeWidth = biomeWidth;
        this.landmassOffsetX = landmassOffsetX;
        this.landmassOffsetZ = landmassOffsetZ;
        this.biomes = biomes;

        terrainNoise = NoiseSelector.createNoiseGenerator(seed);
        terrainCellNoise = new CellNoise(seed, (short) 0, true);
        biomeCellNoise = new CellNoise(seed, (short) 0);
        climateWarpNoise = NoiseSelector.createNoiseGenerator(seed ^ CLIMATE_WARP_SALT);
        smallBiomePointNoise = new PoissonPointNoise(seed ^ SMALL_BIOME_SALT, 1020D, 4);
        continentNoise = continental ? new ContinentalNoise(seed ^ CONTINENT_SALT) : null;
    }

    public int landmassX(int worldX) {
        return worldX + landmassOffsetX;
    }

    public int landmassZ(int worldZ) {
        return worldZ + landmassOffsetZ;
    }
}
