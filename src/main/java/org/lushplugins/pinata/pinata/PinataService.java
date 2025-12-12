package org.lushplugins.pinata.pinata;

import org.lushplugins.pinata.RegrowthPinata;
import org.lushplugins.pinata.config.MessageManager;
import org.lushplugins.pinata.config.SettingsManager;
import org.lushplugins.pinata.pinata.model.Pinata;
import org.lushplugins.pinata.pinata.model.PinataAbility;
import org.lushplugins.pinata.pinata.model.PinataType;
import org.lushplugins.pinata.service.*;
import org.lushplugins.pinata.stats.PlayerStatsService;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class PinataService {

    private final RegrowthPinata plugin;
    private final SettingsManager settingsManager;
    private final MessageManager messageManager;
    private final PinataRepository pinataRepository;
    private final EffectService effectService;
    private final RewardService rewardService;
    private final PlaceholderService placeholderService;
    private final BossBarService bossBarService;
    private final AbilityService abilityService;
    private final PlayerStatsService playerStatsService;
    private final MobCustomizerService mobCustomizerService;

    private final Map<String, PinataType> loadedPinataTypes = new HashMap<>();
    private final Map<UUID, BukkitTask> activeCountdownTasks = new HashMap<>();
    private final Map<UUID, Long> playerCooldowns = new HashMap<>();

    public PinataService(RegrowthPinata plugin, SettingsManager settingsManager, MessageManager messageManager,
                         PinataRepository pinataRepository, EffectService effectService, RewardService rewardService,
                         PlaceholderService placeholderService, BossBarService bossBarService,
                         AbilityService abilityService, PlayerStatsService playerStatsService,
                         MobCustomizerService mobCustomizerService) {
        this.plugin = plugin;
        this.settingsManager = settingsManager;
        this.messageManager = messageManager;
        this.pinataRepository = pinataRepository;
        this.effectService = effectService;
        this.rewardService = rewardService;
        this.placeholderService = placeholderService;
        this.bossBarService = bossBarService;
        this.abilityService = abilityService;
        this.playerStatsService = playerStatsService;
        this.mobCustomizerService = mobCustomizerService;
    }

    public void loadPinataTypes() {
        loadedPinataTypes.clear();
        FileConfiguration mainConfig = plugin.getConfigManager().getMainConfig();
        FileConfiguration abilitiesConfig = plugin.getConfigManager().getAbilitiesConfig();

        if (abilitiesConfig == null) {
            plugin.getLogger().severe("Abilities config (abilities.yml) yüklenemedi! Yetenekler devre dışı kalacak.");
        }

        String typesPath = "pinata-types";
        ConfigurationSection pinataTypesSection = mainConfig.getConfigurationSection(typesPath);

        if (pinataTypesSection == null) {
            plugin.getLogger().warning("config.yml içinde 'pinata-types' bölümü bulunamadı. Hiçbir Piñata türü yüklenmedi.");
            return;
        }

        for (String id : pinataTypesSection.getKeys(false)) {
            String typePath = typesPath + "." + id;

            String locationString = mainConfig.getString(typePath + ".spawn-location");
            Location spawnLocation = parseLocation(locationString);
            if (spawnLocation == null) {
                plugin.getLogger().warning(id + " adlı Piñata türü için konum geçersiz. Bu tür yüklenmedi.");
                continue;
            }

            int health = mainConfig.getInt(typePath + ".health", settingsManager.getDefaultPinataHealth());

            EntityType entityType;
            String entityTypeName = Objects.requireNonNull(mainConfig.getString(typePath + ".entity-type", "SHEEP")).toUpperCase();
            try {
                entityType = EntityType.valueOf(entityTypeName);
                if (!entityType.isAlive()) {
                    throw new IllegalArgumentException("Varlık canlı bir mob değil.");
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning(id + " adlı Piñata için geçersiz entity-type: '" + entityTypeName + "'. Varsayılan olarak KOYUN (SHEEP) kullanılıyor.");
                entityType = EntityType.SHEEP;
            }

            Map<String, Object> mobOptions = new HashMap<>();
            ConfigurationSection optionsSection = mainConfig.getConfigurationSection(typePath + ".mob-options");
            if (optionsSection != null) {
                for (String key : optionsSection.getKeys(false)) {
                    mobOptions.put(key, optionsSection.get(key));
                }
            }

            List<PinataAbility> abilities = loadAbilitiesForType(id, abilitiesConfig);

            PinataType pinataType = new PinataType(id, spawnLocation, health, abilities, entityType, mobOptions);
            loadedPinataTypes.put(id.toLowerCase(), pinataType);
            plugin.getLogger().info(id + " adlı Piñata türü (" + entityType.name() + ") " + abilities.size() + " yetenek ile yüklendi.");
        }
    }

    private int getIntFromMap(Map<?, ?> map, String key, int defaultValue) {
        Object obj = map.get(key);
        if (obj instanceof Integer) {
            return (Integer) obj;
        }
        return defaultValue;
    }

    public void startEvent(String typeName) {
        startEvent(typeName, null);
    }

    public boolean startEvent(String typeName, Location customLocation) {
        Optional<PinataType> typeOpt = getPinataType(typeName);
        if (!typeOpt.isPresent()) {
            return false;
        }

        PinataType type = typeOpt.get();
        final Location finalLocation = (customLocation != null) ? customLocation : type.getSpawnLocation();

        broadcastTitle("countdown-started");
        effectService.playSoundForAll("countdown-start", finalLocation);

        int countdownTime = settingsManager.getCountdownTime();
        final int[] remainingTime = {countdownTime};

        UUID eventId = UUID.randomUUID();
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (remainingTime[0] < 1) {
                spawnPinata(type, finalLocation);
                BukkitTask finishedTask = activeCountdownTasks.remove(eventId);
                if (finishedTask != null) {
                    finishedTask.cancel();
                }
                return;
            }

            broadcastTitle("countdown-title", "%time%", String.valueOf(remainingTime[0]));
            effectService.playSoundForAll("countdown-tick", finalLocation);
            remainingTime[0]--;

        }, 0L, 20L);

        activeCountdownTasks.put(eventId, task);
        return true;
    }

    public void handleDamage(Player damager, Pinata pinata) {
        long now = System.currentTimeMillis();
        long cooldownEnd = playerCooldowns.getOrDefault(damager.getUniqueId(), 0L);
        if (now < cooldownEnd) return;
        playerCooldowns.put(damager.getUniqueId(), now + settingsManager.getHitCooldownMillis());

        if (pinata.isDying()) return;

        boolean isDead = pinata.applyDamage(damager, 1);
        playerStatsService.addDamage(damager, 1);

        abilityService.tryTriggerAbilities(pinata);
        bossBarService.updateProgress(pinata);
        effectService.playSoundForAll("hit", pinata.getEntity().getLocation());
        effectService.spawnParticle("hit", pinata.getEntity().getEyeLocation());

        int totalDamage = pinata.getDamagers().getOrDefault(damager.getUniqueId(), 0);
        sendHitActionBar(damager,
                "%health%", String.valueOf(pinata.getCurrentHealth()),
                "%total_damage%", String.valueOf(totalDamage));

        rewardService.giveOnHitReward(damager);
        rewardService.checkAndGrantThresholdRewards(damager, pinata);

        if (isDead) {
            pinata.setDying(true);
            handleDeath(pinata);
        }
    }

    public void killAll() {
        new ArrayList<>(activeCountdownTasks.values()).forEach(BukkitTask::cancel);
        activeCountdownTasks.clear();

        new ArrayList<>(pinataRepository.findAll()).forEach(this::cleanupPinata);
        pinataRepository.clear();

        playerCooldowns.clear();
    }

    public Optional<PinataType> getPinataType(String id) {
        return Optional.ofNullable(loadedPinataTypes.get(id.toLowerCase()));
    }

    private void spawnPinata(PinataType type, Location loc) {
        World world = loc.getWorld();
        if (world == null) {
            plugin.getLogger().severe(type.getId() + " için dünya bulunamadı! Piñata oluşturulamadı.");
            return;
        }

        Entity spawnedEntity = world.spawnEntity(loc, type.getEntityType());
        if (!(spawnedEntity instanceof LivingEntity)) {
            plugin.getLogger().severe(type.getId() + " için yaratılan varlık bir LivingEntity değil! Bu bir hata. Lütfen kontrol edin.");
            spawnedEntity.remove();
            return;
        }
        LivingEntity livingEntity = (LivingEntity) spawnedEntity;

        livingEntity.setRemoveWhenFarAway(false);
        livingEntity.setMetadata(Pinata.METADATA_KEY, new FixedMetadataValue(plugin, type.getId()));

        mobCustomizerService.applyOptions(livingEntity, type.getMobOptions());

        Pinata pinata = new Pinata(type, livingEntity);
        pinataRepository.save(pinata);
        bossBarService.createBossBar(pinata);

        broadcastTitle("pinata-spawned");
        effectService.playSoundForAll("spawn", livingEntity.getLocation());
    }

    private List<PinataAbility> loadAbilitiesForType(String typeId, FileConfiguration abilitiesConfig) {
        List<PinataAbility> abilities = new ArrayList<>();
        if (abilitiesConfig == null || !abilitiesConfig.isSet(typeId)) {
            return abilities;
        }

        List<Map<?, ?>> abilityMaps = abilitiesConfig.getMapList(typeId);
        for (Map<?, ?> map : abilityMaps) {
            try {
                String type = (String) map.get("type");
                String trigger = (String) map.get("trigger");
                int value = getIntFromMap(map, "value", 0);
                int range = getIntFromMap(map, "range", 10);
                int power = getIntFromMap(map, "power", 1);
                int duration = getIntFromMap(map, "duration", 5);
                String message = (String) map.get("message");
                String sound = (String) map.get("sound");

                List<PotionEffectType> potions = new ArrayList<>();
                if (map.get("potion-effects") instanceof List<?>) {
                    List<?> rawPotionList = (List<?>) map.get("potion-effects");
                    for (Object rawEffect : rawPotionList) {
                        if (rawEffect instanceof String) {
                            PotionEffectType effectType = PotionEffectType.getByName((String) rawEffect);
                            if (effectType != null) {
                                potions.add(effectType);
                            } else {
                                plugin.getLogger().warning(typeId + " türü için yetenekte geçersiz iksir efekti: " + rawEffect);
                            }
                        }
                    }
                }
                abilities.add(new PinataAbility(type, trigger, value, range, power, duration, message, sound, potions));
            } catch (Exception e) {
                plugin.getLogger().severe(typeId + " türü için bir yetenek yüklenirken hata oluştu: " + e.getMessage());
            }
        }
        return abilities;
    }

    public void handleDeath(Pinata pinata) {
        effectService.playSoundForAll("death", pinata.getEntity().getLocation());
        effectService.spawnParticle("death", pinata.getEntity().getLocation());
        broadcastTitle("pinata-death-title");

        List<Map.Entry<UUID, Integer>> sortedDamagers = pinata.getSortedDamagers();

        if (!sortedDamagers.isEmpty()) {
            UUID topDamagerId = sortedDamagers.get(0).getKey();
            playerStatsService.addKill(topDamagerId);
        }

        messageManager.getMessageList("death-broadcast").forEach(line -> {
            String processedLine = placeholderService.parseTopDamagers(line, sortedDamagers);
            Bukkit.broadcastMessage(processedLine);
        });

        rewardService.giveFinalRewards(sortedDamagers);
        cleanupPinata(pinata);
    }

    private void cleanupPinata(Pinata pinata) {
        if (pinata == null) return;

        bossBarService.removeBossBar(pinata);
        if (pinata.getEntity() != null) {
            pinata.getEntity().remove();
        }
        pinataRepository.remove(pinata);
    }

    private void broadcastTitle(String path, String... placeholders) {
        String combined = messageManager.getMessage(path, placeholders);
        String[] parts = combined.split(";", 2);
        String title = parts[0];
        String subtitle = parts.length > 1 ? parts[1] : "";

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(title, subtitle, 10, 70, 20);
        }
    }

    private void sendHitActionBar(Player player, String... placeholders) {
        String message = messageManager.getMessage("actionbar-hit", placeholders);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }

    private Location parseLocation(String locStr) {
        if (locStr == null || locStr.isEmpty()) return null;
        try {
            String[] parts = locStr.split(";");
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) return null;
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            return new Location(world, x, y, z);
        } catch (Exception e) {
            plugin.getLogger().severe("Konum formatı hatalı: " + locStr);
            return null;
        }
    }

    public boolean killPinata(UUID pinataId) {
        for (Pinata pinata : pinataRepository.findAll()) {
            if (pinata.getUniqueId().equals(pinataId)) {
                cleanupPinata(pinata);
                return true;
            }
        }
        return false;
    }

    public Set<String> getLoadedTypeIds() {
        return loadedPinataTypes.keySet();
    }
}