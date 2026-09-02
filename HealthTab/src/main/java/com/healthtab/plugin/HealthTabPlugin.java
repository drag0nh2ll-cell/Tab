package com.healthtab.plugin;

import com.healthtab.plugin.commands.HealthTabCommand;
import com.healthtab.plugin.commands.NametagCommand;
import com.healthtab.plugin.listeners.PlayerListener;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class HealthTabPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private LuckPermsHook luckPermsHook;
    private ScoreboardManager scoreboardManager;
    private TitleManager titleManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        configManager.load();

        this.luckPermsHook = new LuckPermsHook(this);
        if (!luckPermsHook.setup()) {
            getLogger().warning("Continuing without LuckPerms -- prefixes/suffixes will be blank until it's installed.");
        }

        this.scoreboardManager = new ScoreboardManager(this);
        this.titleManager = new TitleManager(this);
        titleManager.start();

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        getCommand("healthtab").setExecutor(new HealthTabCommand(this));
        getCommand("nametag").setExecutor(new NametagCommand(this));

        // In case of a /reload, wire up already-online players.
        for (Player player : getServer().getOnlinePlayers()) {
            scoreboardManager.createBoardFor(player);
            titleManager.applyTitle(player);
        }

        getLogger().info("HealthTab enabled.");
    }

    @Override
    public void onDisable() {
        if (titleManager != null) {
            titleManager.stop();
        }
        getLogger().info("HealthTab disabled.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LuckPermsHook getLuckPermsHook() {
        return luckPermsHook;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public TitleManager getTitleManager() {
        return titleManager;
    }
}
