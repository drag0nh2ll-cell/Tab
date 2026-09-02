package com.healthtab.plugin;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Thin wrapper around the LuckPerms API so the rest of the plugin never has to
 * deal with LuckPerms directly. If LuckPerms isn't installed, everything here
 * degrades gracefully to empty strings instead of throwing.
 */
public class LuckPermsHook {

    private final HealthTabPlugin plugin;
    private LuckPerms luckPerms;
    private boolean enabled;

    public LuckPermsHook(HealthTabPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            plugin.getLogger().warning("LuckPerms not found! Prefixes/suffixes will be empty.");
            enabled = false;
            return false;
        }
        try {
            this.luckPerms = LuckPermsProvider.get();
            enabled = true;
            return true;
        } catch (IllegalStateException e) {
            plugin.getLogger().warning("LuckPerms API not ready yet: " + e.getMessage());
            enabled = false;
            return false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Subscribes to LuckPerms' UserDataRecalculateEvent so tab prefixes/suffixes
     * update live the instant an admin changes someone's group/permissions,
     * with no relog required.
     */
    public void subscribeToDataChanges(java.util.function.Consumer<UUID> onChange) {
        if (!enabled) return;
        luckPerms.getEventBus().subscribe(
                plugin,
                net.luckperms.api.event.user.UserDataRecalculateEvent.class,
                event -> {
                    UUID uuid = event.getUser().getUniqueId();
                    Bukkit.getScheduler().runTask(plugin, () -> onChange.accept(uuid));
                }
        );
    }

    public String getPrefix(Player player) {
        if (!enabled) return "";
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return "";
        CachedMetaData metaData = user.getCachedData().getMetaData();
        String prefix = metaData.getPrefix();
        return prefix == null ? "" : prefix;
    }

    public String getSuffix(Player player) {
        if (!enabled) return "";
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return "";
        CachedMetaData metaData = user.getCachedData().getMetaData();
        String suffix = metaData.getSuffix();
        return suffix == null ? "" : suffix;
    }

    /**
     * Weight of the player's primary group, used to sort the tab list /
     * team priority. Higher weight = higher rank. Falls back to 0.
     */
    public int getWeight(Player player) {
        if (!enabled) return 0;
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return 0;
        return user.getCachedData().getMetaData().getWeight().orElse(0);
    }
}
