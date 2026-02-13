package org.minimap.Services;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.minimap.worldmapData.ChunkInfo;
import org.minimap.worldmapData.WaypointIcon;
import org.minimap.worldmapData.WorldmapState;
import org.minimap.Services.WaypointIconRenderer;
import org.minimap.worldmapData.Waypoint;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * WorldmapRenderer
 *
 * Pure projection renderer.
 * Renders discovered chunks into a shared (gridSize * 128) x (gridSize * 128) virtual map
 * and displays one quadrant per physical map.
 */
public class WorldmapRenderer extends MapRenderer {

    private final int groupId;
    private final int slot;
    private final WorldmapStateService stateService;
    private boolean debugLogged;

    /* ---------------------------------------------------------------------
     * Map Asteatics
     * --------------------------------------------------------------------- */
// ------------- FRAME
    // --- colors
    private static final byte WOOD_DARK  = MapPalette.matchColor(96, 72, 44);
    private static final byte WOOD_MID   = MapPalette.matchColor(121, 91, 58);
    private static final byte WOOD_LIGHT = MapPalette.matchColor(152, 118, 74);
    // --- drawing
    private boolean drawTop;
    private boolean drawBottom;
    private boolean drawLeft;
    private boolean drawRight;

    // ------------- FOG
    private static final byte FOG_DARK   = MapPalette.matchColor(15, 15, 15);
    private static final byte FOG_MID    = MapPalette.matchColor(30, 30, 30);
    private static final byte FOG_LIGHT  = MapPalette.matchColor(45, 45, 45);

    /* ---------------------------------------------------------------------
     * Base terrain colors (RGB intent, NOT palette bytes)
     * --------------------------------------------------------------------- */
    private static final Map<Material, Color> BASE_COLORS = Map.ofEntries(
            Map.entry(Material.WATER, new Color(64, 64, 255)),
            Map.entry(Material.SAND, new Color(218, 210, 158)),
            Map.entry(Material.GRASS_BLOCK, new Color(90, 170, 90)),
            Map.entry(Material.OAK_LEAVES, new Color(90, 170, 90)),
            Map.entry(Material.SPRUCE_LEAVES, new Color(60, 120, 60)),
            Map.entry(Material.STONE, new Color(120, 120, 120)),
            Map.entry(Material.DIRT, new Color(134, 96, 67)),
            Map.entry(Material.SNOW, Color.WHITE),
            Map.entry(Material.SNOW_BLOCK, Color.WHITE)
    );

    public static Color baseColorFor(Material material) {
        if (material == null) {
            return new Color(40, 120, 40);
        }
        return BASE_COLORS.getOrDefault(material, new Color(40, 120, 40));
    }

    /**
     * Chunk tile cache
     *
     * Key encodes:
     * - material
     * - shade index (-1 / 0 / +1)
     * - pixels-per-chunk
     */
    private final Map<Long, BufferedImage> tileCache = new HashMap<>();

    public WorldmapRenderer(int groupId, int slot, WorldmapStateService stateService) {
        super(false); // disable vanilla rendering
        this.groupId = groupId;
        this.slot = slot;
        this.stateService = stateService;

    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {

        WorldmapState state = stateService.get(groupId);
        int gridSize = state.gridSize();
        int combinedSize = gridSize * 128;
        int originPx = combinedSize / 2;
        int tileOriginX = tileOriginX(slot, gridSize);
        int tileOriginY = tileOriginY(slot, gridSize);

        if (!debugLogged) {
            Bukkit.getLogger().info(
                    "[MiniMap] Render group=" + groupId +
                            " slot=" + slot +
                            " grid=" + gridSize +
                            " tileOrigin=(" + tileOriginX + "," + tileOriginY + ")"
            );
            debugLogged = true;
        }

        /* ---------------------------------------------------------------
         * Phase 1: gradient fog background
         * --------------------------------------------------------------- */
        for (int lx = 0; lx < 128; lx++) {
            for (int ly = 0; ly < 128; ly++) {
                int combinedX = tileOriginX + lx;
                int combinedY = tileOriginY + ly;
                byte fog = fogColorForCombinedPixel(combinedX, combinedY, combinedSize);
                canvas.setPixel(lx, ly, fog);
            }
        }

        /* ---------------------------------------------------------------
         * Phase 2: immutable state
         * --------------------------------------------------------------- */
        int originX = state.originX();
        int originZ = state.originZ();
        int pixelsPerChunk = pixelsPerChunk(state);

        /* ---------------------------------------------------------------
         * Phase 3: render chunks
         * --------------------------------------------------------------- */
        for (ChunkInfo info : state.discovered().values()) {

            int dx = info.chunkX - originX;
            int dz = info.chunkZ - originZ;

            int combinedX = originPx + dx * pixelsPerChunk;
            int combinedY = originPx + dz * pixelsPerChunk;

            if (combinedX > combinedSize - 1 || combinedY > combinedSize - 1 ||
                    combinedX + pixelsPerChunk < 0 ||
                    combinedY + pixelsPerChunk < 0) {
                continue;
            }

            if (!isInTile(combinedX, combinedY, tileOriginX, tileOriginY)) {
                continue;
            }

            int[] samples = info.sampleRgb();
            if (samples != null && pixelsPerChunk >= ChunkSampling.SAMPLE_GRID) {
                byte[] palette = info.samplePalette();
                int cellSize = pixelsPerChunk / ChunkSampling.SAMPLE_GRID;
                int localX = combinedX - tileOriginX;
                int localY = combinedY - tileOriginY;
                drawSampleGrid(canvas, localX, localY, cellSize, palette);
            } else {
                BufferedImage tile = getChunkTile(info, state);
                canvas.drawImage(combinedX - tileOriginX, combinedY - tileOriginY, tile);
            }
        }
        // Phase 4: Render waypoint icons (overlay)
        for (Waypoint wp : state.waypoints().values()) {

            // Convert world coords -> chunk coords
            int chunkX = Math.floorDiv(wp.x, 16);
            int chunkZ = Math.floorDiv(wp.z, 16);

            int dx = chunkX - originX;
            int dz = chunkZ - originZ;

            int combinedX = originPx + dx * pixelsPerChunk;
            int combinedY = originPx + dz * pixelsPerChunk;

            // Offset within chunk (more precise placement)
            int offsetX = (wp.x & 15) * pixelsPerChunk / 16;
            int offsetZ = (wp.z & 15) * pixelsPerChunk / 16;

            combinedX += offsetX;
            combinedY += offsetZ;

            // Clip combined space
            if (combinedX < 0 || combinedX > combinedSize - 1 ||
                    combinedY < 0 || combinedY > combinedSize - 1) {
                continue;
            }

            // Project into local tile and draw
            drawCombinedIcon(canvas, combinedX, combinedY, wp.icon, tileOriginX, tileOriginY);
        }
        // phase 5 draw frame
        updateFrameEdges(gridSize);
        drawFrame(canvas);
    }

    /* ---------------------------------------------------------------------
     * Projection helpers
     * --------------------------------------------------------------------- */
    private void drawCombinedIcon(
            MapCanvas canvas,
            int combinedX,
            int combinedY,
            WaypointIcon icon,
            int tileOriginX,
            int tileOriginY
    ) {
        if (!isInTile(combinedX, combinedY, tileOriginX, tileOriginY)) {
            return;
        }
        int localX = combinedX - tileOriginX;
        int localY = combinedY - tileOriginY;

        WaypointIconRenderer.drawIcon(
                canvas,
                localX,
                localY,
                icon,
                MapRenderTuning.WORLD_ICON_SCALE
        );
    }

    private boolean isInTile(int combinedX, int combinedY, int tileOriginX, int tileOriginY) {
        return combinedX >= tileOriginX && combinedX < tileOriginX + 128
                && combinedY >= tileOriginY && combinedY < tileOriginY + 128;
    }

    private int tileOriginX(int slot, int gridSize) {
        int col = (slot - 1) % gridSize;
        return col * 128;
    }

    private int tileOriginY(int slot, int gridSize) {
        int row = (slot - 1) / gridSize;
        return row * 128;
    }

    private int pixelsPerChunk(WorldmapState state) {
        return state.pixelsPerChunk();
    }

    private void drawSampleGrid(
            MapCanvas canvas,
            int localX,
            int localY,
            int cellSize,
            byte[] palette
    ) {
        int index = 0;
        for (int sz = 0; sz < ChunkSampling.SAMPLE_GRID; sz++) {
            for (int sx = 0; sx < ChunkSampling.SAMPLE_GRID; sx++) {
                byte color = palette[index++];
                int startX = localX + (sx * cellSize);
                int startY = localY + (sz * cellSize);
                for (int x = 0; x < cellSize; x++) {
                    for (int y = 0; y < cellSize; y++) {
                        canvas.setPixel(startX + x, startY + y, color);
                    }
                }
            }
        }
    }

    /* ---------------------------------------------------------------------
     * Tile generation & caching
     * --------------------------------------------------------------------- */

    private BufferedImage getChunkTile(ChunkInfo info, WorldmapState state) {

        int size = pixelsPerChunk(state);
        int shade = heightShadeIndex(info, state);

        long key =
                ((long) info.topMaterial.ordinal() << 32) |
                        ((long) (shade + 1) << 16) |
                        size;

        return tileCache.computeIfAbsent(key, k -> {

            Color base = baseColorFor(info.topMaterial);

            Color shaded = switch (shade) {
                case -1 -> shade(base, 0.75f);
                case  1 -> shade(base, 1.25f);
                default -> base;
            };

            BufferedImage img =
                    new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);

            Graphics2D g = img.createGraphics();
            g.setColor(shaded);
            g.fillRect(0, 0, size, size);
            g.dispose();

            return img;
        });
    }

    /* ---------------------------------------------------------------------
     * Height shading
     * --------------------------------------------------------------------- */

    private int heightShadeIndex(ChunkInfo info, WorldmapState state) {

        int sum = 0;
        int count = 0;

        int[][] offsets = { {1,0}, {-1,0}, {0,1}, {0,-1} };

        for (int[] o : offsets) {
            long key = WorldmapStateService.chunkKey(
                    info.chunkX + o[0],
                    info.chunkZ + o[1]
            );
            ChunkInfo n = state.discovered().get(key);
            if (n != null) {
                sum += n.topY;
                count++;
            }
        }

        if (count == 0) return 0;

        int delta = info.topY - (sum / count);
        if (delta >= 2) return 1;
        if (delta <= -2) return -1;
        return 0;
    }

    /* ---------------------------------------------------------------------
     * Utility
     * --------------------------------------------------------------------- */

    private static Color shade(Color c, float factor) {
        return new Color(
                Math.min(255, Math.max(0, (int) (c.getRed()   * factor))),
                Math.min(255, Math.max(0, (int) (c.getGreen() * factor))),
                Math.min(255, Math.max(0, (int) (c.getBlue()  * factor)))
        );
    }

    private static BufferedImage backgroundImage() {
        BufferedImage img = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(200, 200, 200));
        g.fillRect(0, 0, 128, 128);
        g.dispose();
        return img;
    }
    /* ---------------------------------------------------------------------
     * Asteatics
     * --------------------------------------------------------------------- */
// ------------ FRAME
    private void drawFrame(MapCanvas canvas) {

        int thickness = 4;

        for (int i = 0; i < thickness; i++) {

            byte color;
            if (i == 0) color = WOOD_DARK;
            else if (i < 2) color = WOOD_MID;
            else color = WOOD_LIGHT;

            // TOP
            if (drawTop) {
                for (int x = i; x < 128 - i; x++) {
                    canvas.setPixel(x, i, color);
                }
            }

            // BOTTOM
            if (drawBottom) {
                for (int x = i; x < 128 - i; x++) {
                    canvas.setPixel(x, 127 - i, color);
                }
            }

            // LEFT
            if (drawLeft) {
                for (int y = i; y < 128 - i; y++) {
                    canvas.setPixel(i, y, color);
                }
            }

            // RIGHT
            if (drawRight) {
                for (int y = i; y < 128 - i; y++) {
                    canvas.setPixel(127 - i, y, color);
                }
            }
        }
    }

    private void updateFrameEdges(int gridSize) {
        int row = (slot - 1) / gridSize;
        int col = (slot - 1) % gridSize;
        drawTop = row == 0;
        drawBottom = row == gridSize - 1;
        drawLeft = col == 0;
        drawRight = col == gridSize - 1;
    }

    // -------------- FOG
    private byte fogColorForCombinedPixel(int x, int y, int combinedSize) {

        int cx = combinedSize / 2;
        int cy = combinedSize / 2;

        int dx = x - cx;
        int dy = y - cy;

        double dist = Math.sqrt(dx * dx + dy * dy);
        double maxDist = Math.sqrt(2) * (combinedSize / 2.0);
        double t = dist / maxDist;

        if (t < 0.33) {
            return FOG_LIGHT;
        } else if (t < 0.66) {
            return FOG_MID;
        } else {
            return FOG_DARK;
        }
    }




}
