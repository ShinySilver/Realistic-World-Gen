package rwg.registry;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.world.biome.BiomeGenBase;

import rwg.biomes.decorators.BiomeDecorator;
import rwg.biomes.surface.SurfaceBase;
import rwg.biomes.terrain.TerrainBase;

public final class BiomeRegistration {

    public final int id;
    public final BiomeGenBase biome;
    public final BiomeGenBase riverBiome;
    public final TerrainBase terrain;
    public final SurfaceBase[] surfaces;
    public final BiomeDecorator[] decorators;
    public final Climate climate;
    public final TerrainCategory category;
    public final TerrainSubcategory subcategory;
    public final int weight;
    public final boolean selectable;
    public final boolean builtin;

    public BiomeRegistration(int id, BiomeGenBase biome, BiomeGenBase riverBiome, TerrainBase terrain,
            SurfaceBase[] surfaces, BiomeDecorator[] decorators, Climate climate, TerrainCategory category,
            TerrainSubcategory subcategory, int weight, boolean selectable, boolean builtin) {
        if (biome == null || riverBiome == null
                || terrain == null
                || surfaces == null
                || climate == null
                || category == null
                || subcategory == null)
            throw new NullPointerException("biome registration values");
        if (weight < 1) throw new IllegalArgumentException("weight must be positive");
        this.id = id;
        this.biome = biome;
        this.riverBiome = riverBiome;
        this.terrain = terrain;
        this.surfaces = surfaces.clone();
        this.decorators = decorators == null ? new BiomeDecorator[0] : decorators.clone();
        this.climate = climate;
        this.category = category;
        this.subcategory = subcategory;
        this.weight = weight;
        this.selectable = selectable;
        this.builtin = builtin;
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
        BiomeDecorator[] decorators;
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

        public Builder decorators(BiomeDecorator... decorators) {
            this.decorators = decorators == null ? null : decorators.clone();
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
        SurfaceBase[] surfaces;
        BiomeDecorator[] decorators;
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

        public Variant surfaces(SurfaceBase... surfaces) {
            this.surfaces = surfaces == null ? null : surfaces.clone();
            return this;
        }

        public Variant decorators(BiomeDecorator... decorators) {
            this.decorators = decorators == null ? null : decorators.clone();
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
