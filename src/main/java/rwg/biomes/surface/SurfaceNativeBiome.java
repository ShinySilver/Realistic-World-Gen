package rwg.biomes.surface;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;

/** Uses a biome's native surface painter on RWG-provided terrain. Intended for reference galleries only. */
public final class SurfaceNativeBiome extends SurfaceBase {

    private final BiomeGenBase biome;

    public SurfaceNativeBiome(BiomeGenBase biome) {
        super(biome.topBlock, biome.fillerBlock);
        this.biome = biome;
    }

    @Override
    public void paintTerrain(Block[] blocks, byte[] metadata, int i, int j, int x, int y, int depth, World world,
            Random rand, NoiseGenerator perlin, CellNoise cell, float[] noise, float river, BiomeGenBase[] base) {
        // Vanilla addresses this array in the opposite coordinate order from RWG's surface implementations.
        biome.genTerrainBlocks(world, rand, blocks, metadata, j, i, perlin.noise2(i / 32f, j / 32f) * 4d);
    }
}
