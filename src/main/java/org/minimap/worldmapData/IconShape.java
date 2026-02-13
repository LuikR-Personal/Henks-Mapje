package org.minimap.worldmapData;

/**
 * Defines the visual shape of a waypoint icon.
 *
 * Shapes are rendered as small pixel patterns
 * and must be readable at all zoom levels.
 */
public enum IconShape {

    DOT,        // 3x3 filled square
    CROSS,      // plus shape
    DIAMOND,    // rotated square
    SQUARE,     // 5x5 filled square
    TRIANGLE    // upward triangle
}
