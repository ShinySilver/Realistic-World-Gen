package rwg.world.debug;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import rwg.RWG;
import rwg.registry.BiomeRegistration;
import rwg.world.ChunkManager;
import rwg.world.layout.WorldgenSelector;

/** Reports the atlas cell under each player at most once per second. */
public final class GridBiomeReporter {

    private final Map<EntityPlayer, Cell> cells = new WeakHashMap<EntityPlayer, Cell>();

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        if (event.phase != TickEvent.Phase.END || player.worldObj.isRemote || player.ticksExisted % 20 != 0) return;
        if (player.worldObj.getWorldInfo().getTerrainType() != RWG.gridWorldtype) {
            cells.remove(player);
            return;
        }
        int blockX = (int) Math.floor(player.posX);
        int blockZ = (int) Math.floor(player.posZ);
        ChunkManager manager = (ChunkManager) player.worldObj.getWorldChunkManager();
        WorldgenSelector selector = manager.worldgenSelector();
        GridSaplingGallery.Entry sapling = selector instanceof GridWorldgenSelector
                ? ((GridWorldgenSelector) selector).saplingAtChunk(Math.floorDiv(blockX, 16), Math.floorDiv(blockZ, 16))
                : null;
        String region = selector instanceof GridWorldgenSelector
                ? ((GridWorldgenSelector) selector).regionAt(blockX, blockZ)
                : "RWG grid";
        String cellKey = selector instanceof GridWorldgenSelector
                ? ((GridWorldgenSelector) selector).reportCellKey(blockX, blockZ)
                : "grid";
        Cell previous = cells.get(player);
        if (previous != null && previous.key.equals(cellKey) && previous.region.equals(region)) return;
        cells.put(player, new Cell(cellKey, region));
        if (previous == null || !previous.region.equals(region)) {
            String transition = previous == null ? "RWG grid: entered " + region
                    : "RWG grid: left " + previous.region + "; entered " + region;
            player.addChatMessage(new ChatComponentText(transition));
        }
        if ("Tree gallery".equals(region)) {
            if (sapling != null) player.addChatMessage(new ChatComponentText("RWG tree gallery: " + sapling.name));
            else player.addChatMessage(new ChatComponentText("RWG tree gallery: empty cell"));
            return;
        }
        if (sapling != null) {
            player.addChatMessage(new ChatComponentText("RWG tree gallery: " + sapling.name));
            return;
        }
        BiomeRegistration registration = manager.gridRegistrationAt(blockX, blockZ);
        if (registration == null) {
            player.addChatMessage(new ChatComponentText("RWG grid: empty cell"));
            return;
        }
        player.addChatMessage(
                new ChatComponentText(registration.biome.biomeName + " (biome " + registration.biome.biomeID + ")"));
        for (int id = 0; id < selector.registrationCount(); id++) {
            BiomeRegistration entry = selector.registration(id);
            if (entry.biome != registration.biome) continue;
            player.addChatMessage(
                    new ChatComponentText(
                            "  RWG #" + entry.id
                                    + ": "
                                    + entry.climate
                                    + " / "
                                    + entry.category
                                    + " / "
                                    + entry.subcategory
                                    + " / "
                                    + entry.terrain.getClass().getSimpleName()));
        }
    }

    private static final class Cell {

        final String key;
        final String region;

        Cell(String key, String region) {
            this.key = key;
            this.region = region;
        }
    }
}
