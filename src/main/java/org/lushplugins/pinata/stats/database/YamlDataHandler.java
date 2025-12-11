package org.lushplugins.pinata.stats.database;

import org.lushplugins.pinata.stats.PlayerStats;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

public class YamlDataHandler implements IStatsDataHandler {

    private final JavaPlugin plugin;
    private final File statsFile;

    public YamlDataHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
    }

    @Override
    public void initialize() {
        try {
            if (!statsFile.exists() && statsFile.createNewFile()) {
                plugin.getLogger().info("stats.yml dosyası oluşturuldu.");
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "stats.yml dosyası oluşturulamadı!", e);
        }
    }

    @Override
    public void shutdown() { }

    @Override
    public Map<UUID, PlayerStats> loadAllStats() {
        FileConfiguration statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        Map<UUID, PlayerStats> loadedStats = new HashMap<>();

        if (statsConfig.isConfigurationSection("stats")) {
            Objects.requireNonNull(statsConfig.getConfigurationSection("stats")).getKeys(false).forEach(uuidString -> {
                try {
                    UUID playerId = UUID.fromString(uuidString);
                    PlayerStats stats = new PlayerStats(playerId);
                    stats.setTotalDamage(statsConfig.getInt("stats." + uuidString + ".totalDamage"));
                    stats.setPinataKills(statsConfig.getInt("stats." + uuidString + ".pinataKills"));
                    loadedStats.put(playerId, stats);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("stats.yml içinde geçersiz UUID formatı: " + uuidString);
                }
            });
        }
        return loadedStats;
    }

    @Override
    public void saveStats(Map<UUID, PlayerStats> statsMap) {
        FileConfiguration statsConfig = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerStats> entry : statsMap.entrySet()) {
            String uuidString = entry.getKey().toString();
            statsConfig.set("stats." + uuidString + ".totalDamage", entry.getValue().getTotalDamage());
            statsConfig.set("stats." + uuidString + ".pinataKills", entry.getValue().getPinataKills());
        }
        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "İstatistikler dosyaya kaydedilemedi!", e);
        }
    }

    @Override
    public void resetPlayerStats(UUID playerId) {
        FileConfiguration statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        statsConfig.set("stats." + playerId.toString(), null);
        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Oyuncu istatistiği sıfırlanırken dosya kaydedilemedi!", e);
        }
    }

    @Override
    public void resetAllStats() {
        FileConfiguration statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        statsConfig.set("stats", null);
        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Tüm istatistikler sıfırlanırken dosya kaydedilemedi!", e);
        }
    }
}