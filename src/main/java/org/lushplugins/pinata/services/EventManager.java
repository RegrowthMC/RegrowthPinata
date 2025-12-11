package org.lushplugins.pinata.services;

import org.lushplugins.pinata.RegrowthPinata;
import org.lushplugins.pinata.configuration.ConfigManager;
import org.lushplugins.pinata.pinata.PinataService;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.concurrent.TimeUnit;

public class EventManager implements Listener {

    private final RegrowthPinata plugin;
    private final PinataService pinataService;
    private final ConfigManager configManager;

    private long lastPlayerCountEventTimestamp = 0;

    public EventManager(RegrowthPinata plugin, PinataService pinataService, ConfigManager configManager) {
        this.plugin = plugin;
        this.pinataService = pinataService;
        this.configManager = configManager;
    }

    /**
     * Otomatik etkinlik zamanlayıcılarını ve dinleyicilerini başlatır.
     */
    public void start() {
        startTimedEventScheduler();
    }

    private void startTimedEventScheduler() {
        ConfigurationSection timedConfig = configManager.getMainConfig().getConfigurationSection("automatic-events.timed");
        if (timedConfig == null || !timedConfig.getBoolean("enabled", false)) {
            return;
        }

        long intervalTicks = TimeUnit.MINUTES.toSeconds(timedConfig.getInt("interval-minutes", 120)) * 20;

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (Bukkit.getOnlinePlayers().size() >= timedConfig.getInt("minimum-players", 5)) {
                if (plugin.getPinataRepository().findAll().isEmpty()) {
                    String type = timedConfig.getString("pinata-type", "default");
                    Bukkit.broadcastMessage("§d[BenthPiñata] §eSunucu canlandı! Otomatik bir Piñata etkinliği başlıyor!");
                    pinataService.startEvent(type);
                }
            }
        }, intervalTicks, intervalTicks);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        ConfigurationSection playerCountConfig = configManager.getMainConfig().getConfigurationSection("automatic-events.player-count");
        if (playerCountConfig == null || !playerCountConfig.getBoolean("enabled", false)) {
            return;
        }

        int currentPlayers = Bukkit.getOnlinePlayers().size();
        int requiredPlayers = playerCountConfig.getInt("required-players", 20);

        if (currentPlayers == requiredPlayers) {
            long cooldownMillis = TimeUnit.MINUTES.toMillis(playerCountConfig.getInt("cooldown-minutes", 90));
            if (System.currentTimeMillis() - lastPlayerCountEventTimestamp > cooldownMillis) {
                if (plugin.getPinataRepository().findAll().isEmpty()) {
                    lastPlayerCountEventTimestamp = System.currentTimeMillis();
                    String type = playerCountConfig.getString("pinata-type", "default");
                    Bukkit.broadcastMessage("§d[BenthPinata] §bSunucu dolup taşıyor! §6" + requiredPlayers + " §boyuncuya ulaşıldığı için özel bir Piñata etkinliği başlıyor!");
                    pinataService.startEvent(type);
                }
            }
        }
    }
}