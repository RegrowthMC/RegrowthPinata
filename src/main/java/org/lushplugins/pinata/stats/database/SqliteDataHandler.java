package org.lushplugins.pinata.stats.database;

import org.lushplugins.pinata.stats.PlayerStats;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class SqliteDataHandler implements IStatsDataHandler {

    private final JavaPlugin plugin;
    private Connection connection;

    public SqliteDataHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        File dbFile = new File(plugin.getDataFolder(), "database.db");
        if (!dbFile.exists()) {
            try {
                dbFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "database.db dosyası oluşturulamadı!", e);
                return;
            }
        }

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement statement = connection.createStatement()) {
                String query = "CREATE TABLE IF NOT EXISTS player_stats (" +
                        "uuid TEXT PRIMARY KEY NOT NULL," +
                        "total_damage INTEGER DEFAULT 0," +
                        "pinata_kills INTEGER DEFAULT 0" +
                        ");";
                statement.execute(query);
            }
        } catch (SQLException | ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "SQLite veritabanı bağlantısı kurulamadı!", e);
        }
    }

    @Override
    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "SQLite bağlantısı kapatılırken hata oluştu!", e);
        }
    }

    @Override
    public Map<UUID, PlayerStats> loadAllStats() {
        Map<UUID, PlayerStats> statsMap = new HashMap<>();
        String query = "SELECT * FROM player_stats;";
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(query)) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                PlayerStats stats = new PlayerStats(uuid);
                stats.setTotalDamage(rs.getInt("total_damage"));
                stats.setPinataKills(rs.getInt("pinata_kills"));
                statsMap.put(uuid, stats);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "SQLite'den istatistikler yüklenemedi!", e);
        }
        return statsMap;
    }

    @Override
    public void saveStats(Map<UUID, PlayerStats> statsMap) {
        String query = "INSERT OR REPLACE INTO player_stats (uuid, total_damage, pinata_kills) VALUES (?, ?, ?);";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            connection.setAutoCommit(false);
            for (PlayerStats stats : statsMap.values()) {
                ps.setString(1, stats.getPlayerId().toString());
                ps.setInt(2, stats.getTotalDamage());
                ps.setInt(3, stats.getPinataKills());
                ps.addBatch();
            }
            ps.executeBatch();
            connection.commit();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "İstatistikler SQLite'ye kaydedilemedi!", e);
            try {
                connection.rollback();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Rollback başarısız!", ex);
            }
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Auto-commit etkinleştirilemedi!", e);
            }
        }
    }

    @Override
    public void resetPlayerStats(UUID playerId) {
        String query = "DELETE FROM player_stats WHERE uuid = ?;";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, playerId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "SQLite'den oyuncu verisi silinemedi!", e);
        }
    }

    @Override
    public void resetAllStats() {
        String query = "DELETE FROM player_stats;";
        try (Statement statement = connection.createStatement()) {
            statement.execute(query);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Tüm istatistikler SQLite'den silinemedi!", e);
        }
    }
}