package rwg.biomes.decorators;

import java.util.Random;

import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import rwg.biomes.features.BlobFeature;
import rwg.biomes.features.SmallSpruceFeature;
import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;

public final class PolarDecorator extends BiomeDecorator {

    @Override
    public void decorate(World world, Random rand, int chunkX, int chunkY, NoiseGenerator perlin, CellNoise cell,
            float strength, float river) {
        if (river > 0.86f) {
            for (int j = 0; j < 5f * strength; j++) {
                int i1 = chunkX + rand.nextInt(16) + 8;
                int j1 = chunkY + rand.nextInt(16) + 8;
                int k1 = world.getHeightValue(i1, j1);
                if (k1 < 64) {
                    (new BlobFeature(Blocks.packed_ice, 0)).generate(world, rand, i1, k1, j1);
                }
            }

            if (rand.nextInt((int) (2f / strength)) == 0) {
                int j6 = chunkX + rand.nextInt(16) + 8;
                int k10 = chunkY + rand.nextInt(16) + 8;
                int z52 = world.getHeightValue(j6, k10);

                WorldGenerator worldgenerator = new SmallSpruceFeature(rand.nextInt(2));
                worldgenerator.setScale(1.0D, 1.0D, 1.0D);
                worldgenerator.generate(world, rand, j6, z52, k10);
            }
        }
    }
}
