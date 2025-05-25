package org.avo.diamondAVO;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {
    private final DiamondAVO plugin;
    private File configFile;
    private FileConfiguration config;
    private File scoreFile;
    private FileConfiguration scoreConfig;
    private File commandsFile;
    private FileConfiguration commandsConfig;

    public ConfigManager(DiamondAVO plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    private void loadConfigs() {
        // config.yml
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        // sco.yml
        scoreFile = new File(plugin.getDataFolder(), "sco.yml");
        if (!scoreFile.exists()) {
            plugin.saveResource("sco.yml", false);
        }
        scoreConfig = YamlConfiguration.loadConfiguration(scoreFile);

        // commands.yml
        commandsFile = new File(plugin.getDataFolder(), "commands.yml");
        if (!commandsFile.exists()) {
            plugin.saveResource("commands.yml", false);
        }
        commandsConfig = YamlConfiguration.loadConfiguration(commandsFile);
    }

    public void reloadConfigs() {
        loadConfigs();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getScoreConfig() {
        return scoreConfig;
    }

    public FileConfiguration getCommandsConfig() {
        return commandsConfig;
    }

    public void saveScoreConfig() {
        try {
            scoreConfig.save(scoreFile);
        } catch (IOException e) {
            plugin.getLogger().severe("ไม่สามารถบันทึก sco.yml: " + e.getMessage());
        }
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("ไม่สามารถบันทึก config.yml: " + e.getMessage());
        }
    }
}