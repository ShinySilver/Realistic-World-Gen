package rwg.biomes.decorators;

import java.util.Random;

import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import rwg.biomes.features.BlobFeature;
import rwg.biomes.features.SmallSpruceFeature;
import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;
import rwg.world.population.SurfaceHeightLookup;

public final class PolarDecorator extends CubicFeature {

    @Override
    protected void place(World world, Random random, int chunkX, int chunkY, SurfaceHeightLookup surface,
            NoiseGenerator noise, CellNoise cell, float strength, float river) {
        if (river > 0.86f) {
            for (int j = 0; j < 5f * strength; j++) {
                int i1 = chunkX + random.nextInt(16) + 8;
                int j1 = chunkY + random.nextInt(16) + 8;
                int k1 = surface.surfaceY(i1, j1);
                if (k1 < 64) {
                    (new BlobFeature(Blocks.packed_ice, 0)).generate(world, random, i1, k1, j1);
                }
            }

            if (random.nextInt((int) (2f / strength)) == 0) {
                int j6 = chunkX + random.nextInt(16) + 8;
                int k10 = chunkY + random.nextInt(16) + 8;
                int z52 = surface.surfaceY(j6, k10);

                WorldGenerator worldgenerator = new SmallSpruceFeature(random.nextInt(2));
                worldgenerator.setScale(1.0D, 1.0D, 1.0D);
                worldgenerator.generate(world, random, j6, z52, k10);
            }
        }
    }
}
