package com.zyromc.zenithhomes.managers;

import com.zyromc.zenithhomes.ZenithHomes;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LanguageManager {
    
    private final ZenithHomes plugin;
    private final Map<String, FileConfiguration> languages = new HashMap<>();
    private final Map<UUID, String> playerLanguages = new HashMap<>();
    private String defaultLanguage;
    
    public LanguageManager(ZenithHomes plugin) {
        this.plugin = plugin;
    }
    
    public void loadLanguages() {
        File langsFolder = new File(plugin.getDataFolder(), "langs");
        if (!langsFolder.exists()) {
            langsFolder.mkdirs();
        }
        
        // Save default language files
        saveDefaultLanguage("vi.yml");
        saveDefaultLanguage("en.yml");
        
        // Load all language files
        for (File file : langsFolder.listFiles((dir, name) -> name.endsWith(".yml"))) {
            String langName = file.getName().replace(".yml", "");
            languages.put(langName, YamlConfiguration.loadConfiguration(file));
        }
        
        defaultLanguage = plugin.getConfig().getString("default-language", "en");
    }
    
    private void saveDefaultLanguage(String fileName) {
        File file = new File(plugin.getDataFolder(), "langs/" + fileName);
        if (!file.exists()) {
            plugin.saveResource("langs/" + fileName, false);
        }
    }
    
    public String getMessage(Player player, String path) {
        String lang = playerLanguages.getOrDefault(player.getUniqueId(), defaultLanguage);
        FileConfiguration langConfig = languages.get(lang);
        
        if (langConfig == null) {
            langConfig = languages.get(defaultLanguage);
        }
        
        String message = langConfig.getString(path);
        if (message == null) {
            // Fallback to English
            FileConfiguration enConfig = languages.get("en");
            message = enConfig != null ? enConfig.getString(path) : "Message not found: " + path;
        }
        
        return colorize(message);
    }
    
    public String getMessage(String language, String path) {
        FileConfiguration langConfig = languages.get(language);
        
        if (langConfig == null) {
            langConfig = languages.get(defaultLanguage);
        }
        
        String message = langConfig.getString(path);
        if (message == null) {
            FileConfiguration enConfig = languages.get("en");
            message = enConfig != null ? enConfig.getString(path) : "Message not found: " + path;
        }
        
        return colorize(message);
    }
    
    public void setPlayerLanguage(Player player, String language) {
        if (languages.containsKey(language)) {
            playerLanguages.put(player.getUniqueId(), language);
        }
    }
    
    public String getPlayerLanguage(Player player) {
        return playerLanguages.getOrDefault(player.getUniqueId(), defaultLanguage);
    }
    
    private String colorize(String text) {
        return text.replace('&', '§');
    }
}