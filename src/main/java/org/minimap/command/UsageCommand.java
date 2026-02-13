package org.minimap.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UsageCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("\u00A7cOnly players can use this command.");
            return true;
        }

        player.sendMessage("MiniMap Commands:");
        player.sendMessage("/minimap group <gridSize> - Create a shared worldmap group (1-6).");
        player.sendMessage("/minimap give_personal - Get a new personal minimap.");
        player.sendMessage("/minimap discover - Discover the chunk you're standing in.");
        player.sendMessage("/minimap zoom +|-|<pixelsPerChunk> - Adjust zoom (1-50).");
        player.sendMessage("/minimap import_chunks - Import your discovered chunks into the worldmap.");
        player.sendMessage("/minimap give_waypoints - Get a waypoint draft book.");
        player.sendMessage("/minimap import_waypoints - Import waypoints from the draft book.");
        player.sendMessage("/minimap focus <label|clear> - Set or clear focused waypoint.");
        player.sendMessage("/minimap redraw - Force all maps to redraw (admin).");
        return true;
    }
}
