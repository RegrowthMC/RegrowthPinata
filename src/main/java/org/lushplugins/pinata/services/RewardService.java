package org.lushplugins.pinata.services;

import org.lushplugins.pinata.configuration.ConfigManager;
import org.lushplugins.pinata.pinata.model.Pinata;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * rewards.yml dosyasını okuyarak şans ve ödül mantığını yönetir.
 */
public class RewardService {

    private final ConfigManager configManager;
    private final PlaceholderService placeholderService;

    public RewardService(ConfigManager configManager, PlaceholderService placeholderService) {
        this.configManager = configManager;
        this.placeholderService = placeholderService;
    }

    public void giveOnHitReward(Player player) {
        ConfigurationSection onHitSection = configManager.getRewardsConfig().getConfigurationSection("on-hit");
        if (onHitSection == null) return;

        Optional<String> rewardKeyOpt = getWeightedRandomResult(onHitSection);

        if (rewardKeyOpt.isPresent()) {
            String rewardKey = rewardKeyOpt.get();

            String message = onHitSection.getString(rewardKey + ".message");
            if (message != null && !message.isEmpty()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            }

            List<String> commands = onHitSection.getStringList(rewardKey + ".commands");
            executeCommands(commands, player, null);
        }
    }

    public void giveFinalRewards(List<Map.Entry<UUID, Integer>> sortedDamagers) {
        ConfigurationSection finalSection = configManager.getRewardsConfig().getConfigurationSection("final");
        if (finalSection == null) return;

        Optional<String> rewardSetKey = getWeightedRandomResult(finalSection);
        if (rewardSetKey.isPresent()) {
            List<String> commands = finalSection.getStringList(rewardSetKey.get() + ".commands");
            executeCommands(commands, null, sortedDamagers);
        }
    }

    private void executeCommands(List<String> commands, Player singlePlayer, List<Map.Entry<UUID, Integer>> sortedDamagers) {
        for (String command : commands) {
            String processedCommand = command;
            if (singlePlayer != null) {
                processedCommand = processedCommand.replace("%player%", singlePlayer.getName());
            }
            if (sortedDamagers != null) {
                processedCommand = placeholderService.parseTopDamagers(processedCommand, sortedDamagers);
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
        }
    }

    private Optional<String> getWeightedRandomResult(ConfigurationSection section) {
        List<String> weightedList = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            int chance = section.getInt(key + ".chance", 0);
            for (int i = 0; i < chance; i++) {
                weightedList.add(key);
            }
        }
        if (weightedList.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(weightedList.get(new Random().nextInt(weightedList.size())));
    }

    /**
     * Bir oyuncunun hasarını kontrol eder ve hak ettiği eşik ödüllerini verir.
     * @param player Ödül alacak oyuncu.
     * @param pinata Hasar verilen Piñata.
     */
    public void checkAndGrantThresholdRewards(Player player, Pinata pinata) {
        ConfigurationSection thresholdSection = configManager.getRewardsConfig().getConfigurationSection("damage-thresholds");
        if (thresholdSection == null) {
            return;
        }

        int totalDamage = pinata.getDamagers().getOrDefault(player.getUniqueId(), 0);
        if (totalDamage == 0) {
            return;
        }

        for (String thresholdId : thresholdSection.getKeys(false)) {
            int requiredDamage = thresholdSection.getInt(thresholdId + ".required-damage");
            if (totalDamage >= requiredDamage && !pinata.hasClaimedThreshold(player.getUniqueId(), thresholdId)) {
                pinata.markThresholdAsClaimed(player.getUniqueId(), thresholdId);
                String message = thresholdSection.getString(thresholdId + ".message");
                if (message != null && !message.isEmpty()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                }

                List<String> commands = thresholdSection.getStringList(thresholdId + ".commands");
                executeCommands(commands, player, null);
            }
        }
    }
}