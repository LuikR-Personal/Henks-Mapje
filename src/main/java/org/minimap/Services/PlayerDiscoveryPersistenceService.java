package org.minimap.Services;



import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.minimap.personalData.PlayerDiscoveryState;
import org.minimap.worldmapData.ChunkInfo;
import org.minimap.Services.SampleCodec;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public final class PlayerDiscoveryPersistenceService {

    private final Plugin plugin;
    private final PlayerDiscoveryStateService stateService;
    private final File baseDir;

    public PlayerDiscoveryPersistenceService(
            Plugin plugin,
            PlayerDiscoveryStateService stateService
    ) {
        this.plugin = plugin;
        this.stateService = stateService;
        this.baseDir = new File(plugin.getDataFolder(), "player_discovery");
    }

    // ───────────────────────── LOAD ─────────────────────────

    public void loadAll() {
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            plugin.getLogger().warning("[MiniMap] Failed to create player_discovery directory");
            return;
        }

        File[] files = baseDir.listFiles((dir, name) -> name.startsWith("player_") && name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            loadOne(file);
        }

        plugin.getLogger().info("[MiniMap] Loaded " + files.length + " player discovery files");
    }

    private void loadOne(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        String idString = yaml.getString("playerId");
        if (idString == null) return;

        UUID playerId;
        try {
            playerId = UUID.fromString(idString);
        } catch (IllegalArgumentException e) {
            return;
        }

        PlayerDiscoveryState state = stateService.get(playerId);

        if (!yaml.isConfigurationSection("discovered")) return;

        for (String key : yaml.getConfigurationSection("discovered").getKeys(false)) {
            long chunkKey;
            try {
                chunkKey = Long.parseLong(key);
            } catch (NumberFormatException e) {
                continue;
            }

            int cx = yaml.getInt("discovered." + key + ".x");
            int cz = yaml.getInt("discovered." + key + ".z");
            int topY = yaml.getInt("discovered." + key + ".topY");

            Material material = Material.matchMaterial(
                    yaml.getString("discovered." + key + ".material", "")
            );

            Biome biome;
            try {
                biome = Biome.valueOf(
                        yaml.getString("discovered." + key + ".biome", "PLAINS")
                );
            } catch (IllegalArgumentException e) {
                biome = Biome.PLAINS;
            }

            String samplesRaw = yaml.getString("discovered." + key + ".samples");
            int[] samples = SampleCodec.decode(samplesRaw);
            state.discover(chunkKey, new ChunkInfo(cx, cz, topY, material, biome, samples));
        }
    }

    // ───────────────────────── SAVE ─────────────────────────

    public void saveAll() {
        if (!baseDir.exists() && !baseDir.mkdirs()) return;

        for (PlayerDiscoveryState state : stateService.getAll()) {
            saveOne(state);
        }
    }

    public void saveOne(PlayerDiscoveryState state) {
        File file = new File(baseDir, "player_" + state.playerId() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();

        yaml.set("playerId", state.playerId().toString());

        for (Map.Entry<Long, ChunkInfo> entry : state.discovered().entrySet()) {
            long key = entry.getKey();
            ChunkInfo info = entry.getValue();

            String base = "discovered." + key;
            yaml.set(base + ".x", info.chunkX);
            yaml.set(base + ".z", info.chunkZ);
            yaml.set(base + ".topY", info.topY);
            yaml.set(
                    base + ".material",
                    info.topMaterial != null ? info.topMaterial.name() : null
            );
            yaml.set(base + ".biome", info.biome.name());
            String samples = SampleCodec.encode(info.sampleRgb());
            if (samples != null) {
                yaml.set(base + ".samples", samples);
            }

        }

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("[MiniMap] Failed to save discovery for " + state.playerId());
        }
    }
}

