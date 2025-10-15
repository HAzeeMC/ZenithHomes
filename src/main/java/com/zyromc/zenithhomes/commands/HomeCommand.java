package com.zyromc.zenithhomes.commands;

import com.zyromc.zenithhomes.ZenithHomes;
import com.zyromc.zenithhomes.gui.HomeGUI;
import com.zyromc.zenithhomes.utils.Home;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeCommand implements CommandExecutor {
    
    private final ZenithHomes plugin;
    
    public HomeCommand(ZenithHomes plugin) {
        this.plugin = plugin;
        plugin.getCommand("home").setExecutor(this);
        plugin.getCommand("sethome").setExecutor(this);
        plugin.getCommand("delhome").setExecutor(this);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        
        Player player = (Player) sender;
        String commandName = command.getName().toLowerCase();
        
        switch (commandName) {
            case "home":
                handleHomeCommand(player, args);
                break;
            case "sethome":
                handleSetHomeCommand(player, args);
                break;
            case "delhome":
                handleDeleteHomeCommand(player, args);
                break;
        }
        
        return true;
    }
    
    private void handleHomeCommand(Player player, String[] args) {
        if (args.length == 0) {
            // Open homes GUI
            plugin.getLanguageManager().sendMessage(player, "commands.home.gui-opening");
            openHomesGUI(player);
        } else {
            // Teleport to specific home
            teleportToHome(player, args[0]);
        }
    }
    
    private void handleSetHomeCommand(Player player, String[] args) {
        if (args.length == 0) {
            plugin.getLanguageManager().sendMessage(player, "commands.sethome.usage");
            return;
        }
        
        String homeName = args[0];
        setHome(player, homeName);
    }
    
    private void handleDeleteHomeCommand(Player player, String[] args) {
        if (args.length == 0) {
            plugin.getLanguageManager().sendMessage(player, "commands.delhome.usage");
            return;
        }
        
        String homeName = args[0];
        deleteHome(player, homeName);
    }
    
    private void openHomesGUI(Player player) {
        plugin.getHomeManager().getHomes(player).thenAccept(homes -> {
            if (homes.isEmpty()) {
                plugin.getLanguageManager().sendMessage(player, "commands.home.no-homes");
                return;
            }
            
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                new HomeGUI(plugin, player, homes).open();
            });
        });
    }
    
    private void teleportToHome(Player player, String homeName) {
        plugin.getHomeManager().getHomes(player).thenAccept(homes -> {
            Home targetHome = homes.stream()
                    .filter(home -> home.getName().equalsIgnoreCase(homeName))
                    .findFirst()
                    .orElse(null);
            
            if (targetHome == null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("{home}", homeName);
                plugin.getLanguageManager().sendMessage(player, "errors.home-not-found", placeholders);
                return;
            }
            
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                HomeGUI.teleportToHome(plugin, player, targetHome);
            });
        });
    }
    
    private void setHome(Player player, String homeName) {
        // Validate home name
        int maxLength = (int) plugin.getConfigManager().getSetting("homes.max-name-length");
        if (homeName.length() > maxLength) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("{max}", String.valueOf(maxLength));
            plugin.getLanguageManager().sendMessage(player, "errors.home-name-too-long", placeholders);
            return;
        }
        
        if (!homeName.matches("[a-zA-Z0-9_]+")) {
            plugin.getLanguageManager().sendMessage(player, "errors.invalid-home-name");
            return;
        }
        
        // Check home limit
        plugin.getHomeManager().getHomes(player).thenAccept(homes -> {
            plugin.getHomeManager().getHomeLimit(player).thenAccept(limit -> {
                if (homes.size() >= limit && !player.hasPermission("zenithhomes.bypass.limit")) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("{current}", String.valueOf(homes.size()));
                    placeholders.put("{limit}", String.valueOf(limit));
                    plugin.getLanguageManager().sendMessage(player, "commands.sethome.limit-reached", placeholders);
                    return;
                }
                
                // Set home
                plugin.getHomeManager().setHome(player, homeName).thenAccept(success -> {
                    if (success) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("{home}", homeName);
                        plugin.getLanguageManager().sendMessage(player, "success.home-created", placeholders);
                    } else {
                        plugin.getLanguageManager().sendMessage(player, "errors.generic");
                    }
                });
            });
        });
    }
    
    private void deleteHome(Player player, String homeName) {
        plugin.getHomeManager().deleteHome(player, homeName).thenAccept(success -> {
            if (success) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("{home}", homeName);
                plugin.getLanguageManager().sendMessage(player, "success.home-deleted", placeholders);
            } else {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("{home}", homeName);
                plugin.getLanguageManager().sendMessage(player, "errors.home-not-found", placeholders);
            }
        });
    }
}
