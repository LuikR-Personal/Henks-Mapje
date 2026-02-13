package org.minimap;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.minimap.Listeners.PlayerDiscoveryListener;
import org.minimap.Listeners.PlayerInventoryMapListener;
import org.minimap.Listeners.WorldmapFrameListener;
import org.minimap.Services.*;
import org.minimap.command.MinimapCommand;
import org.minimap.command.UsageCommand;

import java.util.Objects;

/**
 * Plugin entry point.
 *
 * Responsibilities:
 * - Initialize core services
 * - Wire persistence lifecycle
 * - Register commands and listeners
 *
 * Contains no domain logic.
 */
public class MinimapPlugin extends JavaPlugin {

    private NamespacedKey minimapKey;
    private NamespacedKey worldmapGroupKey;
    private NamespacedKey worldmapSlotKey;
    private NamespacedKey waypointBookKey;
    private NamespacedKey mapTypeKey;
    private NamespacedKey mapLegendKey;

    private WorldmapGroupIdService worldmapGroupIdService;
    private WorldmapStateService worldmapStateService;
    private WorldmapPersistenceService worldmapPersistenceService;
    public PlayerDiscoveryStateService playerDiscoveryStateService;
    private PlayerDiscoveryPersistenceService playerDiscoveryPersistenceService;


    @Override
    public void onEnable() {

        saveDefaultConfig();
        getLogger().info("MiniMap plugin enabled");

        // Namespaced keys (persistent identity)
        minimapKey = new NamespacedKey(this, "minimap");
        worldmapGroupKey = new NamespacedKey(this, "worldmap_group");
        worldmapSlotKey = new NamespacedKey(this, "worldmap_slot");
        waypointBookKey = new NamespacedKey(this, "waypoints");
        mapTypeKey = new NamespacedKey(this, "map_type");
        mapLegendKey = new NamespacedKey(this, "legend_key");

        // Core services
        worldmapStateService = new WorldmapStateService();
        worldmapGroupIdService = new WorldmapGroupIdService(this);

        worldmapPersistenceService =
                new WorldmapPersistenceService(this, worldmapStateService);
        this.playerDiscoveryStateService = new PlayerDiscoveryStateService();
        this.playerDiscoveryPersistenceService =
                new PlayerDiscoveryPersistenceService(this, playerDiscoveryStateService);

        playerDiscoveryPersistenceService.loadAll();

        getServer().getPluginManager().registerEvents(
                new PlayerDiscoveryListener(playerDiscoveryStateService),
                this
        );
        getServer().getPluginManager().registerEvents(
                new WorldmapFrameListener(this),
                this
        );
        getServer().getPluginManager().registerEvents(
                new PlayerInventoryMapListener(this),
                this
        );
        scanLoadedItemFrames();

        getLogger().info("[MiniMap] Player discovery system enabled");
        // Load persisted worldmap state
        worldmapPersistenceService.loadAll();

        // Commands
        Objects.requireNonNull(getCommand("minimap"))
                .setExecutor(new MinimapCommand(this));
        Objects.requireNonNull(getCommand("usage"))
                .setExecutor(new UsageCommand());

        // Listeners (renderer reattachment, etc.)
        // getServer().getPluginManager().registerEvents(
        //     new MapInitListener(this),
        //     this
        // );

        getLogger().info("MiniMap fully initialized.");
    }

    @Override
    public void onDisable() {

        if (worldmapPersistenceService != null) {
            worldmapPersistenceService.saveAll();
        }
        if (playerDiscoveryStateService != null) {
            int players = playerDiscoveryStateService.getAll().size();
            int totalChunks = playerDiscoveryStateService.getAll().stream()
                    .mapToInt(s -> s.discovered().size())
                    .sum();

            getLogger().info("[MiniMap] Player discovery states: " + players);
            getLogger().info("[MiniMap] Total discovered chunks: " + totalChunks);
        }
        if (playerDiscoveryPersistenceService != null) {
            playerDiscoveryPersistenceService.saveAll();
        }
        getLogger().info("MiniMap disabled successfully.");
    }

    /* ------------------------------------------------------------------
     * Getters
     * ------------------------------------------------------------------ */

    public NamespacedKey getMinimapKey() {
        return minimapKey;
    }
    public NamespacedKey getMapTypeKey() { return mapTypeKey;}
    public NamespacedKey getWaypointBookKey() {
        return waypointBookKey;
    }
    public NamespacedKey getMapGroupIdKey() {
        return worldmapGroupKey;
    }
    public NamespacedKey getMapSlotKey() {
        return worldmapSlotKey;
    }
    public NamespacedKey getMapLegendKey() {return mapLegendKey;}
    public WorldmapGroupIdService getWorldmapGroupId() {
        return worldmapGroupIdService;
    }

    public WorldmapStateService getWorldMapStateService() {
        return worldmapStateService;
    }

    public WorldmapPersistenceService getWorldmapPersistenceService() {
        return worldmapPersistenceService;
    }
    public WorldmapStateService getWorldmapStateService() {
        return worldmapStateService;
    }
    public PlayerDiscoveryStateService getPlayerDiscoveryStateService() {
        return playerDiscoveryStateService;
    }

    /**
     * Scans all currently loaded chunks for item frames
     * and reattaches WorldmapRenderers where applicable.
     *
     * This runs ONCE at startup.
     */
    private void scanLoadedItemFrames() {

        for (World world : getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (Entity entity : chunk.getEntities()) {
                    if (entity instanceof ItemFrame frame) {
                        tryAttachRenderer(frame.getItem());
                    }
                }
            }
        }
    }
    public void redrawAllMaps() {
        Bukkit.getWorlds().forEach(world ->
                world.getEntitiesByClass(ItemFrame.class).forEach(frame -> {
                    ItemStack item = frame.getItem();
                    if (item == null || item.getType() != Material.FILLED_MAP) return;

                    tryAttachRenderer(item);
                })
        );
    }

    /**
     * Attaches a WorldmapRenderer to a map item if it belongs
     * to this plugin.
     */
    public void tryAttachRenderer(ItemStack item) {

        if (item == null || item.getType() != Material.FILLED_MAP) return;
        if (!(item.getItemMeta() instanceof MapMeta meta)) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        Integer groupId = pdc.get(worldmapGroupKey, PersistentDataType.INTEGER);
        if (groupId == null) return;

        MapView view = meta.getMapView();
        if (view == null) return;

        // Clear existing renderers
        view.getRenderers().clear();

        // ─────────────────────────────
        // WORLD MAP (terrain quadrant)
        // ─────────────────────────────
        Integer slot = pdc.get(worldmapSlotKey, PersistentDataType.INTEGER);
        if (slot != null) {
            view.addRenderer(
                    new WorldmapRenderer(groupId, slot, worldmapStateService)
            );
            return;
        }

        Byte personal = pdc.get(minimapKey, PersistentDataType.BYTE);
        if (personal != null && personal == (byte) 1) {
            view.addRenderer(
                    new PersonalMinimapRenderer(
                            groupId,
                            worldmapStateService,
                            playerDiscoveryStateService
                    )
            );
            return;
        }

        // ─────────────────────────────
        // LEGEND MAP (text legend)
        // ─────────────────────────────
        Integer legendPart = pdc.get(mapLegendKey, PersistentDataType.INTEGER);
        if (legendPart != null) {
            view.addRenderer(
                    new WaypointLegendRenderer(
                            groupId,
                            worldmapStateService,
                            legendPart
                    )
            );
        }
    }

}

