package com.zyromc.zenithhomes.listeners;

import com.zyromc.zenithhomes.ZenithHomes;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class TeleportListener implements Listener {
    
    private final ZenithHomes plugin;
    
    public TeleportListener(ZenithHomes plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        // This will be used to cancel teleport if player moves during countdown
        // The actual cancellation is handled in the HomeGUI teleport method
    }
}