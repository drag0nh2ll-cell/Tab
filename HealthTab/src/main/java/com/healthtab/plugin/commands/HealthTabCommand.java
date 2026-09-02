package com.healthtab.plugin.commands;

import com.healthtab.plugin.HealthTabPlugin;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * /healthtab toggle
 * /healthtab world <world> <on|off>
 * /healthtab format <text with %health%>
 * /healthtab reload
 * /healthtab status
 */
public class HealthTabCommand implements CommandExecutor {

    private final HealthTabPlugin plugin;

    public HealthTabCommand(HealthTabPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("healthtab.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "toggle": {
                boolean newState = !plugin.getConfigManager().isHealthEnabledThisServer();
                plugin.getConfigManager().setHealthEnabledThisServer(newState);
                plugin.getScoreboardManager().refreshAllHealthObjectives();
                sender.sendMessage(ChatColor.GREEN + "Health display on this server ("
                        + plugin.getConfigManager().getServerName() + ") is now "
                        + (newState ? ChatColor.AQUA + "ENABLED" : ChatColor.RED + "DISABLED") + ChatColor.GREEN + ".");
                return true;
            }

            case "world": {
                if (args.length < 3 || !(args[2].equalsIgnoreCase("on") || args[2].equalsIgnoreCase("off"))) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " world <world> <on|off>");
                    return true;
                }
                String worldName = args[1];
                World world = plugin.getServer().getWorld(worldName);
                if (world == null) {
                    sender.sendMessage(ChatColor.YELLOW + "Note: world '" + worldName
                            + "' isn't currently loaded on this server, but the setting will still be saved.");
                }
                boolean disable = args[2].equalsIgnoreCase("off");
                plugin.getConfigManager().setWorldDisabled(worldName, disable);
                plugin.getScoreboardManager().refreshAllHealthObjectives();
                sender.sendMessage(ChatColor.GREEN + "Health display in world '" + worldName + "' is now "
                        + (disable ? ChatColor.RED + "DISABLED" : ChatColor.AQUA + "ENABLED") + ChatColor.GREEN + ".");
                return true;
            }

            case "format": {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " format <text, use %health% as placeholder>");
                    sender.sendMessage(ChatColor.GRAY + "Example: /" + label + " format &c%health%\u2764");
                    return true;
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    sb.append(args[i]);
                    if (i < args.length - 1) sb.append(" ");
                }
                plugin.getConfigManager().setHealthFormat(sb.toString());
                plugin.getScoreboardManager().refreshAllHealthObjectives();
                sender.sendMessage(ChatColor.GREEN + "Health format updated to: "
                        + ChatColor.translateAlternateColorCodes('&', sb.toString()));
                return true;
            }

            case "reload": {
                plugin.getConfigManager().load();
                plugin.getScoreboardManager().refreshAllHealthObjectives();
                sender.sendMessage(ChatColor.GREEN + "HealthTab config.yml and titles.yml reloaded.");
                return true;
            }

            case "status": {
                sender.sendMessage(ChatColor.GOLD + "--- HealthTab status (" + plugin.getConfigManager().getServerName() + ") ---");
                sender.sendMessage(ChatColor.YELLOW + "Server-wide health display: "
                        + (plugin.getConfigManager().isHealthEnabledThisServer() ? ChatColor.AQUA + "ENABLED" : ChatColor.RED + "DISABLED"));
                sender.sendMessage(ChatColor.YELLOW + "Health format: " + ChatColor.RESET
                        + ChatColor.translateAlternateColorCodes('&', plugin.getConfigManager().getHealthFormat()));
                sender.sendMessage(ChatColor.YELLOW + "LuckPerms hook: "
                        + (plugin.getLuckPermsHook().isEnabled() ? ChatColor.AQUA + "connected" : ChatColor.RED + "not found"));
                return true;
            }

            default:
                sendHelp(sender, label);
                return true;
        }
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "--- HealthTab commands ---");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " toggle" + ChatColor.GRAY + " - enable/disable health display on this server");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " world <world> <on|off>" + ChatColor.GRAY + " - enable/disable health display in one world");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " format <text>" + ChatColor.GRAY + " - set the tab health format, use %health%");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload" + ChatColor.GRAY + " - reload config.yml and titles.yml");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " status" + ChatColor.GRAY + " - show current settings");
    }
}
