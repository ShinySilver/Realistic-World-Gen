package rwg.biomes.decorators;

import java.util.Random;

import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenDeadBush;
import net.minecraft.world.gen.feature.WorldGenShrub;
import net.minecraft.world.gen.feature.WorldGenerator;

import rwg.biomes.features.BlobFeature;
import rwg.biomes.features.CactiFeature;
import rwg.biomes.features.FlowersFeature;
import rwg.biomes.features.GrassFeature;
import rwg.biomes.features.SavannahFeature;
import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;

public final class MesaPlainsDecorator extends BiomeDecorator {

    @Override
    public void decorate(World world, Random rand, int chunkX, int chunkY, NoiseGenerator perlin, CellNoise cell,
            float strength, float river) {
        for (int l = 0; l < 1f * strength; ++l) {
            int i1 = chunkX + rand.nextInt(16) + 8;
            int j1 = chunkY + rand.nextInt(16) + 8;
            int k1 = world.getHeightValue(i1, j1);
            if (k1 < 83) {
                (new BlobFeature(Blocks.cobblestone, 0)).generate(world, rand, i1, k1, j1);
            }
        }

        if (river > 0.7f) {
            for (int b33 = 0; b33 < 6f * strength; b33++) {
                int j6 = chunkX + rand.nextInt(16) + 8;
                int k10 = chunkY + rand.nextInt(16) + 8;
                int z52 = world.getHeightValue(j6, k10);

                if (z52 < 90) {
                    WorldGenerator worldgenerator = rand.nextInt(3) != 0 ? new WorldGenShrub(0, 0)
                            : new SavannahFeature(1, false);
                    worldgenerator.setScale(1.0D, 1.0D, 1.0D);
                    worldgenerator.generate(world, rand, j6, z52, k10);
                }
            }
        } else {
            if (rand.nextInt((int) (2f / strength)) == 0) {
                int j6 = chunkX + rand.nextInt(16) + 8;
                int k10 = chunkY + rand.nextInt(16) + 8;
                int z52 = world.getHeightValue(j6, k10);

                if (z52 < 90) {
                    WorldGenerator worldgenerator = rand.nextInt(4) != 0 ? new WorldGenShrub(0, 0)
                            : rand.nextInt(12) == 0 ? new SavannahFeature(0) : new SavannahFeature(1);
                    worldgenerator.setScale(1.0D, 1.0D, 1.0D);
                    worldgenerator.generate(world, rand, j6, z52, k10);
                }
            }
        }

        for (int i15 = 0; i15 < 5; i15++) {
            int i17 = chunkX + rand.nextInt(16) + 8;
            int i20 = 64 + rand.nextInt(100);
            int l22 = chunkY + rand.nextInt(16) + 8;
            (new WorldGenDeadBush(Blocks.deadbush)).generate(world, rand, i17, i20, l22);
        }

        for (int k18 = 0; k18 < 15; k18++) {
            int k21 = chunkX + rand.nextInt(16) + 8;
            int j23 = 64 + rand.nextInt(80);
            int k24 = chunkY + rand.nextInt(16) + 8;
            (new CactiFeature(false)).generate(world, rand, k21, j23, k24);
        }

        for (int f23 = 0; f23 < 3; f23++) {
            int j15 = chunkX + rand.nextInt(16) + 8;
            int j17 = rand.nextInt(128);
            int j20 = chunkY + rand.nextInt(16) + 8;
            (new FlowersFeature(new int[] { 9, 9, 9, 9, 3, 3, 3, 3, 3, 2, 2, 2, 11, 11, 11 }))
                    .generate(world, rand, j15, j17, j20);
        }

        for (int l14 = 0; l14 < 8f * strength; l14++) {
            int l19 = chunkX + rand.nextInt(16) + 8;
            int k22 = 60 + rand.nextInt(70);
            int j24 = chunkY + rand.nextInt(16) + 8;
            (new GrassFeature(Blocks.tallgrass, 1)).generate(world, rand, l19, k22, j24);
        }
    }
}
