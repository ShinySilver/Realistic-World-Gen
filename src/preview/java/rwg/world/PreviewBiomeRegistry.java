package rwg.world;

import rwg.registry.BiomeRegistry;
import rwg.registry.BuiltinBiomes;

/** Creates the same built-in-only registry used by the WIP cubic generator. */
final class PreviewBiomeRegistry {

    private PreviewBiomeRegistry() {}

    static BiomeRegistry create() {
        BiomeRegistry registry = new BiomeRegistry();
        BuiltinBiomes.register(registry);
        registry.freeze();
        return registry;
    }
}
