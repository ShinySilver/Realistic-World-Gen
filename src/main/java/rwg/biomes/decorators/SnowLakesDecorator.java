package rwg.biomes.decorators;

import java.util.Random;

import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenLakes;
import net.minecraft.world.gen.feature.WorldGenPumpkin;
import net.minecraft.world.gen.feature.WorldGenerator;

import rwg.biomes.features.BlobFeature;
import rwg.biomes.features.FlowersFeature;
import rwg.biomes.features.GrassFeature;
import rwg.biomes.features.ShrubFeature;
import rwg.biomes.features.SmallPineFeature;
import rwg.biomes.features.SmallSpruceFeature;
import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;
import rwg.world.population.SurfaceHeightLookup;

public final class SnowLakesDecorator extends CubicFeature {

    @Override
    protected void place(World world, Random random, int chunkX, int chunkY, SurfaceHeightLookup surface,
            NoiseGenerator noise, CellNoise cell, float strength, float river) {
        if (random.nextInt((int) (15f / strength)) == 0) {
            int i2 = chunkX + random.nextInt(16) + 8;
            int i8 = chunkY + random.nextInt(16) + 8;
            int l4 = surface.surfaceY(i2, i8);
            if (l4 > 63 && l4 < 105) {
                (new WorldGenLakes(Blocks.water)).generate(world, random, i2, l4, i8);
            }
        }

        for (int l = 0; l < 6f * strength; ++l) {
            int i1 = chunkX + random.nextInt(16) + 8;
            int j1 = chunkY + random.nextInt(16) + 8;
            int k1 = surface.surfaceY(i1, j1);
            if (k1 < 95 && (k1 < 64 || random.nextInt(15) == 0)) {
                (new BlobFeature(Blocks.mossy_cobblestone, 0)).generate(world, random, i1, k1, j1);
            }
        }

        if (random.nextInt((int) (25f / strength)) == 0) {
            int j16 = chunkX + random.nextInt(16) + 8;

            int j21 = chunkY + random.nextInt(16) + 8;

            int j18 = surface.surfaceY(j16, j21);
            (new WorldGenPumpkin()).generate(world, random, j16, j18, j21);
        }

        for (int f23 = 0; f23 < 2f * strength; f23++) {
            int j15 = chunkX + random.nextInt(16) + 8;

            int j20 = chunkY + random.nextInt(16) + 8;

            int j17 = surface.surfaceY(j15, j20);
            (new FlowersFeature(new int[] { 9, 0, 3 })).generate(world, random, j15, j17, j20);
        }

        for (int l14 = 0; l14 < 3f * strength; l14++) {
            int l19 = chunkX + random.nextInt(16) + 8;

            int j24 = chunkY + random.nextInt(16) + 8;

            int k22 = surface.surfaceY(l19, j24);
            (new GrassFeature(Blocks.tallgrass, 1)).generate(world, random, l19, k22, j24);
        }

        if (random.nextInt((int) (4f / strength)) == 0) {
            int j6 = chunkX + random.nextInt(16) + 8;
            int k10 = chunkY + random.nextInt(16) + 8;
            int z52 = surface.surfaceY(j6, k10);

            if (z52 < 120) {
                WorldGenerator worldgenerator = random.nextInt(3) != 0
                        ? new SmallPineFeature(1 + random.nextInt(3), 3 + random.nextInt(5), 0)
                        : new SmallSpruceFeature(random.nextInt(2));
                worldgenerator.setScale(1.0D, 1.0D, 1.0D);
                worldgenerator.generate(world, random, j6, z52, k10);
            }
        }

        if (random.nextInt((int) (2f / strength)) == 0) {
            int i1 = chunkX + random.nextInt(16) + 8;
            int j1 = chunkY + random.nextInt(16) + 8;
            int k1 = surface.surfaceY(i1, j1);
            if (k1 < 110) {
                (new ShrubFeature(random.nextInt(4) + 1, random.nextInt(2), random.nextInt(2)))
                        .generate(world, random, i1, k1, j1);
            }
        }
    }
}
