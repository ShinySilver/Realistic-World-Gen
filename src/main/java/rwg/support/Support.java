package rwg.support;

import java.util.ArrayList;

import net.minecraft.world.biome.BiomeGenBase;

import cpw.mods.fml.common.Loader;
import rwg.api.RWGBiomes;
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
    public static RealisticBiomeBase oceanShallowKelp;
    public static RealisticBiomeBase oceanShallowSnow;
    public static RealisticBiomeBase oceanShallowCold;
    public static RealisticBiomeBase oceanShallowTemperate;
    public static RealisticBiomeBase oceanShallowCoral;
    public static RealisticBiomeBase oceanShallowHot;
    public static RealisticBiomeBase oceanShallowWet;
    public static RealisticBiomeBase oceanDeep;
    public static RealisticBiomeBase oceanDeepSnow;
    public static RealisticBiomeBase oceanDeepCold;
    public static RealisticBiomeBase oceanDeepHot;
    public static RealisticBiomeBase oceanDeepWet;

    public enum BiomeCategory {
        SNOW,
        COLD,
        HOT,
        WET,
        SMALL,
        TEST
    }

    public static void init() {
        init(true);
    }

    public static void init(boolean loadModBiomes) {
        biomes_snow = new ArrayList<RealisticBiomeBase>();
        biomes_cold = new ArrayList<RealisticBiomeBase>();
        biomes_hot = new ArrayList<RealisticBiomeBase>();
        biomes_wet = new ArrayList<RealisticBiomeBase>();
        biomes_small = new ArrayList<RealisticBiomeBase>();
        biomes_test = new ArrayList<RealisticBiomeBase>();
        volcanoIsland = null;
        oceanShallowSnow =
                new RealisticBiomeOcean(RWGBiomes.baseOceanIce, true, false, "RealisticBiomeOceanSnowShallow");
        oceanShallowCold =
                new RealisticBiomeOcean(RWGBiomes.baseOceanCold, true, false, "RealisticBiomeOceanColdShallow");
        oceanShallowKelp = oceanShallowCold;
        oceanShallowTemperate = oceanShallowCold;
        oceanShallowCoral = oceanShallowTemperate;
        oceanShallowHot =
                new RealisticBiomeOcean(RWGBiomes.baseOceanHot, true, false, "RealisticBiomeOceanHotShallow");
        oceanShallowWet =
                new RealisticBiomeOcean(RWGBiomes.baseOceanWet, true, false, "RealisticBiomeOceanWetShallow");
        oceanDeepSnow = new RealisticBiomeOcean(RWGBiomes.baseOceanIce, false, false, "RealisticBiomeOceanSnowDeep");
        oceanDeepCold =
                new RealisticBiomeOcean(RWGBiomes.baseOceanCold, false, false, "RealisticBiomeOceanColdDeep");
        oceanDeepHot = new RealisticBiomeOcean(RWGBiomes.baseOceanHot, false, false, "RealisticBiomeOceanHotDeep");
        oceanDeepWet = new RealisticBiomeOcean(RWGBiomes.baseOceanWet, false, false, "RealisticBiomeOceanWetDeep");
        oceanDeep = oceanDeepCold;

        if (loadModBiomes && Loader.isModLoaded("BiomesOPlenty")) {
            SupportBOP.init();
        }

        if (loadModBiomes && Loader.isModLoaded("ExtrabiomesXL")) {
            SupportEBXL.init();
        }

        if (loadModBiomes && Loader.isModLoaded("Thaumcraft")) {
            SupportTC.init();
        }
        if (loadModBiomes && Loader.isModLoaded("ChromatiCraft")) {
            SupportCC.init();
        }
        /** ChromatiCraft is non-supported content. if this ever errors out in some way feel free to remove this. */
    }

    public static void addBiome(RealisticBiomeSupport b, BiomeCategory cat) {
        try {
            switch (cat) {
                case SNOW:
                    biomes_snow.add(b);
                    break;
                case COLD:
                    biomes_cold.add(b);
                    break;
                case HOT:
                    biomes_hot.add(b);
                    break;
                case WET:
                    biomes_wet.add(b);
                    break;
                case SMALL:
                    biomes_small.add(b);
                    break;
                case TEST:
                    biomes_test.add(b);
                    break;
            }
        } catch (Error e) {
            System.out.println("RWG Support: failed to add biome");
        }
    }
}
