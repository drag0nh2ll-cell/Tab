package com.healthtab.plugin;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Renders a small floating text ("title") just below a player's nametag by
 * riding an invisible, harmless ArmorStand along with them and updating its
 * position every couple of ticks. This works on vanilla 1.8 without any
 * NMS/packet hacks.
 */
public class TitleManager {

    // Height above the player's feet, tuned to sit just BELOW the player's
    // real nametag (which vanilla renders ~2.3-2.4 blocks above the feet).
    // Tweak in config.yml under "title.y-offset" if it looks off with your
    // resource pack / player model.
    private double yOffset = 1.95;

    private final HealthTabPlugin plugin;
    private final Map<UUID, ArmorStand> stands = new HashMap<>();
    private BukkitTask followTask;

    public TitleManager(HealthTabPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        this.yOffset = plugin.getConfig().getDouble("title.y-offset", 1.95);
        // Re-position every stand every 2 ticks so it tracks its owner smoothly
        // and never visibly falls due to gravity.
        followTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<UUID, ArmorStand> entry : stands.entrySet()) {
                Player owner = plugin.getServer().getPlayer(entry.getKey());
                ArmorStand stand = entry.getValue();
                if (owner == null || !owner.isOnline() || stand == null || stand.isDead()) {
                    continue;
                }
                if (!owner.getWorld().equals(stand.getWorld())) {
                    // Player changed world -- rebuild the stand there instead of teleporting cross-world.
                    respawnFor(owner);
                    continue;
                }
                stand.teleport(standLocation(owner));
            }
        }, 1L, 2L);
    }

    public void stop() {
        if (followTask != null) {
            followTask.cancel();
        }
        for (ArmorStand stand : stands.values()) {
            if (stand != null && !stand.isDead()) {
                stand.remove();
            }
        }
        stands.clear();
    }

    /** Call on join, and whenever a title is set/changed while the player is online. */
    public void applyTitle(Player player) {
        String title = plugin.getConfigManager().getTitle(player.getUniqueId());
        removeStand(player.getUniqueId());
        if (title == null || title.isEmpty()) {
            return;
        }
        ArmorStand stand = player.getWorld().spawn(standLocation(player), ArmorStand.class);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setBasePlate(false);
        stand.setArms(false);
        stand.setSmall(true);
        stand.setCustomName(ChatColor.translateAlternateColorCodes('&', title));
        stand.setCustomNameVisible(true);
        stand.setCanPickupItems(false);
        stand.setRemoveWhenFarAway(false);
        stands.put(player.getUniqueId(), stand);
    }

    public void removeStand(UUID uuid) {
        ArmorStand stand = stands.remove(uuid);
        if (stand != null && !stand.isDead()) {
            stand.remove();
        }
    }

    private void respawnFor(Player player) {
        String title = plugin.getConfigManager().getTitle(player.getUniqueId());
        removeStand(player.getUniqueId());
        if (title != null && !title.isEmpty()) {
            applyTitle(player);
        }
    }

    private Location standLocation(Player player) {
        Location loc = player.getLocation().clone();
        loc.setY(loc.getY() + yOffset);
        return loc;
    }
}
