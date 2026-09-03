package rwg.support;

import net.minecraft.init.Blocks;
import net.minecraft.world.biome.BiomeGenBase;

import rwg.api.RWGBiomes;
import rwg.config.ConfigRWG;
import rwg.support.Support.BiomeCategory;
import rwg.surface.SurfaceGrassland;
import rwg.terrain.TerrainHighland;
import rwg.terrain.TerrainSmallSupport;

public class SupportTC {
    /*
     * Resolve by name because Thaumcraft biome IDs are pack-configurable. GTNH currently assigns Eerie 190, Eldritch
     * Lands 191, Magical Forest 192, and Tainted Land 193.
     */

    public static void init() {
        BiomeGenBase[] b = BiomeGenBase.getBiomeGenArray();

        for (int i = 0; i < 256; i++) {
            if (b[i] != null) {
                if ("Tainted Land".equals(b[i].biomeName)) {
                    Support.addBiome(
                            new RealisticBiomeSupport(
                                    b[i],
                                    RWGBiomes.baseRiverTemperate,
                                    new TerrainSmallSupport(),
                                    new SurfaceGrassland(
                                            b[i].topBlock,
                                            b[i].fillerBlock,
                                            Blocks.stone,
                                            Blocks.cobblestone)),
                            BiomeCategory.SMALL);
                }

                if ("Magical Forest".equals(b[i].biomeName)) {
                    if (ConfigRWG.generateLargeThaumcraftBiomes) {
                        Support.addBiome(
                                new RealisticBiomeSupport(
                                        b[i],
                                        RWGBiomes.baseTemperateForest,
                                        new TerrainHighland(6f, 120f, 65f, 150f),
                                        new SurfaceGrassland(
                                                b[i].topBlock,
                                                b[i].fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone)),
                                BiomeCategory.WET);
                    } else {
                        Support.addBiome(
                                new RealisticBiomeSupport(
                                        b[i],
                                        RWGBiomes.baseRiverTemperate,
                                        new TerrainSmallSupport(),
                                        new SurfaceGrassland(
                                                b[i].topBlock,
                                                b[i].fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone)),
                                BiomeCategory.SMALL);
                    }
                }
            }
        }
    }
}
