package com.zyromc.zenithhomes;

import com.zyromc.zenithhomes.commands.AdminCommand;
import com.zyromc.zenithhomes.commands.HomeCommand;
import com.zyromc.zenithhomes.database.DatabaseManager;
import com.zyromc.zenithhomes.gui.GUIListener;
import com.zyromc.zenithhomes.listeners.TeleportListener;
import com.zyromc.zenithhomes.managers.ConfigManager;
import com.zyromc.zenithhomes.managers.HomeManager;
import com.zyromc.zenithhomes.managers.LanguageManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class ZenithHomes extends JavaPlugin {
    
    private static ZenithHomes instance;
    private DatabaseManager databaseManager;
    private HomeManager homeManager;
    private ConfigManager configManager;
    private LanguageManager languageManager;
    private Logger logger;
    
    @Override
    public void onEnable() {
        instance = this;
        this.logger = getLogger();
        
        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.languageManager = new LanguageManager(this);
        this.databaseManager = new DatabaseManager(this);
        this.homeManager = new HomeManager(this);
        
        // Load configurations
        configManager.loadConfigs();
        languageManager.loadLanguages();
        
        // Initialize database
        if (!databaseManager.initialize()) {
            getLogger().severe("Failed to initialize database! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Register commands
        registerCommands();
        
        // Register listeners
        registerListeners();
        
        logger.info("ZenithHomes has been enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        logger.info("ZenithHomes has been disabled!");
    }
    
    private void registerCommands() {
        new HomeCommand(this);
        new AdminCommand(this);
    }
    
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new TeleportListener(this), this);
    }
    
    public static ZenithHomes getInstance() {
        return instance;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public HomeManager getHomeManager() {
        return homeManager;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public LanguageManager getLanguageManager() {
        return languageManager;
    }
}