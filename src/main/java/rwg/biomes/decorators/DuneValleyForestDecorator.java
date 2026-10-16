package rwg.biomes.decorators;

import java.util.Random;

import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenPumpkin;
import net.minecraft.world.gen.feature.WorldGenReed;
import net.minecraft.world.gen.feature.WorldGenShrub;
import net.minecraft.world.gen.feature.WorldGenerator;

import rwg.biomes.features.BlobFeature;
import rwg.biomes.features.CactiFeature;
import rwg.biomes.features.FlowersFeature;
import rwg.biomes.features.GrassFeature;
import rwg.biomes.features.JungleTallFeature;
import rwg.biomes.features.SavannahFeature;
import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;
import rwg.world.population.SurfaceHeightLookup;

public final class DuneValleyForestDecorator extends CubicFeature {

    @Override
    protected void place(World world, Random random, int chunkX, int chunkY, SurfaceHeightLookup surface,
            NoiseGenerator noise, CellNoise cell, float strength, float river) {
        if (random.nextInt((int) (2f / strength)) == 0) {
            int i1 = chunkX + random.nextInt(16) + 8;
            int j1 = chunkY + random.nextInt(16) + 8;
            int k1 = surface.surfaceY(i1, j1);
            if (k1 < 85) {
                (new BlobFeature(Blocks.cobblestone, 0)).generate(world, random, i1, k1, j1);
            }
        }

        float t = noise.noise2(chunkX / 30f, chunkY / 30f) * 8f;
        t = t > 4f ? 4f : t;

        if (t > 0f) {
            if (random.nextInt((int) ((10f - t) / strength)) == 0) {
                int j6 = chunkX + random.nextInt(16) + 8;
                int k10 = chunkY + random.nextInt(16) + 8;
                int z52 = surface.surfaceY(j6, k10);

                if (z52 < 82) {
                    WorldGenerator worldgenerator = new JungleTallFeature(
                            Blocks.log,
                            2,
                            Blocks.leaves,
                            2,
                            6 + random.nextInt(4),
                            3 + random.nextInt(2),
                            9f + random.nextFloat() * 4f,
                            3,
                            0.32f,
                            0.1f);
                    worldgenerator.setScale(1.0D, 1.0D, 1.0D);
                    worldgenerator.generate(world, random, j6, z52, k10);
                }
            }
        }

        if (river > 0.8f) {
            for (int b33 = 0; b33 < 8f * strength; b33++) {
                int j6 = chunkX + random.nextInt(16) + 8;
                int k10 = chunkY + random.nextInt(16) + 8;
                int z52 = surface.surfaceY(j6, k10);

                if (z52 < 82) {
                    WorldGenerator worldgenerator = random.nextInt(2) != 0 ? new WorldGenShrub(0, 0)
                            : new SavannahFeature(1, false);
                    worldgenerator.setScale(1.0D, 1.0D, 1.0D);
                    worldgenerator.generate(world, random, j6, z52, k10);
                }
            }
        } else {
            for (int b33 = 0; b33 < (1f + t) * strength; b33++) {
                int j6 = chunkX + random.nextInt(16) + 8;
                int k10 = chunkY + random.nextInt(16) + 8;
                int z52 = surface.surfaceY(j6, k10);

                if (z52 < 82) {
                    WorldGenerator worldgenerator = random.nextInt(6) != 0 ? new WorldGenShrub(0, 0)
                            : new SavannahFeature(1, false);
                    worldgenerator.setScale(1.0D, 1.0D, 1.0D);
                    worldgenerator.generate(world, random, j6, z52, k10);
                }
            }
        }

        if (random.nextInt((int) (3f / strength)) == 0) {
            int i18 = chunkX + random.nextInt(16) + 8;
            int i23 = chunkY + random.nextInt(16) + 8;
            (new WorldGenReed()).generate(world, random, i18, surface.surfaceY(i18, i23), i23);
        }

        if (random.nextInt(28) == 0) {
            int j16 = chunkX + random.nextInt(16) + 8;

            int j21 = chunkY + random.nextInt(16) + 8;

            int j18 = surface.surfaceY(j16, j21);
            (new WorldGenPumpkin()).generate(world, random, j16, j18, j21);
        }

        for (int f23 = 0; f23 < 3; f23++) {
            int j15 = chunkX + random.nextInt(16) + 8;

            int j20 = chunkY + random.nextInt(16) + 8;

            int j17 = surface.surfaceY(j15, j20);
            (new FlowersFeature(new int[] { 9, 9, 9, 9, 3, 3, 3, 3, 3, 2, 2, 2, 11, 11, 11 }))
                    .generate(world, random, j15, j17, j20);
        }

        for (int k18 = 0; k18 < 26f * strength; k18++) {
            int k21 = chunkX + random.nextInt(16) + 8;

            int k24 = chunkY + random.nextInt(16) + 8;

            int j23 = surface.surfaceY(k21, k24);
            (new CactiFeature(false)).generate(world, random, k21, j23, k24);
        }

        for (int l14 = 0; l14 < 8f * strength; l14++) {
            int l19 = chunkX + random.nextInt(16) + 8;

            int j24 = chunkY + random.nextInt(16) + 8;

            int k22 = surface.surfaceY(l19, j24);

            if (random.nextInt(3) == 0) {
                (new GrassFeature(Blocks.double_plant, 2)).generate(world, random, l19, k22, j24);
            } else {
                (new GrassFeature(Blocks.tallgrass, 1)).generate(world, random, l19, k22, j24);
            }
        }
    }
}
