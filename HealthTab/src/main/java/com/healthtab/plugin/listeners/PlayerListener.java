package com.healthtab.plugin.listeners;

import com.healthtab.plugin.HealthTabPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerListener implements Listener {

    private final HealthTabPlugin plugin;

    public PlayerListener(HealthTabPlugin plugin) {
        this.plugin = plugin;

        // Live-refresh prefixes/suffixes the moment LuckPerms recalculates a user
        // (group change, permission edit, etc.) -- no relog needed.
        plugin.getLuckPermsHook().subscribeToDataChanges(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                plugin.getScoreboardManager().refreshTeamEntryEverywhere(player);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Run one tick later so LuckPerms has definitely finished loading this user's data.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            plugin.getScoreboardManager().createBoardFor(player);
            plugin.getScoreboardManager().refreshTeamEntryEverywhere(player);
            plugin.getTitleManager().applyTitle(player);
        }, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getScoreboardManager().removeTeamEntryEverywhere(player);
        plugin.getScoreboardManager().removeBoard(player);
        plugin.getTitleManager().removeStand(player.getUniqueId());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        // Health-in-tab visibility can differ per world -- re-evaluate it.
        plugin.getScoreboardManager().setupHealthObjective(player);
        // Title follows the player automatically via TitleManager's tick task,
        // but rebuild immediately so it doesn't lag behind on world change.
        plugin.getTitleManager().applyTitle(player);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getTitleManager().applyTitle(player);
            }
        }, 2L);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        // Hide the floating title while dead; it'll be re-created on respawn.
        plugin.getTitleManager().removeStand(event.getEntity().getUniqueId());
    }
}
