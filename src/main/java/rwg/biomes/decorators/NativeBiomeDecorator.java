package rwg.biomes.decorators;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;

/** Adapts a biome's native decoration pass to RWG's weighted decoration pipeline. */
public final class NativeBiomeDecorator extends BiomeDecorator {

    public static final float LEGACY_STRENGTH_THRESHOLD = 0.3f;

    private final BiomeGenBase biome;
    private final float strengthThreshold;

    public NativeBiomeDecorator(BiomeGenBase biome) {
        this(biome, LEGACY_STRENGTH_THRESHOLD);
    }

    public NativeBiomeDecorator(BiomeGenBase biome, float strengthThreshold) {
        if (biome == null) throw new NullPointerException("biome");
        if (Float.isNaN(strengthThreshold)) throw new IllegalArgumentException("strength threshold must be a number");
        this.biome = biome;
        this.strengthThreshold = strengthThreshold;
    }

    @Override
    public void decorate(World world, Random rand, int chunkX, int chunkY, NoiseGenerator perlin, CellNoise cell,
            float strength, float river) {
        if (strength > strengthThreshold) biome.decorate(world, rand, chunkX, chunkY);
    }

    public BiomeGenBase biome() {
        return biome;
    }

    public float strengthThreshold() {
        return strengthThreshold;
    }
}
