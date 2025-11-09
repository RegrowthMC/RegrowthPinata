package com.bentahsin.benthPinata.expansion;

import com.bentahsin.benthPinata.expansion.placeholders.IPlaceholder;
import com.bentahsin.benthPinata.expansion.placeholders.PlayerStatsPlaceholder;
import com.bentahsin.benthPinata.expansion.placeholders.TopListPlaceholder;
import com.bentahsin.benthPinata.stats.PlayerStats;
import com.bentahsin.benthPinata.stats.PlayerStatsService;
import com.bentahsin.benthPinata.stats.StatsLeaderboardService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class BenthPinataExpansion extends PlaceholderExpansion {

    private final PlayerStatsService playerStatsService;
    private final StatsLeaderboardService leaderboardService;
    private final Map<String, IPlaceholder> placeholders = new HashMap<>();

    public BenthPinataExpansion(PlayerStatsService playerStatsService, StatsLeaderboardService leaderboardService) {
        this.playerStatsService = playerStatsService;
        this.leaderboardService = leaderboardService;
        registerPlaceholders();
    }

    private void registerPlaceholders() {
        addPlaceholder(new PlayerStatsPlaceholder("stats_damage", playerStatsService, PlayerStats::getTotalDamage));
        addPlaceholder(new PlayerStatsPlaceholder("stats_kills", playerStatsService, PlayerStats::getPinataKills));

        for (int i = 1; i <= 10; i++) {
            addPlaceholder(new TopListPlaceholder("top_damage_" + i + "_name", i, leaderboardService,
                    StatsLeaderboardService::getTopDamage,
                    stats -> Bukkit.getOfflinePlayer(stats.getPlayerId()).getName()));
            addPlaceholder(new TopListPlaceholder("top_damage_" + i + "_value", i, leaderboardService,
                    StatsLeaderboardService::getTopDamage,
                    stats -> String.valueOf(stats.getTotalDamage())));

            addPlaceholder(new TopListPlaceholder("top_kills_" + i + "_name", i, leaderboardService,
                    StatsLeaderboardService::getTopKills,
                    stats -> Bukkit.getOfflinePlayer(stats.getPlayerId()).getName()));
            addPlaceholder(new TopListPlaceholder("top_kills_" + i + "_value", i, leaderboardService,
                    StatsLeaderboardService::getTopKills,
                    stats -> String.valueOf(stats.getPinataKills())));
        }
    }

    private void addPlaceholder(IPlaceholder placeholder) {
        this.placeholders.put(placeholder.getIdentifier().toLowerCase(), placeholder);
    }

    @Override
    public @NotNull String getIdentifier() { return "bp"; }

    @Override
    public @NotNull String getAuthor() { return "bentahsin"; }

    @Override
    public @NotNull String getVersion() { return "1.0.1-RELEASE"; }

    @Override
    public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        IPlaceholder placeholder = placeholders.get(params.toLowerCase());
        if (placeholder != null) {
            return placeholder.getValue(player);
        }

        return null;
    }
}