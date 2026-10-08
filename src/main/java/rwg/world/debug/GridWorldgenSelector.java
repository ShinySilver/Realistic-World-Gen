package rwg.world.debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Random;

import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import rwg.biomes.poi.PointOfInterestSet;
import rwg.biomes.surface.SurfaceBase;
import rwg.biomes.surface.SurfaceNativeBiome;
import rwg.biomes.terrain.TerrainBase;
import rwg.biomes.terrain.TerrainConstant;
import rwg.biomes.terrain.TerrainMarsh;
import rwg.registry.BiomeRegistration;
import rwg.registry.BiomeRegistry;
import rwg.registry.Climate;
import rwg.registry.TerrainCategory;
import rwg.registry.TerrainSubcategory;
import rwg.world.layout.WorldgenSelector;
import rwg.world.sample.BiomeSample;
import rwg.world.sample.ClimateSample;
import rwg.world.sample.ColumnSample;
import rwg.world.sample.ContinentalSample;
import rwg.world.sample.MorphologySample;
import rwg.world.sample.RegionalSample;
import rwg.world.sample.WorldgenPoint;

/** Finite diagnostic atlas with one climate in each quadrant and one cell per selectable registration. */
public final class GridWorldgenSelector implements WorldgenSelector {

    public static final int CELL_SIZE = 64;
    public static final int AXIS_GAP_SIZE = 16;
    public static final int SPAWN_CELL_X = 1;
    public static final int SPAWN_CELL_Z = 1;
    public static final int SPAWN_BLOCK_X = AXIS_GAP_SIZE + (SPAWN_CELL_X - 1) * CELL_SIZE + CELL_SIZE / 2;
    public static final int SPAWN_BLOCK_Z = AXIS_GAP_SIZE + (SPAWN_CELL_Z - 1) * CELL_SIZE + CELL_SIZE / 2;

    private final long seed;
    private final BiomeRegistry registry;
    private final BiomeRegistration[][] cells;
    private final BiomeRegistration[] riverRegistrations;
    private final TerrainBase flatTerrain = new TerrainConstant(67f);
    private final GridSaplingGallery saplingGallery;
    private final BiomeRegistration galleryRegistration;
    private final int atlasOriginX;
    private final int atlasOriginZ;
    private BiomeRegistration fallback;

    public GridWorldgenSelector(long seed, BiomeRegistry registry) {
        if (registry == null) throw new NullPointerException("biome registry");
        this.seed = seed;
        this.registry = registry;
        EnumMap<Climate, List<BiomeRegistration>> byClimate = new EnumMap<Climate, List<BiomeRegistration>>(
                Climate.class);
        IdentityHashMap<BiomeGenBase, Boolean> represented = new IdentityHashMap<BiomeGenBase, Boolean>();
        for (Climate climate : Climate.values()) byClimate.put(climate, new ArrayList<BiomeRegistration>());
        for (BiomeRegistration registration : registry.registrations()) {
            if (registration.selectable && (registration.builtin || !represented.containsKey(registration.biome))) {
                if (!registration.builtin) represented.put(registration.biome, Boolean.TRUE);
                byClimate.get(registration.climate).add(registration);
                if (fallback == null) fallback = registration;
            }
        }
        moveSpawnBiomeFirst(byClimate.get(Climate.WET));
        int snowSize = squareWidth(byClimate.get(Climate.SNOW));
        int coldSize = squareWidth(byClimate.get(Climate.COLD));
        int hotSize = squareWidth(byClimate.get(Climate.HOT));
        int wetSize = squareWidth(byClimate.get(Climate.WET));
        if (snowSize + coldSize + hotSize + wetSize == 0)
            throw new IllegalStateException("No selectable biome registrations");
        int negativeWidth = Math.max(snowSize, hotSize);
        int positiveWidth = Math.max(coldSize, wetSize);
        int negativeDepth = Math.max(snowSize, coldSize);
        int positiveDepth = Math.max(hotSize, wetSize);
        atlasOriginX = negativeWidth;
        atlasOriginZ = negativeDepth;
        cells = new BiomeRegistration[negativeWidth + 1 + positiveWidth][negativeDepth + 1 + positiveDepth];
        placeQuadrant(byClimate.get(Climate.SNOW), snowSize, -1, -1);
        placeQuadrant(byClimate.get(Climate.COLD), coldSize, 1, -1);
        placeQuadrant(byClimate.get(Climate.HOT), hotSize, -1, 1);
        placeQuadrant(byClimate.get(Climate.WET), wetSize, 1, 1);
        int galleryChunkX = AXIS_GAP_SIZE / 16 + wetSize * (CELL_SIZE / 16) + 2;
        int galleryChunkZ = AXIS_GAP_SIZE / 16;
        saplingGallery = new GridSaplingGallery(galleryChunkX, galleryChunkZ);
        galleryRegistration = new BiomeRegistration(
                fallback.id,
                registry.plains(),
                registry.river(Climate.WET),
                flatTerrain,
                new SurfaceBase[] { new SurfaceNativeBiome(registry.plains()) },
                null,
                Climate.WET,
                TerrainCategory.PLAIN,
                TerrainSubcategory.CORE,
                1,
                false,
                true);

        riverRegistrations = new BiomeRegistration[registry.registrations().size()];
        for (List<BiomeRegistration> entries : byClimate.values()) {
            for (BiomeRegistration registration : entries) {
                riverRegistrations[registration.id] = registry.registrationOf(
                        registration.riverBiome,
                        registration.climate,
                        TerrainCategory.RIVER,
                        TerrainSubcategory.CORE);
            }
        }
    }

    private void placeQuadrant(List<BiomeRegistration> entries, int width, int directionX, int directionZ) {
        for (int index = 0; index < entries.size(); index++) {
            int distanceX = index % width;
            int distanceZ = index / width;
            int cellX = directionX < 0 ? -1 - distanceX : 1 + distanceX;
            int cellZ = directionZ < 0 ? -1 - distanceZ : 1 + distanceZ;
            cells[atlasOriginX + cellX][atlasOriginZ + cellZ] = entries.get(index);
        }
    }

    private static int squareWidth(List<BiomeRegistration> entries) {
        return (int) Math.ceil(Math.sqrt(entries.size()));
    }

    private static void moveSpawnBiomeFirst(List<BiomeRegistration> entries) {
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).biome.topBlock == Blocks.grass) {
                Collections.swap(entries, 0, index);
                return;
            }
        }
        if (entries.isEmpty()) throw new IllegalStateException("RWG grid requires a wet-climate spawn biome");
        throw new IllegalStateException("RWG grid requires a grass-topped wet-climate spawn biome");
    }

    @Override
    public ColumnSample select(int x, int z) {
        BiomeRegistration registration = biomeForBlend(x, z);
        boolean ocean = registration.category == TerrainCategory.SHALLOW_OCEAN
                || registration.category == TerrainCategory.DEEP_OCEAN;
        int islandTier = registration.category == TerrainCategory.SMALL_ISLAND ? 0
                : registration.category == TerrainCategory.MEDIUM_ISLAND ? 1
                        : registration.category == TerrainCategory.LARGE_ISLAND ? 2 : -1;
        float selector = registration.id / (float) registry.registrations().size();
        ContinentalSample continent = new ContinentalSample(ocean ? -100f : 100f, ocean, islandTier);
        ClimateSample climate = new ClimateSample(
                registration.climate,
                selector,
                0L,
                Float.POSITIVE_INFINITY,
                false,
                Collections.<ClimateSample.Neighbor>emptyList());
        MorphologySample morphology = new MorphologySample(
                registration.category,
                selector,
                0f,
                0f,
                Float.POSITIVE_INFINITY,
                false);
        RegionalSample regional = new RegionalSample(67f, morphology.mountainDistance, 67f);
        return new ColumnSample(
                new WorldgenPoint(seed, x, z),
                continent,
                climate,
                PointOfInterestSet.EMPTY,
                morphology,
                regional,
                new BiomeSample(registration, selector));
    }

    @Override
    public MorphologySample morphologyAt(int x, int z) {
        BiomeRegistration registration = registrationAt(x, z);
        return new MorphologySample(registration.category, 0f, 0f, 0f, Float.POSITIVE_INFINITY, false);
    }

    @Override
    public BiomeRegistration biomeForBlend(int x, int z) {
        BiomeRegistration exact = registrationAt(x, z);
        if (exact != null) return exact;
        int cellX = atlasCellX(x);
        int cellZ = atlasCellZ(z);
        int maximumRadius = Math.max(cells.length, cells[0].length);
        for (int radius = 1; radius <= maximumRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                BiomeRegistration north = cell(cellX + dx, cellZ - radius);
                if (north != null) return north;
                BiomeRegistration south = cell(cellX + dx, cellZ + radius);
                if (south != null) return south;
            }
            for (int dz = -radius + 1; dz < radius; dz++) {
                BiomeRegistration west = cell(cellX - radius, cellZ + dz);
                if (west != null) return west;
                BiomeRegistration east = cell(cellX + radius, cellZ + dz);
                if (east != null) return east;
            }
        }
        return fallback;
    }

    @Override
    public BiomeRegistration registrationAt(int x, int z) {
        if (saplingGallery.containsChunk(Math.floorDiv(x, 16), Math.floorDiv(z, 16))) return galleryRegistration;
        return cell(atlasCellX(x), atlasCellZ(z));
    }

    @Override
    public boolean isVoidChunk(int chunkX, int chunkZ) {
        return registrationAt(chunkX * 16 + 8, chunkZ * 16 + 8) == null;
    }

    @Override
    public TerrainBase terrainFor(BiomeRegistration registration) {
        return registration.terrain instanceof TerrainMarsh ? registration.terrain : flatTerrain;
    }

    @Override
    public boolean usesReferenceBiomeGeneration() {
        return false;
    }

    private int atlasCellX(int coordinate) {
        return atlasCell(coordinate) + atlasOriginX;
    }

    private int atlasCellZ(int coordinate) {
        return atlasCell(coordinate) + atlasOriginZ;
    }

    private static int atlasCell(int coordinate) {
        // Negative quadrants end at -1, the empty axis occupies exactly chunk zero (0..15), and positive quadrants
        // begin at 16. Biome cells remain CELL_SIZE blocks wide on either side of that one-chunk gap.
        if (coordinate < 0) return Math.floorDiv(coordinate, CELL_SIZE);
        if (coordinate < AXIS_GAP_SIZE) return 0;
        return 1 + Math.floorDiv(coordinate - AXIS_GAP_SIZE, CELL_SIZE);
    }

    private BiomeRegistration cell(int x, int z) {
        return x < 0 || z < 0 || x >= cells.length || z >= cells[x].length ? null : cells[x][z];
    }

    @Override
    public BiomeRegistration registration(int id) {
        return registry.registration(id);
    }

    @Override
    public int registrationCount() {
        return registry.registrations().size();
    }

    @Override
    public BiomeRegistration riverRegistration(BiomeRegistration land) {
        return riverRegistrations[land.id];
    }

    @Override
    public float riverStrength(int x, int z) {
        return 0f;
    }

    @Override
    public float undergroundRiverStrength(int x, int z) {
        return 0f;
    }

    @Override
    public float riverJunctionStrength(int x, int z) {
        return 0f;
    }

    public GridSaplingGallery.Entry saplingAtChunk(int chunkX, int chunkZ) {
        return saplingGallery.entryAtChunk(chunkX, chunkZ);
    }

    public boolean isSaplingGalleryChunk(int chunkX, int chunkZ) {
        return saplingGallery.containsChunk(chunkX, chunkZ);
    }

    public void drawSaplingCellBorder(World world, int chunkX, int chunkZ) {
        saplingGallery.drawCellBorder(world, chunkX, chunkZ);
    }

    public void generateSapling(World world, Random random, int chunkX, int chunkZ, GridSaplingGallery.Entry entry) {
        saplingGallery.generate(world, random, chunkX, chunkZ, entry);
    }

    public String regionAt(int blockX, int blockZ) {
        if (saplingGallery.containsChunk(Math.floorDiv(blockX, 16), Math.floorDiv(blockZ, 16))) return "Tree gallery";
        int logicalX = atlasCell(blockX);
        int logicalZ = atlasCell(blockZ);
        if (cell(atlasOriginX + logicalX, atlasOriginZ + logicalZ) == null) return "Empty space";
        if (logicalX < 0 && logicalZ < 0) return "Snow quadrant";
        if (logicalX > 0 && logicalZ < 0) return "Cold quadrant";
        if (logicalX < 0 && logicalZ > 0) return "Hot quadrant";
        if (logicalX > 0 && logicalZ > 0) return "Wet quadrant";
        return "Empty space";
    }

    /** Stable local cell identity used by the chat reporter; each gallery owns its coordinate origin. */
    public String reportCellKey(int blockX, int blockZ) {
        int chunkX = Math.floorDiv(blockX, 16);
        int chunkZ = Math.floorDiv(blockZ, 16);
        if (saplingGallery.containsChunk(chunkX, chunkZ)) return saplingGallery.cellKey(chunkX, chunkZ);
        return regionAt(blockX, blockZ) + ':' + atlasCell(blockX) + ':' + atlasCell(blockZ);
    }
}
