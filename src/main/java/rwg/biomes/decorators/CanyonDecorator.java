package rwg.biomes.decorators;

import java.util.Random;

import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenDeadBush;
import net.minecraft.world.gen.feature.WorldGenPumpkin;
import net.minecraft.world.gen.feature.WorldGenReed;
import net.minecraft.world.gen.feature.WorldGenShrub;
import net.minecraft.world.gen.feature.WorldGenerator;

import rwg.biomes.features.BlobFeature;
import rwg.biomes.features.CactiFeature;
import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;
import rwg.world.population.SurfaceHeightLookup;

public final class CanyonDecorator extends CubicFeature {

    @Override
    protected void place(World world, Random random, int chunkX, int chunkY, SurfaceHeightLookup surface,
            NoiseGenerator noise, CellNoise cell, float strength, float river) {
        for (int l = 0; l < 1; ++l) {
            int i1 = chunkX + random.nextInt(16) + 8;
            int j1 = chunkY + random.nextInt(16) + 8;
            int k1 = surface.surfaceY(i1, j1);
            if (k1 < 70) {
                (new BlobFeature(Blocks.mossy_cobblestone, 0)).generate(world, random, i1, k1, j1);
            }
        }

        if (river > 0.8f) {
            for (int b1 = 0; b1 < 10; b1++) {
                int j6 = chunkX + random.nextInt(16) + 8;
                int k10 = chunkY + random.nextInt(16) + 8;
                int z52 = surface.surfaceY(j6, k10);

                WorldGenerator worldgenerator;
                worldgenerator = new WorldGenShrub(0, 0);
                worldgenerator.setScale(1.0D, 1.0D, 1.0D);
                worldgenerator.generate(world, random, j6, z52, k10);
            }
        } else {
            for (int b1 = 0; b1 < 5; b1++) {
                int j6 = chunkX + random.nextInt(16) + 8;
                int k10 = chunkY + random.nextInt(16) + 8;
                int z52 = surface.surfaceY(j6, k10);

                WorldGenerator worldgenerator;
                worldgenerator = new WorldGenShrub(0, 0);
                worldgenerator.setScale(1.0D, 1.0D, 1.0D);
                worldgenerator.generate(world, random, j6, z52, k10);
            }

            if (random.nextInt(3) == 0) {
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

            for (int i15 = 0; i15 < 5; i15++) {
                int i17 = chunkX + random.nextInt(16) + 8;

                int l22 = chunkY + random.nextInt(16) + 8;

                int i20 = surface.surfaceY(i17, l22);
                (new WorldGenDeadBush(Blocks.deadbush)).generate(world, random, i17, i20, l22);
            }

            for (int k18 = 0; k18 < 25; k18++) {
                int k21 = chunkX + random.nextInt(16) + 8;

                int k24 = chunkY + random.nextInt(16) + 8;

                int j23 = surface.surfaceY(k21, k24);
                (new CactiFeature(true)).generate(world, random, k21, j23, k24);
            }
        }
    }
}
