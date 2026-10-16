package rwg.biomes.decorators;

import java.util.Random;

import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenForest;
import net.minecraft.world.gen.feature.WorldGenPumpkin;
import net.minecraft.world.gen.feature.WorldGenTrees;
import net.minecraft.world.gen.feature.WorldGenerator;

import rwg.biomes.features.BigTreeFeature;
import rwg.biomes.features.FlowersFeature;
import rwg.biomes.features.GrassFeature;
import rwg.biomes.features.LogFeature;
import rwg.biomes.features.ShrubFeature;
import rwg.biomes.features.WildWheatFeature;
import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;
import rwg.world.population.SurfaceHeightLookup;

public final class WoodHillsDecorator extends CubicFeature {

    @Override
    protected void place(World world, Random random, int chunkX, int chunkY, SurfaceHeightLookup surface,
            NoiseGenerator noise, CellNoise cell, float strength, float river) {
        float l = noise.noise2(chunkX / 80f, chunkY / 80f) * 20f;
        l = l > 10f ? 10f : l;

        for (int b2 = 0; b2 < strength * l; b2++) {
            int j6 = chunkX + random.nextInt(16) + 8;
            int k10 = chunkY + random.nextInt(16) + 8;
            int z52 = surface.surfaceY(j6, k10);

            if (z52 < 120) {
                WorldGenerator worldgenerator = random.nextInt(10) == 0 ? new BigTreeFeature()
                        : random.nextInt(6) != 0 ? new WorldGenTrees(false) : new WorldGenForest(false, false);
                worldgenerator.setScale(1.0D, 1.0D, 1.0D);
                worldgenerator.generate(world, random, j6, z52, k10);
            }
        }

        if (l > -1f) {
            if (random.nextInt((int) (4f / strength)) == 0) {
                int x22 = chunkX + random.nextInt(16) + 8;
                int z22 = chunkY + random.nextInt(16) + 8;
                int y22 = surface.surfaceY(x22, z22);
                if (y22 < 100) {
                    (new LogFeature(1, 3 + random.nextInt(4), false)).generate(world, random, x22, y22, z22);
                }
            }

            for (int f24 = 0; f24 < 3f * strength; f24++) {
                int i1 = chunkX + random.nextInt(16) + 8;
                int j1 = chunkY + random.nextInt(16) + 8;
                int k1 = surface.surfaceY(i1, j1);
                if (k1 < 110) {
                    (new ShrubFeature(random.nextInt(4) + 1, 0, random.nextInt(3))).generate(world, random, i1, k1, j1);
                }
            }
        }

        if (random.nextInt((int) (150f / strength)) == 0) {
            int k21 = chunkX + random.nextInt(16) + 8;

            int k24 = chunkY + random.nextInt(16) + 8;

            int j23 = surface.surfaceY(k21, k24);
            (new WildWheatFeature(random.nextInt(3))).generate(world, random, k21, j23, k24);
        }

        if (random.nextInt((int) (15f / strength)) == 0) {
            int j16 = chunkX + random.nextInt(16) + 8;

            int j21 = chunkY + random.nextInt(16) + 8;

            int j18 = surface.surfaceY(j16, j21);
            (new WorldGenPumpkin()).generate(world, random, j16, j18, j21);
        }

        for (int f23 = 0; f23 < 8f * strength; f23++) {
            int j15 = chunkX + random.nextInt(16) + 8;

            int j20 = chunkY + random.nextInt(16) + 8;

            int j17 = surface.surfaceY(j15, j20);
            (new FlowersFeature(new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 }))
                    .generate(world, random, j15, j17, j20);
        }

        for (int l14 = 0; l14 < 12f * strength; l14++) {
            int l19 = chunkX + random.nextInt(16) + 8;

            int j24 = chunkY + random.nextInt(16) + 8;

            int k22 = surface.surfaceY(l19, j24);
            (new GrassFeature(Blocks.tallgrass, 1)).generate(world, random, l19, k22, j24);
        }
    }
}
