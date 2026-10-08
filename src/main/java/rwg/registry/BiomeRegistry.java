package rwg.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.world.biome.BiomeGenBase;

import rwg.biomes.decorators.BiomeDecorator;
import rwg.biomes.decorators.NativeBiomeDecorator;

/** Immutable-after-freeze biome metadata and selection pools. */
public final class BiomeRegistry {

    private final List<BiomeRegistration> registrations = new ArrayList<BiomeRegistration>();
    private final List<BiomeGenBase> ownedBiomes = new ArrayList<BiomeGenBase>();
    private final BiomeGenBase[] climateRivers = new BiomeGenBase[Climate.values().length];
    private BiomeGenBase temperateRiver;
    private BiomeGenBase oasisRiver;
    private BiomeGenBase hotDesert;
    private BiomeGenBase plains;
    private boolean registeringBuiltins = true;
    private boolean frozen;

    /**
     * Registers a complete biome variant. Integrations retain the legacy {@code RealisticBiomeSupport} behavior by
     * receiving a native-biome decorator before their compatibility edits; built-ins declare their decorators
     * explicitly because their legacy implementations did not invoke {@link BiomeGenBase#decorate}.
     */
    public void register(BiomeRegistration.Builder builder) {
        if (frozen) throw new IllegalStateException("biome registry is frozen");
        if (builder == null) throw new NullPointerException("biome registration builder");
        if (builder.climate == null) throw new IllegalArgumentException("biome climate is required");
        if (builder.variants.isEmpty()) throw new IllegalArgumentException("at least one biome variant is required");
        for (BiomeRegistration.Variant variant : builder.variants) {
            if (variant.terrain == null) throw new IllegalArgumentException("biome variant terrain is required");
            if (variant.surfaces == null || variant.surfaces.length == 0)
                throw new IllegalArgumentException("at least one biome variant surface is required");
            for (TerrainCategory category : variant.categories) register(category, builder, variant);
        }
    }

    private void register(TerrainCategory category, BiomeRegistration.Builder builder,
            BiomeRegistration.Variant variant) {
        BiomeGenBase riverBiome = builder.riverBiome == null ? river(builder.climate) : builder.riverBiome;
        BiomeDecorator[] registeredDecorators = variant.decorators == null ? builder.decorators : variant.decorators;
        if (!registeringBuiltins && category != TerrainCategory.SHALLOW_OCEAN
                && category != TerrainCategory.DEEP_OCEAN) {
            int editCount = registeredDecorators == null ? 0 : registeredDecorators.length;
            registeredDecorators = new BiomeDecorator[editCount + 1];
            registeredDecorators[0] = new NativeBiomeDecorator(builder.biome);
            if (editCount != 0) {
                BiomeDecorator[] configured = variant.decorators == null ? builder.decorators : variant.decorators;
                System.arraycopy(configured, 0, registeredDecorators, 1, editCount);
            }
        }
        TerrainSubcategory subcategory = variant.subcategory == null ? builder.subcategory : variant.subcategory;
        int weight = variant.weight == null ? builder.weight : variant.weight;
        boolean selectable = variant.selectable == null ? builder.selectable : variant.selectable;
        registrations.add(
                new BiomeRegistration(
                        registrations.size(),
                        builder.biome,
                        riverBiome,
                        variant.terrain,
                        variant.surfaces,
                        registeredDecorators,
                        builder.climate,
                        category,
                        subcategory,
                        weight,
                        selectable,
                        registeringBuiltins));
    }

    <T extends BiomeGenBase> T registerOwned(T biome) {
        if (frozen) throw new IllegalStateException("biome registry is frozen");
        if (biome == null) throw new NullPointerException("owned biome");
        ownedBiomes.add(biome);
        return biome;
    }

    <T extends BiomeGenBase> T registerRiver(Climate climate, T biome) {
        if (climateRivers[climate.ordinal()] != null) throw new IllegalStateException("Duplicate river for " + climate);
        climateRivers[climate.ordinal()] = registerOwned(biome);
        return biome;
    }

    void setTemperateRiver(BiomeGenBase biome) {
        temperateRiver = biome;
    }

    void setOasisRiver(BiomeGenBase biome) {
        oasisRiver = biome;
    }

    void setHotDesert(BiomeGenBase biome) {
        hotDesert = biome;
    }

    void setPlains(BiomeGenBase biome) {
        plains = biome;
    }

    public BiomeGenBase river(Climate climate) {
        return climateRivers[climate.ordinal()];
    }

    public BiomeGenBase temperateRiver() {
        return temperateRiver;
    }

    public BiomeGenBase oasisRiver() {
        return oasisRiver;
    }

    public BiomeGenBase hotDesert() {
        return hotDesert;
    }

    public BiomeGenBase plains() {
        return plains;
    }

    void finishBuiltinRegistrations() {
        registeringBuiltins = false;
    }

    public List<BiomeRegistration> entries(Climate climate, TerrainCategory category, TerrainSubcategory subcategory) {
        ArrayList<BiomeRegistration> result = new ArrayList<BiomeRegistration>();
        for (BiomeRegistration registration : registrations) {
            if (registration.selectable && registration.climate == climate
                    && registration.category == category
                    && registration.subcategory == subcategory) {
                for (int copy = 0; copy < registration.weight; copy++) result.add(registration);
            }
        }
        return result;
    }

    /** Categories available to the future category-first sampler, in enum order. */
    public List<TerrainCategory> categories(Climate climate, TerrainSubcategory subcategory) {
        EnumSet<TerrainCategory> found = EnumSet.noneOf(TerrainCategory.class);
        for (BiomeRegistration registration : registrations) {
            if (registration.selectable && registration.climate == climate && registration.subcategory == subcategory) {
                found.add(registration.category);
            }
        }
        return Collections.unmodifiableList(new ArrayList<TerrainCategory>(found));
    }

    public List<BiomeRegistration> registrations() {
        return Collections.unmodifiableList(registrations);
    }

    public BiomeRegistration registration(int id) {
        if (!frozen) throw new IllegalStateException("biome registry is not frozen");
        return registrations.get(id);
    }

    public BiomeRegistration registrationOf(BiomeGenBase biome, Climate climate, TerrainCategory category,
            TerrainSubcategory subcategory) {
        if (!frozen) throw new IllegalStateException("biome registry is not frozen");
        for (BiomeRegistration registration : registrations) {
            if (registration.biome == biome && registration.climate == climate
                    && registration.category == category
                    && registration.subcategory == subcategory)
                return registration;
        }
        throw new IllegalArgumentException(
                "Unregistered biome variant: biome=" + biome
                        + ", climate="
                        + climate
                        + ", category="
                        + category
                        + ", subcategory="
                        + subcategory);
    }

    public void freeze() {
        validate();
        frozen = true;
    }

    private void validate() {
        BiomeGenBase[] minecraftBiomes = BiomeGenBase.getBiomeGenArray();
        if (ownedBiomes.size() != 26 || temperateRiver == null
                || oasisRiver == null
                || hotDesert == null
                || plains == null)
            throw new IllegalStateException("Missing RWG-owned biomes");
        for (BiomeGenBase river : climateRivers)
            if (river == null) throw new IllegalStateException("Missing climate river");
        for (BiomeGenBase biome : ownedBiomes) {
            BiomeGenBase registered = biome.biomeID >= 0 && biome.biomeID < minecraftBiomes.length
                    ? minecraftBiomes[biome.biomeID]
                    : null;
            if (registered != biome) {
                String actualName = registered == null ? "nothing" : '"' + registered.biomeName + '"';
                throw new IllegalStateException(
                        "RWG biome ID collision at " + biome.biomeID
                                + ": expected \""
                                + biome.biomeName
                                + "\", but the registry contains "
                                + actualName
                                + ". Assign unique IDs in config/RWG.cfg before loading a world.");
            }
        }
        for (BiomeRegistration registration : registrations) {
            if (registration.biome == null || registration.riverBiome == null
                    || registration.terrain == null
                    || registration.surfaces.length == 0
                    || registration.climate == null
                    || registration.category == null
                    || registration.subcategory == null
                    || registration.weight < 1)
                throw new IllegalStateException("Incomplete biome registration at id " + registration.id);
        }
    }
}
