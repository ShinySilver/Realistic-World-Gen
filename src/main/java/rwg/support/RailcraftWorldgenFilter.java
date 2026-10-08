package rwg.support;

import net.minecraftforge.event.terraingen.PopulateChunkEvent;

import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import rwg.world.ChunkManager;

/** Prevents Railcraft abyssal-stone geodes from modifying RWG ocean floors. */
public class RailcraftWorldgenFilter {

    @SubscribeEvent
    public void filterPopulation(PopulateChunkEvent.Populate event) {
        if (!"RAILCRAFT_GEODE".equals(event.type.name())) return;
        if (!(event.world.getWorldChunkManager() instanceof ChunkManager)) return;

        ChunkManager manager = (ChunkManager) event.world.getWorldChunkManager();
        if (manager.isOceanBiomeAt(event.chunkX * 16 + 8, event.chunkZ * 16 + 8)) {
            event.setResult(Result.DENY);
        }
    }
}
