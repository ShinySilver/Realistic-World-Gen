package rwg;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import rwg.biomes.villages.VillageMaterials;
import rwg.registry.BiomeRegistry;
import rwg.registry.BiomeRegistryBootstrap;
import rwg.support.RailcraftWorldgenFilter;
import rwg.world.WorldType;
import rwg.world.debug.GridBiomeReporter;

@Mod(
        modid = "RWG",
        name = "RealisticWorldGen",
        version = Tags.VERSION,
        dependencies = "required-after:endlessids",
        acceptableRemoteVersions = "*")
public class RWG {

    @Instance("RWG")
    public static RWG instance;

    public static final WorldType worldtype = new WorldType("RWG", false);
    public static final WorldType continentWorldtype = new WorldType("RWG_CONTINENT", true);
    public static final WorldType gridWorldtype = new WorldType("RWG_GRID", false, true);
    private static BiomeRegistry biomeRegistry;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        instance = this;

        ConfigRWG.init(event);
        biomeRegistry = BiomeRegistryBootstrap.createBuiltins();

        MinecraftForge.TERRAIN_GEN_BUS.register(new VillageMaterials());
        MinecraftForge.TERRAIN_GEN_BUS.register(new RailcraftWorldgenFilter());

        // MinecraftForge.TERRAIN_GEN_BUS.register(new rwg.world.TreeReplacement());
    }

    @EventHandler
    public void Init(FMLInitializationEvent event) {}

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        BiomeRegistryBootstrap.finish(biomeRegistry, true);
        FMLCommonHandler.instance().bus().register(new GridBiomeReporter());
    }

    public static BiomeRegistry biomeRegistry() {
        if (biomeRegistry == null) throw new IllegalStateException("RWG biome registry has not been initialized");
        return biomeRegistry;
    }

}
