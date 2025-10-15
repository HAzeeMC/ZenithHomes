package com.zyromc.zenithhomes.managers;

import com.zyromc.zenithhomes.ZenithHomes;
import com.zyromc.zenithhomes.utils.Home;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class HomeManager {
    
    private final ZenithHomes plugin;
    private final Map<UUID, Long> teleportCooldowns = new HashMap<>();
    private final Map<UUID, Location> teleportLocations = new HashMap<>();
    private final Map<UUID, Integer> customHomeLimits = new HashMap<>();
    
    public HomeManager(ZenithHomes plugin) {
        this.plugin = plugin;
    }
    
    public CompletableFuture<Boolean> setHome(Player player, String homeName) {
        return CompletableFuture.supplyAsync(() -> {
            Location location = player.getLocation();
            UUID playerUUID = player.getUniqueId();
            String playerName = player.getName();
            
            String sql = "INSERT OR REPLACE INTO zenith_homes (player_uuid, player_name, home_name, world, x, y, z, yaw, pitch) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, playerUUID.toString());
                stmt.setString(2, playerName);
                stmt.setString(3, homeName.toLowerCase());
                stmt.setString(4, location.getWorld().getName());
                stmt.setDouble(5, location.getX());
                stmt.setDouble(6, location.getY());
                stmt.setDouble(7, location.getZ());
                stmt.setFloat(8, location.getYaw());
                stmt.setFloat(9, location.getPitch());
                
                int result = stmt.executeUpdate();
                return result > 0;
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to set home for player " + playerName + ": " + e.getMessage());
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> deleteHome(Player player, String homeName) {
        return deleteHome(player.getUniqueId(), homeName);
    }
    
    public CompletableFuture<Boolean> deleteHome(UUID playerUUID, String homeName) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM zenith_homes WHERE player_uuid = ? AND home_name = ?";
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, playerUUID.toString());
                stmt.setString(2, homeName.toLowerCase());
                
                int result = stmt.executeUpdate();
                return result > 0;
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to delete home " + homeName + " for player " + playerUUID + ": " + e.getMessage());
                return false;
            }
        });
    }
    
    public CompletableFuture<List<Home>> getHomes(Player player) {
        return getHomes(player.getUniqueId());
    }
    
    public CompletableFuture<List<Home>> getHomes(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            List<Home> homes = new ArrayList<>();
            String sql = "SELECT * FROM zenith_homes WHERE player_uuid = ? ORDER BY home_name";
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, playerUUID.toString());
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    homes.add(new Home(
                        rs.getString("home_name"),
                        rs.getString("player_uuid"),
                        rs.getString("player_name"),
                        rs.getString("world"),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch"),
                        rs.getTimestamp("created_at")
                    ));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get homes for player " + playerUUID + ": " + e.getMessage());
            }
            
            return homes;
        });
    }
    
    public CompletableFuture<List<Home>> getHomes(String playerUUID) {
        return getHomes(UUID.fromString(playerUUID));
    }
    
    public CompletableFuture<List<Home>> getAllHomes() {
        return CompletableFuture.supplyAsync(() -> {
            List<Home> homes = new ArrayList<>();
            String sql = "SELECT * FROM zenith_homes ORDER BY player_name, home_name";
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    homes.add(new Home(
                        rs.getString("home_name"),
                        rs.getString("player_uuid"),
                        rs.getString("player_name"),
                        rs.getString("world"),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch"),
                        rs.getTimestamp("created_at")
                    ));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get all homes: " + e.getMessage());
            }
            
            return homes;
        });
    }
    
    public CompletableFuture<Integer> getHomeLimit(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            UUID playerUUID = player.getUniqueId();
            
            // Check if player has bypass permission
            if (player.hasPermission("zenithhomes.bypass.limit")) {
                return Integer.MAX_VALUE;
            }
            
            // Check permission-based limits first (zenithhomes.limit.10, zenithhomes.limit.20, etc.)
            int maxPermissionLimit = 0;
            for (int i = 100; i >= 1; i--) {
                if (player.hasPermission("zenithhomes.limit." + i)) {
                    maxPermissionLimit = i;
                    break;
                }
            }
            
            if (maxPermissionLimit > 0) {
                return maxPermissionLimit;
            }
            
            // Check database for custom limit
            String sql = "SELECT home_limit FROM zenith_player_data WHERE player_uuid = ?";
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, playerUUID.toString());
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    int dbLimit = rs.getInt("home_limit");
                    if (dbLimit > 0) {
                        return dbLimit;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to get home limit from database for player " + player.getName() + ": " + e.getMessage());
            }
            
            // Return default limit from config
            return (int) plugin.getConfigManager().getSetting("homes.default-limit");
        });
    }
    
    public void setHomeLimit(UUID playerUUID, int limit) {
        CompletableFuture.runAsync(() -> {
            String playerName = getPlayerName(playerUUID);
            
            String sql = "INSERT OR REPLACE INTO zenith_player_data (player_uuid, player_name, home_limit) " +
                        "VALUES (?, ?, ?)";
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, playerUUID.toString());
                stmt.setString(2, playerName);
                stmt.setInt(3, limit);
                
                stmt.executeUpdate();
                
                // Update cache
                customHomeLimits.put(playerUUID, limit);
                
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to set home limit for player " + playerName + ": " + e.getMessage());
            }
        });
    }
    
    public CompletableFuture<Boolean> hasHome(Player player, String homeName) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT 1 FROM zenith_homes WHERE player_uuid = ? AND home_name = ?";
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setString(2, homeName.toLowerCase());
                
                ResultSet rs = stmt.executeQuery();
                return rs.next();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to check if home exists: " + e.getMessage());
                return false;
            }
        });
    }
    
    public CompletableFuture<Integer> getTotalHomesCount() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) as total FROM zenith_homes";
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                if (rs.next()) {
                    return rs.getInt("total");
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get total homes count: " + e.getMessage());
            }
            
            return 0;
        });
    }
    
    public CompletableFuture<Integer> getPlayerHomesCount(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) as count FROM zenith_homes WHERE player_uuid = ?";
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, playerUUID.toString());
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    return rs.getInt("count");
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get player homes count: " + e.getMessage());
            }
            
            return 0;
        });
    }
    
    private String getPlayerName(UUID playerUUID) {
        // Try to get from homes table first
        String sql = "SELECT player_name FROM zenith_homes WHERE player_uuid = ? LIMIT 1";
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("player_name");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get player name from homes: " + e.getMessage());
        }
        
        // Try to get from player data table
        sql = "SELECT player_name FROM zenith_player_data WHERE player_uuid = ?";
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("player_name");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get player name from player data: " + e.getMessage());
        }
        
        return "Unknown";
    }
    
    public boolean canTeleport(Player player) {
        if (player.hasPermission("zenithhomes.bypass.cooldown")) {
            return true;
        }
        
        long lastTeleport = teleportCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long cooldown = (long) plugin.getConfigManager().getSetting("homes.teleport-cooldown") * 1000;
        
        return System.currentTimeMillis() - lastTeleport >= cooldown;
    }
    
    public long getRemainingCooldown(Player player) {
        if (player.hasPermission("zenithhomes.bypass.cooldown")) {
            return 0;
        }
        
        long lastTeleport = teleportCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long cooldown = (long) plugin.getConfigManager().getSetting("homes.teleport-cooldown") * 1000;
        long remaining = (lastTeleport + cooldown) - System.currentTimeMillis();
        
        return Math
