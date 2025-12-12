package org.lushplugins.pinata;

import org.lushplugins.pinata.config.ConfigManager;
import org.lushplugins.pinata.config.MessageManager;
import org.lushplugins.pinata.config.SettingsManager;
import org.lushplugins.pinata.expansion.BenthPinataExpansion;
import org.lushplugins.pinata.listener.PinataInteractionListener;
import org.lushplugins.pinata.pinata.PinataRepository;
import org.lushplugins.pinata.pinata.PinataService;
import org.lushplugins.pinata.service.*;
import org.lushplugins.pinata.stats.PlayerStatsService;
import org.lushplugins.pinata.stats.StatsLeaderboardService;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

public final class RegrowthPinata extends JavaPlugin {
    private static RegrowthPinata plugin;

    private ConfigManager configManager;
    private MessageManager messageManager;
    private SettingsManager settingsManager;
    private BossBarService bossBarService;
    private PinataRepository pinataRepository;
    private PinataService pinataService;
    private PinataAuraService auraUpdater;
    private EventManager eventManager;
    private PlayerStatsService playerStatsService;
    private StatsLeaderboardService statsLeaderboardService;
    private BenthPinataExpansion expansion;
    private BukkitTask autoSaveTask;
    private BukkitTask hologramUpdateTask;
    private BukkitTask pinataAuraTask;

    @Override
    public void onLoad() {
        plugin = this;
    }


    @Override
    public void onEnable() {
        startup();
    }

    @Override
    public void onDisable() {
        if (this.playerStatsService != null) {
            this.playerStatsService.saveStatsSync();
            this.playerStatsService.shutdown();
        }
        shutdown();
        getLogger().info("BenthPinata eklentisi devre dışı bırakıldı.");
    }

    public void startup() {
        this.configManager = new ConfigManager(this);
        this.configManager.setup();

        this.settingsManager = new SettingsManager(configManager);
        this.messageManager = new MessageManager(configManager);

        PlaceholderService placeholderService = new PlaceholderService();
        EffectService effectService = new EffectService(configManager);

        this.bossBarService = new BossBarService(Objects.requireNonNull(configManager.getMainConfig().getConfigurationSection("boss-bar")), this.messageManager);

        RewardService rewardService = new RewardService(configManager, placeholderService);
        AbilityService abilityService = new AbilityService(effectService);
        this.playerStatsService = new PlayerStatsService(this);
        this.playerStatsService.loadStatsAsync();
        this.statsLeaderboardService = new StatsLeaderboardService(this, this.playerStatsService);
        MobCustomizerService mobCustomizerService = new MobCustomizerService(this);

        this.pinataRepository = new PinataRepository();
        this.pinataService = new PinataService(
                this,
                settingsManager,
                messageManager,
                pinataRepository,
                effectService,
                rewardService,
                placeholderService,
                bossBarService,
                abilityService,
                playerStatsService,
                mobCustomizerService
        );

        this.auraUpdater = new PinataAuraService(this.pinataRepository, abilityService);
        this.pinataAuraTask = this.auraUpdater.runTaskTimer(this, 40L, 4L);

        this.eventManager = new EventManager(this, this.pinataService, this.configManager);
        this.eventManager.start();

        SchedulerService schedulerService = new SchedulerService(this, this.pinataService);
        schedulerService.loadAndStart();

        this.pinataService.loadPinataTypes();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.expansion = new BenthPinataExpansion(this.playerStatsService, this.statsLeaderboardService);
            this.expansion.register();
            getLogger().info("PlaceholderAPI desteği başarıyla aktif edildi.");
        }

        long autoSaveInterval = 20L * 60 * 10; // 10 Dakika (tick cinsinden)
        this.autoSaveTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (playerStatsService != null) {
                    playerStatsService.saveStatsAsync();
                    getLogger().info("Oyuncu istatistikleri otomatik olarak kaydedildi.");
                }
            }
        }.runTaskTimerAsynchronously(this, autoSaveInterval, autoSaveInterval);
        registerHandlers();

        getLogger().info("BenthPinata eklentisi başlatıldı!");
    }

    public void shutdown() {
        if (this.hologramUpdateTask != null && !this.hologramUpdateTask.isCancelled()) {
            this.hologramUpdateTask.cancel();
        }
        if (this.pinataAuraTask != null && !this.pinataAuraTask.isCancelled()) {
            this.pinataAuraTask.cancel();
        }
        if (this.autoSaveTask != null && !this.autoSaveTask.isCancelled()) {
            this.autoSaveTask.cancel();
        }
        if (this.statsLeaderboardService != null) {
            this.statsLeaderboardService.cancelTask();
        }
        if (this.expansion != null && this.expansion.isRegistered()) {
            this.expansion.unregister();
        }
        if (this.pinataService != null) {
            this.pinataService.killAll();
        }
        HandlerList.unregisterAll(this);
    }

    public void registerHandlers() {
        getServer().getPluginManager().registerEvents(new PinataInteractionListener(this.pinataRepository, this.pinataService, this.bossBarService), this);
        getServer().getPluginManager().registerEvents(this.eventManager, this);
    }

    public void reload() {
        shutdown();
        startup();
        getLogger().info("BenthPinata eklentisi başarıyla yeniden yüklendi.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    public PinataRepository getPinataRepository() {
        return pinataRepository;
    }

    public PinataService getPinataService() {
        return pinataService;
    }

    public PlayerStatsService getPlayerStatsService() {
        return playerStatsService;
    }

    public static RegrowthPinata getInstance() {
        return plugin;
    }
}