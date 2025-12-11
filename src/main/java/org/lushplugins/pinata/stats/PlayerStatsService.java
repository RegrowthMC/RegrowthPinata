package org.lushplugins.pinata.stats;

import org.lushplugins.pinata.stats.database.IStatsDataHandler;
import org.lushplugins.pinata.stats.database.SqliteDataHandler;
import org.lushplugins.pinata.stats.database.YamlDataHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PlayerStatsService {
    private final Map<UUID, PlayerStats> statsMap = new ConcurrentHashMap<>();
    private final JavaPlugin plugin;
    private final IStatsDataHandler dataHandler;

    public PlayerStatsService(JavaPlugin plugin) {
        this.plugin = plugin;

        String storageType = Objects.requireNonNull(plugin.getConfig().getString("storage.type", "SQLITE")).toUpperCase();
        if (storageType.equals("YAML")) {
            this.dataHandler = new YamlDataHandler(plugin);
            plugin.getLogger().info("Depolama yöntemi olarak YAML kullanılıyor.");
        } else {
            this.dataHandler = new SqliteDataHandler(plugin);
            plugin.getLogger().info("Depolama yöntemi olarak SQLite kullanılıyor.");
        }
        this.dataHandler.initialize();
    }

    public PlayerStats getStats(Player player) {
        return statsMap.computeIfAbsent(player.getUniqueId(), PlayerStats::new);
    }

    public void addDamage(Player player, int amount) {
        getStats(player).addDamage(amount);
    }

    public void addKill(UUID playerId) {
        statsMap.computeIfAbsent(playerId, PlayerStats::new).addKill();
    }

    public void loadStatsAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<UUID, PlayerStats> loadedStats = dataHandler.loadAllStats();
            Bukkit.getScheduler().runTask(plugin, () -> {
                statsMap.clear();
                statsMap.putAll(loadedStats);
                plugin.getLogger().info(loadedStats.size() + " oyuncu istatistiği asenkron olarak yüklendi.");
            });
        });
    }

    public void saveStatsAsync() {
        Map<UUID, PlayerStats> statsToSave = statsMap.values().stream()
                .filter(PlayerStats::isDirty)
                .collect(Collectors.toMap(PlayerStats::getPlayerId, stats -> stats));

        if (statsToSave.isEmpty()) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            dataHandler.saveStats(statsToSave);

            Bukkit.getScheduler().runTask(plugin, () -> {
                statsToSave.keySet().forEach(uuid -> {
                    PlayerStats stats = statsMap.get(uuid);
                    if (stats != null) {
                        stats.setDirty(false);
                    }
                });
                plugin.getLogger().info(statsToSave.size() + " adet değiştirilmiş oyuncu istatistiği kaydedildi.");
            });
        });
    }

    public void saveStatsSync() {
        dataHandler.saveStats(statsMap);
        plugin.getLogger().info("Oyuncu istatistikleri kaydedildi.");
    }

    public void shutdown() {
        dataHandler.shutdown();
    }

    public List<PlayerStats> getAllStats() {
        return new ArrayList<>(statsMap.values());
    }

    public void resetPlayerStats(UUID playerId) {
        statsMap.remove(playerId);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> dataHandler.resetPlayerStats(playerId)
        );
    }



    public void resetAllStats() {
        statsMap.clear();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, dataHandler::resetAllStats);
    }
}