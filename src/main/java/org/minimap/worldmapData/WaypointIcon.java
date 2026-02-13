package org.minimap.worldmapData;

import org.bukkit.Color;
import org.bukkit.map.MapPalette;

/**
 * Immutable visual identity for a waypoint.
 *
 * Combines a shape and a MapPalette color byte.
 * Shared by all renderers.
 */


public final class WaypointIcon {

    public final IconShape shape;
    public final byte color; // MapPalette index

    public WaypointIcon(IconShape shape, byte color) {
        this.shape = shape;
        this.color = color;
    }
}
