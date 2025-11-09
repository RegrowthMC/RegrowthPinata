package com.bentahsin.benthPinata;

import com.bentahsin.benthPinata.commands.CommandManager;
import com.bentahsin.benthPinata.commands.impl.*;
import com.bentahsin.benthPinata.configuration.ConfigManager;
import com.bentahsin.benthPinata.configuration.MessageManager;
import com.bentahsin.benthPinata.configuration.SettingsManager;
import com.bentahsin.benthPinata.expansion.BenthPinataExpansion;
import com.bentahsin.benthPinata.hologram.HologramServiceProvider;
import com.bentahsin.benthPinata.hologram.services.IHologramService;
import com.bentahsin.benthPinata.listeners.PinataInteractionListener;
import com.bentahsin.benthPinata.pinata.PinataRepository;
import com.bentahsin.benthPinata.pinata.PinataService;
import com.bentahsin.benthPinata.services.*;
import com.bentahsin.benthPinata.stats.PlayerStatsService;
import com.bentahsin.benthPinata.stats.StatsLeaderboardService;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

public final class BenthPinata extends JavaPlugin {

    private ConfigManager configManager;
    private MessageManager messageManager;
    private SettingsManager settingsManager;
    private IHologramService hologramService;
    private BossBarService bossBarService;
    private PinataRepository pinataRepository;
    private PinataService pinataService;
    private PinataUpdateService pinataUpdateService;
    private EventManager eventManager;
    private PlayerStatsService playerStatsService;
    private StatsLeaderboardService statsLeaderboardService;
    private BenthPinataExpansion expansion;
    private BukkitTask autoSaveTask;

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
        this.hologramService = HologramServiceProvider.create(this);

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
                hologramService,
                effectService,
                rewardService,
                placeholderService,
                bossBarService,
                abilityService,
                playerStatsService,
                mobCustomizerService
        );


        this.pinataUpdateService = new PinataUpdateService(this.pinataRepository, this.hologramService, abilityService);
        this.pinataUpdateService.runTaskTimer(this, 40L, 4L);

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
        if (this.pinataUpdateService != null && !this.pinataUpdateService.isCancelled()) {
            this.pinataUpdateService.cancel();
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
        CommandManager commandManager = new CommandManager(this.messageManager);
        commandManager.registerCommand(new PinataStartCommand(this.pinataService, this.messageManager));
        commandManager.registerCommand(new PinataReloadCommand(this));
        commandManager.registerCommand(new PinataKillAllCommand(this.pinataService, this.messageManager));
        commandManager.registerCommand(new PinataHelpCommand(this.messageManager));
        commandManager.registerCommand(new PinataStatsCommand(this.playerStatsService, this.messageManager, this.statsLeaderboardService));
        commandManager.registerCommand(new PinataListCommand(this.pinataRepository, this.messageManager));
        commandManager.registerCommand(new PinataKillCommand(this.pinataService, this.pinataRepository, this.messageManager));

        Objects.requireNonNull(getCommand("pinata")).setExecutor(commandManager);
        Objects.requireNonNull(getCommand("pinata")).setTabCompleter(commandManager);

        getServer().getPluginManager().registerEvents(new PinataInteractionListener(this.pinataRepository, this.pinataService, this.bossBarService), this);
        getServer().getPluginManager().registerEvents(this.eventManager, this);
    }

    public void reload() {
        shutdown();
        startup();
        getLogger().info("BenthPinata eklentisi başarıyla yeniden yüklendi.");
    }

    public ConfigManager getConfigManager() { return configManager; }
    public MessageManager getMessageManager() { return messageManager; }
    public SettingsManager getSettingsManager() { return settingsManager; }
    public IHologramService getHologramService() { return hologramService; }
    public PinataRepository getPinataRepository() { return pinataRepository; }
}