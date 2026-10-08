package rwg.world.sample;

import rwg.registry.BiomeRegistration;

/** Biome selected from registrations valid for the preceding climate and category. */
public final class BiomeSample {

    public final BiomeRegistration registration;
    public final float selector;

    public BiomeSample(BiomeRegistration registration, float selector) {
        if (registration == null) throw new NullPointerException("biome registration");
        this.registration = registration;
        this.selector = selector;
    }
}
