package com.zyromc.zenithhomes.database;

import com.zyromc.zenithhomes.ZenithHomes;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {
    
    private final ZenithHomes plugin;
    private HikariDataSource dataSource;
    
    public DatabaseManager(ZenithHomes plugin) {
        this.plugin = plugin;
    }
    
    public boolean initialize() {
        String type = plugin.getConfigManager().getSetting("database.type").toString();
        
        HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        try {
            if (type.equalsIgnoreCase("MYSQL")) {
                String host = plugin.getConfigManager().getSetting("database.host").toString();
                int port = (int) plugin.getConfigManager().getSetting("database.port");
                String database = plugin.getConfigManager().getSetting("database.name").toString();
                String username = plugin.getConfigManager().getSetting("database.username").toString();
                String password = plugin.getConfigManager().getSetting("database.password").toString();
                
                config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
                config.setUsername(username);
                config.setPassword(password);
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            } else {
                // SQLite
                config.setJdbcUrl("jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/homes.db");
                config.setDriverClassName("org.sqlite.JDBC");
            }
            
            dataSource = new HikariDataSource(config);
            createTables();
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database", e);
            return false;
        }
    }
    
    private void createTables() {
        String createHomesTable = "CREATE TABLE IF NOT EXISTS zenith_homes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "player_uuid VARCHAR(36) NOT NULL," +
                "player_name VARCHAR(16) NOT NULL," +
                "home_name VARCHAR(32) NOT NULL," +
                "world VARCHAR(255) NOT NULL," +
                "x DOUBLE NOT NULL," +
                "y DOUBLE NOT NULL," +
                "z DOUBLE NOT NULL," +
                "yaw FLOAT NOT NULL," +
                "pitch FLOAT NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "UNIQUE(player_uuid, home_name)" +
                ");";
        
        String createPlayerDataTable = "CREATE TABLE IF NOT EXISTS zenith_player_data (" +
                "player_uuid VARCHAR(36) PRIMARY KEY," +
                "player_name VARCHAR(16) NOT NULL," +
                "home_limit INTEGER DEFAULT 5," +
                "language VARCHAR(8) DEFAULT 'en'," +
                "last_teleport BIGINT DEFAULT 0" +
                ");";
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createHomesTable);
            stmt.execute(createPlayerDataTable);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create tables", e);
        }
    }
    
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}