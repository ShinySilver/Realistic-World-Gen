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
import rwg.biomes.features.SavannahFeature;
import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;
import rwg.world.population.SurfaceHeightLookup;

public final class StoneMountainsDecorator extends CubicFeature {

    @Override
    protected void place(World world, Random random, int chunkX, int chunkY, SurfaceHeightLookup surface,
            NoiseGenerator noise, CellNoise cell, float strength, float river) {
        for (int i23 = 0; i23 < 1; i23++) {
            int i1 = chunkX + random.nextInt(16) + 8;
            int j1 = chunkY + random.nextInt(16) + 8;
            int k1 = surface.surfaceY(i1, j1);

            if (k1 < 80) {
                (new BlobFeature(Blocks.cobblestone, 0)).generate(world, random, i1, k1, j1);
            }
        }

        if (river > 0.7f) {
            if (river > 0.86f) {
                for (int b33 = 0; b33 < 10f * strength; b33++) {
                    int j6 = chunkX + random.nextInt(16) + 8;
                    int k10 = chunkY + random.nextInt(16) + 8;
                    int z52 = surface.surfaceY(j6, k10);

                    if (z52 < 100f || (z52 < 120f && random.nextInt(10) == 0)) {
                        WorldGenerator worldgenerator = random.nextInt(4) != 0 ? new WorldGenShrub(0, 0)
                                : new SavannahFeature(1);
                        worldgenerator.setScale(1.0D, 1.0D, 1.0D);
                        worldgenerator.generate(world, random, j6, z52, k10);
                    }
                }
            }

            for (int k18 = 0; k18 < 12f * strength; k18++) {
                int k21 = chunkX + random.nextInt(16) + 8;

                int k24 = chunkY + random.nextInt(16) + 8;

                int j23 = surface.surfaceY(k21, k24);
                if (j23 < 120f) {
                    (new CactiFeature(false)).generate(world, random, k21, j23, k24);
                }
            }

            for (int f25 = 0; f25 < 2f * strength; f25++) {
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

            for (int l14 = 0; l14 < 15; l14++) {
                int l19 = chunkX + random.nextInt(16) + 8;

                int j24 = chunkY + random.nextInt(16) + 8;

                int k22 = surface.surfaceY(l19, j24);

                if (random.nextInt(3) == 0) {
                    (new GrassFeature(Blocks.double_plant, 2)).generate(world, random, l19, k22, j24);
                } else {
                    (new GrassFeature(Blocks.tallgrass, 1)).generate(world, random, l19, k22, j24);
                }
            }
        } else {
            int a = 3 - (int) (noise.noise2(chunkX / 100f, chunkY / 100f) * 10);
            if (a < 1 || random.nextInt(a) == 0) {
                int j6 = chunkX + random.nextInt(16) + 8;
                int k10 = chunkY + random.nextInt(16) + 8;
                int z52 = surface.surfaceY(j6, k10);

                if (z52 < 100f || (z52 < 120f && random.nextInt(10) == 0)) {
                    WorldGenerator worldgenerator = random.nextInt(14) != 0 ? new WorldGenShrub(0, 0)
                            : new SavannahFeature(1);
                    worldgenerator.setScale(1.0D, 1.0D, 1.0D);
                    worldgenerator.generate(world, random, j6, z52, k10);
                }
            }

            for (int k18 = 0; k18 < 10; k18++) {
                int k21 = chunkX + random.nextInt(16) + 8;

                int k24 = chunkY + random.nextInt(16) + 8;

                int j23 = surface.surfaceY(k21, k24);
                if (j23 < 120f) {
                    (new CactiFeature(false)).generate(world, random, k21, j23, k24);
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

            for (int l14 = 0; l14 < 15; l14++) {
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
}
