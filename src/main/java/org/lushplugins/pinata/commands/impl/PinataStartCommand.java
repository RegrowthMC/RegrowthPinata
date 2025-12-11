package org.lushplugins.pinata.commands.impl;

import org.lushplugins.pinata.commands.ISubCommand;
import org.lushplugins.pinata.configuration.MessageManager;
import org.lushplugins.pinata.pinata.PinataService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PinataStartCommand implements ISubCommand {

    private final PinataService pinataService;
    private final MessageManager messageManager;

    public PinataStartCommand(PinataService pinataService, MessageManager messageManager) {
        this.pinataService = pinataService;
        this.messageManager = messageManager;
    }

    @Override
    public String getName() {
        return "start";
    }

    @Override
    public String getPermission() {
        return "benthpinata.command.start";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            messageManager.sendMessage(sender, "start-command-usage");
            return;
        }

        String pinataTypeName = args[0];
        Location customLocation = null;

        if (args.length > 1) {
            if (args.length == 2 && args[1].equalsIgnoreCase("here")) {
                if (!(sender instanceof Player)) {
                    messageManager.sendMessage(sender, "player-only-command");
                    return;
                }
                Player player = (Player) sender;
                customLocation = player.getLocation();
            }
            else if (args.length == 5) {
                try {
                    World world = Bukkit.getWorld(args[1]);
                    if (world == null) {
                        messageManager.sendMessage(sender, "invalid-world", "%world%", args[1]);
                        return;
                    }
                    double x = Double.parseDouble(args[2]);
                    double y = Double.parseDouble(args[3]);
                    double z = Double.parseDouble(args[4]);
                    customLocation = new Location(world, x, y, z);
                } catch (NumberFormatException e) {
                    messageManager.sendMessage(sender, "invalid-coordinates");
                    return;
                }
            } else {
                messageManager.sendMessage(sender, "start-command-usage");
                return;
            }
        }
        boolean success = pinataService.startEvent(pinataTypeName, customLocation);

        if (!success) {
            messageManager.sendMessage(sender, "pinata-type-not-found");
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String currentArg = args[0].toLowerCase();
            return pinataService.getLoadedTypeIds().stream()
                    .filter(type -> type.toLowerCase().startsWith(currentArg))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            List<String> suggestions = new ArrayList<>();
            if (sender instanceof Player) {
                suggestions.add("here");
            }
            List<String> worldNames = Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList());
            suggestions.addAll(worldNames);

            return suggestions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length >= 3 && args.length <= 5) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                Location loc = player.getLocation();
                if (args.length == 3) return Collections.singletonList(String.valueOf(loc.getBlockX()));
                if (args.length == 4) return Collections.singletonList(String.valueOf(loc.getBlockY()));
                if (args.length == 5) return Collections.singletonList(String.valueOf(loc.getBlockZ()));
            }
        }

        return Collections.emptyList();
    }
}