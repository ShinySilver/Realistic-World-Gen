package rwg.support;

import static rwg.registry.BiomeRegistration.builder;
import static rwg.registry.BiomeRegistration.variant;

import net.minecraft.init.Blocks;

import extrabiomes.api.BiomeManager;
import rwg.biomes.decorators.BiomeDecorator;
import rwg.biomes.decorators.RiverOasisDecorator;
import rwg.biomes.surface.SurfaceBase;
import rwg.biomes.surface.SurfaceDesertMountain;
import rwg.biomes.surface.SurfaceGrassland;
import rwg.biomes.surface.SurfaceGrasslandMix1;
import rwg.biomes.surface.SurfaceMarshFix;
import rwg.biomes.surface.SurfaceMountainSnow;
import rwg.biomes.surface.SurfaceMountainStone;
import rwg.biomes.surface.SurfacePolar;
import rwg.biomes.surface.SurfaceRiverOasis;
import rwg.biomes.terrain.TerrainGrasslandFlats;
import rwg.biomes.terrain.TerrainGrasslandHills;
import rwg.biomes.terrain.TerrainHighland;
import rwg.biomes.terrain.TerrainHilly;
import rwg.biomes.terrain.TerrainMarsh;
import rwg.biomes.terrain.TerrainMountainRiver;
import rwg.biomes.terrain.TerrainMountainSpikes;
import rwg.biomes.terrain.TerrainPolar;
import rwg.biomes.terrain.TerrainSwampMountain;
import rwg.biomes.terrain.TerrainSwampRiver;
import rwg.biomes.terrain.TerrainGenericHills;
import rwg.biomes.terrain.TerrainGenericMountain;
import rwg.registry.BiomeRegistry;
import rwg.registry.Climate;
import rwg.registry.TerrainCategory;
import rwg.registry.TerrainSubcategory;

public final class ExtrabiomesBiomeRegistrations {

    private ExtrabiomesBiomeRegistrations() {}

    public static void register(BiomeRegistry registry) {
        // ALPINE
        if (BiomeManager.alpine.isPresent()) {
            registry.register(
                    builder(BiomeManager.alpine.get()).climate(Climate.SNOW).river(registry.river(Climate.SNOW))
                            .variant(
                                    variant(TerrainCategory.PLAIN)
                                            .terrain(new TerrainMountainRiver()).surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainSnow(
                                                            Blocks.grass,
                                                            Blocks.dirt,
                                                            false,
                                                            null,
                                                            0.45f) }))
                            .variant(
                                    variant(TerrainCategory.HILLS)
                                            .terrain(new TerrainGenericHills()).surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainSnow(
                                                            Blocks.grass,
                                                            Blocks.dirt,
                                                            false,
                                                            null,
                                                            0.45f) }))
                            .variant(
                                    variant(TerrainCategory.MOUNTAIN)
                                            .terrain(new TerrainGenericMountain()).surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainSnow(
                                                            Blocks.grass,
                                                            Blocks.dirt,
                                                            false,
                                                            null,
                                                            0.45f) })));
        }

        // AUTUMNWOODS
        if (BiomeManager.autumnwoods.isPresent()) {
            registry.register(
                    builder(BiomeManager.autumnwoods.get()).climate(Climate.COLD).river(registry.temperateRiver())
                            .variant(
                                    variant(TerrainCategory.PLAIN)
                                            .terrain(new TerrainHighland(0f, 140f, 68f, 200f)).surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            BiomeManager.autumnwoods.get().topBlock,
                                                            BiomeManager.autumnwoods.get().fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone) }))
                            .variant(
                                    variant(TerrainCategory.HILLS)
                                            .terrain(new TerrainGenericHills()).surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            BiomeManager.autumnwoods.get().topBlock,
                                                            BiomeManager.autumnwoods.get().fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone) }))
                            .variant(
                                    variant(TerrainCategory.MOUNTAIN)
                                            .terrain(new TerrainGenericMountain()).surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            BiomeManager.autumnwoods.get().topBlock,
                                                            BiomeManager.autumnwoods.get().fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone) })));
        }

        // BIRCHFOREST
        if (BiomeManager.birchforest.isPresent()) {
            registry.register(
                    builder(BiomeManager.birchforest.get()).climate(Climate.COLD).river(registry.temperateRiver())
                            .variant(
                                    variant(TerrainCategory.PLAIN)
                                            .terrain(new TerrainHilly(230f, 120f, 0f)).surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainStone(
                                                            BiomeManager.birchforest.get().topBlock,
                                                            BiomeManager.birchforest.get().fillerBlock,
                                                            false,
                                                            null,
                                                            0.95f) }))
                            .variant(
                                    variant(TerrainCategory.HILLS)
                                            .terrain(new TerrainGenericHills()).surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainStone(
                                                            BiomeManager.birchforest.get().topBlock,
                                                            BiomeManager.birchforest.get().fillerBlock,
                                                            false,
                                                            null,
                                                            0.95f) }))
                            .variant(
                                    variant(TerrainCategory.MOUNTAIN)
                                            .terrain(new TerrainGenericMountain()).surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainStone(
                                                            BiomeManager.birchforest.get().topBlock,
                                                            BiomeManager.birchforest.get().fillerBlock,
                                                            false,
                                                            null,
                                                            0.95f) })));
        }

        // EXTREME JUNGLE
        if (BiomeManager.extremejungle.isPresent()) {
            registry.register(
                    builder(BiomeManager.extremejungle.get()).climate(Climate.WET).river(registry.river(Climate.WET))
                            .variant(
                                    variant(TerrainCategory.PLAIN)
                                            .terrain(new TerrainSwampMountain(135f, 300f)).surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainStone(
                                                            BiomeManager.extremejungle.get().topBlock,
                                                            BiomeManager.extremejungle.get().fillerBlock,
                                                            false,
                                                            null,
                                                            0.95f) }))
                            .variant(
                                    variant(TerrainCategory.HILLS)
                                            .terrain(new TerrainGenericHills()).surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainStone(
                                                            BiomeManager.extremejungle.get().topBlock,
                                                            BiomeManager.extremejungle.get().fillerBlock,
                                                            false,
                                                            null,
                                                            0.95f) }))
                            .variant(
                                    variant(TerrainCategory.MOUNTAIN)
                                            .terrain(new TerrainGenericMountain()).surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainStone(
                                                            BiomeManager.extremejungle.get().topBlock,
                                                            BiomeManager.extremejungle.get().fillerBlock,
                                                            false,
                                                            null,
                                                            0.95f) })));
        }

        // FORESTED ISLAND
        if (BiomeManager.forestedisland.isPresent()) {
            registry.register(
                    builder(BiomeManager.forestedisland.get()).climate(Climate.COLD).river(registry.temperateRiver())
                            .variant(
                                    variant(TerrainCategory.PLAIN)
                                            .terrain(new TerrainGrasslandHills(90f, 180f, 13f, 100f, 1f, 260f, 59f))
                                            .surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            BiomeManager.forestedisland.get().topBlock,
                                                            BiomeManager.forestedisland.get().fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone) }))
                            .variant(
                                    variant(TerrainCategory.HILLS)
                                            .terrain(new TerrainGenericHills())
                                            .surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            BiomeManager.forestedisland.get().topBlock,
                                                            BiomeManager.forestedisland.get().fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone) }))
                            .variant(
                                    variant(TerrainCategory.MOUNTAIN)
                                            .terrain(new TerrainGenericMountain())
                                            .surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            BiomeManager.forestedisland.get().topBlock,
                                                            BiomeManager.forestedisland.get().fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone) })));
        }

        // FORESTED HILLDS
        if (BiomeManager.forestedhills.isPresent()) {
            registry.register(
                    builder(BiomeManager.forestedhills.get()).climate(Climate.COLD).river(registry.temperateRiver())
                            .variant(
                                    variant(TerrainCategory.PLAIN)
                                            .terrain(new TerrainHilly(230f, 120f, 0f)).surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainStone(
                                                            BiomeManager.forestedhills.get().topBlock,
                                                            BiomeManager.forestedhills.get().fillerBlock,
                                                            false,
                                                            null,
                                                            0.95f) }))
                            .variant(
                                    variant(TerrainCategory.HILLS)
                                            .terrain(new TerrainGenericHills()).surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainStone(
                                                            BiomeManager.forestedhills.get().topBlock,
                                                            BiomeManager.forestedhills.get().fillerBlock,
                                                            false,
                                                            null,
                                                            0.95f) }))
                            .variant(
                                    variant(TerrainCategory.MOUNTAIN)
                                            .terrain(new TerrainGenericMountain()).surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainStone(
                                                            BiomeManager.forestedhills.get().topBlock,
                                                            BiomeManager.forestedhills.get().fillerBlock,
                                                            false,
                                                            null,
                                                            0.95f) })));
        }

        // GLACIER
        if (BiomeManager.glacier.isPresent()) {
            registry.register(
                    builder(BiomeManager.glacier.get()).climate(Climate.SNOW).river(registry.river(Climate.SNOW))
                            .variant(
                                    variant(TerrainCategory.PLAIN)
                                            .terrain(new TerrainGrasslandHills(90f, 180f, 13f, 100f, 38f, 260f, 71f))
                                            .surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainStone(
                                                            BiomeManager.glacier.get().topBlock,
                                                            BiomeManager.glacier.get().fillerBlock,
                                                            false,
                                                            null,
                                                            0.95f) }))
                            .variant(
                                    variant(TerrainCategory.HILLS)
                                            .terrain(new TerrainGenericHills())
                                            .surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainStone(
                                                            BiomeManager.glacier.get().topBlock,
                                                            BiomeManager.glacier.get().fillerBlock,
                                                            false,
                                                            null,
                                                            0.95f) }))
                            .variant(
                                    variant(TerrainCategory.MOUNTAIN)
                                            .terrain(new TerrainGenericMountain())
                                            .surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainStone(
                                                            BiomeManager.glacier.get().topBlock,
                                                            BiomeManager.glacier.get().fillerBlock,
                                                            false,
                                                            null,
                                                            0.95f) })));
        }

        // GREENHILLS
        if (BiomeManager.greenhills.isPresent()) {
            registry.register(
                    builder(BiomeManager.greenhills.get()).climate(Climate.COLD).river(registry.temperateRiver())
                            .variant(
                                    variant(TerrainCategory.PLAIN)
                                            .terrain(new TerrainHilly(230f, 120f, 0f)).surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainStone(
                                                            BiomeManager.greenhills.get().topBlock,
                                                            BiomeManager.greenhills.get().fillerBlock,
                                                            false,
                                                            null,
                                                            0.95f) }))
                            .variant(
                                    variant(TerrainCategory.HILLS)
                                            .terrain(new TerrainGenericHills()).surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainStone(
                                                            BiomeManager.greenhills.get().topBlock,
                                                            BiomeManager.greenhills.get().fillerBlock,
                                                            false,
                                                            null,
                                                            0.95f) }))
                            .variant(
                                    variant(TerrainCategory.MOUNTAIN)
                                            .terrain(new TerrainGenericMountain()).surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainStone(
                                                            BiomeManager.greenhills.get().topBlock,
                                                            BiomeManager.greenhills.get().fillerBlock,
                                                            false,
                                                            null,
                                                            0.95f) })));
        }

        // ICEWASTELAND
        if (BiomeManager.icewasteland.isPresent()) {
            registry.register(
                    builder(BiomeManager.icewasteland.get()).climate(Climate.SNOW).river(registry.river(Climate.SNOW))
                            .variant(
                                    variant(TerrainCategory.PLAIN)
                                            .terrain(new TerrainPolar()).surfaces(
                                                    new SurfaceBase[] { new SurfacePolar(
                                                            BiomeManager.icewasteland.get().topBlock,
                                                            BiomeManager.icewasteland.get().fillerBlock) }))
                            .variant(
                                    variant(TerrainCategory.HILLS)
                                            .terrain(new TerrainGenericHills()).surfaces(
                                                    new SurfaceBase[] { new SurfacePolar(
                                                            BiomeManager.icewasteland.get().topBlock,
                                                            BiomeManager.icewasteland.get().fillerBlock) }))
                            .variant(
                                    variant(TerrainCategory.MOUNTAIN)
                                            .terrain(new TerrainGenericMountain()).surfaces(
                                                    new SurfaceBase[] { new SurfacePolar(
                                                            BiomeManager.icewasteland.get().topBlock,
                                                            BiomeManager.icewasteland.get().fillerBlock) })));
        }

        // GREENSWAMP
        if (BiomeManager.greenswamp.isPresent()) {
            registry.register(
                    builder(BiomeManager.greenswamp.get()).climate(Climate.WET).river(registry.river(Climate.WET))
                            .subcategory(TerrainSubcategory.LITTORAL).variant(
                                    variant(TerrainCategory.SWAMP).terrain(new TerrainSwampRiver()).surfaces(
                                            new SurfaceBase[] { new SurfaceGrassland(
                                                    BiomeManager.greenswamp.get().topBlock,
                                                    BiomeManager.greenswamp.get().fillerBlock,
                                                    Blocks.stone,
                                                    Blocks.cobblestone) })));
        }

        // MARSH
        if (BiomeManager.marsh.isPresent()) {
            registry.register(
                    builder(BiomeManager.marsh.get()).climate(Climate.WET).river(registry.river(Climate.WET))
                            .subcategory(TerrainSubcategory.LITTORAL).variant(
                                    variant(TerrainCategory.SWAMP).terrain(new TerrainMarsh()).surfaces(
                                            new SurfaceBase[] { new SurfaceMarshFix(
                                                    BiomeManager.marsh.get().topBlock,
                                                    BiomeManager.marsh.get().fillerBlock,
                                                    Blocks.stone,
                                                    Blocks.cobblestone) })));
        }

        // MEADOW
        if (BiomeManager.meadow.isPresent()) {
            registry.register(
                    builder(BiomeManager.meadow.get()).climate(Climate.COLD).river(registry.river(Climate.COLD))
                            .variant(
                                    variant(TerrainCategory.PLAIN)
                                            .terrain(new TerrainGrasslandHills(90f, 180f, 13f, 100f, 38f, 260f, 71f))
                                            .surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            BiomeManager.meadow.get().topBlock,
                                                            BiomeManager.meadow.get().fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone) }))
                            .variant(
                                    variant(TerrainCategory.HILLS)
                                            .terrain(new TerrainGenericHills())
                                            .surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            BiomeManager.meadow.get().topBlock,
                                                            BiomeManager.meadow.get().fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone) }))
                            .variant(
                                    variant(TerrainCategory.MOUNTAIN)
                                            .terrain(new TerrainGenericMountain())
                                            .surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            BiomeManager.meadow.get().topBlock,
                                                            BiomeManager.meadow.get().fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone) })));
        }

        // MINI JUNGLE
        if (BiomeManager.minijungle.isPresent()) {
            registry.register(
                    builder(BiomeManager.minijungle.get()).climate(Climate.WET).river(registry.river(Climate.WET))
                            .variant(
                                    variant(TerrainCategory.PLAIN)
                                            .terrain(new TerrainHighland(0f, 140f, 68f, 200f)).surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            BiomeManager.minijungle.get().topBlock,
                                                            BiomeManager.minijungle.get().fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone) }))
                            .variant(
                                    variant(TerrainCategory.HILLS)
                                            .terrain(new TerrainGenericHills()).surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            BiomeManager.minijungle.get().topBlock,
                                                            BiomeManager.minijungle.get().fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone) }))
                            .variant(
                                    variant(TerrainCategory.MOUNTAIN)
                                            .terrain(new TerrainGenericMountain()).surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            BiomeManager.minijungle.get().topBlock,
                                                            BiomeManager.minijungle.get().fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone) })));
        }

        // MOUNTAIN DESERT
        if (BiomeManager.mountaindesert.isPresent()) {
            registry.register(
                    builder(BiomeManager.mountaindesert.get()).climate(Climate.HOT).river(registry.oasisRiver())

                            .decorators(new BiomeDecorator[] { new RiverOasisDecorator() }).variant(
                                    variant(TerrainCategory.PLAIN)
                                            .terrain(new TerrainHilly(230f, 100f, 0f)).surfaces(
                                                    new SurfaceBase[] { new SurfaceDesertMountain(
                                                            BiomeManager.mountaindesert.get().topBlock,
                                                            BiomeManager.mountaindesert.get().fillerBlock,
                                                            false,
                                                            null,
                                                            0f,
                                                            1.5f,
                                                            60f,
                                                            65f,
                                                            1.5f), new SurfaceRiverOasis() }))
                            .variant(
                                    variant(TerrainCategory.HILLS)
                                            .terrain(new TerrainGenericHills()).surfaces(
                                                    new SurfaceBase[] { new SurfaceDesertMountain(
                                                            BiomeManager.mountaindesert.get().topBlock,
                                                            BiomeManager.mountaindesert.get().fillerBlock,
                                                            false,
                                                            null,
                                                            0f,
                                                            1.5f,
                                                            60f,
                                                            65f,
                                                            1.5f), new SurfaceRiverOasis() }))
                            .variant(
                                    variant(TerrainCategory.MOUNTAIN)
                                            .terrain(new TerrainGenericMountain()).surfaces(
                                                    new SurfaceBase[] { new SurfaceDesertMountain(
                                                            BiomeManager.mountaindesert.get().topBlock,
                                                            BiomeManager.mountaindesert.get().fillerBlock,
                                                            false,
                                                            null,
                                                            0f,
                                                            1.5f,
                                                            60f,
                                                            65f,
                                                            1.5f), new SurfaceRiverOasis() })));
        }

        // MOUNTAIN RIDGE
        if (BiomeManager.mountainridge.isPresent()) {
            registry.register(
                    builder(BiomeManager.mountainridge.get()).climate(Climate.HOT).river(registry.oasisRiver())

                            .decorators(new BiomeDecorator[] { new RiverOasisDecorator() }).variant(
                                    variant(TerrainCategory.PLAIN)
                                            .terrain(new TerrainHilly(230f, 110f, 0f)).surfaces(
                                                    new SurfaceBase[] { new SurfaceDesertMountain(
                                                            BiomeManager.mountainridge.get().topBlock,
                                                            BiomeManager.mountainridge.get().fillerBlock,
                                                            false,
                                                            null,
                                                            0f,
                                                            1.5f,
                                                            60f,
                                                            65f,
                                                            1.5f), new SurfaceRiverOasis() }))
                            .variant(
                                    variant(TerrainCategory.HILLS)
                                            .terrain(new TerrainGenericHills()).surfaces(
                                                    new SurfaceBase[] { new SurfaceDesertMountain(
                                                            BiomeManager.mountainridge.get().topBlock,
                                                            BiomeManager.mountainridge.get().fillerBlock,
                                                            false,
                                                            null,
                                                            0f,
                                                            1.5f,
                                                            60f,
                                                            65f,
                                                            1.5f), new SurfaceRiverOasis() }))
                            .variant(
                                    variant(TerrainCategory.MOUNTAIN)
                                            .terrain(new TerrainGenericMountain()).surfaces(
                                                    new SurfaceBase[] { new SurfaceDesertMountain(
                                                            BiomeManager.mountainridge.get().topBlock,
                                                            BiomeManager.mountainridge.get().fillerBlock,
                                                            false,
                                                            null,
                                                            0f,
                                                            1.5f,
                                                            60f,
                                                            65f,
                                                            1.5f), new SurfaceRiverOasis() })));
        }

        // MOUNTAIN TAIGA
        if (BiomeManager.mountaintaiga.isPresent()) {
            registry.register(
                    builder(BiomeManager.mountaintaiga.get()).climate(Climate.SNOW).river(registry.river(Climate.SNOW))
                            .variant(
                                    variant(TerrainCategory.PLAIN)
                                            .terrain(new TerrainMountainSpikes()).surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainStone(
                                                            BiomeManager.mountaintaiga.get().topBlock,
                                                            BiomeManager.mountaintaiga.get().fillerBlock,
                                                            false,
                                                            null,
                                                            1.2f) }))
                            .variant(
                                    variant(TerrainCategory.HILLS)
                                            .terrain(new TerrainGenericHills()).surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainStone(
                                                            BiomeManager.mountaintaiga.get().topBlock,
                                                            BiomeManager.mountaintaiga.get().fillerBlock,
                                                            false,
                                                            null,
                                                            1.2f) }))
                            .variant(
                                    variant(TerrainCategory.MOUNTAIN)
                                            .terrain(new TerrainGenericMountain()).surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainStone(
                                                            BiomeManager.mountaintaiga.get().topBlock,
                                                            BiomeManager.mountaintaiga.get().fillerBlock,
                                                            false,
                                                            null,
                                                            1.2f) })));
        }

        // PINE FOREST
        if (BiomeManager.pineforest.isPresent()) {
            registry.register(
                    builder(BiomeManager.pineforest.get()).climate(Climate.COLD).river(registry.river(Climate.COLD))
                            .variant(
                                    variant(TerrainCategory.PLAIN)
                                            .terrain(new TerrainMountainSpikes()).surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainStone(
                                                            BiomeManager.pineforest.get().topBlock,
                                                            BiomeManager.pineforest.get().fillerBlock,
                                                            false,
                                                            null,
                                                            1.2f) }))
                            .variant(
                                    variant(TerrainCategory.HILLS)
                                            .terrain(new TerrainGenericHills()).surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainStone(
                                                            BiomeManager.pineforest.get().topBlock,
                                                            BiomeManager.pineforest.get().fillerBlock,
                                                            false,
                                                            null,
                                                            1.2f) }))
                            .variant(
                                    variant(TerrainCategory.MOUNTAIN)
                                            .terrain(new TerrainGenericMountain()).surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainStone(
                                                            BiomeManager.pineforest.get().topBlock,
                                                            BiomeManager.pineforest.get().fillerBlock,
                                                            false,
                                                            null,
                                                            1.2f) })));
        }

        // RAINFOREST
        if (BiomeManager.rainforest.isPresent()) {
            registry.register(
                    builder(BiomeManager.rainforest.get()).climate(Climate.WET).river(registry.river(Climate.WET))
                            .subcategory(TerrainSubcategory.COLD_BORDER).variant(
                                    variant(TerrainCategory.PLAIN).terrain(new TerrainHilly(230f, 100f, 0f)).surfaces(
                                            new SurfaceBase[] { new SurfaceGrassland(
                                                    BiomeManager.rainforest.get().topBlock,
                                                    BiomeManager.rainforest.get().fillerBlock,
                                                    Blocks.stone,
                                                    Blocks.cobblestone) }))
                            .variant(
                                    variant(TerrainCategory.HILLS).terrain(new TerrainGenericHills()).surfaces(
                                            new SurfaceBase[] { new SurfaceGrassland(
                                                    BiomeManager.rainforest.get().topBlock,
                                                    BiomeManager.rainforest.get().fillerBlock,
                                                    Blocks.stone,
                                                    Blocks.cobblestone) }))
                            .variant(
                                    variant(TerrainCategory.MOUNTAIN).terrain(new TerrainGenericMountain()).surfaces(
                                            new SurfaceBase[] { new SurfaceGrassland(
                                                    BiomeManager.rainforest.get().topBlock,
                                                    BiomeManager.rainforest.get().fillerBlock,
                                                    Blocks.stone,
                                                    Blocks.cobblestone) })));
        }

        // REDWOOD FOREST
        if (BiomeManager.redwoodforest.isPresent()) {
            registry.register(
                    builder(BiomeManager.redwoodforest.get()).climate(Climate.COLD).river(registry.temperateRiver())
                            .variant(
                                    variant(TerrainCategory.PLAIN)
                                            .terrain(new TerrainHilly(230f, 120f, 0f)).surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainStone(
                                                            BiomeManager.redwoodforest.get().topBlock,
                                                            BiomeManager.redwoodforest.get().fillerBlock,
                                                            true,
                                                            Blocks.sand,
                                                            0.2f) }))
                            .variant(
                                    variant(TerrainCategory.HILLS)
                                            .terrain(new TerrainGenericHills()).surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainStone(
                                                            BiomeManager.redwoodforest.get().topBlock,
                                                            BiomeManager.redwoodforest.get().fillerBlock,
                                                            true,
                                                            Blocks.sand,
                                                            0.2f) }))
                            .variant(
                                    variant(TerrainCategory.MOUNTAIN)
                                            .terrain(new TerrainGenericMountain()).surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainStone(
                                                            BiomeManager.redwoodforest.get().topBlock,
                                                            BiomeManager.redwoodforest.get().fillerBlock,
                                                            true,
                                                            Blocks.sand,
                                                            0.2f) })));
        }

        // REDWOOD LUSH
        if (BiomeManager.redwoodlush.isPresent()) {
            registry.register(
                    builder(BiomeManager.redwoodlush.get()).climate(Climate.WET).river(registry.river(Climate.WET))
                            .variant(
                                    variant(TerrainCategory.PLAIN)
                                            .terrain(new TerrainHilly(230f, 120f, 0f)).surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainStone(
                                                            BiomeManager.redwoodlush.get().topBlock,
                                                            BiomeManager.redwoodlush.get().fillerBlock,
                                                            true,
                                                            Blocks.sand,
                                                            0.2f) }))
                            .variant(
                                    variant(TerrainCategory.HILLS)
                                            .terrain(new TerrainGenericHills()).surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainStone(
                                                            BiomeManager.redwoodlush.get().topBlock,
                                                            BiomeManager.redwoodlush.get().fillerBlock,
                                                            true,
                                                            Blocks.sand,
                                                            0.2f) }))
                            .variant(
                                    variant(TerrainCategory.MOUNTAIN)
                                            .terrain(new TerrainGenericMountain()).surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainStone(
                                                            BiomeManager.redwoodlush.get().topBlock,
                                                            BiomeManager.redwoodlush.get().fillerBlock,
                                                            true,
                                                            Blocks.sand,
                                                            0.2f) })));
        }

        // SAVANNA
        if (BiomeManager.savanna.isPresent()) {
            registry.register(
                    builder(BiomeManager.savanna.get()).climate(Climate.HOT).river(registry.river(Climate.HOT)).variant(
                            variant(TerrainCategory.PLAIN)
                                    .terrain(new TerrainGrasslandFlats()).surfaces(
                                            new SurfaceBase[] { new SurfaceGrasslandMix1(
                                                    BiomeManager.savanna.get().topBlock,
                                                    BiomeManager.savanna.get().fillerBlock,
                                                    Blocks.sand,
                                                    Blocks.stone,
                                                    Blocks.cobblestone,
                                                    13f,
                                                    0.27f) }))
                    .variant(
                            variant(TerrainCategory.HILLS)
                                    .terrain(new TerrainGenericHills()).surfaces(
                                            new SurfaceBase[] { new SurfaceGrasslandMix1(
                                                    BiomeManager.savanna.get().topBlock,
                                                    BiomeManager.savanna.get().fillerBlock,
                                                    Blocks.sand,
                                                    Blocks.stone,
                                                    Blocks.cobblestone,
                                                    13f,
                                                    0.27f) }))
                    .variant(
                            variant(TerrainCategory.MOUNTAIN)
                                    .terrain(new TerrainGenericMountain()).surfaces(
                                            new SurfaceBase[] { new SurfaceGrasslandMix1(
                                                    BiomeManager.savanna.get().topBlock,
                                                    BiomeManager.savanna.get().fillerBlock,
                                                    Blocks.sand,
                                                    Blocks.stone,
                                                    Blocks.cobblestone,
                                                    13f,
                                                    0.27f) })));
        }

        // SHRUBLAND
        if (BiomeManager.shrubland.isPresent()) {
            registry.register(
                    builder(BiomeManager.shrubland.get()).climate(Climate.COLD).river(registry.temperateRiver())
                            .variant(
                                    variant(TerrainCategory.PLAIN)
                                            .terrain(new TerrainGrasslandHills(90f, 180f, 13f, 100f, 38f, 260f, 71f))
                                            .surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            BiomeManager.shrubland.get().topBlock,
                                                            BiomeManager.shrubland.get().fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone) }))
                            .variant(
                                    variant(TerrainCategory.HILLS)
                                            .terrain(new TerrainGenericHills())
                                            .surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            BiomeManager.shrubland.get().topBlock,
                                                            BiomeManager.shrubland.get().fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone) }))
                            .variant(
                                    variant(TerrainCategory.MOUNTAIN)
                                            .terrain(new TerrainGenericMountain())
                                            .surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            BiomeManager.shrubland.get().topBlock,
                                                            BiomeManager.shrubland.get().fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone) })));
        }

        // SNOW FOREST
        if (BiomeManager.snowforest.isPresent()) {
            registry.register(
                    builder(BiomeManager.snowforest.get()).climate(Climate.SNOW).river(registry.river(Climate.SNOW))
                            .variant(
                                    variant(TerrainCategory.PLAIN)
                                            .terrain(new TerrainHighland(0f, 140f, 68f, 200f)).surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            BiomeManager.snowforest.get().topBlock,
                                                            BiomeManager.snowforest.get().fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone) }))
                            .variant(
                                    variant(TerrainCategory.HILLS)
                                            .terrain(new TerrainGenericHills()).surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            BiomeManager.snowforest.get().topBlock,
                                                            BiomeManager.snowforest.get().fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone) }))
                            .variant(
                                    variant(TerrainCategory.MOUNTAIN)
                                            .terrain(new TerrainGenericMountain()).surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            BiomeManager.snowforest.get().topBlock,
                                                            BiomeManager.snowforest.get().fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone) })));
        }

        // SNOWY RAIN FOREST
        if (BiomeManager.snowyrainforest.isPresent()) {
            registry.register(
                    builder(BiomeManager.snowyrainforest.get()).climate(Climate.SNOW)
                            .river(registry.river(Climate.SNOW)).variant(
                                    variant(TerrainCategory.PLAIN)
                                            .terrain(new TerrainHilly(230f, 120f, 0f)).surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            BiomeManager.snowforest.get().topBlock,
                                                            BiomeManager.snowforest.get().fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone) }))
                            .variant(
                                    variant(TerrainCategory.HILLS)
                                            .terrain(new TerrainGenericHills()).surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            BiomeManager.snowforest.get().topBlock,
                                                            BiomeManager.snowforest.get().fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone) }))
                            .variant(
                                    variant(TerrainCategory.MOUNTAIN)
                                            .terrain(new TerrainGenericMountain()).surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            BiomeManager.snowforest.get().topBlock,
                                                            BiomeManager.snowforest.get().fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone) })));
        }

        // TEMPERATE RAINFOREST
        if (BiomeManager.temperaterainforest.isPresent()) {
            registry.register(
                    builder(BiomeManager.temperaterainforest.get()).climate(Climate.WET)
                            .river(registry.river(Climate.WET)).variant(
                                    variant(TerrainCategory.PLAIN)
                                            .terrain(new TerrainHilly(230f, 120f, 0f)).surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainStone(
                                                            BiomeManager.temperaterainforest.get().topBlock,
                                                            BiomeManager.temperaterainforest.get().fillerBlock,
                                                            true,
                                                            Blocks.sand,
                                                            0.2f) }))
                            .variant(
                                    variant(TerrainCategory.HILLS)
                                            .terrain(new TerrainGenericHills()).surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainStone(
                                                            BiomeManager.temperaterainforest.get().topBlock,
                                                            BiomeManager.temperaterainforest.get().fillerBlock,
                                                            true,
                                                            Blocks.sand,
                                                            0.2f) }))
                            .variant(
                                    variant(TerrainCategory.MOUNTAIN)
                                            .terrain(new TerrainGenericMountain()).surfaces(
                                                    new SurfaceBase[] { new SurfaceMountainStone(
                                                            BiomeManager.temperaterainforest.get().topBlock,
                                                            BiomeManager.temperaterainforest.get().fillerBlock,
                                                            true,
                                                            Blocks.sand,
                                                            0.2f) })));
        }

        // TUNDRA
        if (BiomeManager.tundra.isPresent()) {
            registry.register(
                    builder(BiomeManager.tundra.get()).climate(Climate.COLD).river(registry.river(Climate.COLD))
                            .variant(
                                    variant(TerrainCategory.PLAIN)
                                            .terrain(new TerrainGrasslandHills(90f, 180f, 13f, 100f, 38f, 260f, 71f))
                                            .surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            BiomeManager.tundra.get().topBlock,
                                                            BiomeManager.tundra.get().fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone) }))
                            .variant(
                                    variant(TerrainCategory.HILLS)
                                            .terrain(new TerrainGenericHills())
                                            .surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            BiomeManager.tundra.get().topBlock,
                                                            BiomeManager.tundra.get().fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone) }))
                            .variant(
                                    variant(TerrainCategory.MOUNTAIN)
                                            .terrain(new TerrainGenericMountain())
                                            .surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            BiomeManager.tundra.get().topBlock,
                                                            BiomeManager.tundra.get().fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone) })));
        }

        // WASTELAND
        if (BiomeManager.wasteland.isPresent()) {
            registry.register(
                    builder(BiomeManager.wasteland.get()).climate(Climate.HOT).river(registry.oasisRiver())

                            .decorators(new BiomeDecorator[] { new RiverOasisDecorator() }).selectable(
                                    true)
                            .variant(
                                    variant(TerrainCategory.PLAIN)
                                            .terrain(new TerrainGrasslandHills(30f, 180f, 13f, 100f, 28f, 260f, 70f))
                                            .surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            BiomeManager.wasteland.get().topBlock,
                                                            BiomeManager.wasteland.get().fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone), new SurfaceRiverOasis() }))
                            .variant(
                                    variant(TerrainCategory.HILLS)
                                            .terrain(new TerrainGenericHills())
                                            .surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            BiomeManager.wasteland.get().topBlock,
                                                            BiomeManager.wasteland.get().fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone), new SurfaceRiverOasis() }))
                            .variant(
                                    variant(TerrainCategory.MOUNTAIN)
                                            .terrain(new TerrainGenericMountain())
                                            .surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            BiomeManager.wasteland.get().topBlock,
                                                            BiomeManager.wasteland.get().fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone), new SurfaceRiverOasis() })));
        }

        // WOODLANDS
        if (BiomeManager.woodlands.isPresent()) {
            registry.register(
                    builder(BiomeManager.woodlands.get()).climate(Climate.COLD).river(registry.temperateRiver())
                            .variant(
                                    variant(TerrainCategory.PLAIN)
                                            .terrain(new TerrainHighland(0f, 140f, 68f, 200f)).surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            BiomeManager.woodlands.get().topBlock,
                                                            BiomeManager.woodlands.get().fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone) }))
                            .variant(
                                    variant(TerrainCategory.HILLS)
                                            .terrain(new TerrainGenericHills()).surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            BiomeManager.woodlands.get().topBlock,
                                                            BiomeManager.woodlands.get().fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone) }))
                            .variant(
                                    variant(TerrainCategory.MOUNTAIN)
                                            .terrain(new TerrainGenericMountain()).surfaces(
                                                    new SurfaceBase[] { new SurfaceGrassland(
                                                            BiomeManager.woodlands.get().topBlock,
                                                            BiomeManager.woodlands.get().fillerBlock,
                                                            Blocks.stone,
                                                            Blocks.cobblestone) })));
        }
    }
}
