package com.zyromc.zenithhomes.commands;

import com.zyromc.zenithhomes.ZenithHomes;
import com.zyromc.zenithhomes.gui.HomeGUI;
import com.zyromc.zenithhomes.utils.Home;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

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
        
        switch (command.getName().toLowerCase()) {
            case "home":
                if (args.length == 0) {
                    // Open homes GUI
                    openHomesGUI(player);
                } else {
                    // Teleport to specific home
                    teleportToHome(player, args[0]);
                }
                break;
                
            case "sethome":
                if (args.length == 0) {
                    player.sendMessage(plugin.getLanguageManager().getMessage(player, "commands.sethome.usage"));
                    return true;
                }
                setHome(player, args[0]);
                break;
                
            case "delhome":
                if (args.length == 0) {
                    player.sendMessage(plugin.getLanguageManager().getMessage(player, "commands.delhome.usage"));
                    return true;
                }
                deleteHome(player, args[0]);
                break;
        }
        
        return true;
    }
    
    private void openHomesGUI(Player player) {
        plugin.getHomeManager().getHomes(player).thenAccept(homes -> {
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
                player.sendMessage(plugin.getLanguageManager().getMessage(player, "errors.home-not-found"));
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
            player.sendMessage(plugin.getLanguageManager().getMessage(player, "errors.home-name-too-long")
                    .replace("{max}", String.valueOf(maxLength)));
            return;
        }
        
        if (!homeName.matches("[a-zA-Z0-9_]+")) {
            player.sendMessage(plugin.getLanguageManager().getMessage(player, "errors.invalid-home-name"));
            return;
        }
        
        // Check home limit
        plugin.getHomeManager().getHomes(player).thenAccept(homes -> {
            plugin.getHomeManager().getHomeLimit(player).thenAccept(limit -> {
                if (homes.size() >= limit && !player.hasPermission("zenithhomes.bypass.limit")) {
                    player.sendMessage(plugin.getLanguageManager().getMessage(player, "errors.home-limit-reached")
                            .replace("{limit}", String.valueOf(limit)));
                    return;
                }
                
                // Set home
                plugin.getHomeManager().setHome(player, homeName).thenAccept(success -> {
                    if (success) {
                        player.sendMessage(plugin.getLanguageManager().getMessage(player, "commands.sethome.success")
                                .replace("{home}", homeName));
                    } else {
                        player.sendMessage(plugin.getLanguageManager().getMessage(player, "errors.generic"));
                    }
                });
            });
        });
    }
    
    private void deleteHome(Player player, String homeName) {
        plugin.getHomeManager().deleteHome(player, homeName).thenAccept(success -> {
            if (success) {
                player.sendMessage(plugin.getLanguageManager().getMessage(player, "commands.delhome.success")
                        .replace("{home}", homeName));
            } else {
                player.sendMessage(plugin.getLanguageManager().getMessage(player, "errors.home-not-found"));
            }
        });
    }
}