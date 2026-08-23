package rwg.support;

import java.util.ArrayList;

import net.minecraft.world.biome.BiomeGenBase;

import cpw.mods.fml.common.Loader;
import rwg.biomes.realistic.RealisticBiomeBase;
import rwg.biomes.realistic.ocean.RealisticBiomeOcean;

public class Support {

    public static ArrayList<RealisticBiomeBase> biomes_snow;
    public static ArrayList<RealisticBiomeBase> biomes_cold;
    public static ArrayList<RealisticBiomeBase> biomes_hot;
    public static ArrayList<RealisticBiomeBase> biomes_wet;
    public static ArrayList<RealisticBiomeBase> biomes_small;
    public static ArrayList<RealisticBiomeBase> biomes_test;
    public static RealisticBiomeBase volcanoIsland;
    public static RealisticBiomeBase oceanShallowTemperate;
    public static RealisticBiomeBase oceanShallowWarm;
    public static RealisticBiomeBase oceanDeep;

    public enum BiomeCategory {
        SNOW,
        COLD,
        HOT,
        WET,
        SMALL,
        TEST
    }

    public static void init() {
        biomes_snow = new ArrayList<RealisticBiomeBase>();
        biomes_cold = new ArrayList<RealisticBiomeBase>();
        biomes_hot = new ArrayList<RealisticBiomeBase>();
        biomes_wet = new ArrayList<RealisticBiomeBase>();
        biomes_small = new ArrayList<RealisticBiomeBase>();
        biomes_test = new ArrayList<RealisticBiomeBase>();
        volcanoIsland = null;
        oceanShallowTemperate = new RealisticBiomeOcean(BiomeGenBase.ocean, true, false);
        oceanShallowWarm = new RealisticBiomeOcean(BiomeGenBase.ocean, true, false);
        oceanDeep = new RealisticBiomeOcean(BiomeGenBase.deepOcean, false, false);

        if (Loader.isModLoaded("BiomesOPlenty")) {
            SupportBOP.init();
        }

        if (Loader.isModLoaded("ExtrabiomesXL")) {
            SupportEBXL.init();
        }

        if (Loader.isModLoaded("Thaumcraft")) {
            SupportTC.init();
        }
        if (Loader.isModLoaded("ChromatiCraft")) {
            SupportCC.init();
        }
        /** ChromatiCraft is non-supported content. if this ever errors out in some way feel free to remove this. */
    }

    public static void addBiome(RealisticBiomeSupport b, BiomeCategory cat) {
        try {
            switch (cat) {
                case SNOW:
                    biomes_snow.add(b);
                case COLD:
                    biomes_cold.add(b);
                case HOT:
                    biomes_hot.add(b);
                case WET:
                    biomes_wet.add(b);
                case SMALL:
                    biomes_small.add(b);
                case TEST:
                    biomes_test.add(b);
            }
        } catch (Error e) {
            System.out.println("RWG Support: failed to add biome");
        }
    }
}
