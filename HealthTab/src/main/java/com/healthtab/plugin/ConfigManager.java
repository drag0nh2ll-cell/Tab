package com.healthtab.plugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles config.yml (toggles / formatting) and titles.yml (persisted per-player
 * nametag titles) so everything survives a restart.
 */
public class ConfigManager {

    private final HealthTabPlugin plugin;

    private File configFile;
    private FileConfiguration config;

    private File titlesFile;
    private FileConfiguration titlesConfig;

    // Runtime state
    private boolean healthEnabledThisServer;
    private final Set<String> disabledWorlds = new HashSet<>();
    private final Map<UUID, String> playerTitles = new HashMap<>();

    private String healthFormat;
    private String serverName;

    public ConfigManager(HealthTabPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.config = YamlConfiguration.loadConfiguration(configFile);

        this.titlesFile = new File(plugin.getDataFolder(), "titles.yml");
        if (!titlesFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                titlesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create titles.yml: " + e.getMessage());
            }
        }
        this.titlesConfig = YamlConfiguration.loadConfiguration(titlesFile);

        // Identifies THIS server/instance for the /healthtab server on|off toggle.
        // On a BungeeCord network each backend server has its own config.yml,
        // so this is effectively "this server's" switch.
        this.serverName = config.getString("server-name", "this-server");

        this.healthEnabledThisServer = config.getBoolean("health-display.enabled-globally", true);

        disabledWorlds.clear();
        disabledWorlds.addAll(config.getStringList("health-display.disabled-worlds"));

        this.healthFormat = config.getString("health-display.format", "&c%health%\u2764");

        playerTitles.clear();
        if (titlesConfig.isConfigurationSection("titles")) {
            for (String key : titlesConfig.getConfigurationSection("titles").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    playerTitles.put(uuid, titlesConfig.getString("titles." + key));
                } catch (IllegalArgumentException ignored) {
                    // malformed key, skip
                }
            }
        }
    }

    public void saveConfig() {
        try {
            config.set("server-name", serverName);
            config.set("health-display.enabled-globally", healthEnabledThisServer);
            config.set("health-display.disabled-worlds", new java.util.ArrayList<>(disabledWorlds));
            config.set("health-display.format", healthFormat);
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save config.yml: " + e.getMessage());
        }
    }

    public void saveTitles() {
        try {
            titlesConfig.set("titles", null);
            for (Map.Entry<UUID, String> entry : playerTitles.entrySet()) {
                titlesConfig.set("titles." + entry.getKey().toString(), entry.getValue());
            }
            titlesConfig.save(titlesFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save titles.yml: " + e.getMessage());
        }
    }

    // ---- Health toggle state ----

    public boolean isHealthEnabledThisServer() {
        return healthEnabledThisServer;
    }

    public void setHealthEnabledThisServer(boolean enabled) {
        this.healthEnabledThisServer = enabled;
        saveConfig();
    }

    public boolean isWorldDisabled(String worldName) {
        return disabledWorlds.contains(worldName.toLowerCase());
    }

    public void setWorldDisabled(String worldName, boolean disabled) {
        if (disabled) {
            disabledWorlds.add(worldName.toLowerCase());
        } else {
            disabledWorlds.remove(worldName.toLowerCase());
        }
        saveConfig();
    }

    /**
     * Final decision on whether health should show for a player in this world,
     * combining the global per-server switch and the per-world switch.
     */
    public boolean shouldShowHealth(String worldName) {
        return healthEnabledThisServer && !isWorldDisabled(worldName);
    }

    public String getHealthFormat() {
        return healthFormat;
    }

    public void setHealthFormat(String healthFormat) {
        this.healthFormat = healthFormat;
        saveConfig();
    }

    public String getServerName() {
        return serverName;
    }

    // ---- Nametag titles ----

    public String getTitle(UUID uuid) {
        return playerTitles.get(uuid);
    }

    public void setTitle(UUID uuid, String title) {
        playerTitles.put(uuid, title);
        saveTitles();
    }

    public void removeTitle(UUID uuid) {
        playerTitles.remove(uuid);
        saveTitles();
    }
}
