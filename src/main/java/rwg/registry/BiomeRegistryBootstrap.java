package rwg.registry;

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
        registry.freeze();
    }
}
