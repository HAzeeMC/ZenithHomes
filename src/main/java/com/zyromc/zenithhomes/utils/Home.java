package com.zyromc.zenithhomes.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.Timestamp;
import java.util.UUID;

public class Home {
    
    private final String name;
    private final UUID playerUUID;
    private final String playerName;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    private final Timestamp createdAt;
    
    public Home(String name, String playerUUID, String playerName, String worldName, 
                double x, double y, double z, float yaw, float pitch, Timestamp createdAt) {
        this.name = name;
        this.playerUUID = UUID.fromString(playerUUID);
        this.playerName = playerName;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.createdAt = createdAt;
    }
    
    public Location getLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }
    
    // Getters
    public String getName() { return name; }
    public UUID getPlayerUUID() { return playerUUID; }
    public String getPlayerName() { return playerName; }
    public String getWorldName() { return worldName; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public Timestamp getCreatedAt() { return createdAt; }
}