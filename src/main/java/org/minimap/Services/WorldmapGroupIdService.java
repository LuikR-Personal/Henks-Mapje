package org.minimap.Services;
import org.minimap.MinimapPlugin;

public class WorldmapGroupIdService {

    private final MinimapPlugin plugin;
    public WorldmapGroupIdService(MinimapPlugin plugin) {
        this.plugin = plugin;
    }

    public int getNextGroupId() {

        int id = plugin.getConfig().getInt("next-group-id", 1);
        plugin.getConfig().set("next-group-id", id + 1);
        plugin.saveConfig();
        return id;
    }

}
