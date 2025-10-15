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
    
    public HomeManager(ZenithHomes plugin) {
        this.plugin = plugin;
    }
    
    public CompletableFuture<Boolean> setHome(Player player, String homeName) {
        return CompletableFuture.supplyAsync(() -> {
            Location location = player.getLocation();
            
            String sql = "INSERT OR REPLACE INTO zenith_homes (player_uuid, player_name, home_name, world, x, y, z, yaw, pitch) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setString(2, player.getName());
                stmt.setString(3, homeName.toLowerCase());
                stmt.setString(4, location.getWorld().getName());
                stmt.setDouble(5, location.getX());
                stmt.setDouble(6, location.getY());
                stmt.setDouble(7, location.getZ());
                stmt.setFloat(8, location.getYaw());
                stmt.setFloat(9, location.getPitch());
                
                return stmt.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to set home: " + e.getMessage());
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> deleteHome(Player player, String homeName) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM zenith_homes WHERE player_uuid = ? AND home_name = ?";
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setString(2, homeName.toLowerCase());
                
                return stmt.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to delete home: " + e.getMessage());
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> deleteHome(UUID playerUUID, String homeName) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM zenith_homes WHERE player_uuid = ? AND home_name = ?";
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, playerUUID.toString());
                stmt.setString(2, homeName.toLowerCase());
                
                return stmt.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to delete home: " + e.getMessage());
                return false;
            }
        });
    }
    
    public CompletableFuture<List<Home>> getHomes(Player player) {
        return getHomes(player.getUniqueId().toString());
    }
    
    public CompletableFuture<List<Home>> getHomes(String playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            List<Home> homes = new ArrayList<>();
            String sql = "SELECT * FROM zenith_homes WHERE player_uuid = ? ORDER BY home_name";
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, playerUUID);
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
                plugin.getLogger().severe("Failed to get homes: " + e.getMessage());
            }
            
            return homes;
        });
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
            // Check permission-based limits first
            for (int i = 100; i >= 1; i--) {
                if (player.hasPermission("zenithhomes.limit." + i)) {
                    return i;
                }
            }
            
            // Check database for custom limit
            String sql = "SELECT home_limit FROM zenith_player_data WHERE player_uuid = ?";
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, player.getUniqueId().toString());
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    return rs.getInt("home_limit");
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get home limit: " + e.getMessage());
            }
            
            // Return default limit
            return (int) plugin.getConfigManager().getSetting("homes.default-limit");
        });
    }
    
    public void setHomeLimit(UUID playerUUID, int limit) {
        CompletableFuture.runAsync(() -> {
            String sql = "INSERT OR REPLACE INTO zenith_player_data (player_uuid, player_name, home_limit) " +
                        "VALUES (?, ?, ?)";
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                // We need to get player name first
                String playerName = getPlayerName(playerUUID);
                
                stmt.setString(1, playerUUID.toString());
                stmt.setString(2, playerName);
                stmt.setInt(3, limit);
                
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to set home limit: " + e.getMessage());
            }
        });
    }
    
    private String getPlayerName(UUID playerUUID) {
        String sql = "SELECT player_name FROM zenith_homes WHERE player_uuid = ? LIMIT 1";
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("player_name");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get player name: " + e.getMessage());
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
        long lastTeleport = teleportCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long cooldown = (long) plugin.getConfigManager().getSetting("homes.teleport-cooldown") * 1000;
        long remaining = (lastTeleport + cooldown) - System.currentTimeMillis();
        
        return Math.max(0, remaining / 1000);
    }
    
    public void setLastTeleport(Player player) {
        teleportCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    public void setTeleportLocation(Player player, Location location) {
        teleportLocations.put(player.getUniqueId(), location);
    }
    
    public Location getTeleportLocation(Player player) {
        return teleportLocations.get(player.getUniqueId());
    }
    
    public void removeTeleportLocation(Player player) {
        teleportLocations.remove(player.getUniqueId());
    }
}