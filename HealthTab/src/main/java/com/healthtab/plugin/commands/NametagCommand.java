package com.healthtab.plugin.commands;

import com.healthtab.plugin.HealthTabPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * /nametag <player>                -> shows their current title
 * /nametag <player> <title text>   -> sets the floating title under their nametag
 * /nametag <player> remove         -> clears it
 */
public class NametagCommand implements CommandExecutor {

    private final HealthTabPlugin plugin;

    public NametagCommand(HealthTabPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("healthtab.nametag")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <player> <title text>");
            sender.sendMessage(ChatColor.RED + "       /" + label + " <player> remove");
            return true;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null || (target.getName() == null && !target.hasPlayedBefore())) {
            sender.sendMessage(ChatColor.RED + "That player has never joined this server.");
            return true;
        }
        UUID uuid = target.getUniqueId();

        if (args.length == 1) {
            String current = plugin.getConfigManager().getTitle(uuid);
            sender.sendMessage(ChatColor.YELLOW + args[0] + "'s current title: " + ChatColor.RESET
                    + (current == null || current.isEmpty()
                        ? ChatColor.GRAY + "(none)"
                        : ChatColor.translateAlternateColorCodes('&', current)));
            return true;
        }

        if (args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("clear")) {
            plugin.getConfigManager().removeTitle(uuid);
            plugin.getTitleManager().removeStand(uuid);
            sender.sendMessage(ChatColor.GREEN + "Removed the title for " + args[0] + ".");
            return true;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            sb.append(args[i]);
            if (i < args.length - 1) sb.append(" ");
        }
        String title = sb.toString();
        plugin.getConfigManager().setTitle(uuid, title);

        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            plugin.getTitleManager().applyTitle(online);
        }

        sender.sendMessage(ChatColor.GREEN + "Set title for " + args[0] + " to: "
                + ChatColor.translateAlternateColorCodes('&', title));
        return true;
    }
}
