package com.zyromc.zenithhomes.gui;

import com.zyromc.zenithhomes.ZenithHomes;
import com.zyromc.zenithhomes.utils.Home;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class AdminHomesGUI implements InventoryHolder {
    
    private final ZenithHomes plugin;
    private final Player player;
    private final List<Home> homes;
    private Inventory inventory;
    
    public AdminHomesGUI(ZenithHomes plugin, Player player, List<Home> homes) {
        this.plugin = plugin;
        this.player = player;
        this.homes = homes;
        createGUI();
    }
    
    private void createGUI() {
        String title = "§6Admin Homes - " + homes.size() + " homes";
        int size = 54;
        inventory = Bukkit.createInventory(this, size, title);
        updateGUI();
    }
    
    private void updateGUI() {
        inventory.clear();
        
        // Add home items
        for (int i = 0; i < homes.size() && i < inventory.getSize(); i++) {
            Home home = homes.get(i);
            inventory.setItem(i, createHomeItem(home));
        }
    }
    
    private ItemStack createHomeItem(Home home) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName("§e" + home.getName() + " §7- §6" + home.getPlayerName());
        
        List<String> lore = Arrays.asList(
            "§7Player: §f" + home.getPlayerName(),
            "§7UUID: §f" + home.getPlayerUUID(),
            "§7World: §f" + home.getWorldName(),
            "§7Coordinates: §f" + String.format("%.1f", home.getX()) + ", " + 
                            String.format("%.1f", home.getY()) + ", " + 
                            String.format("%.1f", home.getZ()),
            "§7Created: §f" + home.getCreatedAt(),
            "",
            "§aLeft-click to teleport",
            "§cRight-click to delete"
        );
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    public void open() {
        player.openInventory(inventory);
    }
    
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        
        if (event.getCurrentItem() == null) return;
        
        int slot = event.getRawSlot();
        ItemStack item = event.getCurrentItem();
        
        // Handle home teleport
        if (slot < homes.size() && item.getType() == Material.COMPASS) {
            Home home = homes.get(slot);
            
            if (event.isLeftClick()) {
                HomeGUI.teleportToHome(plugin, player, home);
            } else if (event.isRightClick()) {
                // Delete home
                plugin.getHomeManager().deleteHome(home.getPlayerUUID(), home.getName()).thenAccept(success -> {
                    if (success) {
                        player.sendMessage("§aDeleted home '" + home.getName() + "' for " + home.getPlayerName());
                        // Refresh GUI
                        plugin.getHomeManager().getAllHomes().thenAccept(updatedHomes -> {
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                new AdminHomesGUI(plugin, player, updatedHomes).open();
                            });
                        });
                    } else {
                        player.sendMessage("§cFailed to delete home!");
                    }
                });
            }
        }
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
