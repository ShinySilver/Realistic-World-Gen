package rwg.biomes.realistic.ocean;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import rwg.api.RWGBiomes;
import rwg.biomes.realistic.RealisticBiomeBase;
import rwg.surface.SurfaceBase;
import rwg.surface.SurfaceVolcanoAsh;
import rwg.util.CellNoise;
import rwg.util.ContinentalNoise;
import rwg.util.NoiseGenerator;
import rwg.world.ChunkManagerRealistic;

/** A compact volcanic island with a broad, lava-filled summit crater. */
public class RealisticBiomeIslandVolcano extends RealisticBiomeBase {

    private static final float CRATER_RADIUS = 26f;
    private static final float LAVA_FILL_RADIUS = 37.5f;
    private static final float RIM_RADIUS = 44f;
    private static final float OUTER_SLOPE_WIDTH = 51f;
    private static final int CRATER_FLOOR = 65;
    private static final int LAVA_LEVEL = 79;
    private static final int VENT_RADIUS = 5;
    private static final int VENT_SHELL_RADIUS = 10;
    private static final int CHAMBER_CENTER_Y = 40;
    private static final int CHAMBER_RADIUS = 46;
    private static final int CHAMBER_HALF_HEIGHT = 22;
    private static final int CHAMBER_SHELL = 5;

    private final SurfaceBase surface;

    public RealisticBiomeIslandVolcano(BiomeGenBase biome, Block ash, Block ashStone) {
        super(0, biome, RealisticBiomeBase.coastDunes, RWGBiomes.baseRiverHot);
        surface = new SurfaceVolcanoAsh(ash, ashStone);
    }

    @Override
    public float rNoise(NoiseGenerator perlin, CellNoise cell, int x, int y, float ocean, float border, float river) {
        return rNoiseAt(perlin, x, y);
    }

    public float rNoiseAt(NoiseGenerator perlin, float localX, float localZ) {
        float distance = (float) Math.sqrt((double) localX * localX + (double) localZ * localZ);
        distance += perlin.noise2(localX / 18f, localZ / 18f) * 3f;

        float height;
        if (distance < CRATER_RADIUS) {
            height = CRATER_FLOOR + perlin.noise2(localX / 9f, localZ / 9f) * 1.5f;
        } else if (distance < RIM_RADIUS) {
            float rim = (distance - CRATER_RADIUS) / (RIM_RADIUS - CRATER_RADIUS);
            height = CRATER_FLOOR + rim * 29f;
        } else {
            float slope = Math.max(0f, 1f - (distance - RIM_RADIUS) / OUTER_SLOPE_WIDTH);
            height = 61f + slope * 33f;
        }

        return height + perlin.noise2(localX / 24f, localZ / 24f) * 2f;
    }

    @Override
    public void rReplace(Block[] blocks, byte[] metadata, int i, int j, int x, int y, int depth, World world,
            Random rand, NoiseGenerator perlin, CellNoise cell, float[] noise, float river, BiomeGenBase[] base) {
        rReplaceAt(blocks, metadata, i, j, x, y, depth, world, rand, perlin, cell, noise, river, base, i, j);
    }

    public void rReplaceAt(Block[] blocks, byte[] metadata, int i, int j, int x, int y, int depth, World world,
            Random rand, NoiseGenerator perlin, CellNoise cell, float[] noise, float river, BiomeGenBase[] base,
            float localX, float localZ) {
        surface.paintTerrain(blocks, metadata, i, j, x, y, depth, world, rand, perlin, cell, noise, river, base);

        float distance = (float) Math.sqrt((double) localX * localX + (double) localZ * localZ);
        distance += perlin.noise2(localX / 18f, localZ / 18f) * 3f;
        if (distance >= LAVA_FILL_RADIUS) {
            return;
        }

        int column = (y * 16 + x) * 256;
        int surfaceLevel = 255;
        while (surfaceLevel > 1 && isReplaceableFluidOrAir(blocks[column + surfaceLevel])) {
            surfaceLevel--;
        }
        if (surfaceLevel >= LAVA_LEVEL) {
            return;
        }

        blocks[column + surfaceLevel] = Blocks.obsidian;
        metadata[column + surfaceLevel] = 0;
        for (int level = surfaceLevel + 1; level <= LAVA_LEVEL; level++) {
            blocks[column + level] = Blocks.lava;
            metadata[column + level] = 0;
        }
    }

    private boolean isReplaceableFluidOrAir(Block block) {
        return block == Blocks.air || block == Blocks.water
                || block == Blocks.flowing_water
                || block == Blocks.lava
                || block == Blocks.flowing_lava;
    }

    /**
     * Builds the magma chamber after caves and structures, so its obsidian shell seals any openings they created.
     */
    public void generateMagmaChamber(Block[] blocks, byte[] metadata, int chunkX, int chunkZ,
            ChunkManagerRealistic chunkManager) {
        int outerRadius = CHAMBER_RADIUS + CHAMBER_SHELL;
        int outerHalfHeight = CHAMBER_HALF_HEIGHT + CHAMBER_SHELL;

        for (int localX = 0; localX < 16; localX++) {
            int worldX = chunkX * 16 + localX;
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldZ = chunkZ * 16 + localZ;
                long coordinates = chunkManager.getVolcanoCoordinates(worldX, worldZ);
                if (coordinates == Long.MIN_VALUE) {
                    continue;
                }
                float volcanoX = ContinentalNoise.unpackVolcanoX(coordinates);
                float volcanoZ = ContinentalNoise.unpackVolcanoY(coordinates);
                float horizontalDistanceSquared = volcanoX * volcanoX + volcanoZ * volcanoZ;

                for (int level = CHAMBER_CENTER_Y - outerHalfHeight; level <= CRATER_FLOOR; level++) {
                    int verticalDistance = level - CHAMBER_CENTER_Y;
                    double outerDistance = horizontalDistanceSquared / (double) (outerRadius * outerRadius)
                            + verticalDistance * verticalDistance / (double) (outerHalfHeight * outerHalfHeight);
                    double innerDistance = horizontalDistanceSquared / (double) (CHAMBER_RADIUS * CHAMBER_RADIUS)
                            + verticalDistance * verticalDistance
                                    / (double) (CHAMBER_HALF_HEIGHT * CHAMBER_HALF_HEIGHT);

                    boolean insideChamber = innerDistance <= 1D;
                    boolean insideChamberShell = outerDistance <= 1D;
                    boolean aboveChamberCentre = level >= CHAMBER_CENTER_Y;
                    boolean insideVent = aboveChamberCentre && horizontalDistanceSquared <= VENT_RADIUS * VENT_RADIUS;
                    boolean insideVentShell = aboveChamberCentre
                            && horizontalDistanceSquared <= VENT_SHELL_RADIUS * VENT_SHELL_RADIUS;

                    if (!insideChamber && !insideChamberShell && !insideVent && !insideVentShell) {
                        continue;
                    }

                    int index = (localZ * 16 + localX) * 256 + level;
                    blocks[index] = insideChamber || insideVent ? Blocks.lava : Blocks.obsidian;
                    metadata[index] = 0;
                }
            }
        }
    }

}
