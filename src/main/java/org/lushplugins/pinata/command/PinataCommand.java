package org.lushplugins.pinata.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.lushplugins.pinata.RegrowthPinata;
import org.lushplugins.pinata.pinata.model.Pinata;
import org.lushplugins.pinata.stats.PlayerStats;
import org.lushplugins.pinata.utils.lamp.parameter.annotation.PinataId;
import org.lushplugins.pinata.utils.lamp.parameter.annotation.PinataType;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
@Command("pinata")
public class PinataCommand {

    @Subcommand("help")
    @CommandPermission("pinata.command.help")
    public void help(CommandSender sender) {
        RegrowthPinata.getInstance().getMessageManager().sendMessageList(sender, "help-command");
    }

    @Subcommand("kill")
    @CommandPermission("pinata.command.kill")
    public void kill(CommandSender sender, @PinataId int pinataId) {
        List<Pinata> activePinatas = new ArrayList<>(RegrowthPinata.getInstance().getPinataRepository().findAll());

        if (pinataId <= 0 || pinataId > activePinatas.size()) {
            RegrowthPinata.getInstance().getMessageManager().sendMessage(sender, "kill-command-invalid-id", "%id%", String.valueOf(pinataId));
            return;
        }

        Pinata pinata = activePinatas.get(pinataId - 1);
        if (!RegrowthPinata.getInstance().getPinataService().killPinata(pinata.getUniqueId())) {
            RegrowthPinata.getInstance().getMessageManager().sendMessage(sender, "kill-command-failure", "%id%", String.valueOf(pinataId));
            return;
        }

        RegrowthPinata.getInstance().getMessageManager().sendMessage(sender, "kill-command-success", "%id%", String.valueOf(pinataId), "%type%", pinata.getType().getId());
    }

    @Subcommand("killall")
    @CommandPermission("pinata.command.kill.all")
    public void killAll(CommandSender sender) {
        RegrowthPinata.getInstance().getPinataService().killAll();
        RegrowthPinata.getInstance().getMessageManager().sendMessage(sender, "killall-success");
    }

    @Subcommand("list")
    @CommandPermission("pinata.command.list")
    public void list(CommandSender sender) {
        List<Pinata> activePinatas = new ArrayList<>(RegrowthPinata.getInstance().getPinataRepository().findAll());

        if (activePinatas.isEmpty()) {
            RegrowthPinata.getInstance().getMessageManager().sendMessage(sender, "list-command-empty");
            return;
        }

        RegrowthPinata.getInstance().getMessageManager().sendMessageList(sender, "list-command-header");
        for (int i = 0; i < activePinatas.size(); i++) {
            Pinata pinata = activePinatas.get(i);
            sender.sendMessage(RegrowthPinata.getInstance().getMessageManager().getMessage("list-command-format",
                "%id%", String.valueOf(i + 1),
                "%type%", pinata.getType().getId(),
                "%health%", String.valueOf(pinata.getCurrentHealth()),
                "%max_health%", String.valueOf(pinata.getType().getMaxHealth()),
                "%world%", pinata.getEntity().getWorld().getName(),
                "%x%", String.valueOf(pinata.getEntity().getLocation().getBlockX()),
                "%y%", String.valueOf(pinata.getEntity().getLocation().getBlockY()),
                "%z%", String.valueOf(pinata.getEntity().getLocation().getBlockZ())
            ));
        }

        RegrowthPinata.getInstance().getMessageManager().sendMessageList(sender, "list-command-footer");
    }

    @Subcommand("reload")
    @CommandPermission("pinata.command.reload")
    public void reload(CommandSender sender) {
        RegrowthPinata.getInstance().reload();
        RegrowthPinata.getInstance().getMessageManager().sendMessage(sender, "reload-success");
    }

    public void spawn(BukkitCommandActor actor, String pinataType, Location location) {
        if (!RegrowthPinata.getInstance().getPinataService().startEvent(pinataType, location)) {
            RegrowthPinata.getInstance().getMessageManager().sendMessage(actor.sender(), "pinata-type-not-found");
        }
    }

    @Subcommand("spawn")
    @CommandPermission("pinata.command.spawn")
    public void spawn(BukkitCommandActor actor, @PinataType String pinataType) {
        spawn(actor, pinataType, actor.requirePlayer().getLocation());
    }

    @Subcommand("spawn")
    @CommandPermission("pinata.command.spawn")
    public void spawn(BukkitCommandActor actor, @PinataType String pinataType, double x, double y, double z, @Optional String worldName) {
        World world;
        if (worldName != null) {
            world = Bukkit.getWorld(worldName);
        } else if (actor.isPlayer()) {
            world = actor.requirePlayer().getWorld();
        } else {
            world = Bukkit.getWorlds().getFirst();
        }

        spawn(actor, pinataType, new Location(world, x, y, z));
    }

    @Subcommand("stats")
    @CommandPermission("pinata.command.stats")
    public void stats(BukkitCommandActor actor) {
        Player player = actor.requirePlayer();

        PlayerStats stats = RegrowthPinata.getInstance().getPlayerStatsService().getStats(player);
        RegrowthPinata.getInstance().getMessageManager().sendMessageList(player, "stats.header");
        RegrowthPinata.getInstance().getMessageManager().sendMessage(player, "stats.total-damage", "%damage%", String.valueOf(stats.getTotalDamage()));
        RegrowthPinata.getInstance().getMessageManager().sendMessage(player, "stats.pinata-kills", "%kills%", String.valueOf(stats.getPinataKills()));
        RegrowthPinata.getInstance().getMessageManager().sendMessageList(player, "stats.footer");
    }

    // TODO: Migrate
    @Subcommand("stats reset")
    @CommandPermission("pinata.command.stats.reset")
    public void statsReset() {}

    // TODO: Migrate
    @Subcommand("stats top")
    @CommandPermission("pinata.command.stats.top")
    public void statsTop() {}
}
