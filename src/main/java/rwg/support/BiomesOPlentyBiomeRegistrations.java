package rwg.support;

import static rwg.registry.BiomeRegistration.builder;
import static rwg.registry.BiomeRegistration.variant;

import net.minecraft.init.Blocks;

import biomesoplenty.api.content.BOPCBiomes;
import rwg.biomes.decorators.BiomeDecorator;
import rwg.biomes.decorators.RiverOasisDecorator;
import rwg.biomes.surface.SurfaceBase;
import rwg.biomes.surface.SurfaceCanyon;
import rwg.biomes.surface.SurfaceDuneValley;
import rwg.biomes.surface.SurfaceGrassland;
import rwg.biomes.surface.SurfaceGrasslandMix1;
import rwg.biomes.surface.SurfaceMountainSnow;
import rwg.biomes.surface.SurfaceMountainStone;
import rwg.biomes.surface.SurfaceRiverOasis;
import rwg.biomes.terrain.TerrainCanyon;
import rwg.biomes.terrain.TerrainConstant;
import rwg.biomes.terrain.TerrainDuneValley;
import rwg.biomes.terrain.TerrainGrasslandHills;
import rwg.biomes.terrain.TerrainHighland;
import rwg.biomes.terrain.TerrainHilly;
import rwg.biomes.terrain.TerrainMarsh;
import rwg.biomes.terrain.TerrainMesa;
import rwg.biomes.terrain.TerrainMountainRiver;
import rwg.biomes.terrain.TerrainMountainSpikes;
import rwg.biomes.terrain.TerrainSwampMountain;
import rwg.biomes.terrain.TerrainSwampRiver;
import rwg.biomes.terrain.TerrainGenericHills;
import rwg.biomes.terrain.TerrainGenericMountain;
import rwg.registry.BiomeRegistry;
import rwg.registry.Climate;
import rwg.registry.TerrainCategory;
import rwg.registry.TerrainSubcategory;

public final class BiomesOPlentyBiomeRegistrations {

    private BiomesOPlentyBiomeRegistrations() {}

    public static void register(BiomeRegistry registry) {

        if (BOPCBiomes.kelpForest != null) {
            for (Climate climate : new Climate[] { Climate.SNOW, Climate.COLD }) {
                registry.register(
                        builder(BOPCBiomes.kelpForest).climate(climate).river(registry.temperateRiver()).variant(
                                variant(TerrainCategory.SHALLOW_OCEAN).terrain(new TerrainConstant(52f)).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.kelpForest.topBlock,
                                                BOPCBiomes.kelpForest.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) })));
            }
        }
        if (BOPCBiomes.coralReef != null) {
            for (Climate climate : new Climate[] { Climate.HOT, Climate.WET }) {
                registry.register(
                        builder(BOPCBiomes.coralReef).climate(climate).river(registry.temperateRiver()).variant(
                                variant(TerrainCategory.SHALLOW_OCEAN).terrain(new TerrainConstant(52f)).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.coralReef.topBlock,
                                                BOPCBiomes.coralReef.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) })));
            }
        }
        // BAMBOO FOREST
        registry.register(
                builder(BOPCBiomes.bambooForest).climate(Climate.WET).river(registry.river(Climate.WET)).variant(
                        variant(TerrainCategory.PLAIN)
                                .terrain(new TerrainSwampMountain(135f, 300f)).surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.bambooForest.topBlock,
                                                BOPCBiomes.bambooForest.fillerBlock,
                                                false,
                                                null,
                                                0.95f) })));

        // BAYOU
        registry.register(
                builder(BOPCBiomes.bayou).climate(Climate.WET).river(registry.river(Climate.WET))
                        .subcategory(TerrainSubcategory.LITTORAL).variant(
                                variant(TerrainCategory.SWAMP).terrain(new TerrainSwampRiver()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.bayou.topBlock,
                                                BOPCBiomes.bayou.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) })));

        // BOG
        registry.register(
                builder(BOPCBiomes.bog).climate(Climate.HOT).river(registry.river(Climate.HOT)).variant(
                        variant(TerrainCategory.PLAIN).terrain(new TerrainMarsh()).surfaces(
                                new SurfaceBase[] { new SurfaceGrassland(
                                        BOPCBiomes.bog.topBlock,
                                        BOPCBiomes.bog.fillerBlock,
                                        Blocks.stone,
                                        Blocks.cobblestone) })));

        // BOREAL FOREST
        registry.register(
                builder(BOPCBiomes.borealForest).climate(Climate.SNOW).river(registry.river(Climate.COLD))
                        .subcategory(TerrainSubcategory.HOT_BORDER).variant(
                                variant(TerrainCategory.PLAIN).terrain(new TerrainMountainSpikes()).surfaces(
                                        new SurfaceBase[] { new SurfaceMountainSnow(
                                                BOPCBiomes.borealForest.topBlock,
                                                BOPCBiomes.borealForest.fillerBlock,
                                                true,
                                                Blocks.sand,
                                                0.45f,
                                                1.5f,
                                                60f,
                                                65f,
                                                0.4f,
                                                130f,
                                                50f,
                                                1.5f) }))
                        .variant(
                                variant(TerrainCategory.HILLS).terrain(new TerrainGenericHills()).surfaces(
                                        new SurfaceBase[] { new SurfaceMountainSnow(
                                                BOPCBiomes.borealForest.topBlock,
                                                BOPCBiomes.borealForest.fillerBlock,
                                                true,
                                                Blocks.sand,
                                                0.45f,
                                                1.5f,
                                                60f,
                                                65f,
                                                0.4f,
                                                130f,
                                                50f,
                                                1.5f) }))
                        .variant(
                                variant(TerrainCategory.MOUNTAIN).terrain(new TerrainGenericMountain()).surfaces(
                                        new SurfaceBase[] { new SurfaceMountainSnow(
                                                BOPCBiomes.borealForest.topBlock,
                                                BOPCBiomes.borealForest.fillerBlock,
                                                true,
                                                Blocks.sand,
                                                0.45f,
                                                1.5f,
                                                60f,
                                                65f,
                                                0.4f,
                                                130f,
                                                50f,
                                                1.5f) })));

        // BRUSHLAND
        registry.register(
                builder(BOPCBiomes.brushland).climate(Climate.HOT).river(registry.river(Climate.HOT)).variant(
                        variant(TerrainCategory.PLAIN)
                                .terrain(new TerrainGrasslandHills(90f, 180f, 13f, 100f, 38f, 260f, 71f)).surfaces(
                                        new SurfaceBase[] { new SurfaceGrasslandMix1(
                                                BOPCBiomes.brushland.topBlock,
                                                BOPCBiomes.brushland.fillerBlock,
                                                Blocks.sand,
                                                Blocks.stone,
                                                Blocks.cobblestone,
                                                13f,
                                                0.27f) }))
                .variant(
                        variant(TerrainCategory.HILLS)
                                .terrain(new TerrainGenericHills()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrasslandMix1(
                                                BOPCBiomes.brushland.topBlock,
                                                BOPCBiomes.brushland.fillerBlock,
                                                Blocks.sand,
                                                Blocks.stone,
                                                Blocks.cobblestone,
                                                13f,
                                                0.27f) }))
                .variant(
                        variant(TerrainCategory.MOUNTAIN)
                                .terrain(new TerrainGenericMountain()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrasslandMix1(
                                                BOPCBiomes.brushland.topBlock,
                                                BOPCBiomes.brushland.fillerBlock,
                                                Blocks.sand,
                                                Blocks.stone,
                                                Blocks.cobblestone,
                                                13f,
                                                0.27f) })));
        registry.register(
                builder(BOPCBiomes.brushland).climate(Climate.HOT).river(registry.oasisRiver())

                        .decorators(new BiomeDecorator[] { new RiverOasisDecorator() }).selectable(
                                true)
                        .variant(
                                variant(TerrainCategory.PLAIN)
                                        .terrain(
                                                new TerrainDuneValley(300f))
                                        .surfaces(
                                                new SurfaceBase[] { new SurfaceDuneValley(
                                                        BOPCBiomes.brushland.topBlock,
                                                        BOPCBiomes.brushland.fillerBlock,
                                                        300f,
                                                        false,
                                                        true,
                                                        registry.hotDesert()), new SurfaceRiverOasis(), }))
                        .variant(
                                variant(TerrainCategory.HILLS)
                                        .terrain(new TerrainGenericHills())
                                        .surfaces(
                                                new SurfaceBase[] { new SurfaceDuneValley(
                                                        BOPCBiomes.brushland.topBlock,
                                                        BOPCBiomes.brushland.fillerBlock,
                                                        300f,
                                                        false,
                                                        true,
                                                        registry.hotDesert()), new SurfaceRiverOasis(), }))
                        .variant(
                                variant(TerrainCategory.MOUNTAIN)
                                        .terrain(new TerrainGenericMountain())
                                        .surfaces(
                                                new SurfaceBase[] { new SurfaceDuneValley(
                                                        BOPCBiomes.brushland.topBlock,
                                                        BOPCBiomes.brushland.fillerBlock,
                                                        300f,
                                                        false,
                                                        true,
                                                        registry.hotDesert()), new SurfaceRiverOasis(), })));

        // CANYON
        registry.register(
                builder(BOPCBiomes.canyon).climate(Climate.HOT).river(registry.river(Climate.HOT)).variant(
                        variant(TerrainCategory.CANYON)
                                .terrain(new TerrainCanyon(true, 35f, 160f, 60f, 40f, 69f)).surfaces(
                                        new SurfaceBase[] { new SurfaceCanyon(
                                                BOPCBiomes.canyon.topBlock,
                                                BOPCBiomes.canyon.fillerBlock,
                                                (byte) 0,
                                                0) })));

        // CHAPARRAL
        registry.register(
                builder(BOPCBiomes.chaparral).climate(Climate.HOT).river(registry.river(Climate.HOT))
                        .subcategory(TerrainSubcategory.COLD_BORDER).variant(
                                variant(TerrainCategory.PLAIN)
                                        .terrain(new TerrainGrasslandHills(90f, 180f, 13f, 100f, 38f, 260f, 71f))
                                        .surfaces(
                                                new SurfaceBase[] { new SurfaceGrasslandMix1(
                                                        BOPCBiomes.chaparral.topBlock,
                                                        BOPCBiomes.chaparral.fillerBlock,
                                                        Blocks.sand,
                                                        Blocks.stone,
                                                        Blocks.cobblestone,
                                                        26f,
                                                        0.35f) }))
                        .variant(
                                variant(TerrainCategory.HILLS)
                                        .terrain(new TerrainGenericHills())
                                        .surfaces(
                                                new SurfaceBase[] { new SurfaceGrasslandMix1(
                                                        BOPCBiomes.chaparral.topBlock,
                                                        BOPCBiomes.chaparral.fillerBlock,
                                                        Blocks.sand,
                                                        Blocks.stone,
                                                        Blocks.cobblestone,
                                                        26f,
                                                        0.35f) }))
                        .variant(
                                variant(TerrainCategory.MOUNTAIN)
                                        .terrain(new TerrainGenericMountain())
                                        .surfaces(
                                                new SurfaceBase[] { new SurfaceGrasslandMix1(
                                                        BOPCBiomes.chaparral.topBlock,
                                                        BOPCBiomes.chaparral.fillerBlock,
                                                        Blocks.sand,
                                                        Blocks.stone,
                                                        Blocks.cobblestone,
                                                        26f,
                                                        0.35f) })));

        // CHERRYBLOSSOM GROVE
        registry.register(
                builder(BOPCBiomes.cherryBlossomGrove).climate(Climate.COLD).river(registry.temperateRiver()).variant(
                        variant(TerrainCategory.PLAIN)
                                .terrain(new TerrainHighland(6f, 120f, 65f, 200f)).surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.borealForest.topBlock,
                                                BOPCBiomes.borealForest.fillerBlock,
                                                true,
                                                Blocks.sand,
                                                0.45f,
                                                1.5f,
                                                60f,
                                                65f,
                                                1.5f) }))
                .variant(
                        variant(TerrainCategory.HILLS)
                                .terrain(new TerrainGenericHills()).surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.borealForest.topBlock,
                                                BOPCBiomes.borealForest.fillerBlock,
                                                true,
                                                Blocks.sand,
                                                0.45f,
                                                1.5f,
                                                60f,
                                                65f,
                                                1.5f) }))
                .variant(
                        variant(TerrainCategory.MOUNTAIN)
                                .terrain(new TerrainGenericMountain()).surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.borealForest.topBlock,
                                                BOPCBiomes.borealForest.fillerBlock,
                                                true,
                                                Blocks.sand,
                                                0.45f,
                                                1.5f,
                                                60f,
                                                65f,
                                                1.5f) })));

        // CONIFEROUS FOREST
        registry.register(
                builder(BOPCBiomes.coniferousForest).climate(Climate.COLD).river(registry.river(Climate.COLD)).variant(
                        variant(TerrainCategory.PLAIN).terrain(new TerrainMountainRiver())
                                .surfaces(
                                        new SurfaceBase[] { new SurfaceMountainSnow(
                                                Blocks.grass,
                                                Blocks.dirt,
                                                false,
                                                null,
                                                0.45f) }))
                .variant(
                        variant(TerrainCategory.HILLS).terrain(new TerrainGenericHills())
                                .surfaces(
                                        new SurfaceBase[] { new SurfaceMountainSnow(
                                                Blocks.grass,
                                                Blocks.dirt,
                                                false,
                                                null,
                                                0.45f) }))
                .variant(
                        variant(TerrainCategory.MOUNTAIN).terrain(new TerrainGenericMountain())
                                .surfaces(
                                        new SurfaceBase[] { new SurfaceMountainSnow(
                                                Blocks.grass,
                                                Blocks.dirt,
                                                false,
                                                null,
                                                0.45f) })));
        registry.register(
                builder(BOPCBiomes.snowyConiferousForest).climate(Climate.SNOW).river(registry.river(Climate.SNOW))
                        .variant(
                                variant(TerrainCategory.PLAIN)
                                        .terrain(new TerrainMountainRiver()).surfaces(
                                                new SurfaceBase[] { new SurfaceMountainSnow(
                                                        Blocks.grass,
                                                        Blocks.dirt,
                                                        false,
                                                        null,
                                                        0.45f,
                                                        1.5f,
                                                        50f,
                                                        60f,
                                                        0.4f,
                                                        100f,
                                                        50f,
                                                        1.5f) }))
                        .variant(
                                variant(TerrainCategory.HILLS)
                                        .terrain(new TerrainGenericHills()).surfaces(
                                                new SurfaceBase[] { new SurfaceMountainSnow(
                                                        Blocks.grass,
                                                        Blocks.dirt,
                                                        false,
                                                        null,
                                                        0.45f,
                                                        1.5f,
                                                        50f,
                                                        60f,
                                                        0.4f,
                                                        100f,
                                                        50f,
                                                        1.5f) }))
                        .variant(
                                variant(TerrainCategory.MOUNTAIN)
                                        .terrain(new TerrainGenericMountain()).surfaces(
                                                new SurfaceBase[] { new SurfaceMountainSnow(
                                                        Blocks.grass,
                                                        Blocks.dirt,
                                                        false,
                                                        null,
                                                        0.45f,
                                                        1.5f,
                                                        50f,
                                                        60f,
                                                        0.4f,
                                                        100f,
                                                        50f,
                                                        1.5f) })));

        // DEAD FOREST
        registry.register(
                builder(BOPCBiomes.deadForest).climate(Climate.COLD).river(registry.temperateRiver()).variant(
                        variant(TerrainCategory.PLAIN)
                                .terrain(new TerrainGrasslandHills(50f, 180f, 13f, 100f, 28f, 260f, 70f)).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.deadForest.topBlock,
                                                BOPCBiomes.deadForest.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) }))
                .variant(
                        variant(TerrainCategory.HILLS)
                                .terrain(new TerrainGenericHills()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.deadForest.topBlock,
                                                BOPCBiomes.deadForest.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) }))
                .variant(
                        variant(TerrainCategory.MOUNTAIN)
                                .terrain(new TerrainGenericMountain()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.deadForest.topBlock,
                                                BOPCBiomes.deadForest.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) })));

        // DEAD SWAMP
        registry.register(
                builder(BOPCBiomes.deadSwamp).climate(Climate.WET).river(registry.river(Climate.WET))
                        .subcategory(TerrainSubcategory.LITTORAL).variant(
                                variant(TerrainCategory.SWAMP).terrain(new TerrainMarsh()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.deadSwamp.topBlock,
                                                BOPCBiomes.deadSwamp.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) })));

        // DECIDUOUS FOREST
        registry.register(
                builder(BOPCBiomes.deciduousForest).climate(Climate.HOT).river(registry.river(Climate.HOT)).variant(
                        variant(TerrainCategory.PLAIN)
                                .terrain(new TerrainHighland(0f, 140f, 68f, 200f)).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.deciduousForest.topBlock,
                                                BOPCBiomes.deciduousForest.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) }))
                .variant(
                        variant(TerrainCategory.HILLS)
                                .terrain(new TerrainGenericHills()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.deciduousForest.topBlock,
                                                BOPCBiomes.deciduousForest.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) }))
                .variant(
                        variant(TerrainCategory.MOUNTAIN)
                                .terrain(new TerrainGenericMountain()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.deciduousForest.topBlock,
                                                BOPCBiomes.deciduousForest.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) })));

        // EUCALYPTUS FOREST
        registry.register(
                builder(BOPCBiomes.eucalyptusForest).climate(Climate.WET).river(registry.river(Climate.WET)).variant(
                        variant(TerrainCategory.PLAIN)
                                .terrain(new TerrainSwampMountain(135f, 300f)).surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.eucalyptusForest.topBlock,
                                                BOPCBiomes.eucalyptusForest.fillerBlock,
                                                false,
                                                null,
                                                0.95f) }))
                .variant(
                        variant(TerrainCategory.HILLS)
                                .terrain(new TerrainGenericHills()).surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.eucalyptusForest.topBlock,
                                                BOPCBiomes.eucalyptusForest.fillerBlock,
                                                false,
                                                null,
                                                0.95f) }))
                .variant(
                        variant(TerrainCategory.MOUNTAIN)
                                .terrain(new TerrainGenericMountain()).surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.eucalyptusForest.topBlock,
                                                BOPCBiomes.eucalyptusForest.fillerBlock,
                                                false,
                                                null,
                                                0.95f) })));

        // FEN
        registry.register(
                builder(BOPCBiomes.fen).climate(Climate.HOT).river(registry.river(Climate.HOT)).variant(
                        variant(TerrainCategory.PLAIN)
                                .terrain(new TerrainHighland(0f, 140f, 68f, 200f)).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.fen.topBlock,
                                                BOPCBiomes.fen.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) }))
                .variant(
                        variant(TerrainCategory.HILLS)
                                .terrain(new TerrainGenericHills()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.fen.topBlock,
                                                BOPCBiomes.fen.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) }))
                .variant(
                        variant(TerrainCategory.MOUNTAIN)
                                .terrain(new TerrainGenericMountain()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.fen.topBlock,
                                                BOPCBiomes.fen.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) })));

        // FLOWER FIELD
        registry.register(
                builder(BOPCBiomes.flowerField).climate(Climate.COLD).river(registry.temperateRiver())
                        .subcategory(TerrainSubcategory.SMALL).variant(
                                variant(TerrainCategory.PLAIN).terrain(new TerrainMarsh()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.flowerField.topBlock,
                                                BOPCBiomes.flowerField.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) })));

        // FROST FOREST
        registry.register(
                builder(BOPCBiomes.frostForest).climate(Climate.SNOW).river(registry.river(Climate.SNOW)).variant(
                        variant(TerrainCategory.PLAIN)
                                .terrain(new TerrainHighland(0f, 140f, 68f, 200f)).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.frostForest.topBlock,
                                                BOPCBiomes.frostForest.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) }))
                .variant(
                        variant(TerrainCategory.HILLS)
                                .terrain(new TerrainGenericHills()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.frostForest.topBlock,
                                                BOPCBiomes.frostForest.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) }))
                .variant(
                        variant(TerrainCategory.MOUNTAIN)
                                .terrain(new TerrainGenericMountain()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.frostForest.topBlock,
                                                BOPCBiomes.frostForest.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) })));

        // FUNGI FOREST
        registry.register(
                builder(BOPCBiomes.fungiForest).climate(Climate.WET).river(registry.river(Climate.WET)).variant(
                        variant(TerrainCategory.PLAIN)
                                .terrain(new TerrainSwampMountain(135f, 300f)).surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.fungiForest.topBlock,
                                                BOPCBiomes.fungiForest.fillerBlock,
                                                false,
                                                null,
                                                0.95f) }))
                .variant(
                        variant(TerrainCategory.HILLS)
                                .terrain(new TerrainGenericHills()).surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.fungiForest.topBlock,
                                                BOPCBiomes.fungiForest.fillerBlock,
                                                false,
                                                null,
                                                0.95f) }))
                .variant(
                        variant(TerrainCategory.MOUNTAIN)
                                .terrain(new TerrainGenericMountain()).surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.fungiForest.topBlock,
                                                BOPCBiomes.fungiForest.fillerBlock,
                                                false,
                                                null,
                                                0.95f) })));

        // GARDEN
        registry.register(
                builder(BOPCBiomes.garden).climate(Climate.COLD).river(registry.temperateRiver()).variant(
                        variant(TerrainCategory.SMALL_ISLAND).terrain(new TerrainMountainSpikes()).surfaces(
                                new SurfaceBase[] { new SurfaceMountainSnow(
                                        BOPCBiomes.garden.topBlock,
                                        BOPCBiomes.garden.fillerBlock,
                                        true,
                                        Blocks.sand,
                                        0.45f,
                                        1.5f,
                                        60f,
                                        65f,
                                        0.4f,
                                        130f,
                                        50f,
                                        1.5f) })));

        // GROVE
        registry.register(
                builder(BOPCBiomes.grove).climate(Climate.COLD).river(registry.temperateRiver()).variant(
                        variant(TerrainCategory.PLAIN)
                                .terrain(new TerrainHighland(0f, 140f, 68f, 200f, .3f)).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.grove.topBlock,
                                                BOPCBiomes.grove.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) }))
                .variant(
                        variant(TerrainCategory.HILLS)
                                .terrain(new TerrainGenericHills()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.grove.topBlock,
                                                BOPCBiomes.grove.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) }))
                .variant(
                        variant(TerrainCategory.MOUNTAIN)
                                .terrain(new TerrainGenericMountain()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.grove.topBlock,
                                                BOPCBiomes.grove.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) })));

        // HEATHLAND
        registry.register(
                builder(BOPCBiomes.heathland).climate(Climate.HOT).river(registry.oasisRiver())

                        .decorators(new BiomeDecorator[] { new RiverOasisDecorator() }).selectable(
                                true)
                        .variant(
                                variant(TerrainCategory.PLAIN)
                                        .terrain(
                                                new TerrainDuneValley(300f))
                                        .surfaces(
                                                new SurfaceBase[] { new SurfaceDuneValley(
                                                        BOPCBiomes.brushland.topBlock,
                                                        BOPCBiomes.brushland.fillerBlock,
                                                        300f,
                                                        false,
                                                        true,
                                                        registry.hotDesert()), new SurfaceRiverOasis(), }))
                        .variant(
                                variant(TerrainCategory.HILLS)
                                        .terrain(new TerrainGenericHills())
                                        .surfaces(
                                                new SurfaceBase[] { new SurfaceDuneValley(
                                                        BOPCBiomes.brushland.topBlock,
                                                        BOPCBiomes.brushland.fillerBlock,
                                                        300f,
                                                        false,
                                                        true,
                                                        registry.hotDesert()), new SurfaceRiverOasis(), }))
                        .variant(
                                variant(TerrainCategory.MOUNTAIN)
                                        .terrain(new TerrainGenericMountain())
                                        .surfaces(
                                                new SurfaceBase[] { new SurfaceDuneValley(
                                                        BOPCBiomes.brushland.topBlock,
                                                        BOPCBiomes.brushland.fillerBlock,
                                                        300f,
                                                        false,
                                                        true,
                                                        registry.hotDesert()), new SurfaceRiverOasis(), })));

        // HIGHLAND
        registry.register(
                builder(BOPCBiomes.highland).climate(Climate.COLD).river(registry.temperateRiver()).variant(
                        variant(TerrainCategory.PLAIN)
                                .terrain(new TerrainHighland(0f, 140f, 68f, 150f)).surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.highland.topBlock,
                                                BOPCBiomes.highland.fillerBlock,
                                                false,
                                                null,
                                                1f,
                                                1.5f,
                                                85f,
                                                20f,
                                                4f) }))
                .variant(
                        variant(TerrainCategory.HILLS)
                                .terrain(new TerrainGenericHills()).surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.highland.topBlock,
                                                BOPCBiomes.highland.fillerBlock,
                                                false,
                                                null,
                                                1f,
                                                1.5f,
                                                85f,
                                                20f,
                                                4f) }))
                .variant(
                        variant(TerrainCategory.MOUNTAIN)
                                .terrain(new TerrainGenericMountain()).surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.highland.topBlock,
                                                BOPCBiomes.highland.fillerBlock,
                                                false,
                                                null,
                                                1f,
                                                1.5f,
                                                85f,
                                                20f,
                                                4f) })));

        // JADE CLIFFS
        registry.register(
                builder(BOPCBiomes.jadeCliffs).climate(Climate.COLD).river(registry.river(Climate.HOT))
                        .subcategory(TerrainSubcategory.HOT_BORDER).variant(
                                variant(TerrainCategory.PLAIN).terrain(new TerrainHilly(230f, 120f, 0f)).surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.jadeCliffs.topBlock,
                                                BOPCBiomes.jadeCliffs.fillerBlock,
                                                false,
                                                null,
                                                0.95f) }))
                        .variant(
                                variant(TerrainCategory.HILLS).terrain(new TerrainGenericHills()).surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.jadeCliffs.topBlock,
                                                BOPCBiomes.jadeCliffs.fillerBlock,
                                                false,
                                                null,
                                                0.95f) }))
                        .variant(
                                variant(TerrainCategory.MOUNTAIN).terrain(new TerrainGenericMountain()).surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.jadeCliffs.topBlock,
                                                BOPCBiomes.jadeCliffs.fillerBlock,
                                                false,
                                                null,
                                                0.95f) })));
        registry.register(
                builder(BOPCBiomes.jadeCliffs).climate(Climate.COLD).river(registry.river(Climate.HOT)).variant(
                        variant(TerrainCategory.PLAIN)
                                .terrain(new TerrainHilly(230f, 120f, 0f)).surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.jadeCliffs.topBlock,
                                                BOPCBiomes.jadeCliffs.fillerBlock,
                                                false,
                                                null,
                                                0.95f) }))
                .variant(
                        variant(TerrainCategory.HILLS)
                                .terrain(new TerrainGenericHills()).surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.jadeCliffs.topBlock,
                                                BOPCBiomes.jadeCliffs.fillerBlock,
                                                false,
                                                null,
                                                0.95f) }))
                .variant(
                        variant(TerrainCategory.MOUNTAIN)
                                .terrain(new TerrainGenericMountain()).surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.jadeCliffs.topBlock,
                                                BOPCBiomes.jadeCliffs.fillerBlock,
                                                false,
                                                null,
                                                0.95f) })));
        registry.register(
                builder(BOPCBiomes.jadeCliffs).climate(Climate.COLD).river(registry.river(Climate.COLD)).variant(
                        variant(TerrainCategory.PLATEAU, TerrainCategory.CANYON).terrain(new TerrainMesa()).surfaces(
                                new SurfaceBase[] { new SurfaceMountainStone(
                                        BOPCBiomes.jadeCliffs.topBlock,
                                        BOPCBiomes.jadeCliffs.fillerBlock,
                                        false,
                                        null,
                                        0.95f) })));

        // LAND OF LAKES MARSH
        registry.register(
                builder(BOPCBiomes.landOfLakesMarsh).climate(Climate.HOT).river(registry.river(Climate.WET))
                        .subcategory(TerrainSubcategory.HOT_BORDER).variant(
                                variant(TerrainCategory.PLAIN)
                                        .terrain(new TerrainGrasslandHills(90f, 180f, 13f, 100f, 38f, 260f, 71f))
                                        .surfaces(
                                                new SurfaceBase[] { new SurfaceGrassland(
                                                        BOPCBiomes.landOfLakesMarsh.topBlock,
                                                        BOPCBiomes.landOfLakesMarsh.fillerBlock,
                                                        Blocks.stone,
                                                        Blocks.cobblestone) }))
                        .variant(
                                variant(TerrainCategory.HILLS)
                                        .terrain(new TerrainGenericHills())
                                        .surfaces(
                                                new SurfaceBase[] { new SurfaceGrassland(
                                                        BOPCBiomes.landOfLakesMarsh.topBlock,
                                                        BOPCBiomes.landOfLakesMarsh.fillerBlock,
                                                        Blocks.stone,
                                                        Blocks.cobblestone) }))
                        .variant(
                                variant(TerrainCategory.MOUNTAIN)
                                        .terrain(new TerrainGenericMountain())
                                        .surfaces(
                                                new SurfaceBase[] { new SurfaceGrassland(
                                                        BOPCBiomes.landOfLakesMarsh.topBlock,
                                                        BOPCBiomes.landOfLakesMarsh.fillerBlock,
                                                        Blocks.stone,
                                                        Blocks.cobblestone) })));

        // LAVENDER FIELDS
        registry.register(
                builder(BOPCBiomes.lavenderFields).climate(Climate.COLD).river(registry.temperateRiver()).variant(
                        variant(TerrainCategory.PLAIN).terrain(new TerrainMountainSpikes())
                                .surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.lavenderFields.topBlock,
                                                BOPCBiomes.lavenderFields.fillerBlock,
                                                false,
                                                null,
                                                1.2f) }))
                .variant(
                        variant(TerrainCategory.HILLS).terrain(new TerrainGenericHills())
                                .surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.lavenderFields.topBlock,
                                                BOPCBiomes.lavenderFields.fillerBlock,
                                                false,
                                                null,
                                                1.2f) }))
                .variant(
                        variant(TerrainCategory.MOUNTAIN).terrain(new TerrainGenericMountain())
                                .surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.lavenderFields.topBlock,
                                                BOPCBiomes.lavenderFields.fillerBlock,
                                                false,
                                                null,
                                                1.2f) })));
        // LUSH DESERT
        registry.register(
                builder(BOPCBiomes.lushDesert).climate(Climate.HOT).river(registry.river(Climate.HOT)).variant(
                        variant(TerrainCategory.PLAIN)
                                .terrain(new TerrainGrasslandHills(90f, 180f, 13f, 100f, 38f, 260f, 71f)).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.lushDesert.topBlock,
                                                BOPCBiomes.lushDesert.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) }))
                .variant(
                        variant(TerrainCategory.HILLS)
                                .terrain(new TerrainGenericHills()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.lushDesert.topBlock,
                                                BOPCBiomes.lushDesert.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) }))
                .variant(
                        variant(TerrainCategory.MOUNTAIN)
                                .terrain(new TerrainGenericMountain()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.lushDesert.topBlock,
                                                BOPCBiomes.lushDesert.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) })));

        // LUSH SWAMP
        registry.register(
                builder(BOPCBiomes.lushSwamp).climate(Climate.WET).river(registry.river(Climate.WET))
                        .subcategory(TerrainSubcategory.LITTORAL).variant(
                                variant(TerrainCategory.SWAMP).terrain(new TerrainSwampRiver()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.lushSwamp.topBlock,
                                                BOPCBiomes.lushSwamp.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) })));

        // MAPLE WOODS
        registry.register(
                builder(BOPCBiomes.mapleWoods).climate(Climate.COLD).river(registry.temperateRiver()).variant(
                        variant(TerrainCategory.PLAIN)
                                .terrain(new TerrainHighland(0f, 140f, 68f, 200f)).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.mapleWoods.topBlock,
                                                BOPCBiomes.mapleWoods.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) }))
                .variant(
                        variant(TerrainCategory.HILLS)
                                .terrain(new TerrainGenericHills()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.mapleWoods.topBlock,
                                                BOPCBiomes.mapleWoods.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) }))
                .variant(
                        variant(TerrainCategory.MOUNTAIN)
                                .terrain(new TerrainGenericMountain()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.mapleWoods.topBlock,
                                                BOPCBiomes.mapleWoods.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) })));

        // MANGROVE
        registry.register(
                builder(BOPCBiomes.mangrove).climate(Climate.WET).river(registry.river(Climate.WET))
                        .subcategory(TerrainSubcategory.LITTORAL).variant(
                                variant(TerrainCategory.SWAMP).terrain(new TerrainSwampRiver()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.mangrove.topBlock,
                                                BOPCBiomes.mangrove.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) })));

        // MEADOW
        registry.register(
                builder(BOPCBiomes.meadow).climate(Climate.COLD).river(registry.temperateRiver())
                        .subcategory(TerrainSubcategory.COLD_BORDER).variant(
                                variant(TerrainCategory.PLAIN).terrain(new TerrainMountainSpikes()).surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.meadow.topBlock,
                                                BOPCBiomes.meadow.fillerBlock,
                                                false,
                                                null,
                                                1.2f) }))
                        .variant(
                                variant(TerrainCategory.HILLS).terrain(new TerrainGenericHills()).surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.meadow.topBlock,
                                                BOPCBiomes.meadow.fillerBlock,
                                                false,
                                                null,
                                                1.2f) }))
                        .variant(
                                variant(TerrainCategory.MOUNTAIN).terrain(new TerrainGenericMountain()).surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.meadow.topBlock,
                                                BOPCBiomes.meadow.fillerBlock,
                                                false,
                                                null,
                                                1.2f) })));

        // GARDEN (MOOR TERRAIN)
        registry.register(
                builder(BOPCBiomes.garden).climate(Climate.WET).river(registry.river(Climate.WET)).variant(
                        variant(TerrainCategory.PLAIN).terrain(new TerrainMountainRiver())
                                .surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.garden.topBlock,
                                                BOPCBiomes.garden.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) }))
                .variant(
                        variant(TerrainCategory.HILLS).terrain(new TerrainGenericHills())
                                .surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.garden.topBlock,
                                                BOPCBiomes.garden.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) }))
                .variant(
                        variant(TerrainCategory.MOUNTAIN).terrain(new TerrainGenericMountain())
                                .surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.garden.topBlock,
                                                BOPCBiomes.garden.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) })));

        // MOOR - NOT USEFUL: excluded because its colors and content are a poor fit for the wet core pool.

        // MOUNTAIN
        registry.register(
                builder(BOPCBiomes.mountain).climate(Climate.HOT).river(registry.river(Climate.HOT)).variant(
                        variant(TerrainCategory.PLAIN).terrain(new TerrainMountainRiver())
                                .surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.mountain.topBlock,
                                                BOPCBiomes.mountain.fillerBlock,
                                                true,
                                                Blocks.sand,
                                                0.75f) }))
                .variant(
                        variant(TerrainCategory.HILLS).terrain(new TerrainGenericHills())
                                .surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.mountain.topBlock,
                                                BOPCBiomes.mountain.fillerBlock,
                                                true,
                                                Blocks.sand,
                                                0.75f) }))
                .variant(
                        variant(TerrainCategory.MOUNTAIN).terrain(new TerrainGenericMountain())
                                .surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.mountain.topBlock,
                                                BOPCBiomes.mountain.fillerBlock,
                                                true,
                                                Blocks.sand,
                                                0.75f) })));

        // OMINOUS WOODS
        registry.register(
                builder(BOPCBiomes.ominousWoods).climate(Climate.SNOW).river(registry.river(Climate.COLD)).variant(
                        variant(TerrainCategory.SMALL_ISLAND).terrain(new TerrainHilly(230f, 120f, 0f)).surfaces(
                                new SurfaceBase[] { new SurfaceGrassland(
                                        BOPCBiomes.ominousWoods.topBlock,
                                        BOPCBiomes.ominousWoods.fillerBlock,
                                        Blocks.stone,
                                        Blocks.cobblestone) })));

        // ORCHARD

        // ORIGIN VALLEY

        // OUTBACK
        registry.register(
                builder(BOPCBiomes.outback).climate(Climate.HOT).river(registry.oasisRiver())

                        .decorators(new BiomeDecorator[] { new RiverOasisDecorator() }).selectable(
                                true)
                        .variant(
                                variant(TerrainCategory.PLAIN)
                                        .terrain(
                                                new TerrainDuneValley(300f))
                                        .surfaces(
                                                new SurfaceBase[] { new SurfaceDuneValley(
                                                        BOPCBiomes.outback.topBlock,
                                                        BOPCBiomes.outback.fillerBlock,
                                                        300f,
                                                        false,
                                                        false,
                                                        registry.hotDesert()), new SurfaceRiverOasis(), }))
                        .variant(
                                variant(TerrainCategory.HILLS)
                                        .terrain(new TerrainGenericHills())
                                        .surfaces(
                                                new SurfaceBase[] { new SurfaceDuneValley(
                                                        BOPCBiomes.outback.topBlock,
                                                        BOPCBiomes.outback.fillerBlock,
                                                        300f,
                                                        false,
                                                        false,
                                                        registry.hotDesert()), new SurfaceRiverOasis(), }))
                        .variant(
                                variant(TerrainCategory.MOUNTAIN)
                                        .terrain(new TerrainGenericMountain())
                                        .surfaces(
                                                new SurfaceBase[] { new SurfaceDuneValley(
                                                        BOPCBiomes.outback.topBlock,
                                                        BOPCBiomes.outback.fillerBlock,
                                                        300f,
                                                        false,
                                                        false,
                                                        registry.hotDesert()), new SurfaceRiverOasis(), })));

        // PRAIRIE
        registry.register(
                builder(BOPCBiomes.prairie).climate(Climate.HOT).river(registry.river(Climate.HOT)).variant(
                        variant(TerrainCategory.PLAIN)
                                .terrain(new TerrainGrasslandHills(90f, 180f, 13f, 100f, 38f, 260f, 71f)).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.prairie.topBlock,
                                                BOPCBiomes.prairie.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) }))
                .variant(
                        variant(TerrainCategory.HILLS)
                                .terrain(new TerrainGenericHills()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.prairie.topBlock,
                                                BOPCBiomes.prairie.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) }))
                .variant(
                        variant(TerrainCategory.MOUNTAIN)
                                .terrain(new TerrainGenericMountain()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.prairie.topBlock,
                                                BOPCBiomes.prairie.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) })));

        // RAINFOREST
        registry.register(
                builder(BOPCBiomes.rainforest).climate(Climate.WET).river(registry.river(Climate.WET))
                        .subcategory(TerrainSubcategory.COLD_BORDER).variant(
                                variant(TerrainCategory.PLAIN).terrain(new TerrainSwampMountain(120f, 300f)).surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.rainforest.topBlock,
                                                BOPCBiomes.rainforest.fillerBlock,
                                                false,
                                                null,
                                                1.3f) })));

        // QUAGMIRE
        registry.register(
                builder(BOPCBiomes.quagmire).climate(Climate.WET).river(registry.river(Climate.WET))
                        .subcategory(TerrainSubcategory.SMALL).variant(
                                variant(TerrainCategory.PLAIN).terrain(new TerrainMarsh()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.quagmire.topBlock,
                                                BOPCBiomes.quagmire.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) })));

        // REDWOOD FOREST
        registry.register(
                builder(BOPCBiomes.redwoodForest).climate(Climate.COLD).river(registry.temperateRiver()).variant(
                        variant(TerrainCategory.PLAIN)
                                .terrain(new TerrainGrasslandHills(80f, 180f, 13f, 100f, 38f, 260f, 71f)).surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.redwoodForest.topBlock,
                                                BOPCBiomes.redwoodForest.fillerBlock,
                                                false,
                                                null,
                                                0.4f) }))
                .variant(
                        variant(TerrainCategory.HILLS)
                                .terrain(new TerrainGenericHills()).surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.redwoodForest.topBlock,
                                                BOPCBiomes.redwoodForest.fillerBlock,
                                                false,
                                                null,
                                                0.4f) }))
                .variant(
                        variant(TerrainCategory.MOUNTAIN)
                                .terrain(new TerrainGenericMountain()).surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.redwoodForest.topBlock,
                                                BOPCBiomes.redwoodForest.fillerBlock,
                                                false,
                                                null,
                                                0.4f) })));

        // SACRED SPRINGS
        registry.register(
                builder(BOPCBiomes.sacredSprings).climate(Climate.WET).river(registry.river(Climate.WET)).variant(
                        variant(TerrainCategory.PLAIN)
                                .terrain(new TerrainHighland(0f, 120f, 68f, 200f)).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.sacredSprings.topBlock,
                                                BOPCBiomes.sacredSprings.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) }))
                .variant(
                        variant(TerrainCategory.HILLS)
                                .terrain(new TerrainGenericHills()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.sacredSprings.topBlock,
                                                BOPCBiomes.sacredSprings.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) }))
                .variant(
                        variant(TerrainCategory.MOUNTAIN)
                                .terrain(new TerrainGenericMountain()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.sacredSprings.topBlock,
                                                BOPCBiomes.sacredSprings.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) })));

        // SEASONAL FOREST
        registry.register(
                builder(BOPCBiomes.seasonalForest).climate(Climate.COLD).river(registry.temperateRiver()).variant(
                        variant(TerrainCategory.PLAIN)
                                .terrain(new TerrainHighland(0f, 140f, 68f, 200f)).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.seasonalForest.topBlock,
                                                BOPCBiomes.seasonalForest.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) }))
                .variant(
                        variant(TerrainCategory.HILLS)
                                .terrain(new TerrainGenericHills()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.seasonalForest.topBlock,
                                                BOPCBiomes.seasonalForest.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) }))
                .variant(
                        variant(TerrainCategory.MOUNTAIN)
                                .terrain(new TerrainGenericMountain()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.seasonalForest.topBlock,
                                                BOPCBiomes.seasonalForest.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) })));

        // SHIELD
        registry.register(
                builder(BOPCBiomes.shield).climate(Climate.COLD).river(registry.river(Climate.COLD)).variant(
                        variant(TerrainCategory.PLAIN)
                                .terrain(new TerrainGrasslandHills(90f, 180f, 13f, 100f, 38f, 260f, 71f)).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.shield.topBlock,
                                                BOPCBiomes.shield.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) }))
                .variant(
                        variant(TerrainCategory.HILLS)
                                .terrain(new TerrainGenericHills()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.shield.topBlock,
                                                BOPCBiomes.shield.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) }))
                .variant(
                        variant(TerrainCategory.MOUNTAIN)
                                .terrain(new TerrainGenericMountain()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.shield.topBlock,
                                                BOPCBiomes.shield.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) })));

        // SHRUBLAND
        registry.register(
                builder(BOPCBiomes.shrubland).climate(Climate.HOT).river(registry.river(Climate.HOT)).variant(
                        variant(TerrainCategory.PLAIN)
                                .terrain(new TerrainGrasslandHills(90f, 180f, 13f, 100f, 38f, 260f, 71f)).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.shrubland.topBlock,
                                                BOPCBiomes.shrubland.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) }))
                .variant(
                        variant(TerrainCategory.HILLS)
                                .terrain(new TerrainGenericHills()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.shrubland.topBlock,
                                                BOPCBiomes.shrubland.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) }))
                .variant(
                        variant(TerrainCategory.MOUNTAIN)
                                .terrain(new TerrainGenericMountain()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.shrubland.topBlock,
                                                BOPCBiomes.shrubland.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) })));

        // SLUDGEPIT
        registry.register(
                builder(BOPCBiomes.sludgepit).climate(Climate.WET).river(registry.river(Climate.WET))
                        .subcategory(TerrainSubcategory.LITTORAL).variant(
                                variant(TerrainCategory.SWAMP).terrain(new TerrainMarsh()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.sludgepit.topBlock,
                                                BOPCBiomes.sludgepit.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) })));

        // TEMPERATE RAINFOREST
        registry.register(
                builder(BOPCBiomes.temperateRainforest).climate(Climate.WET).river(registry.river(Climate.WET)).variant(
                        variant(TerrainCategory.PLAIN).terrain(new TerrainMountainRiver())
                                .surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.temperateRainforest.topBlock,
                                                BOPCBiomes.temperateRainforest.fillerBlock,
                                                false,
                                                null,
                                                0.45f) }))
                .variant(
                        variant(TerrainCategory.HILLS).terrain(new TerrainGenericHills())
                                .surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.temperateRainforest.topBlock,
                                                BOPCBiomes.temperateRainforest.fillerBlock,
                                                false,
                                                null,
                                                0.45f) }))
                .variant(
                        variant(TerrainCategory.MOUNTAIN).terrain(new TerrainGenericMountain())
                                .surfaces(
                                        new SurfaceBase[] { new SurfaceMountainStone(
                                                BOPCBiomes.temperateRainforest.topBlock,
                                                BOPCBiomes.temperateRainforest.fillerBlock,
                                                false,
                                                null,
                                                0.45f) })));

        // TROPICAL RAINFOREST
        registry.register(
                builder(BOPCBiomes.tropicalRainforest).climate(Climate.WET).river(registry.river(Climate.WET))
                        .subcategory(TerrainSubcategory.COLD_BORDER).variant(
                                variant(TerrainCategory.PLAIN).terrain(new TerrainHighland(0f, 140f, 68f, 200f))
                                        .surfaces(
                                                new SurfaceBase[] { new SurfaceGrassland(
                                                        BOPCBiomes.tropicalRainforest.topBlock,
                                                        BOPCBiomes.tropicalRainforest.fillerBlock,
                                                        Blocks.stone,
                                                        Blocks.cobblestone) }))
                        .variant(
                                variant(TerrainCategory.HILLS).terrain(new TerrainGenericHills())
                                        .surfaces(
                                                new SurfaceBase[] { new SurfaceGrassland(
                                                        BOPCBiomes.tropicalRainforest.topBlock,
                                                        BOPCBiomes.tropicalRainforest.fillerBlock,
                                                        Blocks.stone,
                                                        Blocks.cobblestone) }))
                        .variant(
                                variant(TerrainCategory.MOUNTAIN).terrain(new TerrainGenericMountain())
                                        .surfaces(
                                                new SurfaceBase[] { new SurfaceGrassland(
                                                        BOPCBiomes.tropicalRainforest.topBlock,
                                                        BOPCBiomes.tropicalRainforest.fillerBlock,
                                                        Blocks.stone,
                                                        Blocks.cobblestone) })));

        // TROPICS
        registry.register(
                builder(BOPCBiomes.tropics).climate(Climate.WET).river(registry.river(Climate.WET))
                        .subcategory(TerrainSubcategory.LITTORAL).variant(
                                variant(TerrainCategory.WETLANDS).terrain(new TerrainMarsh()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.tropics.topBlock,
                                                BOPCBiomes.tropics.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) })));
        registry.register(
                builder(BOPCBiomes.tropics).climate(Climate.WET).river(registry.river(Climate.WET)).variant(
                        variant(TerrainCategory.SMALL_ISLAND).terrain(new TerrainMarsh()).surfaces(
                                new SurfaceBase[] { new SurfaceGrassland(
                                        BOPCBiomes.tropics.topBlock,
                                        BOPCBiomes.tropics.fillerBlock,
                                        Blocks.stone,
                                        Blocks.cobblestone) })));
        registry.register(
                builder(BOPCBiomes.tropics).climate(Climate.HOT).river(registry.river(Climate.HOT)).variant(
                        variant(TerrainCategory.SMALL_ISLAND).terrain(new TerrainMarsh()).surfaces(
                                new SurfaceBase[] { new SurfaceGrassland(
                                        BOPCBiomes.tropics.topBlock,
                                        BOPCBiomes.tropics.fillerBlock,
                                        Blocks.stone,
                                        Blocks.cobblestone) })));
        registry.register(
                builder(BOPCBiomes.tropics).climate(Climate.HOT).river(registry.river(Climate.HOT))
                        .subcategory(TerrainSubcategory.SMALL).variant(
                                variant(TerrainCategory.PLAIN).terrain(new TerrainMarsh()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.tropics.topBlock,
                                                BOPCBiomes.tropics.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) })));

        // OASIS
        registry.register(
                builder(BOPCBiomes.oasis).climate(Climate.HOT).river(registry.river(Climate.HOT))
                        .subcategory(TerrainSubcategory.SMALL).variant(
                                variant(TerrainCategory.PLAIN).terrain(new TerrainMarsh()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.oasis.topBlock,
                                                BOPCBiomes.oasis.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) })));

        // TUNDRA
        registry.register(
                builder(BOPCBiomes.tundra).climate(Climate.SNOW).river(registry.river(Climate.SNOW)).variant(
                        variant(TerrainCategory.PLAIN)
                                .terrain(new TerrainGrasslandHills(90f, 180f, 13f, 100f, 38f, 260f, 71f)).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.tundra.topBlock,
                                                BOPCBiomes.tundra.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) }))
                .variant(
                        variant(TerrainCategory.HILLS)
                                .terrain(new TerrainGenericHills()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.tundra.topBlock,
                                                BOPCBiomes.tundra.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) }))
                .variant(
                        variant(TerrainCategory.MOUNTAIN)
                                .terrain(new TerrainGenericMountain()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.tundra.topBlock,
                                                BOPCBiomes.tundra.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) })));

        // WETLAND
        registry.register(
                builder(BOPCBiomes.wetland).climate(Climate.COLD).river(registry.river(Climate.COLD)).variant(
                        variant(TerrainCategory.PLAIN).terrain(new TerrainMarsh()).surfaces(
                                new SurfaceBase[] { new SurfaceGrassland(
                                        BOPCBiomes.wetland.topBlock,
                                        BOPCBiomes.wetland.fillerBlock,
                                        Blocks.stone,
                                        Blocks.cobblestone) })));

        // WOODLAND
        registry.register(
                builder(BOPCBiomes.woodland).climate(Climate.COLD).river(registry.temperateRiver()).variant(
                        variant(TerrainCategory.PLAIN)
                                .terrain(new TerrainHighland(0f, 140f, 68f, 200f)).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.woodland.topBlock,
                                                BOPCBiomes.woodland.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) }))
                .variant(
                        variant(TerrainCategory.HILLS)
                                .terrain(new TerrainGenericHills()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.woodland.topBlock,
                                                BOPCBiomes.woodland.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) }))
                .variant(
                        variant(TerrainCategory.MOUNTAIN)
                                .terrain(new TerrainGenericMountain()).surfaces(
                                        new SurfaceBase[] { new SurfaceGrassland(
                                                BOPCBiomes.woodland.topBlock,
                                                BOPCBiomes.woodland.fillerBlock,
                                                Blocks.stone,
                                                Blocks.cobblestone) })));
    }

}
