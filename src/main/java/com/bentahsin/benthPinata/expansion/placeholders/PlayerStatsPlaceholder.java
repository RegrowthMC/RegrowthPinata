package com.bentahsin.benthPinata.expansion.placeholders;

import com.bentahsin.benthPinata.stats.PlayerStats;
import com.bentahsin.benthPinata.stats.PlayerStatsService;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class PlayerStatsPlaceholder implements IPlaceholder {

    private final String identifier;
    private final PlayerStatsService playerStatsService;
    private final Function<PlayerStats, Integer> valueExtractor;

    public PlayerStatsPlaceholder(String identifier, PlayerStatsService playerStatsService, Function<PlayerStats, Integer> valueExtractor) {
        this.identifier = identifier;
        this.playerStatsService = playerStatsService;
        this.valueExtractor = valueExtractor;
    }

    @Override
    public @NotNull String getIdentifier() {
        return identifier;
    }

    @Override
    public String getValue(OfflinePlayer player) {
        Player onlinePlayer = player.getPlayer();
        if (onlinePlayer == null) {
            return "0";
        }
        PlayerStats stats = playerStatsService.getStats(onlinePlayer);
        return String.valueOf(valueExtractor.apply(stats));
    }
}