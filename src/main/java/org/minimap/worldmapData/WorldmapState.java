package org.minimap.worldmapData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WorldmapState represents all render-relevant, shared state
 * for a single worldmap group.
 *
 * This class is intentionally dumb:
 * - It stores state
 * - It exposes intent-revealing mutations
 * - It does NOT perform rendering or projection
 */
public class WorldmapState {

    private int gridSize = 2;

    /**
     * Discovered chunks keyed by a packed long (chunkX, chunkZ).
     *
     * This map is:
     * - Mutated by commands / discovery logic
     * - Read by renderers
     *
     * Concurrent because renderers and commands may access it
     * from different threads.
     */
    private final Map<Long, ChunkInfo> discovered = new ConcurrentHashMap<>();

    /**
     * Fixed origin of the worldmap, expressed in chunk coordinates.
     *
     * This is set ONCE when the group is created (player location),
     * and never changes afterward.
     *
     * All rendering is projected relative to this origin.
     */
    private Integer originChunkX;
    private Integer originChunkZ;

    public static final int MIN_PIXELS_PER_CHUNK = 1;
    public static final int MAX_PIXELS_PER_CHUNK = 50;
    private static final int DEFAULT_PIXELS_PER_CHUNK = 8;
    private static final int ZOOM_STEP = 2;

    /**
     * Pixels per chunk controls projection scale.
     *
     * Zoom affects ONLY how chunks are projected to pixels,
     * never what data is stored or discovered.
     */
    private int pixelsPerChunk = DEFAULT_PIXELS_PER_CHUNK;

    /* ---------------------------------------------------------------------
     * Discovery access
     * ------------------------------------------------------------------ */

    /**
     * Returns the discovered chunk map.
     *
     * Callers are expected to mutate this map intentionally.
     * Renderers must treat it as read-only.
     */
    public Map<Long, ChunkInfo> discovered() {
        return discovered;
    }

    /* ---------------------------------------------------------------------
     * Origin handling
     * ------------------------------------------------------------------ */

    /**
     * @return true if the origin has been initialized
     */
    public boolean hasOrigin() {
        return originChunkX != null && originChunkZ != null;
    }

    /**
     * Sets the origin if it has not yet been defined.
     *
     * This should be called exactly once, typically when the
     * worldmap group is created, using the player's current chunk.
     */
    public void setOriginIfAbsent(int chunkX, int chunkZ) {
        if (!hasOrigin()) {
            this.originChunkX = chunkX;
            this.originChunkZ = chunkZ;
        }
    }

    /**
     * @return origin X in chunk coordinates
     * @throws IllegalStateException if origin has not been initialized
     */
    public int originX() {
        if (originChunkX == null) {
            throw new IllegalStateException("Worldmap originX accessed before initialization");
        }
        return originChunkX;
    }

    /**
     * @return origin Z in chunk coordinates
     * @throws IllegalStateException if origin has not been initialized
     */
    public int originZ() {
        if (originChunkZ == null) {
            throw new IllegalStateException("Worldmap originZ accessed before initialization");
        }
        return originChunkZ;
    }

    /* ---------------------------------------------------------------------
     * Zoom handling
     * ------------------------------------------------------------------ */

    public int pixelsPerChunk() {
        return pixelsPerChunk;
    }

    public void setPixelsPerChunk(int value) {
        pixelsPerChunk = clamp(value);
    }

    public int gridSize() {
        return gridSize;
    }

    public void setGridSize(int gridSize) {
        if (gridSize < 1 || gridSize > 6) {
            throw new IllegalArgumentException("gridSize out of range: " + gridSize);
        }
        this.gridSize = gridSize;
    }

    public void zoomIn() {
        setPixelsPerChunk(pixelsPerChunk + ZOOM_STEP);
    }

    public void zoomOut() {
        setPixelsPerChunk(pixelsPerChunk - ZOOM_STEP);
    }

    private int clamp(int value) {
        if (value < MIN_PIXELS_PER_CHUNK) {
            return MIN_PIXELS_PER_CHUNK;
        }
        if (value > MAX_PIXELS_PER_CHUNK) {
            return MAX_PIXELS_PER_CHUNK;
        }
        return value;
    }
    //-----------------
    // WAYPOINTS
    // ------------------
    private final Map<String, Waypoint> waypoints = new LinkedHashMap<>();
    public Map<String, Waypoint> waypoints() {
        return waypoints;
    }
    public void addWaypoint(Waypoint waypoint) {

        // Simple overwrite semantics for now
        // (later: duplicate handling, limits, permissions)
        waypoints.put(waypoint.label, waypoint);
    }

    private String focusedWaypointLabel;

    public String focusedWaypointLabel() {
        return focusedWaypointLabel;
    }

    public void setFocusedWaypointLabel(String focusedWaypointLabel) {
        this.focusedWaypointLabel = focusedWaypointLabel;
    }


}
