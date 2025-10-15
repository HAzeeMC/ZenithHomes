package com.zyromc.zenithhomes.gui;

import com.zyromc.zenithhomes.ZenithHomes;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

public class GUIListener implements Listener {
    
    private final ZenithHomes plugin;
    
    public GUIListener(ZenithHomes plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof HomeGUI) {
            ((HomeGUI) holder).handleClick(event);
        } else if (holder instanceof AdminHomesGUI) {
            ((AdminHomesGUI) holder).handleClick(event);
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        plugin.getHomeManager().removeTeleportLocation(player);
    }
}