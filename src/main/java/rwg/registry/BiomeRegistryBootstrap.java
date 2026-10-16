package rwg.registry;

/** Assembles the immutable biome registry during mod post-initialization. */
public final class BiomeRegistryBootstrap {

    private BiomeRegistryBootstrap() {}

    public static BiomeRegistry create() {
        BiomeRegistry registry = new BiomeRegistry();
        BuiltinBiomes.register(registry);
        registry.freeze();
        return registry;
    }
}
