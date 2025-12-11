package org.lushplugins.pinata.stats.database;

import org.lushplugins.pinata.stats.PlayerStats;

import java.util.Map;
import java.util.UUID;

/**
 * Oyuncu istatistiklerinin kalıcılığını yöneten operasyonlar için bir arayüz tanımlar.
 * Bu, depolama mekanizmasının (YAML, SQLite vb.) ana mantıktan soyutlanmasını sağlar.
 */
public interface IStatsDataHandler {
    void initialize();
    void shutdown();
    Map<UUID, PlayerStats> loadAllStats();
    void saveStats(Map<UUID, PlayerStats> statsMap);
    void resetPlayerStats(UUID playerId);
    void resetAllStats();
}