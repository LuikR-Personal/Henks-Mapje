package org.minimap.worldmapData;

/**
 * Immutable, authoritative waypoint belonging to a worldmap group.
 *
 * Contains world coordinates, label, and visual identity.
 */
public class Waypoint {

    public final int x;
    public final int y;
    public final int z;
    public final String label;

    /**
     * Visual identity shared across all renderers.
     * Assigned at import time.
     */
    public final WaypointIcon icon;

    public Waypoint(int x, int y, int z, String label, WaypointIcon icon) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.label = label;
        this.icon = icon;
    }
}
