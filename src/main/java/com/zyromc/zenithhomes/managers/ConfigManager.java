package com.zyromc.zenithhomes.managers;

import com.zyromc.zenithhomes.ZenithHomes;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    
    private final ZenithHomes plugin;
    private FileConfiguration config;
    private FileConfiguration homesConfig;
    private final Map<String, Object> settings = new HashMap<>();
    
    public ConfigManager(ZenithHomes plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfigs() {
        // Load main config
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        
        // Load homes.yml
        File homesFile = new File(plugin.getDataFolder(), "homes.yml");
        if (!homesFile.exists()) {
            plugin.saveResource("homes.yml", false);
        }
        homesConfig = YamlConfiguration.loadConfiguration(homesFile);
        
        loadSettings();
    }
    
    private void loadSettings() {
        // Database settings
        settings.put("database.type", config.getString("database.type", "SQLITE"));
        settings.put("database.host", config.getString("database.host", "localhost"));
        settings.put("database.port", config.getInt("database.port", 3306));
        settings.put("database.name", config.getString("database.name", "zenithhomes"));
        settings.put("database.username", config.getString("database.username", "username"));
        settings.put("database.password", config.getString("database.password", "password"));
        
        // Home settings
        settings.put("homes.default-limit", config.getInt("homes.default-limit", 5));
        settings.put("homes.max-name-length", config.getInt("homes.max-name-length", 16));
        settings.put("homes.teleport-cooldown", config.getInt("homes.teleport-cooldown", 5));
        settings.put("homes.teleport-delay", config.getInt("homes.teleport-delay", 3));
        settings.put("homes.cancel-on-move", config.getBoolean("homes.cancel-on-move", true));
        
        // GUI settings
        settings.put("gui.title", config.getString("gui.title", "&6Your Homes"));
        settings.put("gui.size", config.getInt("gui.size", 54));
        settings.put("gui.fill-empty-slots", config.getBoolean("gui.fill-empty-slots", true));
        settings.put("gui.empty-slot-material", config.getString("gui.empty-slot-material", "GRAY_STAINED_GLASS_PANE"));
        
        // Permission settings
        settings.put("permissions.home-limit-format", config.getString("permissions.home-limit-format", "zenithhomes.limit."));
    }
    
    public Object getSetting(String path) {
        return settings.get(path);
    }
    
    public String getString(String path) {
        return config.getString(path);
    }
    
    public int getInt(String path) {
        return config.getInt(path);
    }
    
    public boolean getBoolean(String path) {
        return config.getBoolean(path);
    }
    
    public FileConfiguration getHomesConfig() {
        return homesConfig;
    }
    
    public void saveHomesConfig() {
        try {
            File homesFile = new File(plugin.getDataFolder(), "homes.yml");
            homesConfig.save(homesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save homes.yml: " + e.getMessage());
        }
    }
    
    public void reloadConfigs() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        File homesFile = new File(plugin.getDataFolder(), "homes.yml");
        homesConfig = YamlConfiguration.loadConfiguration(homesFile);
        
        loadSettings();
    }
}