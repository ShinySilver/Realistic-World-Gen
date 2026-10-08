package rwg.biomes.villages;

import net.minecraft.init.Blocks;
import net.minecraft.world.biome.BiomeGenBase;

public enum VillageMaterialPreset {

    ACACIA,
    SANDSTONE;

    VillageMaterialData create(BiomeGenBase biome) {
        VillageMaterialData data = new VillageMaterialData(biome);
        if (this == ACACIA) {
            data.plankBlock = Blocks.planks;
            data.plankBlockMeta = 4;
            data.logBlock = Blocks.log2;
            data.logBlockMeta = 0;
            data.pathBlock = Blocks.cobblestone;
            data.stairsWoodBlock = Blocks.acacia_stairs;
            data.slabsBlock = Blocks.fence;
        } else {
            data.plankBlock = Blocks.sandstone;
            data.logBlock = Blocks.sandstone;
            data.pathBlock = Blocks.sandstone;
            data.stairsWoodBlock = Blocks.sandstone_stairs;
            data.slabsBlock = Blocks.fence;
            data.cobbleBlock = Blocks.sandstone;
        }
        return data;
    }
}
