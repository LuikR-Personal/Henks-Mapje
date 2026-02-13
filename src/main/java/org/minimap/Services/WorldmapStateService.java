package org.minimap.Services;

import org.minimap.worldmapData.WorldmapState;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for all WorldmapState instances.
 *
 * This service owns the in-memory representation of
 * all worldmap groups currently known to the plugin.
 *
 * States are:
 * - Created lazily on first access
 * - Shared across commands and renderers
 * - Persisted externally by WorldmapPersistenceService
 */
public class WorldmapStateService {

    /**
     * All known worldmap states keyed by groupId.
     *
     * Concurrent because:
     * - Renderers may read while commands mutate
     * - Persistence may iterate while server is running
     */
    private final Map<Integer, WorldmapState> states = new ConcurrentHashMap<>();

    /**
     * Returns the WorldmapState for a given groupId.
     *
     * If the state does not yet exist, it is created lazily.
     *
     * @param groupId unique worldmap group identifier
     * @return shared WorldmapState instance
     */
    public WorldmapState get(int groupId) {
        return states.computeIfAbsent(groupId, id -> new WorldmapState());
    }

    /**
     * @return true if a WorldmapState already exists for this groupId
     */
    public boolean has(int groupId) {
        return states.containsKey(groupId);
    }

    /**
     * Returns an immutable view of all known worldmap states.
     *
     * Intended for:
     * - Persistence (saveAll)
     * - Diagnostics / debugging
     *
     * Callers must NOT attempt to modify the returned map.
     */
    public Map<Integer, WorldmapState> getAll() {
        return Collections.unmodifiableMap(states);
    }

    /**
     * Removes a worldmap group from memory.
     *
     * This does NOT delete persisted data on disk.
     * That responsibility belongs to the persistence service.
     *
     * @param groupId worldmap group to remove
     */
    public void remove(int groupId) {
        states.remove(groupId);
    }

    /**
     * Packs a chunk coordinate pair into a single long key.
     *
     * Used for fast lookup in discovered chunk maps.
     *
     * Format:
     *   high 32 bits = chunkX
     *   low  32 bits = chunkZ
     */
    public static long chunkKey(int cx, int cz) {
        return (((long) cx) << 32) ^ (cz & 0xffffffffL);
    }
}
