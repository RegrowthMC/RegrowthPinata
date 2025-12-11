package org.lushplugins.pinata.commands.impl;

import org.lushplugins.pinata.RegrowthPinata;
import org.lushplugins.pinata.commands.ISubCommand;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class PinataReloadCommand implements ISubCommand {
    private final RegrowthPinata plugin;

    public PinataReloadCommand(RegrowthPinata plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "reload"; }

    @Override
    public String getPermission() { return "benthpinata.command.reload"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.reload();
        plugin.getMessageManager().sendMessage(sender, "reload-success");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}