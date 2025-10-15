package com.zyromc.zenithhomes.gui;

import com.zyromc.zenithhomes.ZenithHomes;
import com.zyromc.zenithhomes.utils.Home;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.List;

public class HomeGUI implements InventoryHolder {
    
    private final ZenithHomes plugin;
    private final Player player;
    private final List<Home> homes;
    private Inventory inventory;
    
    public HomeGUI(ZenithHomes plugin, Player player, List<Home> homes) {
        this.plugin = plugin;
        this.player = player;
        this.homes = homes;
        createGUI();
    }
    
    private void createGUI() {
        String title = plugin.getLanguageManager().getMessage(player, "gui.title")
                .replace("{player}", player.getName())
                .replace("{count}", String.valueOf(homes.size()));
        
        int size = (int) Math.min(54, Math.ceil((homes.size() + 8) / 9.0) * 9);
        size = Math.max(18, size);
        
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
        
        // Add control buttons
        addControlButtons();
    }
    
    private ItemStack createHomeItem(Home home) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(plugin.getLanguageManager().getMessage(player, "gui.home-item.name")
                .replace("{home}", home.getName()));
        
        List<String> lore = Arrays.asList(
            plugin.getLanguageManager().getMessage(player, "gui.home-item.world")
                .replace("{world}", home.getWorldName()),
            plugin.getLanguageManager().getMessage(player, "gui.home-item.coordinates")
                .replace("{x}", String.format("%.1f", home.getX()))
                .replace("{y}", String.format("%.1f", home.getY()))
                .replace("{z}", String.format("%.1f", home.getZ())),
            plugin.getLanguageManager().getMessage(player, "gui.home-item.created")
                .replace("{date}", home.getCreatedAt().toString()),
            "",
            plugin.getLanguageManager().getMessage(player, "gui.home-item.left-click"),
            plugin.getLanguageManager().getMessage(player, "gui.home-item.right-click")
        );
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private void addControlButtons() {
        // Close button
        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeButton.getItemMeta();
        closeMeta.setDisplayName(plugin.getLanguageManager().getMessage(player, "gui.buttons.close"));
        closeButton.setItemMeta(closeMeta);
        inventory.setItem(inventory.getSize() - 9, closeButton);
        
        // Info button
        ItemStack infoButton = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoButton.getItemMeta();
        infoMeta.setDisplayName(plugin.getLanguageManager().getMessage(player, "gui.buttons.info"));
        
        plugin.getHomeManager().getHomeLimit(player).thenAccept(limit -> {
            List<String> infoLore = Arrays.asList(
                plugin.getLanguageManager().getMessage(player, "gui.info.homes")
                    .replace("{current}", String.valueOf(homes.size()))
                    .replace("{limit}", String.valueOf(limit)),
                plugin.getLanguageManager().getMessage(player, "gui.info.cooldown")
                    .replace("{cooldown}", String.valueOf(plugin.getConfigManager().getSetting("homes.teleport-cooldown")))
            );
            
            infoMeta.setLore(infoLore);
            infoButton.setItemMeta(infoMeta);
            inventory.setItem(inventory.getSize() - 5, infoButton);
        });
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
                teleportToHome(plugin, player, home);
            } else if (event.isRightClick()) {
                // Delete home confirmation
                confirmDeleteHome(home);
            }
        }
        
        // Handle control buttons
        else if (item.getType() == Material.BARRIER) {
            player.closeInventory();
        }
    }
    
    private void confirmDeleteHome(Home home) {
        // Create confirmation GUI
        Inventory confirmGUI = Bukkit.createInventory(this, 27, 
            plugin.getLanguageManager().getMessage(player, "gui.confirm-delete.title"));
        
        // Confirm button
        ItemStack confirmButton = new ItemStack(Material.GREEN_WOOL);
        ItemMeta confirmMeta = confirmButton.getItemMeta();
        confirmMeta.setDisplayName(plugin.getLanguageManager().getMessage(player, "gui.confirm-delete.confirm"));
        confirmButton.setItemMeta(confirmMeta);
        confirmGUI.setItem(11, confirmButton);
        
        // Cancel button
        ItemStack cancelButton = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancelButton.getItemMeta();
        cancelMeta.setDisplayName(plugin.getLanguageManager().getMessage(player, "gui.confirm-delete.cancel"));
        cancelButton.setItemMeta(cancelMeta);
        confirmGUI.setItem(15, cancelButton);
        
        // Home info
        ItemStack homeInfo = createHomeItem(home);
        confirmGUI.setItem(13, homeInfo);
        
        player.openInventory(confirmGUI);
    }
    
    public static void teleportToHome(ZenithHomes plugin, Player player, Home home) {
        // Check cooldown
        if (!plugin.getHomeManager().canTeleport(player)) {
            long remaining = plugin.getHomeManager().getRemainingCooldown(player);
            player.sendMessage(plugin.getLanguageManager().getMessage(player, "errors.teleport-cooldown")
                    .replace("{time}", String.valueOf(remaining)));
            return;
        }
        
        Location location = home.getLocation();
        if (location == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage(player, "errors.world-not-loaded"));
            return;
        }
        
        int delay = (int) plugin.getConfigManager().getSetting("homes.teleport-delay");
        boolean cancelOnMove = (boolean) plugin.getConfigManager().getSetting("homes.cancel-on-move");
        
        player.sendMessage(plugin.getLanguageManager().getMessage(player, "teleport.teleporting")
                .replace("{home}", home.getName())
                .replace("{time}", String.valueOf(delay)));
        
        Location startLocation = player.getLocation().clone();
        plugin.getHomeManager().setTeleportLocation(player, startLocation);
        
        new BukkitRunnable() {
            int count = delay;
            
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                
                if (cancelOnMove) {
                    Location currentLocation = player.getLocation();
                    Location teleportLocation = plugin.getHomeManager().getTeleportLocation(player);
                    if (teleportLocation != null && currentLocation.distanceSquared(teleportLocation) > 0.1) {
                        player.sendMessage(plugin.getLanguageManager().getMessage(player, "teleport.cancelled-movement"));
                        plugin.getHomeManager().removeTeleportLocation(player);
                        cancel();
                        return;
                    }
                }
                
                if (count <= 0) {
                    plugin.getHomeManager().setLastTeleport(player);
                    plugin.getHomeManager().removeTeleportLocation(player);
                    player.teleport(location);
                    player.sendMessage(plugin.getLanguageManager().getMessage(player, "teleport.success")
                            .replace("{home}", home.getName()));
                    cancel();
                } else {
                    if (count <= 3) {
                        player.sendMessage(plugin.getLanguageManager().getMessage(player, "teleport.countdown")
                                .replace("{time}", String.valueOf(count)));
                    }
                    count--;
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
