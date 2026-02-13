package org.minimap.Listeners;


import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.minimap.Services.ChunkSampling;
import org.minimap.Services.PlayerDiscoveryStateService;
import org.minimap.Services.WorldmapStateService;
import org.minimap.personalData.PlayerDiscoveryState;
import org.minimap.worldmapData.ChunkInfo;

public final class PlayerDiscoveryListener implements Listener {

    private final PlayerDiscoveryStateService discoveryService;

    public PlayerDiscoveryListener(PlayerDiscoveryStateService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) {
            return; // same chunk → ignore
        }

        Player player = event.getPlayer();
        World world = player.getWorld();
        Chunk chunk = event.getTo().getChunk();

        // HARD REQUIREMENT: chunk must be loaded
        if (!chunk.isLoaded()) {
            return;
        }

        int cx = chunk.getX();
        int cz = chunk.getZ();
        long chunkKey = WorldmapStateService.chunkKey(cx, cz);
       // player.sendMessage("Discovered" + chunkKey); DEBUG
        PlayerDiscoveryState state = discoveryService.get(player.getUniqueId());

        if (state.hasDiscovered(chunkKey)) {
            return; // already discovered
        }

        // Sample terrain data (SAFE: chunk is loaded)
        ChunkInfo info = sampleChunk(chunk, cx, cz);

        state.discover(chunkKey, info);

        // NOTE:
        // - No worldmap mutation
        // - No autosave yet
        // - No rendering triggers
    }

    private ChunkInfo sampleChunk(Chunk chunk, int cx, int cz) {
        World world = chunk.getWorld();
        int worldX = (cx << 4) + 8;
        int worldZ = (cz << 4) + 8;

        int highestY = world.getHighestBlockYAt(worldX, worldZ);
        int topY = Math.max(world.getMinHeight(), highestY - 1);
        Block topBlock = world.getBlockAt(worldX, topY, worldZ);
        Biome biome = world.getBiome(worldX, topY, worldZ);
        int[] samples = ChunkSampling.sampleChunkRgb(world, cx, cz);

        return new ChunkInfo(
                cx,
                cz,
                highestY,
                topBlock.getType(),
                biome,
                samples
        );
    }
}
