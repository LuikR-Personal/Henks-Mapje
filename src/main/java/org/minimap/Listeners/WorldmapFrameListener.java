package org.minimap.Listeners;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataType;
import org.minimap.MinimapPlugin;
import org.minimap.Services.WorldmapRenderer;

import static org.bukkit.Bukkit.getServer;

public class WorldmapFrameListener implements Listener {

    private final MinimapPlugin plugin;

    public WorldmapFrameListener(MinimapPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {

        Chunk chunk = event.getChunk();

        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof ItemFrame frame) {
                plugin.tryAttachRenderer(frame.getItem());
            }
        }
    }


}
