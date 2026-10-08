package rwg.biomes.decorators;

import java.util.Random;

import net.minecraft.world.World;

import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;

public class BiomeDecorator {

    public BiomeDecorator() {}

    public void decorate(World world, Random rand, int chunkX, int chunkY, NoiseGenerator perlin, CellNoise cell,
            float strength, float river) {}
}
