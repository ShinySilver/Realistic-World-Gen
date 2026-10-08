package rwg.registry;

import static rwg.registry.BiomeRegistration.builder;
import static rwg.registry.BiomeRegistration.variant;

import net.minecraft.block.Block;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.init.Blocks;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;

import rwg.ConfigRWG;
import rwg.biomes.decorators.BiomeDecorator;
import rwg.biomes.decorators.CanyonDecorator;
import rwg.biomes.decorators.CanyonForestDecorator;
import rwg.biomes.decorators.DarkRedwoodDecorator;
import rwg.biomes.decorators.DarkRedwoodPlainsDecorator;
import rwg.biomes.decorators.DesertDecorator;
import rwg.biomes.decorators.DesertMountainsDecorator;
import rwg.biomes.decorators.DuneValleyDecorator;
import rwg.biomes.decorators.DuneValleyForestDecorator;
import rwg.biomes.decorators.HotForestDecorator;
import rwg.biomes.decorators.HotRedwoodDecorator;
import rwg.biomes.decorators.MesaDecorator;
import rwg.biomes.decorators.MesaPlainsDecorator;
import rwg.biomes.decorators.NativeBiomeDecorator;
import rwg.biomes.decorators.OasisDecorator;
import rwg.biomes.decorators.PolarDecorator;
import rwg.biomes.decorators.RedDesertMountainsDecorator;
import rwg.biomes.decorators.RedOasisDecorator;
import rwg.biomes.decorators.RedwoodDecorator;
import rwg.biomes.decorators.RedwoodSnowDecorator;
import rwg.biomes.decorators.SavannaDecorator;
import rwg.biomes.decorators.SavannaDunesDecorator;
import rwg.biomes.decorators.SavannaForestDecorator;
import rwg.biomes.decorators.SnowHillsDecorator;
import rwg.biomes.decorators.SnowLakesDecorator;
import rwg.biomes.decorators.SnowRiversDecorator;
import rwg.biomes.decorators.StoneMountainsCactusDecorator;
import rwg.biomes.decorators.StoneMountainsDecorator;
import rwg.biomes.decorators.TaigaHillsDecorator;
import rwg.biomes.decorators.TaigaPlainsDecorator;
import rwg.biomes.decorators.TundraHillsDecorator;
import rwg.biomes.decorators.TundraPlainsDecorator;
import rwg.biomes.decorators.WoodHillsDecorator;
import rwg.biomes.decorators.WoodMountainsDecorator;
import rwg.biomes.surface.SurfaceBase;
import rwg.biomes.surface.SurfaceCanyon;
import rwg.biomes.surface.SurfaceDesert;
import rwg.biomes.surface.SurfaceDesertMountain;
import rwg.biomes.surface.SurfaceDesertOasis;
import rwg.biomes.surface.SurfaceDuneValley;
import rwg.biomes.surface.SurfaceGrassland;
import rwg.biomes.surface.SurfaceGrasslandMix1;
import rwg.biomes.surface.SurfaceGrasslandMixBig;
import rwg.biomes.surface.SurfaceMesa;
import rwg.biomes.surface.SurfaceMountainSnow;
import rwg.biomes.surface.SurfaceMountainStone;
import rwg.biomes.surface.SurfaceMountainStoneMix1;
import rwg.biomes.surface.SurfaceNativeBiome;
import rwg.biomes.surface.SurfacePolar;
import rwg.biomes.surface.SurfaceRedDesert;
import rwg.biomes.surface.SurfaceRiverOasis;
import rwg.biomes.surface.SurfaceTundra;
import rwg.biomes.terrain.TerrainBase;
import rwg.biomes.terrain.TerrainCanyon;
import rwg.biomes.terrain.TerrainConstant;
import rwg.biomes.terrain.TerrainDuneValley;
import rwg.biomes.terrain.TerrainDunes;
import rwg.biomes.terrain.TerrainFlatLakes;
import rwg.biomes.terrain.TerrainGenericCanyon;
import rwg.biomes.terrain.TerrainGenericCliff;
import rwg.biomes.terrain.TerrainGenericHills;
import rwg.biomes.terrain.TerrainGenericMountain;
import rwg.biomes.terrain.TerrainGenericPlateau;
import rwg.biomes.terrain.TerrainGrasslandFlats;
import rwg.biomes.terrain.TerrainGrasslandMountains;
import rwg.biomes.terrain.TerrainHighland;
import rwg.biomes.terrain.TerrainHilly;
import rwg.biomes.terrain.TerrainMarsh;
import rwg.biomes.terrain.TerrainMesa;
import rwg.biomes.terrain.TerrainMountain;
import rwg.biomes.terrain.TerrainMountainRiver;
import rwg.biomes.terrain.TerrainMountainSpikes;
import rwg.biomes.terrain.TerrainPolar;
import rwg.biomes.terrain.TerrainSmallIsland;
import rwg.biomes.villages.VillageMaterialPreset;

/** Handwritten component registrations for RWG's built-in biome set. */
public final class BuiltinBiomes {

	private BuiltinBiomes() {
	}

	private static BiomeBuilder biome(int index, String name) {
		return new BiomeBuilder(ConfigRWG.biomeIDs[index], name);
	}

	public static void register(BiomeRegistry registry) {
		BiomeGenBase riverIce = registry.registerRiver(Climate.SNOW,
				biome(0, "Ice River").climate(0f, .1f).creature(EntityWolf.class, 8, 4, 4).build());
		BiomeGenBase riverCold = registry.registerRiver(Climate.COLD,
				biome(1, "Cold River").climate(.5f, .4f).creature(EntityWolf.class, 8, 1, 2).build());
		BiomeGenBase riverTemperate = registry.registerOwned(biome(2, "Temperate River").climate(.8f, .6f).build());
		BiomeGenBase riverHot = registry.registerRiver(Climate.HOT,
				biome(3, "Hot River").climate(.8f, .2f).noRain().build());
		BiomeGenBase riverWet = registry.registerRiver(Climate.WET,
				biome(4, "Wet River").climate(.9f, .9f).monster(EntityOcelot.class, 2, 1, 1).build());
		BiomeGenBase riverOasis = registry.registerOwned(biome(5, "River Oasis").climate(.9f, .9f).build());

		registry.setTemperateRiver(riverTemperate);
		registry.setOasisRiver(riverOasis);

		BiomeGenBase oceanIce = registry
				.registerOwned(biome(6, "Ice Ocean").climate(0f, .1f).creature(EntityWolf.class, 8, 4, 4).build());
		BiomeGenBase oceanCold = registry
				.registerOwned(biome(7, "Cold Ocean").climate(.5f, .4f).creature(EntityWolf.class, 8, 1, 2).build());
		BiomeGenBase oceanTemperate = registry.registerOwned(biome(8, "Temperate Ocean").climate(.8f, .6f).build());
		BiomeGenBase oceanHot = registry.registerOwned(biome(9, "Hot Ocean").climate(.8f, .2f).noRain().build());
		BiomeGenBase oceanWet = registry
				.registerOwned(biome(10, "Wet Ocean").climate(.9f, .9f).monster(EntityOcelot.class, 2, 1, 1).build());
		BiomeGenBase oceanOasis = registry.registerOwned(biome(11, "Ocean Oasis").climate(.9f, .9f).build());

		BiomeGenBase snowDesert = registry
				.registerOwned(biome(12, "Snow Desert").climate(0f, .1f).noCreatures().build());
		BiomeGenBase snowForest = registry
				.registerOwned(biome(13, "Snow Forest").climate(0f, .1f).creature(EntityWolf.class, 8, 4, 4).build());
		BiomeGenBase coldPlains = registry.registerOwned(biome(14, "Cold Plains").climate(.2f, .2f)
				.creature(EntityWolf.class, 5, 2, 3).creature(EntityHorse.class, 5, 2, 3).build());
		BiomeGenBase coldForest = registry
				.registerOwned(biome(15, "Cold Forest").climate(.5f, .4f).creature(EntityWolf.class, 8, 1, 2).build());
		BiomeGenBase hotPlains = registry
				.registerOwned(biome(16, "Hot Plains").climate(.9f, .1f).noRain().climateColors(1f, 0f, .9f, .1f)
						.creature(EntityHorse.class, 5, 2, 3).village(VillageMaterialPreset.ACACIA).build());
		BiomeGenBase hotForest = registry.registerOwned(biome(17, "Hot Forest").climate(.8f, .2f).noRain()
				.climateColors(1f, 0f, .8f, .2f).village(VillageMaterialPreset.ACACIA).build());
		BiomeGenBase hotDesert = registry.registerOwned(biome(18, "Hot Desert").climate(1f, 0f).noRain().noCreatures()
				.village(VillageMaterialPreset.SANDSTONE).build());
		BiomeGenBase plains = registry.registerOwned(
				biome(19, "Plains (RWG)").climate(.6f, .4f).creature(EntityHorse.class, 3, 2, 3).build());
		BiomeGenBase tropicalIsland = registry.registerOwned(biome(20, "Tropical Island").climate(.8f, .8f).build());
		BiomeGenBase redwood = registry.registerOwned(biome(21, "Redwood").climate(.7f, .6f)
				.creature(EntityWolf.class, 8, 1, 2).creature(EntityHorse.class, 3, 2, 3).build());
		BiomeGenBase jungle = registry.registerOwned(
				biome(22, "Jungle (RWG)").climate(.9f, .9f).monster(EntityOcelot.class, 2, 1, 1).build());
		BiomeGenBase oasis = registry.registerOwned(biome(23, "Oasis").climate(.9f, .9f).build());
		BiomeGenBase temperateForest = registry.registerOwned(
				biome(24, "Temperate Forest").climate(.8f, .6f).creature(EntityWolf.class, 8, 1, 2).build());
		BiomeGenBase jungleMesa = registry.registerOwned(biome(25, "Jungle Mesa").climate(.9f, 1f).waterColor(65326)
				.colors(5762404, 5762404).monster(EntityOcelot.class, 2, 1, 1).build());

		registry.setHotDesert(hotDesert);
		registry.setPlains(plains);

		BiomeDictionary.registerBiomeType(riverIce, Type.RIVER, Type.COLD, Type.SNOWY);
		BiomeDictionary.registerBiomeType(riverCold, Type.RIVER, Type.COLD, Type.CONIFEROUS, Type.FOREST);
		BiomeDictionary.registerBiomeType(riverTemperate, Type.RIVER, Type.COLD, Type.FOREST);
		BiomeDictionary.registerBiomeType(riverHot, Type.RIVER, Type.HOT, Type.DRY, Type.SANDY);
		BiomeDictionary.registerBiomeType(riverWet, Type.RIVER, Type.HOT, Type.WET, Type.JUNGLE);
		BiomeDictionary.registerBiomeType(riverOasis, Type.RIVER, Type.HOT, Type.WET, Type.JUNGLE);
		BiomeDictionary.registerBiomeType(oceanIce, Type.OCEAN, Type.BEACH, Type.COLD, Type.SNOWY);
		BiomeDictionary.registerBiomeType(oceanCold, Type.OCEAN, Type.BEACH, Type.COLD, Type.CONIFEROUS, Type.FOREST);
		BiomeDictionary.registerBiomeType(oceanTemperate, Type.OCEAN, Type.BEACH, Type.COLD, Type.FOREST);
		BiomeDictionary.registerBiomeType(oceanHot, Type.OCEAN, Type.BEACH, Type.HOT, Type.DRY, Type.SANDY);
		BiomeDictionary.registerBiomeType(oceanWet, Type.OCEAN, Type.BEACH, Type.HOT, Type.WET, Type.JUNGLE);
		BiomeDictionary.registerBiomeType(oceanOasis, Type.OCEAN, Type.BEACH, Type.HOT, Type.WET, Type.JUNGLE);
		BiomeDictionary.registerBiomeType(snowDesert, Type.COLD, Type.SNOWY, Type.WASTELAND);
		BiomeDictionary.registerBiomeType(snowForest, Type.COLD, Type.SNOWY, Type.CONIFEROUS, Type.FOREST);
		BiomeDictionary.registerBiomeType(coldPlains, Type.COLD, Type.WASTELAND);
		BiomeDictionary.registerBiomeType(coldForest, Type.COLD, Type.CONIFEROUS, Type.FOREST, Type.DENSE, Type.HILLS);
		BiomeDictionary.registerBiomeType(hotPlains, Type.HOT, Type.SAVANNA, Type.PLAINS, Type.SPARSE);
		BiomeDictionary.registerBiomeType(hotForest, Type.HOT, Type.SAVANNA, Type.PLAINS, Type.SPARSE);
		BiomeDictionary.registerBiomeType(hotDesert, Type.HOT, Type.DRY, Type.SANDY);
		BiomeDictionary.registerBiomeType(plains, Type.PLAINS);
		BiomeDictionary.registerBiomeType(tropicalIsland, Type.HOT, Type.WET, Type.JUNGLE);
		BiomeDictionary.registerBiomeType(redwood, Type.COLD, Type.CONIFEROUS, Type.FOREST);
		BiomeDictionary.registerBiomeType(jungle, Type.HOT, Type.WET, Type.JUNGLE);
		BiomeDictionary.registerBiomeType(jungleMesa, Type.HOT, Type.WET, Type.JUNGLE, Type.FOREST, Type.HILLS);
		registry.register(
				builder(snowDesert).climate(Climate.SNOW).river(riverIce).decorators(decorators(new PolarDecorator()))
						.variant(variant(TerrainCategory.PLAIN).terrain(new TerrainPolar())
								.surfaces(new SurfaceBase[] { new SurfacePolar(Blocks.snow, Blocks.snow) }))
						.variant(variant(TerrainCategory.HILLS).terrain(new TerrainGenericHills())
								.surfaces(new SurfaceBase[] { new SurfacePolar(Blocks.snow, Blocks.snow) }))
						.variant(variant(TerrainCategory.MOUNTAIN).terrain(new TerrainGenericMountain())
								.surfaces(new SurfaceBase[] { new SurfacePolar(Blocks.snow, Blocks.snow) })));
		registry.register(builder(snowForest).climate(Climate.SNOW).river(riverIce)
				.variant(variant(TerrainCategory.PLAIN).terrain(new TerrainMountainSpikes())
						.surfaces(surfaces(new SurfaceMountainSnow(Blocks.grass, Blocks.dirt, false, null, 0.2f)))
						.decorators(new SnowHillsDecorator()))
				.variant(variant(TerrainCategory.HILLS).terrain(new TerrainGenericHills())
						.surfaces(surfaces(new SurfaceMountainSnow(Blocks.grass, Blocks.dirt, false, null, 0.2f)))
						.decorators(new SnowHillsDecorator()))
				.variant(variant(TerrainCategory.MOUNTAIN).terrain(new TerrainGenericMountain())
						.surfaces(surfaces(new SurfaceMountainSnow(Blocks.grass, Blocks.dirt, false, null, 0.2f)))
						.decorators(new SnowHillsDecorator()))
				.variant(variant(TerrainCategory.PLAIN).terrain(new TerrainMountainRiver())
						.surfaces(surfaces(new SurfaceMountainSnow(Blocks.grass, Blocks.dirt, true, Blocks.sand, 0.2f)))
						.decorators(new SnowRiversDecorator()))
				.variant(variant(TerrainCategory.HILLS).terrain(new TerrainGenericHills())
						.surfaces(surfaces(new SurfaceMountainSnow(Blocks.grass, Blocks.dirt, true, Blocks.sand, 0.2f)))
						.decorators(new SnowRiversDecorator()))
				.variant(variant(TerrainCategory.MOUNTAIN).terrain(new TerrainGenericMountain())
						.surfaces(surfaces(new SurfaceMountainSnow(Blocks.grass, Blocks.dirt, true, Blocks.sand, 0.2f)))
						.decorators(new SnowRiversDecorator()))
				.variant(variant(TerrainCategory.PLAIN).terrain(new TerrainFlatLakes())
						.surfaces(surfaces(new SurfaceMountainSnow(Blocks.grass, Blocks.dirt, true, Blocks.sand, 0.2f)))
						.decorators(new SnowLakesDecorator()))
				.variant(variant(TerrainCategory.PLAIN)
						.terrain(new TerrainHilly(230f, 120f, 90f))
						.surfaces(surfaces(new SurfaceMountainSnow(Blocks.grass, Blocks.dirt, true, Blocks.sand, 0.2f)))
						.decorators(new RedwoodSnowDecorator()))
				.variant(variant(TerrainCategory.HILLS)
						.terrain(new TerrainGenericHills())
						.surfaces(surfaces(new SurfaceMountainSnow(Blocks.grass, Blocks.dirt, true, Blocks.sand, 0.2f)))
						.decorators(new RedwoodSnowDecorator()))
				.variant(variant(TerrainCategory.MOUNTAIN)
						.terrain(new TerrainGenericMountain())
						.surfaces(surfaces(new SurfaceMountainSnow(Blocks.grass, Blocks.dirt, true, Blocks.sand, 0.2f)))
						.decorators(new RedwoodSnowDecorator()))
				.variant(variant(TerrainCategory.PLATEAU, TerrainCategory.CANYON).terrain(new TerrainMesa())
						.surfaces(surfaces(new SurfaceMountainSnow(Blocks.grass, Blocks.dirt, true, Blocks.sand, 0.2f)))
						.decorators(new SnowHillsDecorator())));
		registry.register(builder(coldPlains).climate(Climate.COLD).river(riverCold)
				.decorators(decorators(new TundraHillsDecorator()))
				.variant(variant(TerrainCategory.PLAIN).terrain(new TerrainMountain())
						.surfaces(new SurfaceBase[] { new SurfaceTundra(Blocks.grass, Blocks.dirt) }))
				.variant(variant(TerrainCategory.HILLS).terrain(new TerrainGenericHills())
						.surfaces(new SurfaceBase[] { new SurfaceTundra(Blocks.grass, Blocks.dirt) }))
				.variant(variant(TerrainCategory.MOUNTAIN).terrain(new TerrainGenericMountain())
						.surfaces(new SurfaceBase[] { new SurfaceTundra(Blocks.grass, Blocks.dirt) })));
		registry.register(builder(coldPlains).climate(Climate.COLD).river(riverCold)
				.decorators(decorators(new TundraPlainsDecorator()))
				.variant(variant(TerrainCategory.PLAIN).terrain(new TerrainFlatLakes())
						.surfaces(new SurfaceBase[] { new SurfaceTundra(Blocks.grass, Blocks.dirt) })));
		registry.register(builder(coldForest).climate(Climate.COLD).river(riverCold)
				.decorators(decorators(new TaigaHillsDecorator()))
				.variant(variant(TerrainCategory.PLAIN).terrain(new TerrainMountainRiver())
						.surfaces(surfaces(
								new SurfaceMountainSnow(Blocks.grass, Blocks.dirt, true, Blocks.sand, 0.2f))))
				.variant(variant(TerrainCategory.HILLS).terrain(new TerrainGenericHills())
						.surfaces(surfaces(
								new SurfaceMountainSnow(Blocks.grass, Blocks.dirt, true, Blocks.sand, 0.2f))))
				.variant(variant(TerrainCategory.MOUNTAIN).terrain(new TerrainGenericMountain())
						.surfaces(surfaces(
								new SurfaceMountainSnow(Blocks.grass, Blocks.dirt, true, Blocks.sand, 0.2f)))));
		registry.register(builder(coldForest).climate(Climate.COLD).river(riverCold)
				.decorators(decorators(new TaigaPlainsDecorator()))
				.variant(variant(TerrainCategory.PLAIN).terrain(new TerrainFlatLakes())
						.surfaces(surfaces(new SurfaceTundra(Blocks.grass, Blocks.dirt)))));
		registry.register(builder(coldForest).climate(Climate.COLD).river(riverCold)
				.decorators(decorators(new RedwoodDecorator()))
				.variant(variant(TerrainCategory.PLAIN).terrain(new TerrainHilly(230f, 120f, 90f)).surfaces(surfaces(
						new SurfaceMountainStone(Blocks.grass, Blocks.dirt, true, Blocks.sand, 0.2f))))
				.variant(variant(TerrainCategory.HILLS).terrain(new TerrainGenericHills()).surfaces(surfaces(
						new SurfaceMountainStone(Blocks.grass, Blocks.dirt, true, Blocks.sand, 0.2f))))
				.variant(variant(TerrainCategory.MOUNTAIN).terrain(new TerrainGenericMountain()).surfaces(surfaces(
						new SurfaceMountainStone(Blocks.grass, Blocks.dirt, true, Blocks.sand, 0.2f)))));
		registry.register(builder(coldForest).climate(Climate.COLD).river(riverTemperate)
				.decorators(decorators(new DarkRedwoodDecorator()))
				.variant(variant(TerrainCategory.PLAIN)
						.terrain(new TerrainHilly(230f, 120f, 0f)).surfaces(surfaces(darkRedwoodSurface())))
				.variant(variant(TerrainCategory.HILLS)
						.terrain(new TerrainGenericHills()).surfaces(surfaces(darkRedwoodSurface())))
				.variant(variant(TerrainCategory.MOUNTAIN)
						.terrain(new TerrainGenericMountain()).surfaces(surfaces(darkRedwoodSurface()))));
		registry.register(builder(coldForest).climate(Climate.COLD).river(riverTemperate)
				.decorators(decorators(new DarkRedwoodPlainsDecorator()))
				.variant(variant(TerrainCategory.PLAIN).terrain(new TerrainGrasslandFlats())
						.surfaces(surfaces(stoneMix(Blocks.grass, Blocks.dirt, 0.15f))))
				.variant(variant(TerrainCategory.HILLS).terrain(new TerrainGenericHills())
						.surfaces(surfaces(stoneMix(Blocks.grass, Blocks.dirt, 0.15f))))
				.variant(variant(TerrainCategory.MOUNTAIN).terrain(new TerrainGenericMountain())
						.surfaces(surfaces(stoneMix(Blocks.grass, Blocks.dirt, 0.15f)))));
		registry.register(builder(temperateForest).climate(Climate.COLD).river(riverTemperate)
				.decorators(decorators(new WoodHillsDecorator())).weight(2)
				.variant(variant(TerrainCategory.PLAIN)
						.terrain(new TerrainHilly(230f, 120f, 0f))
						.surfaces(surfaces(new SurfaceMountainStone(Blocks.grass, Blocks.dirt, false, null, 0f, 1.5f,
								60f, 65f, 1.5f))))
				.variant(variant(TerrainCategory.HILLS)
						.terrain(new TerrainGenericHills())
						.surfaces(surfaces(new SurfaceMountainStone(Blocks.grass, Blocks.dirt, false, null, 0f, 1.5f,
								60f, 65f, 1.5f))))
				.variant(variant(TerrainCategory.MOUNTAIN)
						.terrain(new TerrainGenericMountain())
						.surfaces(surfaces(new SurfaceMountainStone(Blocks.grass, Blocks.dirt, false, null, 0f, 1.5f,
								60f, 65f, 1.5f)))));
		registry.register(builder(temperateForest).climate(Climate.COLD).river(riverTemperate)
				.decorators(decorators(new WoodMountainsDecorator())).weight(2)
				.variant(variant(TerrainCategory.PLAIN).terrain(new TerrainMountainRiver())
						.surfaces(surfaces(
								new SurfaceMountainSnow(Blocks.grass, Blocks.dirt, true, Blocks.sand, 0.2f))))
				.variant(variant(TerrainCategory.HILLS).terrain(new TerrainGenericHills())
						.surfaces(surfaces(
								new SurfaceMountainSnow(Blocks.grass, Blocks.dirt, true, Blocks.sand, 0.2f))))
				.variant(variant(TerrainCategory.MOUNTAIN).terrain(new TerrainGenericMountain())
						.surfaces(surfaces(
								new SurfaceMountainSnow(Blocks.grass, Blocks.dirt, true, Blocks.sand, 0.2f)))));
		registry.register(builder(hotForest).climate(Climate.HOT).river(riverOasis)
				.decorators(decorators(new DuneValleyForestDecorator()))
				.variant(variant(TerrainCategory.PLAIN).terrain(new TerrainDuneValley(220f))
						.surfaces(surfaces(
								new SurfaceDuneValley(Blocks.grass, Blocks.dirt, 220f, false, true, hotDesert))))
				.variant(variant(TerrainCategory.HILLS).terrain(new TerrainGenericHills())
						.surfaces(surfaces(
								new SurfaceDuneValley(Blocks.grass, Blocks.dirt, 220f, false, true, hotDesert))))
				.variant(variant(TerrainCategory.MOUNTAIN).terrain(new TerrainGenericMountain())
						.surfaces(surfaces(
								new SurfaceDuneValley(Blocks.grass, Blocks.dirt, 220f, false, true, hotDesert)))));
		registry.register(builder(hotPlains).climate(Climate.HOT).river(riverHot)
				.decorators(decorators(new SavannaDecorator()))
				.variant(variant(TerrainCategory.PLAIN).terrain(new TerrainGrasslandFlats())
						.surfaces(surfaces(new SurfaceGrasslandMix1(Blocks.grass, Blocks.dirt, Blocks.sand,
								Blocks.stone, Blocks.cobblestone, 13f, 0.27f))))
				.variant(variant(TerrainCategory.HILLS).terrain(new TerrainGenericHills())
						.surfaces(surfaces(new SurfaceGrasslandMix1(Blocks.grass, Blocks.dirt, Blocks.sand,
								Blocks.stone, Blocks.cobblestone, 13f, 0.27f))))
				.variant(variant(TerrainCategory.MOUNTAIN).terrain(new TerrainGenericMountain())
						.surfaces(surfaces(new SurfaceGrasslandMix1(Blocks.grass, Blocks.dirt, Blocks.sand,
								Blocks.stone, Blocks.cobblestone, 13f, 0.27f)))));
		registry.register(builder(hotPlains).climate(Climate.HOT).river(riverHot)
				.decorators(decorators(new SavannaForestDecorator()))
				.variant(variant(TerrainCategory.PLAIN)
						.terrain(new TerrainGrasslandMountains())
						.surfaces(surfaces(new SurfaceMountainStone(Blocks.grass, Blocks.dirt, false, null, 0.6f))))
				.variant(variant(TerrainCategory.HILLS)
						.terrain(new TerrainGenericHills())
						.surfaces(surfaces(new SurfaceMountainStone(Blocks.grass, Blocks.dirt, false, null, 0.6f))))
				.variant(variant(TerrainCategory.MOUNTAIN)
						.terrain(new TerrainGenericMountain())
						.surfaces(surfaces(new SurfaceMountainStone(Blocks.grass, Blocks.dirt, false, null, 0.6f)))));
		registry.register(builder(hotPlains).climate(Climate.HOT).river(riverHot)
				.decorators(decorators(new SavannaDunesDecorator())).variant(
						variant(TerrainCategory.PLAIN).terrain(new TerrainDuneValley(300f))
								.surfaces(surfaces(
										new SurfaceDuneValley(Blocks.grass, Blocks.dirt, 300f, true, true, hotDesert),
										new SurfaceRiverOasis())))
				.variant(
						variant(TerrainCategory.HILLS).terrain(new TerrainGenericHills())
								.surfaces(surfaces(
										new SurfaceDuneValley(Blocks.grass, Blocks.dirt, 300f, true, true, hotDesert),
										new SurfaceRiverOasis())))
				.variant(
						variant(TerrainCategory.MOUNTAIN).terrain(new TerrainGenericMountain())
								.surfaces(surfaces(
										new SurfaceDuneValley(Blocks.grass, Blocks.dirt, 300f, true, true, hotDesert),
										new SurfaceRiverOasis()))));
		registry.register(builder(hotPlains).climate(Climate.HOT).river(riverOasis)
				.decorators(decorators(new StoneMountainsDecorator()))
				.variant(variant(TerrainCategory.PLAIN)
						.terrain(new TerrainHilly(230f, 120f, 0f))
						.surfaces(surfaces(stoneMix(Blocks.grass, Blocks.dirt, 0.08f), new SurfaceRiverOasis())))
				.variant(variant(TerrainCategory.HILLS)
						.terrain(new TerrainGenericHills())
						.surfaces(surfaces(stoneMix(Blocks.grass, Blocks.dirt, 0.08f), new SurfaceRiverOasis())))
				.variant(variant(TerrainCategory.MOUNTAIN)
						.terrain(new TerrainGenericMountain())
						.surfaces(surfaces(stoneMix(Blocks.grass, Blocks.dirt, 0.08f), new SurfaceRiverOasis()))));
		registry.register(builder(hotPlains).climate(Climate.HOT).river(riverOasis)
				.decorators(decorators(new StoneMountainsCactusDecorator()))
				.variant(variant(TerrainCategory.PLAIN)
						.terrain(new TerrainHilly(230f, 120f, 0f))
						.surfaces(surfaces(stoneMix(Blocks.grass, Blocks.dirt, 0.20f), new SurfaceRiverOasis())))
				.variant(variant(TerrainCategory.HILLS)
						.terrain(new TerrainGenericHills())
						.surfaces(surfaces(stoneMix(Blocks.grass, Blocks.dirt, 0.20f), new SurfaceRiverOasis())))
				.variant(variant(TerrainCategory.MOUNTAIN)
						.terrain(new TerrainGenericMountain())
						.surfaces(surfaces(stoneMix(Blocks.grass, Blocks.dirt, 0.20f), new SurfaceRiverOasis()))));
		registry.register(
				builder(hotForest).climate(Climate.HOT).river(riverHot).decorators(decorators(new HotForestDecorator()))
						.variant(variant(TerrainCategory.PLAIN)
								.terrain(new TerrainGrasslandFlats()).surfaces(surfaces(hotForestSurface())))
						.variant(variant(TerrainCategory.HILLS)
								.terrain(new TerrainGenericHills()).surfaces(surfaces(hotForestSurface())))
						.variant(variant(TerrainCategory.MOUNTAIN)
								.terrain(new TerrainGenericMountain()).surfaces(surfaces(hotForestSurface()))));
		registry.register(builder(hotForest).climate(Climate.HOT).river(riverHot)
				.decorators(decorators(new HotRedwoodDecorator()))
				.variant(variant(TerrainCategory.PLAIN).terrain(new TerrainGrasslandFlats())
						.surfaces(surfaces(hotForestSurface())))
				.variant(variant(TerrainCategory.HILLS).terrain(new TerrainGenericHills())
						.surfaces(surfaces(hotForestSurface())))
				.variant(variant(TerrainCategory.MOUNTAIN).terrain(new TerrainGenericMountain())
						.surfaces(surfaces(hotForestSurface()))));
		registry.register(builder(hotForest).climate(Climate.HOT).river(riverOasis)
				.decorators(decorators(new CanyonForestDecorator()))
				.variant(variant(TerrainCategory.CANYON)
						.terrain(new TerrainCanyon(true, 35f, 160f, 60f, 40f, 69f))
						.surfaces(surfaces(new SurfaceCanyon(Blocks.sand, Blocks.sand, (byte) 1, 47)))));

		// Vanilla mushroom-island support.
		registry.register(builder(BiomeGenBase.mushroomIsland).climate(Climate.WET).river(riverWet)

				.decorators(decorators(new NativeBiomeDecorator(BiomeGenBase.mushroomIsland)))
				.variant(variant(TerrainCategory.SMALL_ISLAND).terrain(new TerrainSmallIsland())
						.surfaces(new SurfaceBase[] { grass(BiomeGenBase.mushroomIsland) })));
		// Vanilla jungle support.
		registry.register(builder(BiomeGenBase.jungle).climate(Climate.WET).river(riverWet)
				.decorators(decorators(new NativeBiomeDecorator(BiomeGenBase.jungle)))
				.variant(variant(TerrainCategory.PLAIN)
						.terrain(new TerrainHighland(0f, 140f, 68f, 200f))
						.surfaces(new SurfaceBase[] { grass(BiomeGenBase.jungle) }))
				.variant(variant(TerrainCategory.HILLS)
						.terrain(new TerrainGenericHills())
						.surfaces(new SurfaceBase[] { grass(BiomeGenBase.jungle) }))
				.variant(variant(TerrainCategory.MOUNTAIN)
						.terrain(new TerrainGenericMountain())
						.surfaces(new SurfaceBase[] { grass(BiomeGenBase.jungle) })));
		BiomeGenBase iceSpikes = BiomeGenBase.getBiome(BiomeGenBase.icePlains.biomeID + 128);
		// Vanilla ice-spikes support.
		registry.register(builder(iceSpikes).climate(Climate.SNOW).river(riverIce).subcategory(TerrainSubcategory.SMALL)
				.decorators(decorators(new NativeBiomeDecorator(iceSpikes)))
				.variant(variant(TerrainCategory.PLAIN).terrain(new TerrainHighland(0f, 140f, 68f, 200f))
						.surfaces(new SurfaceBase[] { grass(iceSpikes) }))
				.variant(variant(TerrainCategory.HILLS).terrain(new TerrainGenericHills())
						.surfaces(new SurfaceBase[] { grass(iceSpikes) }))
				.variant(variant(TerrainCategory.MOUNTAIN).terrain(new TerrainGenericMountain())
						.surfaces(new SurfaceBase[] { grass(iceSpikes) })));
		BiomeGenBase sunflowerPlains = BiomeGenBase.getBiome(BiomeGenBase.plains.biomeID + 128);
		// Vanilla sunflower-plains support.
		registry.register(builder(sunflowerPlains).climate(Climate.COLD).river(riverTemperate)
				.subcategory(TerrainSubcategory.SMALL).decorators(decorators(new NativeBiomeDecorator(sunflowerPlains)))
				.variant(variant(TerrainCategory.PLAIN).terrain(new TerrainMarsh())
						.surfaces(new SurfaceBase[] { grass(sunflowerPlains) })));
		registry.register(
				builder(hotPlains).climate(Climate.HOT).river(riverOasis).decorators(decorators(new CanyonDecorator()))
						.variant(variant(TerrainCategory.LARGE_ISLAND)
								.terrain(new TerrainCanyon(true, 35f, 160f, 60f, 40f, 69f))
								.surfaces(surfaces(new SurfaceCanyon(Blocks.sand, Blocks.sand, (byte) 1, 0)))));

		registerHydrology(registry, riverIce, riverCold, riverTemperate, riverHot, riverWet, riverOasis, oceanCold,
				oceanHot, oceanWet);
		registry.register(builder(hotDesert).climate(Climate.HOT).river(riverHot)
				.decorators(decorators(new MesaPlainsDecorator()))
				.variant(variant(TerrainCategory.PLATEAU).terrain(new TerrainMesa())
						.surfaces(surfaces(new SurfaceCanyon(Blocks.sand, Blocks.sand, (byte) 1, 20)))));
		registry.register(
				builder(hotDesert).climate(Climate.HOT).river(riverOasis).decorators(decorators(new DesertDecorator()))
						.variant(variant(TerrainCategory.PLAIN)
								.terrain(new TerrainHilly(150f, 50f, 0f))
								.surfaces(surfaces(desertMountain(), new SurfaceRiverOasis())))
						.variant(variant(TerrainCategory.HILLS).terrain(new TerrainGenericHills())
								.surfaces(surfaces(desertMountain(), new SurfaceRiverOasis())))
						.variant(variant(TerrainCategory.MOUNTAIN).terrain(new TerrainGenericMountain())
								.surfaces(surfaces(desertMountain(), new SurfaceRiverOasis()))));
		registry.register(builder(hotDesert).climate(Climate.HOT).river(riverOasis)
				.decorators(decorators(new DesertMountainsDecorator()))
				.variant(variant(TerrainCategory.PLAIN)
						.terrain(new TerrainHilly(230f, 120f, 0f))
						.surfaces(surfaces(desertMountain(), new SurfaceRiverOasis())))
				.variant(variant(TerrainCategory.HILLS)
						.terrain(new TerrainGenericHills())
						.surfaces(surfaces(desertMountain(), new SurfaceRiverOasis())))
				.variant(variant(TerrainCategory.MOUNTAIN)
						.terrain(new TerrainGenericMountain())
						.surfaces(surfaces(desertMountain(), new SurfaceRiverOasis()))));
		registry.register(builder(hotDesert).climate(Climate.HOT).river(riverOasis)
				.decorators(decorators(new RedDesertMountainsDecorator()))
				.variant(variant(TerrainCategory.PLAIN)
						.terrain(new TerrainHilly(230f, 120f, 0f))
						.surfaces(surfaces(new SurfaceRedDesert(), new SurfaceRiverOasis())))
				.variant(variant(TerrainCategory.HILLS)
						.terrain(new TerrainGenericHills())
						.surfaces(surfaces(new SurfaceRedDesert(), new SurfaceRiverOasis())))
				.variant(variant(TerrainCategory.MOUNTAIN)
						.terrain(new TerrainGenericMountain())
						.surfaces(surfaces(new SurfaceRedDesert(), new SurfaceRiverOasis()))));
		registry.register(builder(hotDesert).climate(Climate.HOT).river(riverOasis)
				.decorators(decorators(new DuneValleyDecorator()))
				.variant(variant(TerrainCategory.PLAIN).terrain(new TerrainDunes())
						.surfaces(surfaces(desert(), new SurfaceRiverOasis())))
				.variant(variant(TerrainCategory.HILLS).terrain(new TerrainGenericHills())
						.surfaces(surfaces(desert(), new SurfaceRiverOasis())))
				.variant(variant(TerrainCategory.MOUNTAIN).terrain(new TerrainGenericMountain())
						.surfaces(surfaces(desert(), new SurfaceRiverOasis()))));
		registry.register(
				builder(oasis)
						.climate(Climate.HOT).river(riverOasis).decorators(
								decorators(new OasisDecorator()))
						.variant(
								variant(TerrainCategory.PLAIN)
										.terrain(new TerrainHilly(230f, 120f, 20f, 60f, 63f)).surfaces(
												surfaces(
														new SurfaceDesertOasis(Blocks.grass, Blocks.dirt, Blocks.stone,
																Blocks.cobblestone, (byte) 0, 0),
														new SurfaceRiverOasis())))
						.variant(
								variant(TerrainCategory.HILLS)
										.terrain(new TerrainGenericHills()).surfaces(
												surfaces(
														new SurfaceDesertOasis(Blocks.grass, Blocks.dirt, Blocks.stone,
																Blocks.cobblestone, (byte) 0, 0),
														new SurfaceRiverOasis())))
						.variant(
								variant(TerrainCategory.MOUNTAIN)
										.terrain(new TerrainGenericMountain()).surfaces(
												surfaces(
														new SurfaceDesertOasis(Blocks.grass, Blocks.dirt, Blocks.stone,
																Blocks.cobblestone, (byte) 0, 0),
														new SurfaceRiverOasis()))));
		registry.register(
				builder(oasis).climate(Climate.HOT).river(riverOasis).decorators(decorators(new RedOasisDecorator()))
						.variant(variant(TerrainCategory.PLAIN)
								.terrain(new TerrainHilly(230f, 120f, 20f, 60f, 63f))
								.surfaces(surfaces(new SurfaceDesertOasis(Blocks.grass, Blocks.dirt,
										Blocks.stained_hardened_clay, Blocks.stained_hardened_clay, (byte) 1, 1),
										new SurfaceRiverOasis())))
						.variant(variant(TerrainCategory.HILLS)
								.terrain(new TerrainGenericHills())
								.surfaces(surfaces(new SurfaceDesertOasis(Blocks.grass, Blocks.dirt,
										Blocks.stained_hardened_clay, Blocks.stained_hardened_clay, (byte) 1, 1),
										new SurfaceRiverOasis())))
						.variant(variant(TerrainCategory.MOUNTAIN)
								.terrain(new TerrainGenericMountain())
								.surfaces(surfaces(new SurfaceDesertOasis(Blocks.grass, Blocks.dirt,
										Blocks.stained_hardened_clay, Blocks.stained_hardened_clay, (byte) 1, 1),
										new SurfaceRiverOasis()))));
		registry.register(
				builder(hotPlains).climate(Climate.HOT).river(riverOasis).decorators(decorators(new CanyonDecorator()))
						.variant(variant(TerrainCategory.CANYON)
								.terrain(new TerrainCanyon(true, 35f, 160f, 60f, 40f, 69f))
								.surfaces(surfaces(new SurfaceCanyon(Blocks.sand, Blocks.sand, (byte) 1, 0)))));
		registry.register(builder(hotDesert).climate(Climate.HOT).river(riverOasis)
				.decorators(decorators(new MesaDecorator()))
				.variant(variant(TerrainCategory.PLATEAU).terrain(new TerrainMesa()).surfaces(
						surfaces(new SurfaceMesa(Blocks.sand, Blocks.sand, (byte) 1), new SurfaceRiverOasis()))));
		registerCategoryPlaceholders(registry);
		registry.finishBuiltinRegistrations();
	}

	private static SurfaceBase grass(BiomeGenBase biome) {
		return new SurfaceGrassland(biome.topBlock, biome.fillerBlock, Blocks.stone, Blocks.cobblestone);
	}

	private static SurfaceBase desert() {
		return new SurfaceDesert(Blocks.sand, Blocks.sand, Blocks.sandstone, Blocks.stone, Blocks.cobblestone);
	}

	private static SurfaceBase[] surfaces(SurfaceBase... surfaces) {
		return surfaces;
	}

	private static BiomeDecorator[] decorators(BiomeDecorator... decorators) {
		return decorators;
	}

	private static SurfaceBase desertMountain() {
		return new SurfaceDesertMountain(Blocks.sand, Blocks.sandstone, false, null, 0f, 1.5f, 60f, 65f, 1.5f);
	}

	private static SurfaceBase stoneMix(Block top, Block filler, float mixWidth) {
		return new SurfaceMountainStoneMix1(top, filler, false, null, 0f, 1.5f, 60f, 65f, 1.5f, Blocks.stone, mixWidth);
	}

	private static SurfaceBase darkRedwoodSurface() {
		SurfaceMountainStoneMix1 surface = (SurfaceMountainStoneMix1) stoneMix(Blocks.dirt, Blocks.dirt, 0.15f);
		surface.topByte = (byte) 2;
		return surface;
	}

	private static SurfaceBase hotForestSurface() {
		return new SurfaceGrasslandMixBig(Blocks.sand, Blocks.sand, Blocks.grass, Blocks.dirt, Blocks.stone,
				Blocks.cobblestone, 60f, -0.14f, 14f, 0.25f);
	}

	private static void registerHydrology(BiomeRegistry registry, BiomeGenBase riverIce, BiomeGenBase riverCold,
			BiomeGenBase riverTemperate, BiomeGenBase riverHot, BiomeGenBase riverWet, BiomeGenBase riverOasis,
			BiomeGenBase oceanCold, BiomeGenBase oceanHot, BiomeGenBase oceanWet) {
		BiomeGenBase[] rivers = { riverIce, riverCold, riverTemperate, riverHot, riverWet, riverOasis };
		for (Climate climate : Climate.values()) {
			for (BiomeGenBase river : rivers) {
				registry.register(builder(river).climate(climate).river(river).selectable(false)
						.variant(variant(TerrainCategory.RIVER).terrain(new TerrainConstant(61f))
								.surfaces(new SurfaceBase[] { grass(river) })));
			}
		}
		registry.register(builder(oceanCold).climate(Climate.SNOW).river(riverTemperate)
				.variant(variant(TerrainCategory.SHALLOW_OCEAN).terrain(new TerrainConstant(52f))
						.surfaces(new SurfaceBase[] { grass(oceanCold) })));
		registry.register(builder(oceanCold).climate(Climate.SNOW).river(riverTemperate)
				.variant(variant(TerrainCategory.DEEP_OCEAN).terrain(new TerrainConstant(34f))
						.surfaces(new SurfaceBase[] { grass(oceanCold) })));
		registry.register(builder(oceanCold).climate(Climate.COLD).river(riverTemperate)
				.variant(variant(TerrainCategory.SHALLOW_OCEAN).terrain(new TerrainConstant(52f))
						.surfaces(new SurfaceBase[] { grass(oceanCold) })));
		registry.register(builder(BiomeGenBase.deepOcean).climate(Climate.COLD).river(riverTemperate)
				.variant(variant(TerrainCategory.DEEP_OCEAN).terrain(new TerrainConstant(34f))
						.surfaces(new SurfaceBase[] { grass(BiomeGenBase.deepOcean) })));
		registry.register(builder(oceanHot).climate(Climate.HOT).river(riverTemperate)
				.variant(variant(TerrainCategory.SHALLOW_OCEAN).terrain(new TerrainConstant(52f))
						.surfaces(new SurfaceBase[] { grass(oceanHot) })));
		registry.register(
				builder(oceanHot).climate(Climate.HOT).river(riverTemperate).variant(variant(TerrainCategory.DEEP_OCEAN)
						.terrain(new TerrainConstant(34f)).surfaces(new SurfaceBase[] { grass(oceanHot) })));
		registry.register(builder(oceanWet).climate(Climate.WET).river(riverTemperate)
				.variant(variant(TerrainCategory.SHALLOW_OCEAN).terrain(new TerrainConstant(52f))
						.surfaces(new SurfaceBase[] { grass(oceanWet) })));
		registry.register(
				builder(oceanWet).climate(Climate.WET).river(riverTemperate).variant(variant(TerrainCategory.DEEP_OCEAN)
						.terrain(new TerrainConstant(34f)).surfaces(new SurfaceBase[] { grass(oceanWet) })));
	}

	private static void registerCategoryPlaceholders(BiomeRegistry registry) {
		TerrainBase[] terrains = { new TerrainGenericHills(), new TerrainGenericMountain(), new TerrainGenericPlateau(),
				new TerrainGenericCanyon(), new TerrainGenericCliff() };
		TerrainCategory[] categories = { TerrainCategory.HILLS, TerrainCategory.MOUNTAIN, TerrainCategory.PLATEAU,
				TerrainCategory.CANYON, TerrainCategory.CLIFF };
		for (Climate climate : Climate.values()) {
			for (int index = 0; index < categories.length; index++) {
				if (!registry.entries(climate, categories[index], TerrainSubcategory.CORE).isEmpty()) continue;
				registry.register(builder(BiomeGenBase.plains).climate(climate)
						.decorators(new NativeBiomeDecorator(BiomeGenBase.plains)).variant(variant(categories[index])
								.terrain(terrains[index]).surfaces(new SurfaceNativeBiome(BiomeGenBase.plains))));
			}
		}
	}
}
