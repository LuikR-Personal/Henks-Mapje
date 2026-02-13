package org.minimap.Services;

import org.bukkit.entity.Player;
import org.bukkit.map.*;
import org.minimap.worldmapData.Waypoint;
import org.minimap.worldmapData.WorldmapState;

import java.util.List;

/**
 * Renders a TWO-MAP (256px logical width) waypoint legend
 * with a painted wooden frame and center beam.
 *
 * legendPart:
 *   0 = left map
 *   1 = right map
 */
public class WaypointLegendRenderer extends MapRenderer {

    private final int groupId;
    private final WorldmapStateService stateService;
    private final int legendPart;

    // ─────────────────────────────
    // Frame palette colors
    // ─────────────────────────────
    private static final byte WOOD_DARK   = MapPalette.matchColor(96, 72, 44);
    private static final byte WOOD_MID    = MapPalette.matchColor(121, 91, 58);
    private static final byte WOOD_LIGHT  = MapPalette.matchColor(152, 118, 74);
    private static final byte WOOD_BG     = MapPalette.matchColor(171, 137, 90);
    private static final byte WOOD_GRAIN  = MapPalette.matchColor(160, 126, 82);

    // ------ FRAME Drawing
    private boolean drawLeft;
    private boolean drawRight;
    public WaypointLegendRenderer(int groupId, WorldmapStateService stateService, int legendPart) {
        super(false); // disable vanilla rendering
        this.groupId = groupId;
        this.stateService = stateService;
        this.legendPart = legendPart;

        drawLeft = legendPart == 0;
        drawRight = legendPart == 1;

    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {

        // ─────────────────────────────
        // Background
        // ─────────────────────────────
        clearBackground(canvas);

        // ─────────────────────────────
        // Frame
        // ─────────────────────────────
        drawFrame(canvas);



        WorldmapState state = stateService.get(groupId);
        if (state == null) return;

        List<Waypoint> waypoints = List.copyOf(state.waypoints().values());

        // ─────────────────────────────
        // Logical 256px layout (inside frame)
        // ─────────────────────────────
        final int ICON_X  = 10;
        final int LABEL_X = 26;
        final int COORD_X = 150;

        // Header (left map only)
        if (legendPart == 0) {
            canvas.drawText(
                    10,
                    8,
                    MinecraftFont.Font,
                    "Waypoints"
            );
        }

        int y = 22;
        int maxLines = 12;

        for (int i = 0; i < waypoints.size() && i < maxLines; i++) {
            Waypoint wp = waypoints.get(i);

            drawIcon(canvas, ICON_X, y + 4, wp);
            drawText(canvas, LABEL_X, y, wp.label);

            String coords = "X:" + wp.x + " Z:" + wp.z;
            drawText(canvas, COORD_X, y, coords);

            y += MapRenderTuning.LEGEND_LINE_HEIGHT;
        }

        // Overflow indicator (left map only)
        if (legendPart == 0 && waypoints.size() > maxLines) {
            canvas.drawText(
                    10,
                    y + 4,
                    MinecraftFont.Font,
                    "..."
            );
        }
    }

    /* ─────────────────────────────
     * Frame drawing
     * ───────────────────────────── */

    private void clearBackground(MapCanvas canvas) {
        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {

                // Base wood color
                byte color = WOOD_BG;

                // Subtle grain pattern (deterministic)
                if ((x + y) % 7 == 0) {
                    color = WOOD_GRAIN;
                }

                canvas.setPixel(x, y, color);
            }
        }
    }


    private void drawFrame(MapCanvas canvas) {

        int thickness = 6;

        for (int i = 0; i < thickness; i++) {

            byte color;
            if (i == 0) color = WOOD_DARK;
            else if (i < 3) color = WOOD_MID;
            else color = WOOD_LIGHT;

            // Top & bottom
            for (int x = i; x < 128 - i; x++) {
                canvas.setPixel(x, i, color);
                canvas.setPixel(x, 127 - i, color);
            }

            // Left & right
            if (drawLeft) {
                for (int y = i; y < 128 - i; y++) {
                    //canvas.setPixel(i, y, color); -> right
                    canvas.setPixel(127 - i, y, color);
                }
            }
            if (drawRight) {
                for (int y = i; y < 128 - i; y++) {
                    canvas.setPixel(i, y, color);   //-> right
                    //canvas.setPixel(127 - i, y, color);
                }
            }
        }
    }


    /**
     * Draws a vertical wooden beam between the two legend maps
     * so the seam looks intentional.
     */




    /* ─────────────────────────────
     * Slice-aware drawing helpers
     * ───────────────────────────── */

    private void drawIcon(MapCanvas canvas, int logicalX, int y, Waypoint wp) {
        if (!inSlice(logicalX)) return;

        WaypointIconRenderer.drawIcon(
                canvas,
                toCanvasX(logicalX),
                y,
                wp.icon,
                MapRenderTuning.LEGEND_ICON_SCALE
        );
    }

    private void drawText(MapCanvas canvas, int logicalX, int y, String text) {
        if (!inSlice(logicalX)) return;

        canvas.drawText(
                toCanvasX(logicalX),
                y,
                MinecraftFont.Font,
                text
        );
    }

    /* ─────────────────────────────
     * Slice math
     * ───────────────────────────── */

    private int sliceStart() {
        return legendPart * 128;
    }

    private boolean inSlice(int logicalX) {
        return logicalX >= sliceStart() && logicalX < sliceStart() + 128;
    }

    private int toCanvasX(int logicalX) {
        return logicalX - sliceStart();
    }
}
