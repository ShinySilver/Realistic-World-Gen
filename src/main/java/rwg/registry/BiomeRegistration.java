package rwg.registry;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.world.biome.BiomeGenBase;

import rwg.biomes.decorators.CubicFeature;
import rwg.biomes.surface.SurfaceRule;
import rwg.biomes.surface.SurfaceRuleSequence;
import rwg.biomes.terrain.TerrainBase;

public final class BiomeRegistration {

    public final int id;
    public final BiomeGenBase biome;
    public final BiomeGenBase riverBiome;
    public final TerrainBase terrain;
    public final SurfaceRule[] surfaceRules;
    public final SurfaceRule surfaceRule;
    public final CubicFeature[] features;
    public final Climate climate;
    public final TerrainCategory category;
    public final TerrainSubcategory subcategory;
    public final int weight;
    public final boolean selectable;

    public BiomeRegistration(int id, BiomeGenBase biome, BiomeGenBase riverBiome, TerrainBase terrain,
            SurfaceRule[] surfaceRules, CubicFeature[] features, Climate climate, TerrainCategory category,
            TerrainSubcategory subcategory, int weight, boolean selectable) {
        if (biome == null || riverBiome == null
                || terrain == null
                || surfaceRules == null
                || climate == null
                || category == null
                || subcategory == null)
            throw new NullPointerException("biome registration values");
        if (weight < 1) throw new IllegalArgumentException("weight must be positive");
        this.id = id;
        this.biome = biome;
        this.riverBiome = riverBiome;
        this.terrain = terrain;
        this.surfaceRules = surfaceRules.clone();
        this.surfaceRule = new SurfaceRuleSequence(this.surfaceRules);
        this.features = features == null ? new CubicFeature[0] : features.clone();
        this.climate = climate;
        this.category = category;
        this.subcategory = subcategory;
        this.weight = weight;
        this.selectable = selectable;
    }

    public static Builder builder(BiomeGenBase biome) {
        return new Builder(biome);
    }

    public static Variant variant(TerrainCategory category, TerrainCategory... additionalCategories) {
        if (category == null) throw new NullPointerException("terrain category");
        EnumSet<TerrainCategory> categories = EnumSet.of(category);
        if (additionalCategories != null) for (TerrainCategory additional : additionalCategories) {
            if (additional == null) throw new NullPointerException("terrain category");
            categories.add(additional);
        }
        return new Variant(categories);
    }

    public static Variant variant(EnumSet<TerrainCategory> categories) {
        return new Variant(categories);
    }

    /** Named registration options keep call sites readable as new optional metadata is added. */
    public static final class Builder {

        final BiomeGenBase biome;
        final List<Variant> variants = new ArrayList<Variant>();
        BiomeGenBase riverBiome;
        CubicFeature[] features;
        Climate climate;
        TerrainSubcategory subcategory = TerrainSubcategory.CORE;
        int weight = 1;
        boolean selectable = true;

        private Builder(BiomeGenBase biome) {
            if (biome == null) throw new NullPointerException("biome");
            this.biome = biome;
        }

        public Builder river(BiomeGenBase riverBiome) {
            this.riverBiome = riverBiome;
            return this;
        }

        public Builder climate(Climate climate) {
            this.climate = climate;
            return this;
        }

        public Builder subcategory(TerrainSubcategory subcategory) {
            this.subcategory = subcategory;
            return this;
        }

        public Builder features(CubicFeature... features) {
            this.features = features == null ? null : features.clone();
            return this;
        }

        public Builder weight(int weight) {
            this.weight = weight;
            return this;
        }

        public Builder selectable(boolean selectable) {
            this.selectable = selectable;
            return this;
        }

        public Builder variant(Variant variant) {
            if (variant == null) throw new NullPointerException("biome variant");
            variants.add(variant);
            return this;
        }
    }

    /** World-generation behavior for one or more terrain categories of the enclosing biome identity. */
    public static final class Variant {

        final EnumSet<TerrainCategory> categories;
        TerrainBase terrain;
        SurfaceRule[] surfaceRules;
        CubicFeature[] features;
        TerrainSubcategory subcategory;
        Integer weight;
        Boolean selectable;

        private Variant(EnumSet<TerrainCategory> categories) {
            if (categories == null || categories.isEmpty())
                throw new IllegalArgumentException("at least one terrain category is required");
            this.categories = categories.clone();
        }

        public Variant terrain(TerrainBase terrain) {
            this.terrain = terrain;
            return this;
        }

        public Variant surfaces(SurfaceRule... surfaceRules) {
            this.surfaceRules = surfaceRules == null ? null : surfaceRules.clone();
            return this;
        }

        public Variant features(CubicFeature... features) {
            this.features = features == null ? null : features.clone();
            return this;
        }

        public Variant subcategory(TerrainSubcategory subcategory) {
            this.subcategory = subcategory;
            return this;
        }

        public Variant weight(int weight) {
            this.weight = weight;
            return this;
        }

        public Variant selectable(boolean selectable) {
            this.selectable = selectable;
            return this;
        }
    }
}
