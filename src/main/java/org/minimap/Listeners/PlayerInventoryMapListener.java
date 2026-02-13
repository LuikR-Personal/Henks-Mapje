package org.minimap.Listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.minimap.MinimapPlugin;

public class PlayerInventoryMapListener implements Listener {

    private final MinimapPlugin plugin;

    public PlayerInventoryMapListener(MinimapPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != Material.FILLED_MAP) {
                continue;
            }
            plugin.tryAttachRenderer(item);
        }
    }
}
