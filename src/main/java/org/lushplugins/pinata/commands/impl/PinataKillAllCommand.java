package org.lushplugins.pinata.commands.impl;

import org.lushplugins.pinata.commands.ISubCommand;
import org.lushplugins.pinata.configuration.MessageManager;
import org.lushplugins.pinata.pinata.PinataService;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * Aktif olan tüm Piñata'ları ve ilişkili görevleri sonlandıran alt komut.
 */
public class PinataKillAllCommand implements ISubCommand {

    private final PinataService pinataService;
    private final MessageManager messageManager;

    public PinataKillAllCommand(PinataService pinataService, MessageManager messageManager) {
        this.pinataService = pinataService;
        this.messageManager = messageManager;
    }

    @Override
    public String getName() {
        return "killall";
    }

    @Override
    public String getPermission() {
        return "benthpinata.command.killall";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        pinataService.killAll();
        messageManager.sendMessage(sender, "killall-success");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}