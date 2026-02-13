package org.minimap.Services;

import org.bukkit.map.MapCanvas;
import org.minimap.worldmapData.IconShape;
import org.minimap.worldmapData.WaypointIcon;

/**
 * Utility class for rendering waypoint icons onto map canvases.
 *
 * This class is stateless and renderer-agnostic.
 */
public final class WaypointIconRenderer {

    private WaypointIconRenderer() {
        // utility class
    }

    /**
     * Backwards-compatible draw method (scale = 1).
     */
    public static void drawIcon(
            MapCanvas canvas,
            int centerX,
            int centerY,
            WaypointIcon icon
    ) {
        drawIcon(canvas, centerX, centerY, icon, 1);
    }

    /**
     * Draws a waypoint icon centered at the given canvas coordinates,
     * with a configurable pixel scale.
     */
    public static void drawIcon(
            MapCanvas canvas,
            int centerX,
            int centerY,
            WaypointIcon icon,
            int scale
    ) {
        if (scale < 1) scale = 1;

        int[][] pixels = pixelsForShape(icon.shape);

        for (int[] p : pixels) {
            drawScaledPixel(
                    canvas,
                    centerX + p[0] * scale,
                    centerY + p[1] * scale,
                    icon.color,
                    scale
            );
        }
    }

    /**
     * Draws a single logical pixel as a scale×scale block.
     */
    private static void drawScaledPixel(
            MapCanvas canvas,
            int centerX,
            int centerY,
            byte color,
            int scale
    ) {
        int half = scale / 2;

        for (int dx = 0; dx < scale; dx++) {
            for (int dy = 0; dy < scale; dy++) {
                int x = centerX + dx - half;
                int y = centerY + dy - half;

                if (x < 0 || x >= 128 || y < 0 || y >= 128) continue;
                canvas.setPixel(x, y, color);
            }
        }
    }

    /**
     * Returns the pixel offsets that define each icon shape.
     *
     * Offsets are relative to the icon center (0,0).
     */
    private static int[][] pixelsForShape(IconShape shape) {
        return switch (shape) {

            case DOT -> DOT;
            case CROSS -> CROSS;
            case DIAMOND -> DIAMOND;
            case SQUARE -> SQUARE;
            case TRIANGLE -> TRIANGLE;
        };
    }

    /* ─────────────────────────────
     * Icon pixel patterns
     * ───────────────────────────── */

    /**
     * 1×1 dot (minimal, very clear)
     */
    private static final int[][] DOT = {
            {0, 0}
    };

    /**
     * 3×3 plus shape
     *
     *   . # .
     *   # # #
     *   . # .
     */
    private static final int[][] CROSS = {
            {0, 0},
            {-1, 0}, {1, 0},
            {0, -1}, {0, 1}
    };

    /**
     * Diamond (rotated square)
     *
     *   . # .
     *   # . #
     *   . # .
     */
    private static final int[][] DIAMOND = {
            {0, -1},
            {-1, 0}, {1, 0},
            {0, 1}
    };

    /**
     * Solid 3×3 square
     *
     *   # # #
     *   # # #
     *   # # #
     */
    private static final int[][] SQUARE = {
            {-1, -1}, {0, -1}, {1, -1},
            {-1,  0}, {0,  0}, {1,  0},
            {-1,  1}, {0,  1}, {1,  1}
    };

    /**
     * Upward-pointing triangle
     *
     *     #
     *   # # #
     */
    private static final int[][] TRIANGLE = {
            {0, -1},
            {-1, 0}, {0, 0}, {1, 0}
    };
}
