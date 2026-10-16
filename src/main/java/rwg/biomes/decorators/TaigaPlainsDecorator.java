package rwg.biomes.decorators;

import java.util.Random;

import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenFlowers;
import net.minecraft.world.gen.feature.WorldGenLakes;
import net.minecraft.world.gen.feature.WorldGenPumpkin;
import net.minecraft.world.gen.feature.WorldGenerator;

import rwg.biomes.features.BlobFeature;
import rwg.biomes.features.FlowersFeature;
import rwg.biomes.features.GrassFeature;
import rwg.biomes.features.LogFeature;
import rwg.biomes.features.ShrubFeature;
import rwg.biomes.features.SmallPineFeature;
import rwg.biomes.features.SmallSpruceFeature;
import rwg.biomes.features.WildWheatFeature;
import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;
import rwg.world.population.SurfaceHeightLookup;

public final class TaigaPlainsDecorator extends CubicFeature {

    @Override
    protected void place(World world, Random random, int chunkX, int chunkY, SurfaceHeightLookup surface,
            NoiseGenerator noise, CellNoise cell, float strength, float river) {
        if (random.nextInt((int) (15f / strength)) == 0) {
            int i2 = chunkX + random.nextInt(16) + 8;
            int i8 = chunkY + random.nextInt(16) + 8;
            int l4 = surface.surfaceY(i2, i8);
            if (l4 > 63) {
                (new WorldGenLakes(Blocks.water)).generate(world, random, i2, l4, i8);
            }
        }

        // boulders
        for (int l = 0; l < 3f * strength; ++l) {
            int i1 = chunkX + random.nextInt(16) + 8;
            int j1 = chunkY + random.nextInt(16) + 8;
            int k1 = surface.surfaceY(i1, j1);
            if (k1 < 95 && (k1 < 64 || random.nextInt(7) == 0)) {
                (new BlobFeature(Blocks.mossy_cobblestone, 0)).generate(world, random, i1, k1, j1);
            }
        }

        // trees
        float l = noise.noise2(chunkX / 100f, chunkY / 100f) * 6f + 0.8f;
        for (int b1 = 0; b1 < l * 4f * strength; b1++) {
            int j6 = chunkX + random.nextInt(16) + 8;
            int k10 = chunkY + random.nextInt(16) + 8;
            int z52 = surface.surfaceY(j6, k10);

            WorldGenerator worldgenerator = random.nextInt(4) == 0 ? new SmallSpruceFeature(1 + random.nextInt(2))
                    : random.nextInt(6) == 0 ? new SmallPineFeature(1 + random.nextInt(3), 4 + random.nextInt(4))
                            : new SmallPineFeature(4 + random.nextInt(6), 5 + random.nextInt(10));
            worldgenerator.setScale(1.0D, 1.0D, 1.0D);
            worldgenerator.generate(world, random, j6, z52, k10);
        }

        if (l > 0f && random.nextInt(6) == 0) {
            int x22 = chunkX + random.nextInt(16) + 8;
            int z22 = chunkY + random.nextInt(16) + 8;
            int y22 = surface.surfaceY(x22, z22);
            (new LogFeature(1, 3 + random.nextInt(4), false)).generate(world, random, x22, y22, z22);
        }

        for (int b = 0; b < 2f * strength; b++) {
            int i1 = chunkX + random.nextInt(16) + 8;
            int j1 = chunkY + random.nextInt(16) + 8;
            int k1 = surface.surfaceY(i1, j1);
            if (random.nextInt(10) == 0) {
                (new ShrubFeature(random.nextInt(5) + 4, random.nextInt(2), random.nextInt(2)))
                        .generate(world, random, i1, k1, j1);
            } else {
                (new ShrubFeature(random.nextInt(4) + 1, random.nextInt(2), random.nextInt(2)))
                        .generate(world, random, i1, k1, j1);
            }
        }

        if (random.nextInt((int) (3f / strength)) == 0) {
            int k15 = chunkX + random.nextInt(16) + 8;

            int k20 = chunkY + random.nextInt(16) + 8;

            int k17 = surface.surfaceY(k15, k20);

            if (random.nextBoolean()) {
                (new WorldGenFlowers(Blocks.brown_mushroom)).generate(world, random, k15, k17, k20);
            } else {
                (new WorldGenFlowers(Blocks.red_mushroom)).generate(world, random, k15, k17, k20);
            }
        }

        if (random.nextInt((int) (20f / strength)) == 0) {
            int j16 = chunkX + random.nextInt(16) + 8;

            int j21 = chunkY + random.nextInt(16) + 8;

            int j18 = surface.surfaceY(j16, j21);
            (new WorldGenPumpkin()).generate(world, random, j16, j18, j21);
        }

        if (random.nextInt((int) (150f / strength)) == 0) {
            int k21 = chunkX + random.nextInt(16) + 8;

            int k24 = chunkY + random.nextInt(16) + 8;

            int j23 = surface.surfaceY(k21, k24);
            (new WildWheatFeature(random.nextInt(3))).generate(world, random, k21, j23, k24);
        }

        for (int f23 = 0; f23 < 4f * strength; f23++) {
            int j15 = chunkX + random.nextInt(16) + 8;

            int j20 = chunkY + random.nextInt(16) + 8;

            int j17 = surface.surfaceY(j15, j20);
            (new FlowersFeature(new int[] { 9, 0, 3 })).generate(world, random, j15, j17, j20);
        }

        for (int l14 = 0; l14 < 10f * strength; l14++) {
            int l19 = chunkX + random.nextInt(16) + 8;

            int j24 = chunkY + random.nextInt(16) + 8;

            int k22 = surface.surfaceY(l19, j24);
            (new GrassFeature(Blocks.tallgrass, 1)).generate(world, random, l19, k22, j24);
        }
    }
}
