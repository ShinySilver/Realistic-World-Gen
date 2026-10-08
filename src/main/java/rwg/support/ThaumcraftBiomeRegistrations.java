package rwg.support;

import static rwg.registry.BiomeRegistration.builder;
import static rwg.registry.BiomeRegistration.variant;

import net.minecraft.init.Blocks;
import net.minecraft.world.biome.BiomeGenBase;

import rwg.biomes.surface.SurfaceBase;
import rwg.biomes.surface.SurfaceGrassland;
import rwg.biomes.terrain.TerrainSmallSupport;
import rwg.biomes.terrain.TerrainSwampMountain;
import rwg.biomes.terrain.TerrainGenericHills;
import rwg.biomes.terrain.TerrainGenericMountain;
import rwg.registry.BiomeRegistry;
import rwg.registry.Climate;
import rwg.registry.TerrainCategory;
import rwg.registry.TerrainSubcategory;

public final class ThaumcraftBiomeRegistrations {

    private ThaumcraftBiomeRegistrations() {}
    /*
     * Resolve by name because Thaumcraft biome IDs are pack-configurable. GTNH currently assigns Eerie 190, Eldritch
     * Lands 191, Magical Forest 192, and Tainted Land 193.
     */

    public static void register(BiomeRegistry registry) {
        BiomeGenBase[] b = BiomeGenBase.getBiomeGenArray();

        for (int i = 0; i < 256; i++) {
            if (b[i] != null) {
                if ("Tainted Land".equals(b[i].biomeName)) {
                    for (Climate climate : Climate.values()) {
                        registry.register(
                                builder(b[i]).climate(climate).river(registry.temperateRiver())
                                        .subcategory(TerrainSubcategory.SMALL).variant(
                                                variant(TerrainCategory.PLAIN).terrain(new TerrainSmallSupport())
                                                        .surfaces(
                                                                new SurfaceBase[] { new SurfaceGrassland(
                                                                        b[i].topBlock,
                                                                        b[i].fillerBlock,
                                                                        Blocks.stone,
                                                                        Blocks.cobblestone) }))
                                        .variant(
                                                variant(TerrainCategory.HILLS).terrain(new TerrainGenericHills())
                                                        .surfaces(
                                                                new SurfaceBase[] { new SurfaceGrassland(
                                                                        b[i].topBlock,
                                                                        b[i].fillerBlock,
                                                                        Blocks.stone,
                                                                        Blocks.cobblestone) }))
                                        .variant(
                                                variant(TerrainCategory.MOUNTAIN).terrain(new TerrainGenericMountain())
                                                        .surfaces(
                                                                new SurfaceBase[] { new SurfaceGrassland(
                                                                        b[i].topBlock,
                                                                        b[i].fillerBlock,
                                                                        Blocks.stone,
                                                                        Blocks.cobblestone) })));
                    }
                }

                if ("Magical Forest".equals(b[i].biomeName)) {
                    registry.register(
                            builder(b[i]).climate(Climate.SNOW).river(registry.river(Climate.COLD)).variant(
                                    variant(TerrainCategory.SMALL_ISLAND).terrain(new TerrainSwampMountain(135f, 300f))
                                            .surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            b[i].topBlock,
                                                            b[i].fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone) })));
                    registry.register(
                            builder(b[i]).climate(Climate.COLD).river(registry.river(Climate.COLD)).variant(
                                    variant(TerrainCategory.SMALL_ISLAND).terrain(new TerrainSwampMountain(135f, 300f))
                                            .surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            b[i].topBlock,
                                                            b[i].fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone) })));
                }
            }
        }
    }

}
