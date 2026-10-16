package rwg;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import rwg.biomes.villages.VillageMaterials;
import rwg.registry.BiomeRegistry;
import rwg.registry.BiomeRegistryBootstrap;
import rwg.support.RailcraftWorldgenFilter;
import rwg.world.WorldType;
import rwg.world.profile.WorldgenProfiler;

@Mod(
        modid = "RWG",
        name = "RealisticWorldGen",
        version = Tags.VERSION,
        dependencies = "required-after:endlessids;required-after:cubicchunks",
        acceptableRemoteVersions = "*")
public class RWG {

    @Instance("RWG")
    public static RWG instance;

    public static final WorldType worldtype = new WorldType("RWG2");
    private static BiomeRegistry biomeRegistry;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        instance = this;

        ConfigRWG.init(event);
        biomeRegistry = BiomeRegistryBootstrap.create();

        MinecraftForge.TERRAIN_GEN_BUS.register(new VillageMaterials());
        MinecraftForge.TERRAIN_GEN_BUS.register(new RailcraftWorldgenFilter());
    }

    @EventHandler
    public void Init(FMLInitializationEvent event) {}

    @EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        if (!Boolean.getBoolean("rwg.profileWorldgen")) return;
        WorldgenProfiler.profilePacedAndStop(MinecraftServer.getServer());
    }

    public static BiomeRegistry biomeRegistry() {
        if (biomeRegistry == null) throw new IllegalStateException("RWG biome registry has not been initialized");
        return biomeRegistry;
    }

}
