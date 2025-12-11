package org.lushplugins.pinata.commands.impl;

import org.lushplugins.pinata.commands.ISubCommand;
import org.lushplugins.pinata.configuration.MessageManager;
import org.lushplugins.pinata.stats.PlayerStats;
import org.lushplugins.pinata.stats.PlayerStatsService;
import org.lushplugins.pinata.stats.StatsLeaderboardService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PinataStatsCommand implements ISubCommand {

    private final PlayerStatsService playerStatsService;
    private final MessageManager messageManager;
    private final PinataStatsTopCommand topCommand;
    private final PinataStatsResetCommand resetCommand;

    public PinataStatsCommand(PlayerStatsService playerStatsService, MessageManager messageManager, StatsLeaderboardService leaderboardService) {
        this.playerStatsService = playerStatsService;
        this.messageManager = messageManager;
        this.topCommand = new PinataStatsTopCommand(leaderboardService, messageManager);
        this.resetCommand = new PinataStatsResetCommand(playerStatsService, messageManager);
    }

    @Override
    public String getName() {
        return "stats";
    }

    @Override
    public String getPermission() {
        return "benthpinata.stats";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "top":
                    if (!sender.hasPermission("benthpinata.stats.top")) {
                        messageManager.sendMessage(sender, "no-permission");
                        return;
                    }
                    topCommand.execute(sender, args);
                    return;
                case "reset":
                    if (!sender.hasPermission("benthpinata.stats.reset")) {
                        messageManager.sendMessage(sender, "no-permission");
                        return;
                    }
                    resetCommand.execute(sender, args);
                    return;
            }
        }

        if (!(sender instanceof Player)) {
            messageManager.sendMessage(sender, "player-only-command");
            return;
        }

        Player player = (Player) sender;

        PlayerStats stats = playerStatsService.getStats(player);

        messageManager.sendMessageList(player, "stats.header");
        messageManager.sendMessage(player, "stats.total-damage", "%damage%", String.valueOf(stats.getTotalDamage()));
        messageManager.sendMessage(player, "stats.pinata-kills", "%kills%", String.valueOf(stats.getPinataKills()));
        messageManager.sendMessageList(player, "stats.footer");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("benthpinata.stats.top")) {
            return Collections.singletonList("top");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("top") && sender.hasPermission("benthpinata.stats.top")) {
            return Arrays.asList("damage", "kills");
        }
        return Collections.emptyList();
    }
}