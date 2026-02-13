package org.minimap.Services;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.minimap.MinimapPlugin;
import org.minimap.worldmapData.ChunkInfo;
import org.minimap.worldmapData.IconShape;
import org.minimap.Services.SampleCodec;
import org.minimap.worldmapData.Waypoint;
import org.minimap.worldmapData.WaypointIcon;
import org.minimap.worldmapData.WorldmapState;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Handles persistence of WorldmapState objects.
 *
 * One file per group:
 *   plugins/MiniMap/worldmaps/group_<id>.yml
 */
public class WorldmapPersistenceService {

    private final MinimapPlugin plugin;
    private final WorldmapStateService stateService;
    private final File baseDir;

    public WorldmapPersistenceService(
            MinimapPlugin plugin,
            WorldmapStateService stateService
    ) {
        this.plugin = plugin;
        this.stateService = stateService;
        this.baseDir = new File(plugin.getDataFolder(), "worldmaps");

        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
    }

    /* ---------------------------------------------------------------------
     * Public API
     * ------------------------------------------------------------------ */

    /**
     * Loads all persisted worldmap groups from disk.
     * Should be called during plugin onEnable().
     */
    public void loadAll() {

        File[] files = baseDir.listFiles((dir, name) -> name.startsWith("group_") && name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            loadGroupFile(file);
        }

        plugin.getLogger().info("Loaded " + files.length + " worldmap groups.");
    }

    /**
     * Saves all currently known worldmap groups to disk.
     * Should be called during plugin onDisable().
     */
    public void saveAll() {

        for (Map.Entry<Integer, WorldmapState> entry : stateService.getAll().entrySet()) {
            saveGroup(entry.getKey(), entry.getValue());
        }

        plugin.getLogger().info("Saved " + stateService.getAll().size() + " worldmap groups.");
    }

    /**
     * Saves a single worldmap group immediately.
     */
    public void saveGroup(int groupId) {
        WorldmapState state = stateService.get(groupId);
        saveGroup(groupId, state);
    }

    /**
     * Deletes a worldmap group from disk.
     * (Optional future use.)
     */
    public void deleteGroup(int groupId) {
        File file = new File(baseDir, "group_" + groupId + ".yml");
        if (file.exists()) {
            file.delete();
        }
    }

    /* ---------------------------------------------------------------------
     * Internal load/save logic
     * ------------------------------------------------------------------ */

    private void loadGroupFile(File file) {

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        int groupId = cfg.getInt("groupId");
        WorldmapState state = stateService.get(groupId);

        // Origin
        if (cfg.contains("origin")) {
            int ox = cfg.getInt("origin.x");
            int oz = cfg.getInt("origin.z");
            state.setOriginIfAbsent(ox, oz);
        }

        int gridSize = cfg.getInt("gridSize", 2);
        if (gridSize < 1 || gridSize > 6) {
            plugin.getLogger().warning("[MiniMap] Invalid gridSize " + gridSize + " in " + file.getName() + ", defaulting to 2");
            gridSize = 2;
        }
        state.setGridSize(gridSize);

        int pixelsPerChunk = cfg.getInt("pixelsPerChunk", -1);
        if (pixelsPerChunk <= 0) {
            Object zoomRaw = cfg.get("zoom");
            if (zoomRaw instanceof String zoomName) {
                switch (zoomName) {
                    case "WORLD" -> pixelsPerChunk = 1;
                    case "REGIONAL" -> pixelsPerChunk = 8;
                    case "LOCAL" -> pixelsPerChunk = WorldmapState.MAX_PIXELS_PER_CHUNK;
                    default -> pixelsPerChunk = 8;
                }
            } else if (zoomRaw instanceof Integer zoomInt) {
                pixelsPerChunk = zoomInt;
            } else {
                pixelsPerChunk = 8;
            }
        }
        state.setPixelsPerChunk(pixelsPerChunk);

        // Discovered chunks
        if (cfg.contains("discovered")) {
            for (String key : cfg.getConfigurationSection("discovered").getKeys(false)) {

                String[] parts = key.split(",");
                int cx = Integer.parseInt(parts[0]);
                int cz = Integer.parseInt(parts[1]);

                int topY = cfg.getInt("discovered." + key + ".topY");
                Material material = Material.valueOf(cfg.getString("discovered." + key + ".material"));
                var biome = org.bukkit.block.Biome.valueOf(cfg.getString("discovered." + key + ".biome"));
                String samplesRaw = cfg.getString("discovered." + key + ".samples");
                int[] samples = SampleCodec.decode(samplesRaw);

                long packedKey = WorldmapStateService.chunkKey(cx, cz);
                state.discovered().put(
                        packedKey,
                        new ChunkInfo(cx, cz, topY, material, biome, samples)
                );
            }
        }

        if (cfg.contains("waypoints")) {
            for (String label : cfg.getConfigurationSection("waypoints").getKeys(false)) {
                String base = "waypoints." + label;
                int x = cfg.getInt(base + ".x");
                int y = cfg.getInt(base + ".y");
                int z = cfg.getInt(base + ".z");
                String shapeRaw = cfg.getString(base + ".icon.shape", IconShape.DOT.name());
                String colorRaw = cfg.getString(base + ".icon.color", Byte.toString((byte) 0));

                IconShape shape;
                try {
                    shape = IconShape.valueOf(shapeRaw);
                } catch (IllegalArgumentException e) {
                    shape = IconShape.DOT;
                }

                byte color;
                try {
                    color = Byte.parseByte(colorRaw);
                } catch (NumberFormatException e) {
                    color = 0;
                }

                state.addWaypoint(new Waypoint(x, y, z, label, new WaypointIcon(shape, color)));
            }
        }

        String focusedWaypoint = cfg.getString("focusedWaypoint");
        if (focusedWaypoint != null && !focusedWaypoint.isBlank()) {
            state.setFocusedWaypointLabel(focusedWaypoint);
        }
    }

    public void saveGroup(int groupId, WorldmapState state) {

        File file = new File(baseDir, "group_" + groupId + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();

        cfg.set("groupId", groupId);

        // Origin
        if (state.hasOrigin()) {
            cfg.set("origin.x", state.originX());
            cfg.set("origin.z", state.originZ());
        } else {
            plugin.getLogger().warning("[MiniMap] Skipping origin save for group " + groupId + " because origin is unset");
        }

        // Zoom
        cfg.set("pixelsPerChunk", state.pixelsPerChunk());
        cfg.set("gridSize", state.gridSize());

        // Discovered chunks
        for (ChunkInfo info : state.discovered().values()) {
            String key = info.chunkX + "," + info.chunkZ;
            cfg.set("discovered." + key + ".topY", info.topY);
            cfg.set("discovered." + key + ".material", info.topMaterial.name());
            cfg.set("discovered." + key + ".biome", info.biome.name());
            String samples = SampleCodec.encode(info.sampleRgb());
            if (samples != null) {
                cfg.set("discovered." + key + ".samples", samples);
            }
        }

        for (Waypoint waypoint : state.waypoints().values()) {
            String base = "waypoints." + waypoint.label;
            cfg.set(base + ".x", waypoint.x);
            cfg.set(base + ".y", waypoint.y);
            cfg.set(base + ".z", waypoint.z);
            cfg.set(base + ".icon.shape", waypoint.icon.shape.name());
            cfg.set(base + ".icon.color", Byte.toString(waypoint.icon.color));
        }

        cfg.set("focusedWaypoint", state.focusedWaypointLabel());

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save worldmap group " + groupId);
            e.printStackTrace();
        }
    }
}


