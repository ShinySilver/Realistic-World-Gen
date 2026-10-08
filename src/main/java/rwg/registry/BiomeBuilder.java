package rwg.registry;

import net.minecraft.entity.EntityLiving;
import net.minecraft.world.ColorizerFoliage;
import net.minecraft.world.ColorizerGrass;
import net.minecraft.world.biome.BiomeGenBase;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import rwg.biomes.villages.VillageMaterialPreset;
import rwg.biomes.villages.VillageMaterials;

/** Compact builder for RWG-owned Minecraft biome identities. */
final class BiomeBuilder {

    private final ConfiguredBiome biome;
    private VillageMaterialPreset villagePreset;

    BiomeBuilder(int id, String name) {
        biome = new ConfiguredBiome(id);
        biome.setBiomeName(name);
    }

    BiomeBuilder climate(float temperature, float rainfall) {
        biome.setTemperatureRainfall(temperature, rainfall);
        return this;
    }

    BiomeBuilder noRain() {
        biome.setDisableRain();
        return this;
    }

    BiomeBuilder noCreatures() {
        biome.clearCreatures();
        return this;
    }

    BiomeBuilder creature(Class<? extends EntityLiving> entity, int weight, int minimum, int maximum) {
        biome.addCreature(entity, weight, minimum, maximum);
        return this;
    }

    BiomeBuilder monster(Class<? extends EntityLiving> entity, int weight, int minimum, int maximum) {
        biome.addMonster(entity, weight, minimum, maximum);
        return this;
    }

    BiomeBuilder colors(int grass, int foliage) {
        biome.grassColor = grass;
        biome.foliageColor = foliage;
        return this;
    }

    BiomeBuilder climateColors(float grassTemperature, float grassRainfall, float foliageTemperature,
            float foliageRainfall) {
        biome.grassTemperature = grassTemperature;
        biome.grassRainfall = grassRainfall;
        biome.foliageTemperature = foliageTemperature;
        biome.foliageRainfall = foliageRainfall;
        return this;
    }

    BiomeBuilder waterColor(int color) {
        biome.waterColorMultiplier = color;
        return this;
    }

    BiomeBuilder village(VillageMaterialPreset preset) {
        villagePreset = preset;
        return this;
    }

    BiomeGenBase build() {
        if (villagePreset != null) VillageMaterials.register(biome, villagePreset);
        return biome;
    }

    private static final class ConfiguredBiome extends BiomeGenBase {

        private Integer grassColor;
        private Integer foliageColor;
        private Float grassTemperature;
        private Float grassRainfall;
        private Float foliageTemperature;
        private Float foliageRainfall;

        private ConfiguredBiome(int id) {
            super(id);
        }

        private void clearCreatures() {
            spawnableCreatureList.clear();
        }

        private void addCreature(Class<? extends EntityLiving> entity, int weight, int minimum, int maximum) {
            spawnableCreatureList.add(new BiomeGenBase.SpawnListEntry(entity, weight, minimum, maximum));
        }

        private void addMonster(Class<? extends EntityLiving> entity, int weight, int minimum, int maximum) {
            spawnableMonsterList.add(new BiomeGenBase.SpawnListEntry(entity, weight, minimum, maximum));
        }

        @Override
        @SideOnly(Side.CLIENT)
        public int getBiomeGrassColor(int x, int y, int z) {
            if (grassColor != null) return grassColor;
            return grassTemperature == null ? super.getBiomeGrassColor(x, y, z)
                    : ColorizerGrass.getGrassColor(grassTemperature, grassRainfall);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public int getBiomeFoliageColor(int x, int y, int z) {
            if (foliageColor != null) return foliageColor;
            return foliageTemperature == null ? super.getBiomeFoliageColor(x, y, z)
                    : ColorizerFoliage.getFoliageColor(foliageTemperature, foliageRainfall);
        }
    }
}
