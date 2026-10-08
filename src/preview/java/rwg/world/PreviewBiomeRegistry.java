package rwg.world;

import java.io.File;

import net.minecraft.creativetab.CreativeTabs;

import biomesoplenty.BiomesOPlenty;
import biomesoplenty.common.configuration.BOPConfiguration;
import biomesoplenty.common.core.BOPBiomes;
import biomesoplenty.common.helpers.CreativeTabsBOP;
import rwg.registry.BiomeRegistry;
import rwg.registry.BuiltinBiomes;
import rwg.support.BiomesOPlentyBiomeRegistrations;

/** Initializes the parts of BOP needed by the standalone preview, without starting Forge. */
final class PreviewBiomeRegistry {

    private PreviewBiomeRegistry() {}

    static BiomeRegistry create() {
        initializeBiomesOPlenty();
        BiomeRegistry registry = new BiomeRegistry();
        BuiltinBiomes.register(registry);
        BiomesOPlentyBiomeRegistrations.register(registry);
        registry.freeze();
        return registry;
    }

    private static void initializeBiomesOPlenty() {
        File directory = new File("run/preview-bop-config");
        BOPConfiguration.init(directory.getPath() + File.separator);
        BiomesOPlenty.tabBiomesOPlenty = new CreativeTabsBOP(CreativeTabs.getNextID(), "tabBiomesOPlenty");
        BOPBiomes.init();
    }
}
