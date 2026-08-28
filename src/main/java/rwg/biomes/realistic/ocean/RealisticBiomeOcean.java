package rwg.biomes.realistic.ocean;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import rwg.biomes.realistic.RealisticBiomeBase;
import rwg.util.CellNoise;
import rwg.util.NoiseGenerator;

public class RealisticBiomeOcean extends RealisticBiomeBase {

    public enum DepthRange {
        SHALLOW,
        DEEP
    }

    private final boolean shallow;
    private final boolean decorateBaseBiome;
    private final String variantName;

    public RealisticBiomeOcean(BiomeGenBase biome, boolean shallow, boolean decorateBaseBiome) {
        this(biome, shallow, decorateBaseBiome, shallow ? "RealisticBiomeOceanShallow" : "RealisticBiomeOceanDeep");
    }

    public RealisticBiomeOcean(BiomeGenBase biome, boolean shallow, boolean decorateBaseBiome, String variantName) {
        super(0, biome);
        this.shallow = shallow;
        this.decorateBaseBiome = decorateBaseBiome;
        this.variantName = variantName;
    }

    public String getVariantName() {
        return variantName;
    }

    public DepthRange getDepthRange() {
        return shallow ? DepthRange.SHALLOW : DepthRange.DEEP;
    }

    @Override
    public void rDecorate(World world, Random rand, int chunkX, int chunkY, NoiseGenerator perlin, CellNoise cell,
            float strength, float river) {}

    public void rDecorateAfterIce(World world, Random rand, int chunkX, int chunkY, float strength) {
        if (decorateBaseBiome && strength > 0.3f) {
            baseBiome.decorate(world, rand, chunkX, chunkY);
        }
    }

    @Override
    public float rNoise(NoiseGenerator perlin, CellNoise cell, int x, int y, float ocean, float border, float river) {
        float height = shallow ? 52f : 34f;
        return height + perlin.noise2(x / 220f, y / 220f) * 4f + perlin.noise2(x / 55f, y / 55f) * 1.5f;
    }

    @Override
    public float rNoise(NoiseGenerator perlin, CellNoise cell, int x, int y, float ocean, float border, float river,
            float continent, float abyssalBasin) {
        float offshore = Math.max(0f, -continent);
        float depth = 4f + smoothstep(0f, 300f, offshore) * 9f
                + smoothstep(300f, 800f, offshore) * 14f
                + smoothstep(800f, 1600f, offshore) * 8f;
        float roughness = smoothstep(40f, 500f, offshore);
        float floor = 63f - depth + perlin.noise2(x / 520f, y / 520f) * (1.5f + roughness * 2.5f)
                + perlin.noise2(x / 115f, y / 115f) * (1f + roughness * 1.5f);
        return Math.max(7f, floor - abyssalBasin * 18f);
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float t = Math.max(0f, Math.min(1f, (value - edge0) / (edge1 - edge0)));
        return t * t * (3f - 2f * t);
    }

    @Override
    public void rReplace(Block[] blocks, byte[] metadata, int i, int j, int x, int y, int depth, World world,
            Random rand, NoiseGenerator perlin, CellNoise cell, float[] noise, float river, BiomeGenBase[] base) {
        Block surface = shallow ? Blocks.sand : Blocks.gravel;
        int column = (y * 16 + x) * 256;
        for (int level = 255; level >= 0; level--) {
            Block block = blocks[column + level];
            if (block == Blocks.air || block == Blocks.water) {
                depth = -1;
            } else if (block == Blocks.stone) {
                depth++;
                if (depth < 6) {
                    blocks[column + level] = surface;
                    metadata[column + level] = 0;
                }
            }
        }
    }
}
