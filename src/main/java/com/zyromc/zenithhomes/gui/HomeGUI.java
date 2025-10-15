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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.List;

public class HomeGUI implements InventoryHolder {
    
    private final ZenithHomes plugin;
    private final Player player;
    private final List<Home> homes;
    private Inventory inventory;
    private int currentPage = 0;
    private static final int HOMES_PER_PAGE = 45;
    
    public HomeGUI(ZenithHomes plugin, Player player, List<Home> homes) {
        this.plugin = plugin;
        this.player = player;
        this.homes = homes;
        createGUI();
    }
    
    private void createGUI() {
        plugin.getHomeManager().getHomeLimit(player).thenAccept(limit -> {
            String title = plugin.getLanguageManager().getMessage(player, "gui.title")
                    .replace("{player}", player.getName())
                    .replace("{current}", String.valueOf(homes.size()))
                    .replace("{limit}", String.valueOf(limit));
            
            int size = 54;
            inventory = Bukkit.createInventory(this, size, title);
            updateGUI();
        });
    }
    
    private void updateGUI() {
        inventory.clear();
        
        int startIndex = currentPage * HOMES_PER_PAGE;
        int endIndex = Math.min(startIndex + HOMES_PER_PAGE, homes.size());
        
        // Add home items for current page
        for (int i = startIndex; i < endIndex; i++) {
            Home home = homes.get(i);
            inventory.setItem(i - startIndex, createHomeItem(home));
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
        inventory.setItem(49, closeButton);
        
        // Previous page button
        if (currentPage > 0) {
            ItemStack prevButton = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevButton.getItemMeta();
            prevMeta.setDisplayName(plugin.getLanguageManager().getMessage(player, "gui.buttons.previous"));
            prevButton.setItemMeta(prevMeta);
            inventory.setItem(45, prevButton);
        }
        
        // Next page button
        if ((currentPage + 1) * HOMES_PER_PAGE < homes.size()) {
            ItemStack nextButton = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextButton.getItemMeta();
            nextMeta.setDisplayName(plugin.getLanguageManager().getMessage(player, "gui.buttons.next"));
            nextButton.setItemMeta(nextMeta);
            inventory.setItem(53, nextButton);
        }
        
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
                    .replace("{cooldown}", String.valueOf(plugin.getConfigManager().getSetting("homes.teleport-cooldown"))),
                "",
                "&7Page: &f" + (currentPage + 1) + "&7/&f" + getTotalPages()
            );
            
            infoMeta.setLore(infoLore);
            infoButton.setItemMeta(infoMeta);
            inventory.setItem(48, infoButton);
        });
        
        // Player head
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta playerMeta = (SkullMeta) playerHead.getItemMeta();
        playerMeta.setOwningPlayer(player);
        playerMeta.setDisplayName("&6" + player.getName() + "'s Homes");
        playerMeta.setLore(Arrays.asList(
            "&7View and manage your",
            "&7personal homes",
            "",
            "&eLeft-click: &7Teleport to home",
            "&cRight-click: &7Delete home"
        ));
        playerHead.setItemMeta(playerMeta);
        inventory.setItem(50, playerHead);
    }
    
    private int getTotalPages() {
        return (int) Math.ceil((double) homes.size() / HOMES_PER_PAGE);
    }
    
    public void open() {
        player.openInventory(inventory);
    }
    
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        
        if (event.getCurrentItem() == null) return;
        
        int slot = event.getRawSlot();
        ItemStack item = event.getCurrentItem();
        
        // Handle navigation buttons
        if (item.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        } else if (item.getType() == Material.ARROW) {
            if (slot == 45 && currentPage > 0) {
                currentPage--;
                updateGUI();
            } else if (slot == 53 && (currentPage + 1) * HOMES_PER_PAGE < homes.size()) {
                currentPage++;
                updateGUI();
            }
            return;
        }
        
        // Handle home items
        int startIndex = currentPage * HOMES_PER_PAGE;
        int endIndex = Math.min(startIndex + HOMES_PER_PAGE, homes.size());
        
        if (slot >= 0 && slot < 45 && (startIndex + slot) < homes.size()) {
            Home home = homes.get(startIndex + slot);
            
            if (event.isLeftClick()) {
                teleportToHome(plugin, player, home);
                player.closeInventory();
            } else if (event.isRightClick()) {
                confirmDeleteHome(home);
            }
        }
    }
    
    private void confirmDeleteHome(Home home) {
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
        ItemMeta homeMeta = homeInfo.getItemMeta();
        List<String> homeLore = homeMeta.getLore();
        homeLore.add("");
        homeLore.add(plugin.getLanguageManager().getMessage(player, "gui.confirm-delete.message")
                .replace("{home}", home.getName()));
        homeMeta.setLore(homeLore);
        homeInfo.setItemMeta(homeMeta);
        confirmGUI.setItem(13, homeInfo);
        
        player.openInventory(confirmGUI);
    }
    
    public void handleConfirmationClick(InventoryClickEvent event, Home home) {
        event.setCancelled(true);
        
        if (event.getCurrentItem() == null) return;
        
        ItemStack item = event.getCurrentItem();
        
        if (item.getType() == Material.GREEN_WOOL) {
            // Confirm deletion
            plugin.getHomeManager().deleteHome(player, home.getName()).thenAccept(success -> {
                if (success) {
                    player.sendMessage(plugin.getLanguageManager().getMessage(player, "success.home-deleted")
                            .replace("{home}", home.getName()));
                    
                    // Refresh the GUI
                    plugin.getHomeManager().getHomes(player).thenAccept(updatedHomes -> {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            new HomeGUI(plugin, player, updatedHomes).open();
                        });
                    });
                } else {
                    player.sendMessage(plugin.getLanguageManager().getMessage(player, "errors.generic"));
                }
            });
        } else if (item.getType() == Material.RED_WOOL) {
            // Cancel - return to homes GUI
            plugin.getHomeManager().getHomes(player).thenAccept(updatedHomes -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    new HomeGUI(plugin, player, updatedHomes).open();
                });
            });
        }
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
    
    public int getCurrentPage() {
        return currentPage;
    }
}
