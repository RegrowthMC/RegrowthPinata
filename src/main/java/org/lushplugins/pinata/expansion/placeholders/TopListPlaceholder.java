package org.lushplugins.pinata.expansion.placeholders;

import org.lushplugins.pinata.stats.PlayerStats;
import org.lushplugins.pinata.stats.StatsLeaderboardService;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

public class TopListPlaceholder implements IPlaceholder {

    private final String identifier;
    private final int rank;
    private final StatsLeaderboardService leaderboardService;
    private final Function<StatsLeaderboardService, List<PlayerStats>> listSupplier;
    private final Function<PlayerStats, String> valueExtractor;

    public TopListPlaceholder(String identifier, int rank, StatsLeaderboardService leaderboardService,
                              Function<StatsLeaderboardService, List<PlayerStats>> listSupplier,
                              Function<PlayerStats, String> valueExtractor) {
        this.identifier = identifier;
        this.rank = rank - 1;
        this.leaderboardService = leaderboardService;
        this.listSupplier = listSupplier;
        this.valueExtractor = valueExtractor;
    }

    @Override
    public @NotNull String getIdentifier() {
        return identifier;
    }

    @Override
    public String getValue(OfflinePlayer player) {
        List<PlayerStats> topList = listSupplier.apply(leaderboardService);

        if (topList == null || rank >= topList.size()) {
            return "Yok";
        }

        PlayerStats stats = topList.get(rank);
        return valueExtractor.apply(stats);
    }
}