package rwg.registry;

import cpw.mods.fml.common.Loader;
import rwg.support.BiomesOPlentyBiomeRegistrations;
import rwg.support.ExtrabiomesBiomeRegistrations;
import rwg.support.ThaumcraftBiomeRegistrations;

/** Assembles the immutable biome registry during mod post-initialization. */
public final class BiomeRegistryBootstrap {

    private BiomeRegistryBootstrap() {}

    public static BiomeRegistry createBuiltins() {
        BiomeRegistry registry = new BiomeRegistry();
        BuiltinBiomes.register(registry);
        return registry;
    }

    public static BiomeRegistry create(boolean loadIntegrations) {
        BiomeRegistry registry = createBuiltins();
        finish(registry, loadIntegrations);
        return registry;
    }

    public static void finish(BiomeRegistry registry, boolean loadIntegrations) {
        if (loadIntegrations) {
            if (Loader.isModLoaded("BiomesOPlenty")) BiomesOPlentyBiomeRegistrations.register(registry);
            if (Loader.isModLoaded("ExtrabiomesXL")) ExtrabiomesBiomeRegistrations.register(registry);
            if (Loader.isModLoaded("Thaumcraft")) ThaumcraftBiomeRegistrations.register(registry);
        }
        registry.freeze();
    }
}
