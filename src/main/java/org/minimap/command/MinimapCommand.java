package org.minimap.command;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.minimap.MinimapPlugin;
import org.minimap.Services.ChunkSampling;
import org.minimap.Services.MapRenderTuning;
import org.minimap.Services.WaypointLegendRenderer;
import org.minimap.Services.WorldmapRenderer;
import org.minimap.personalData.PlayerDiscoveryState;
import org.minimap.worldmapData.ChunkInfo;
import org.minimap.worldmapData.IconShape;
import org.minimap.worldmapData.Waypoint;
import org.minimap.worldmapData.WaypointIcon;
import org.minimap.worldmapData.WorldmapState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class MinimapCommand implements CommandExecutor {

    private static final int MAX_GRID_SIZE = 6;
    private static final Pattern SAFE_LABEL_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,10}$");

    private final MinimapPlugin plugin;

    private record ParsedWaypoint(int x, int y, int z, String label) {}

    public MinimapCommand(MinimapPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("cOnly players can use this command.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "group" -> handleGroupCommand(player, args);
            case "discover" -> discoverCurrentChunk(player);
            case "zoom" -> handleZoomCommand(player, args);
            case "import_chunks" -> importChunks(player);
            case "give_waypoints" -> giveWaypointBook(player);
            case "import_waypoints" -> importWaypoints(player);
            case "give_personal" -> givePersonalMinimap(player);
            case "focus" -> handleFocusCommand(player, args);
            case "spacing" -> {
                if (args.length < 2) {
                    player.sendMessage("Usage: /minimap spacing <4-12>");
                    return true;
                }
                int px;
                try {
                    px = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage("spacing must be a number.");
                    return true;
                }
                MapRenderTuning.LEGEND_LINE_HEIGHT = Math.max(4, Math.min(12, px));
            }
            case "legend_iconscale" -> {
                if (args.length < 2) {
                    player.sendMessage("Usage: /minimap legend_iconscale <1-4>");
                    return true;
                }
                int scale;
                try {
                    scale = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage("legend_iconscale must be a number.");
                    return true;
                }
                MapRenderTuning.LEGEND_ICON_SCALE = Math.max(1, Math.min(4, scale));
            }
            case "world_iconscale" -> {
                if (args.length < 2) {
                    player.sendMessage("Usage: /minimap world_iconscale <1-4>");
                    return true;
                }
                int scale;
                try {
                    scale = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage("world_iconscale must be a number.");
                    return true;
                }
                MapRenderTuning.WORLD_ICON_SCALE = Math.max(1, Math.min(4, scale));
            }
            case "redraw" -> {
                if (!player.hasPermission("minimap.admin.redraw")) {
                    player.sendMessage("You do not have permission to use this command.");
                    return true;
                }
                plugin.redrawAllMaps();
            }
            default -> {
                player.sendMessage("Unknown subcommand.");
                sendUsage(player);
            }
        }

        return true;
    }

    private void handleGroupCommand(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage("Usage: /minimap group <gridSize>");
            return;
        }

        int gridSize;
        try {
            gridSize = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("gridSize must be a number (1-" + MAX_GRID_SIZE + ").");
            return;
        }

        if (gridSize < 1 || gridSize > MAX_GRID_SIZE) {
            player.sendMessage("gridSize must be between 1 and " + MAX_GRID_SIZE + ".");
            return;
        }

        giveGroupedMaps(player, gridSize);
    }

    private void giveGroupedMaps(Player player, int gridSize) {
        int groupId = plugin.getWorldmapGroupId().getNextGroupId();
        WorldmapState state = plugin.getWorldMapStateService().get(groupId);
        state.setGridSize(gridSize);

        int cx = player.getLocation().getChunk().getX();
        int cz = player.getLocation().getChunk().getZ();
        state.setOriginIfAbsent(cx, cz);

        int total = gridSize * gridSize;

        for (int slot = 1; slot <= total; slot++) {
            ItemStack map = new ItemStack(Material.FILLED_MAP);
            MapMeta meta = (MapMeta) map.getItemMeta();

            if (meta != null) {
                MapView view = Bukkit.createMap(player.getWorld());
                view.getRenderers().clear();
                view.addRenderer(new WorldmapRenderer(groupId, slot, plugin.getWorldMapStateService()));

                meta.setMapView(view);
                meta.setDisplayName("aWorld Map 7[" + slot + "/" + total + "]");

                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                pdc.set(plugin.getMapGroupIdKey(), PersistentDataType.INTEGER, groupId);
                pdc.set(plugin.getMapSlotKey(), PersistentDataType.INTEGER, slot);
                pdc.set(plugin.getMapTypeKey(), PersistentDataType.STRING, "WORLD");

                map.setItemMeta(meta);
            }

            giveOrDrop(player, map);
        }

        for (int i = 0; i < 2; i++) {
            ItemStack map = new ItemStack(Material.FILLED_MAP);
            MapMeta meta = (MapMeta) map.getItemMeta();

            if (meta != null) {
                MapView view = Bukkit.createMap(player.getWorld());
                view.getRenderers().clear();
                view.addRenderer(new WaypointLegendRenderer(groupId, plugin.getWorldMapStateService(), i));

                meta.setMapView(view);
                meta.setDisplayName("eWaypoint Legend");

                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                pdc.set(plugin.getMapGroupIdKey(), PersistentDataType.INTEGER, groupId);
                pdc.set(plugin.getMapTypeKey(), PersistentDataType.STRING, "LEGEND");
                pdc.set(plugin.getMapLegendKey(), PersistentDataType.INTEGER, i);
                map.setItemMeta(meta);
            }

            giveOrDrop(player, map);
        }

        player.sendMessage("aYou received World Map Group " + groupId + " (" + gridSize + "x" + gridSize + ") with legend.");
        plugin.getLogger().info("[MiniMap] Created group " + groupId + " gridSize=" + gridSize + " totalMaps=" + total);
    }

    private void givePersonalMinimap(Player player) {
        int groupId = plugin.getWorldmapGroupId().getNextGroupId();
        WorldmapState state = plugin.getWorldMapStateService().get(groupId);
        state.setGridSize(1);
        int cx = player.getLocation().getChunk().getX();
        int cz = player.getLocation().getChunk().getZ();
        state.setOriginIfAbsent(cx, cz);

        ItemStack map = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) map.getItemMeta();
        if (meta == null) {
            return;
        }

        MapView view = Bukkit.createMap(player.getWorld());
        meta.setMapView(view);
        meta.setDisplayName("bPersonal Minimap");

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(plugin.getMapGroupIdKey(), PersistentDataType.INTEGER, groupId);
        pdc.set(plugin.getMinimapKey(), PersistentDataType.BYTE, (byte) 1);
        pdc.set(plugin.getMapTypeKey(), PersistentDataType.STRING, "PERSONAL");

        map.setItemMeta(meta);
        plugin.tryAttachRenderer(map);
        giveOrDrop(player, map);
        plugin.getWorldmapPersistenceService().saveGroup(groupId, state);
        player.sendMessage("aYou received a personal minimap for group " + groupId + ".");
    }

    private void discoverCurrentChunk(Player player) {
        Integer groupId = getLookedAtMapGroupId(player);
        if (groupId == null) {
            player.sendMessage("Look at a shared world map in an item frame.");
            return;
        }

        int cx = player.getLocation().getChunk().getX();
        int cz = player.getLocation().getChunk().getZ();

        int worldX = player.getLocation().getBlockX();
        int worldZ = player.getLocation().getBlockZ();
        int topY = player.getWorld().getHighestBlockYAt(worldX, worldZ);

        Material topMat = player.getWorld().getBlockAt(worldX, topY - 1, worldZ).getType();
        var biome = player.getWorld().getBiome(worldX, topY, worldZ);

        WorldmapState state = plugin.getWorldMapStateService().get(groupId);
        state.setOriginIfAbsent(cx, cz);

        int[] samples = ChunkSampling.sampleChunkRgb(player.getWorld(), cx, cz);
        long key = org.minimap.Services.WorldmapStateService.chunkKey(cx, cz);
        state.discovered().put(key, new ChunkInfo(cx, cz, topY, topMat, biome, samples));
    }

    private Integer getLookedAtMapGroupId(Player player) {
        RayTraceResult result = player.rayTraceEntities(5);
        if (result == null) {
            return null;
        }

        Entity entity = result.getHitEntity();
        if (!(entity instanceof ItemFrame frame)) {
            return null;
        }

        ItemStack item = frame.getItem();
        if (item == null || item.getType() != Material.FILLED_MAP) {
            return null;
        }

        if (!(item.getItemMeta() instanceof MapMeta meta)) {
            return null;
        }

        return meta.getPersistentDataContainer().get(plugin.getMapGroupIdKey(), PersistentDataType.INTEGER);
    }

    private Integer getLookedAtWorldmapGroupId(Player player) {
        RayTraceResult result = player.rayTraceEntities(5);
        if (result == null) {
            return null;
        }

        Entity entity = result.getHitEntity();
        if (!(entity instanceof ItemFrame frame)) {
            return null;
        }

        ItemStack item = frame.getItem();
        if (item == null || item.getType() != Material.FILLED_MAP) {
            return null;
        }

        if (!(item.getItemMeta() instanceof MapMeta meta)) {
            return null;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer groupId = pdc.get(plugin.getMapGroupIdKey(), PersistentDataType.INTEGER);
        if (groupId == null) {
            return null;
        }

        Byte personal = pdc.get(plugin.getMinimapKey(), PersistentDataType.BYTE);
        if (personal != null && personal == (byte) 1) {
            return null;
        }

        Integer slot = pdc.get(plugin.getMapSlotKey(), PersistentDataType.INTEGER);
        Integer legendPart = pdc.get(plugin.getMapLegendKey(), PersistentDataType.INTEGER);
        if (slot != null || legendPart != null) {
            return groupId;
        }

        return null;
    }

    private Integer getHeldMapGroupId(Player player) {
        ItemStack inHand = player.getInventory().getItemInMainHand();

        if (inHand == null || inHand.getType() != Material.FILLED_MAP) {
            return null;
        }

        if (!(inHand.getItemMeta() instanceof MapMeta meta)) {
            return null;
        }

        return meta.getPersistentDataContainer().get(plugin.getMapGroupIdKey(), PersistentDataType.INTEGER);
    }

    private Integer getActiveMapGroupId(Player player) {
        Integer looked = getLookedAtMapGroupId(player);
        if (looked != null) {
            return looked;
        }
        return getHeldMapGroupId(player);
    }

    private void handleZoomCommand(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage("Usage: /minimap zoom +|-|<pixelsPerChunk>");
            return;
        }

        Integer groupId = getActiveMapGroupId(player);
        if (groupId == null) {
            player.sendMessage("Look at a shared world map in an item frame or hold a map.");
            return;
        }

        WorldmapState state = plugin.getWorldMapStateService().get(groupId);

        if (args[1].equals("+")) {
            state.zoomIn();
        } else if (args[1].equals("-")) {
            state.zoomOut();
        } else {
            int value;
            try {
                value = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage("Invalid zoom value. Use a number between " +
                        WorldmapState.MIN_PIXELS_PER_CHUNK + " and " + WorldmapState.MAX_PIXELS_PER_CHUNK + ".");
                return;
            }
            state.setPixelsPerChunk(value);
        }

        player.sendMessage("aZoom set to f" + state.pixelsPerChunk() + " px/chunk");
    }

    private void importChunks(Player player) {
        Integer groupId = getLookedAtMapGroupId(player);
        if (groupId == null) {
            player.sendMessage("Look at a shared world map in an item frame.");
            return;
        }

        WorldmapState worldmapState = plugin.getWorldmapStateService().get(groupId);
        PlayerDiscoveryState playerState = plugin.getPlayerDiscoveryStateService().get(player.getUniqueId());

        int imported = 0;

        for (Map.Entry<Long, ChunkInfo> entry : playerState.discovered().entrySet()) {
            long chunkKey = entry.getKey();
            if (worldmapState.discovered().containsKey(chunkKey)) {
                continue;
            }
            worldmapState.discovered().put(chunkKey, entry.getValue());
            imported++;
        }

        if (imported > 0) {
            plugin.getWorldmapPersistenceService().saveGroup(groupId, worldmapState);
        }

        player.sendMessage("aImported " + imported + " chunks into worldmap group " + groupId);
    }

    private void giveWaypointBook(Player player) {
        ItemStack book = new ItemStack(Material.WRITABLE_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) {
            return;
        }

        meta.setDisplayName("Waypoint Draft");
        meta.setLore(List.of(
                "Write waypoints as:",
                "x,y,z[label]",
                "One per line"
        ));

        meta.getPersistentDataContainer().set(plugin.getWaypointBookKey(), PersistentDataType.BYTE, (byte) 1);

        book.setItemMeta(meta);
        giveOrDrop(player, book);
        player.sendMessage("aYou received a waypoint draft book.");
    }

    private void importWaypoints(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != Material.WRITABLE_BOOK) {
            player.sendMessage("Hold your waypoint book in your main hand.");
            return;
        }

        if (!(item.getItemMeta() instanceof BookMeta meta)) {
            player.sendMessage("Invalid book.");
            return;
        }

        if (!meta.getPersistentDataContainer().has(plugin.getWaypointBookKey(), PersistentDataType.BYTE)) {
            player.sendMessage("This is not a waypoint book.");
            return;
        }

        List<ParsedWaypoint> parsedWaypoints = new ArrayList<>();
        int errors = 0;

        List<String> pages = meta.getPages();
        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
            String page = pages.get(pageIndex);
            String[] lines = page.split("\n");

            for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
                String line = lines[lineIndex].trim();
                if (line.isEmpty()) {
                    continue;
                }

                try {
                    ParsedWaypoint pw = parseWaypoint(line);
                    parsedWaypoints.add(pw);
                } catch (IllegalArgumentException ex) {
                    errors++;
                    player.sendMessage(String.format("Error p%d l%d: %s", pageIndex + 1, lineIndex + 1, ex.getMessage()));
                }
            }
        }

        Integer lookedWorldmapGroupId = getLookedAtWorldmapGroupId(player);
        Set<Integer> targetGroupIds = new LinkedHashSet<>();
        if (lookedWorldmapGroupId != null) {
            targetGroupIds.add(lookedWorldmapGroupId);
        } else {
            targetGroupIds.addAll(getPersonalMapGroupIdsInInventory(player));
        }

        if (targetGroupIds.isEmpty()) {
            player.sendMessage("Look at a worldmap or keep a personal minimap in your inventory.");
            return;
        }

        int updatedGroups = 0;
        for (int groupId : targetGroupIds) {
            WorldmapState state = plugin.getWorldMapStateService().get(groupId);
            state.waypoints().clear();

            List<WaypointIcon> iconPool = buildIconPool();
            int iconIndex = 0;
            for (ParsedWaypoint pw : parsedWaypoints) {
                WaypointIcon icon = iconPool.get(iconIndex % iconPool.size());
                Waypoint waypoint = new Waypoint(pw.x(), pw.y(), pw.z(), pw.label(), icon);
                state.addWaypoint(waypoint);
                iconIndex++;
            }

            if (state.focusedWaypointLabel() != null && !state.waypoints().containsKey(state.focusedWaypointLabel())) {
                state.setFocusedWaypointLabel(null);
            }

            plugin.getWorldmapPersistenceService().saveGroup(groupId, state);
            updatedGroups++;
        }

        if (lookedWorldmapGroupId != null) {
            player.sendMessage(
                    "Imported " + parsedWaypoints.size() + " waypoints into looked-at worldmap group " +
                            lookedWorldmapGroupId + "."
            );
        } else {
            player.sendMessage(
                    "Imported " + parsedWaypoints.size() + " waypoints into " +
                            updatedGroups + " personal minimap group(s)."
            );
        }
        if (errors > 0) {
            player.sendMessage("Skipped " + errors + " invalid lines.");
        }
    }

    private void handleFocusCommand(Player player, String[] args) {
        Integer groupId = getActiveMapGroupId(player);
        if (groupId == null) {
            player.sendMessage("Look at a map in a frame or hold one.");
            return;
        }

        WorldmapState state = plugin.getWorldMapStateService().get(groupId);

        if (args.length < 2) {
            player.sendMessage("Usage: /minimap focus <label|clear>");
            return;
        }

        if (args[1].equalsIgnoreCase("clear")) {
            state.setFocusedWaypointLabel(null);
            plugin.getWorldmapPersistenceService().saveGroup(groupId, state);
            player.sendMessage("aWaypoint focus cleared.");
            return;
        }

        String focusLabel = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)).trim();
        if (!SAFE_LABEL_PATTERN.matcher(focusLabel).matches()) {
            player.sendMessage("Waypoint label must match [A-Za-z0-9_-] and be 1-10 chars.");
            return;
        }
        if (!state.waypoints().containsKey(focusLabel)) {
            player.sendMessage("Waypoint not found: " + focusLabel);
            return;
        }

        state.setFocusedWaypointLabel(focusLabel);
        plugin.getWorldmapPersistenceService().saveGroup(groupId, state);
        player.sendMessage("Focused waypoint set to f" + focusLabel + ".");
    }

    private ParsedWaypoint parseWaypoint(String line) {
        int open = line.indexOf('[');
        int close = line.indexOf(']');

        if (open < 0 || close < open) {
            throw new IllegalArgumentException("Missing [label]");
        }

        String coordPart = line.substring(0, open);
        String label = line.substring(open + 1, close).trim();

        if (!SAFE_LABEL_PATTERN.matcher(label).matches()) {
            throw new IllegalArgumentException("Label must match [A-Za-z0-9_-] and be 1-10 chars");
        }

        String[] coords = coordPart.split(",");
        if (coords.length != 3) {
            throw new IllegalArgumentException("Expected x,y,z");
        }

        int x;
        int y;
        int z;
        try {
            x = Integer.parseInt(coords[0].trim());
            y = Integer.parseInt(coords[1].trim());
            z = Integer.parseInt(coords[2].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number");
        }

        return new ParsedWaypoint(x, y, z, label);
    }

    private List<WaypointIcon> buildIconPool() {
        List<WaypointIcon> pool = new ArrayList<>();

        byte[] colors = {
                MapPalette.RED,
                MapPalette.BLUE,
                MapPalette.DARK_GREEN,
                MapPalette.BROWN,
        };

        for (IconShape shape : IconShape.values()) {
            for (byte color : colors) {
                pool.add(new WaypointIcon(shape, color));
            }
        }

        Collections.shuffle(pool);
        return pool;
    }

    private Set<Integer> getPersonalMapGroupIdsInInventory(Player player) {
        Set<Integer> groupIds = new LinkedHashSet<>();
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.getType() != Material.FILLED_MAP) {
                continue;
            }
            if (!(stack.getItemMeta() instanceof MapMeta meta)) {
                continue;
            }

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            Byte personal = pdc.get(plugin.getMinimapKey(), PersistentDataType.BYTE);
            if (personal == null || personal != (byte) 1) {
                continue;
            }

            Integer groupId = pdc.get(plugin.getMapGroupIdKey(), PersistentDataType.INTEGER);
            if (groupId != null) {
                groupIds.add(groupId);
            }
        }
        return groupIds;
    }

    private void giveOrDrop(Player player, ItemStack item) {
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        if (overflow.isEmpty()) {
            return;
        }

        for (ItemStack stack : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), stack);
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage("eMiniMap Commands:");
        player.sendMessage("7/minimap group <gridSize> - Create a shared worldmap group (1-6).");
        player.sendMessage("7/minimap give_personal - Get a new personal minimap.");
        player.sendMessage("7/minimap discover - Discover the chunk you're standing in.");
        player.sendMessage("7/minimap zoom +|-|<pixelsPerChunk> - Adjust zoom (1-50).");
        player.sendMessage("7/minimap import_chunks - Import your discovered chunks into the worldmap.");
        player.sendMessage("7/minimap give_waypoints - Get a waypoint draft book.");
        player.sendMessage("7/minimap import_waypoints - Import waypoints from the draft book.");
        player.sendMessage("7/minimap focus <label|clear> - Set or clear focused waypoint.");
        player.sendMessage("7/minimap redraw - Force all worldmaps to redraw (admin).");
    }
}
