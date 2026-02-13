package org.minimap.Services;

import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.entity.Player;
import org.minimap.personalData.PlayerDiscoveryState;
import org.minimap.worldmapData.ChunkInfo;
import org.minimap.worldmapData.Waypoint;
import org.minimap.worldmapData.WorldmapState;

import java.awt.Color;

public class PersonalMinimapRenderer extends MapRenderer {

    private static final int MAP_SIZE = 128;
    private static final int CENTER = 64;
    private static final int MIN_PERSONAL_PIXELS = 2;
    private static final int MAX_PERSONAL_PIXELS = 16;
    private static final byte BG = MapPalette.matchColor(18, 18, 18);
    private static final byte ARROW_COLOR = MapPalette.matchColor(250, 250, 250);

    private final int groupId;
    private final WorldmapStateService worldmapStateService;
    private final PlayerDiscoveryStateService playerDiscoveryStateService;

    public PersonalMinimapRenderer(
            int groupId,
            WorldmapStateService worldmapStateService,
            PlayerDiscoveryStateService playerDiscoveryStateService
    ) {
        super(false);
        this.groupId = groupId;
        this.worldmapStateService = worldmapStateService;
        this.playerDiscoveryStateService = playerDiscoveryStateService;
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if (player == null) {
            return;
        }

        WorldmapState state = worldmapStateService.get(groupId);
        PlayerDiscoveryState discoveryState = playerDiscoveryStateService.get(player.getUniqueId());
        int pixelsPerChunk = clampPersonalZoom(state.pixelsPerChunk());
        int playerChunkX = player.getLocation().getChunk().getX();
        int playerChunkZ = player.getLocation().getChunk().getZ();
        int playerBlockX = player.getLocation().getBlockX();
        int playerBlockZ = player.getLocation().getBlockZ();
        String focusedLabel = state.focusedWaypointLabel();

        fill(canvas, BG);

        for (ChunkInfo info : discoveryState.discovered().values()) {
            int dx = info.chunkX - playerChunkX;
            int dz = info.chunkZ - playerChunkZ;

            int x = CENTER + dx * pixelsPerChunk;
            int y = CENTER + dz * pixelsPerChunk;

            if (x >= MAP_SIZE || y >= MAP_SIZE || x + pixelsPerChunk <= 0 || y + pixelsPerChunk <= 0) {
                continue;
            }

            byte color = MapPalette.matchColor(WorldmapRenderer.baseColorFor(info.topMaterial));
            fillRect(canvas, x, y, pixelsPerChunk, pixelsPerChunk, color);
        }

        for (Waypoint waypoint : state.waypoints().values()) {
            double px = CENTER + ((waypoint.x - playerBlockX) * (pixelsPerChunk / 16.0));
            double py = CENTER + ((waypoint.z - playerBlockZ) * (pixelsPerChunk / 16.0));
            int wx = (int) Math.round(px);
            int wy = (int) Math.round(py);

            if (isInside(wx, wy)) {
                WaypointIconRenderer.drawIcon(canvas, wx, wy, waypoint.icon, MapRenderTuning.WORLD_ICON_SCALE);
                continue;
            }

            if (focusedLabel != null && focusedLabel.equals(waypoint.label)) {
                drawBorderArrow(canvas, px - CENTER, py - CENTER);
            }
        }

        fillRect(canvas, CENTER - 1, CENTER - 1, 3, 3, MapPalette.matchColor(Color.WHITE));
    }

    private int clampPersonalZoom(int value) {
        if (value < MIN_PERSONAL_PIXELS) {
            return MIN_PERSONAL_PIXELS;
        }
        if (value > MAX_PERSONAL_PIXELS) {
            return MAX_PERSONAL_PIXELS;
        }
        return value;
    }

    private void fill(MapCanvas canvas, byte color) {
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                canvas.setPixel(x, y, color);
            }
        }
    }

    private void fillRect(MapCanvas canvas, int startX, int startY, int width, int height, byte color) {
        int x0 = Math.max(0, startX);
        int y0 = Math.max(0, startY);
        int x1 = Math.min(MAP_SIZE, startX + width);
        int y1 = Math.min(MAP_SIZE, startY + height);

        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                canvas.setPixel(x, y, color);
            }
        }
    }

    private boolean isInside(int x, int y) {
        return x >= 0 && x < MAP_SIZE && y >= 0 && y < MAP_SIZE;
    }

    private void drawBorderArrow(MapCanvas canvas, double dx, double dy) {
        if (dx == 0.0 && dy == 0.0) {
            return;
        }

        double sx = dx == 0.0 ? Double.POSITIVE_INFINITY : (63.0 / Math.abs(dx));
        double sy = dy == 0.0 ? Double.POSITIVE_INFINITY : (63.0 / Math.abs(dy));
        double s = Math.min(sx, sy);
        int tipX = (int) Math.round(CENTER + dx * s);
        int tipY = (int) Math.round(CENTER + dy * s);

        tipX = Math.max(1, Math.min(126, tipX));
        tipY = Math.max(1, Math.min(126, tipY));

        drawPixel(canvas, tipX, tipY, ARROW_COLOR);

        if (Math.abs(dx) >= Math.abs(dy)) {
            int dir = dx >= 0 ? -1 : 1;
            drawPixel(canvas, tipX + dir, tipY - 1, ARROW_COLOR);
            drawPixel(canvas, tipX + dir, tipY + 1, ARROW_COLOR);
            drawPixel(canvas, tipX + (dir * 2), tipY, ARROW_COLOR);
        } else {
            int dir = dy >= 0 ? -1 : 1;
            drawPixel(canvas, tipX - 1, tipY + dir, ARROW_COLOR);
            drawPixel(canvas, tipX + 1, tipY + dir, ARROW_COLOR);
            drawPixel(canvas, tipX, tipY + (dir * 2), ARROW_COLOR);
        }
    }

    private void drawPixel(MapCanvas canvas, int x, int y, byte color) {
        if (!isInside(x, y)) {
            return;
        }
        canvas.setPixel(x, y, color);
    }
}
