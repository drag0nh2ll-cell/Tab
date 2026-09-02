package com.healthtab.plugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;

/**
 * Every online player gets their OWN Scoreboard object. That is what lets us
 * turn the "health in tab" feature on/off per viewer/per-world/per-server
 * without affecting anyone else, while the LuckPerms prefix/suffix teams are
 * mirrored identically onto every player's board so nametags/tab look the
 * same for everybody.
 *
 * Health uses the vanilla "health" scoreboard criteria, which Minecraft
 * updates automatically -- we never have to manually push new numbers.
 */
public class ScoreboardManager {

    private static final String HEALTH_OBJECTIVE = "ht_health";

    private final HealthTabPlugin plugin;
    private final Map<Player, Scoreboard> boards = new HashMap<>();

    public ScoreboardManager(HealthTabPlugin plugin) {
        this.plugin = plugin;
    }

    /** Creates a fresh personal scoreboard for a player who just joined. */
    public void createBoardFor(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        boards.put(player, board);
        player.setScoreboard(board);

        // Populate teams for every currently-online player (including self).
        for (Player online : Bukkit.getOnlinePlayers()) {
            applyTeam(board, online);
        }

        setupHealthObjective(player);
    }

    public void removeBoard(Player player) {
        boards.remove(player);
    }

    /** Call when a player's prefix/suffix may have changed (join, permission update, world/server switch). */
    public void refreshTeamEntryEverywhere(Player target) {
        for (Map.Entry<Player, Scoreboard> entry : boards.entrySet()) {
            applyTeam(entry.getValue(), target);
        }
    }

    /** Removes a player's entry from everyone else's board, e.g. on quit. */
    public void removeTeamEntryEverywhere(Player target) {
        for (Scoreboard board : boards.values()) {
            Team team = board.getTeam(teamName(target));
            if (team != null) {
                team.unregister();
            }
        }
    }

    private void applyTeam(Scoreboard board, Player target) {
        String name = teamName(target);
        Team team = board.getTeam(name);
        if (team == null) {
            team = board.registerNewTeam(name);
        }

        String prefix = plugin.getLuckPermsHook().getPrefix(target);
        String suffix = plugin.getLuckPermsHook().getSuffix(target);

        team.setPrefix(colorize(truncate(prefix, 16)));
        team.setSuffix(colorize(truncate(suffix, 16)));
        team.setAllowFriendlyFire(true);
        team.setCanSeeFriendlyInvisibles(false);

        if (!team.hasPlayer(target)) {
            team.addPlayer(target);
        }
    }

    /**
     * Team names control tab-list sort order (alphabetical). We prefix with a
     * zero-padded, inverted weight so higher LuckPerms weight sorts first.
     */
    private String teamName(Player target) {
        int weight = plugin.getLuckPermsHook().getWeight(target);
        int inverted = Math.max(0, 9999 - weight);
        String raw = String.format("%04d", inverted) + target.getName();
        return truncate(raw, 16);
    }

    // ---- Health-in-tab objective ----

    /** (Re)configures the health objective visibility for one player based on current config. */
    public void setupHealthObjective(Player player) {
        Scoreboard board = boards.get(player);
        if (board == null) return;

        boolean shouldShow = plugin.getConfigManager().shouldShowHealth(player.getWorld().getName());

        Objective objective = board.getObjective(HEALTH_OBJECTIVE);

        if (shouldShow) {
            if (objective == null) {
                objective = board.registerNewObjective(HEALTH_OBJECTIVE, "health");
            }
            objective.setDisplayName(colorize(plugin.getConfigManager().getHealthFormat().replace("%health%", "").trim().isEmpty()
                    ? "\u2764"
                    : plugin.getConfigManager().getHealthFormat().replace("%health%", "").trim()));
            objective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        } else {
            if (objective != null) {
                objective.unregister();
            }
        }
    }

    /** Refresh the health objective for every online player -- used after a global/world toggle changes. */
    public void refreshAllHealthObjectives() {
        for (Player player : boards.keySet()) {
            setupHealthObjective(player);
        }
    }

    // ---- utils ----

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }

    private String colorize(String s) {
        if (s == null) return "";
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', s);
    }
}
