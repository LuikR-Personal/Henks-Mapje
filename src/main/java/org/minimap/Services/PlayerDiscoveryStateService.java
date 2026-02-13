package org.minimap.Services;



import org.minimap.personalData.PlayerDiscoveryState;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PlayerDiscoveryStateService {

    private final ConcurrentMap<UUID, PlayerDiscoveryState> states = new ConcurrentHashMap<>();

    public PlayerDiscoveryState get(UUID playerId) {
        return states.computeIfAbsent(playerId, PlayerDiscoveryState::new);
    }

    public boolean has(UUID playerId) {
        return states.containsKey(playerId);
    }

    public Collection<PlayerDiscoveryState> getAll() {
        return states.values();
    }

    public void remove(UUID playerId) {
        states.remove(playerId);
    }
}
