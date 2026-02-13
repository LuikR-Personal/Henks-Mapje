package org.minimap.personalData;



import org.minimap.worldmapData.ChunkInfo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerDiscoveryState {

    private final UUID playerId;
    private final Map<Long, ChunkInfo> discovered = new ConcurrentHashMap<>();

    public PlayerDiscoveryState(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID playerId() {
        return playerId;
    }

    /**
     * Mutable map.
     * Commands / listeners may write.
     * Renderers (later) must treat as read-only.
     */
    public Map<Long, ChunkInfo> discovered() {
        return discovered;
    }

    public boolean hasDiscovered(long chunkKey) {
        return discovered.containsKey(chunkKey);
    }

    public void discover(long chunkKey, ChunkInfo info) {
        discovered.putIfAbsent(chunkKey, info);
    }
}
