package com.zyromc.zenithhomes.commands;

import com.zyromc.zenithhomes.ZenithHomes;
import com.zyromc.zenithhomes.gui.AdminHomesGUI;
import com.zyromc.zenithhomes.utils.Home;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AdminCommand implements CommandExecutor {
    
    private final ZenithHomes plugin;
    
    public AdminCommand(ZenithHomes plugin) {
        this.plugin = plugin;
        plugin.getCommand("homesadmin").setExecutor(this);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("zenithhomes.admin")) {
            sendMessage(sender, "errors.no-permission");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload":
                reloadConfig(sender);
                break;
            case "list":
                if (args.length > 1) {
                    listPlayerHomes(sender, args[1]);
                } else {
                    listAllHomes(sender);
                }
                break;
            case "setlimit":
                if (args.length >= 3) {
                    setHomeLimit(sender, args[1], args[2]);
                } else {
                    sendMessage(sender, "commands.homesadmin.usage");
                }
                break;
            case "delete":
                if (args.length >= 3) {
                    deletePlayerHome(sender, args[1], args[2]);
                } else {
                    sendMessage(sender, "commands.homesadmin.usage");
                }
                break;
            case "gui":
                if (sender instanceof Player) {
                    openAdminGUI((Player) sender);
                } else {
                    sender.sendMessage("This command can only be used by players.");
                }
                break;
            default:
                sendHelp(sender);
        }
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        if (sender instanceof Player) {
            plugin.getLanguageManager().sendMessage((Player) sender, "commands.homesadmin.usage");
        } else {
            // Console help
            sender.sendMessage("=== ZenithHomes Admin Commands ===");
            sender.sendMessage("/homesadmin reload - Reload configuration");
            sender.sendMessage("/homesadmin list [player] - List all homes or player's homes");
            sender.sendMessage("/homesadmin setlimit <player> <limit> - Set player's home limit");
            sender.sendMessage("/homesadmin delete <player> <home> - Delete player's home");
            sender.sendMessage("/homesadmin gui - Open admin homes GUI");
        }
    }
    
    private void reloadConfig(CommandSender sender) {
        plugin.getConfigManager().reloadConfigs();
        plugin.getLanguageManager().loadLanguages();
        sendMessage(sender, "success.config-reloaded");
    }
    
    private void listAllHomes(CommandSender sender) {
        plugin.getHomeManager().getAllHomes().thenAccept(homes -> {
            if (homes.isEmpty()) {
                sendMessage(sender, "errors.no-homes");
                return;
            }
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("{total}", String.valueOf(homes.size()));
            sendMessage(sender, "admin.list-all-header", placeholders);
            
            for (Home home : homes) {
                String item = plugin.getLanguageManager().getMessage(
                    sender instanceof Player ? (Player) sender : null, 
                    "admin.list-item"
                )
                .replace("{home}", home.getName())
                .replace("{player}", home.getPlayerName())
                .replace("{world}", home.getWorldName())
                .replace("{x}", String.valueOf((int) home.getX()))
                .replace("{y}", String.valueOf((int) home.getY()))
                .replace("{z}", String.valueOf((int) home.getZ()));
                
                sender.sendMessage(item);
            }
        });
    }
    
    private void listPlayerHomes(CommandSender sender, String playerName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target == null || !target.hasPlayedBefore()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("{player}", playerName);
            sendMessage(sender, "errors.player-not-found", placeholders);
            return;
        }
        
        plugin.getHomeManager().getHomes(target.getUniqueId().toString()).thenAccept(homes -> {
            if (homes.isEmpty()) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("{player}", target.getName());
                sendMessage(sender, "admin.no-homes-player", placeholders);
                return;
            }
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("{player}", target.getName());
            sendMessage(sender, "admin.list-header", placeholders);
            
            for (Home home : homes) {
                String item = plugin.getLanguageManager().getMessage(
                    sender instanceof Player ? (Player) sender : null, 
                    "admin.list-item"
                )
                .replace("{home}", home.getName())
                .replace("{world}", home.getWorldName())
                .replace("{x}", String.valueOf((int) home.getX()))
                .replace("{y}", String.valueOf((int) home.getY()))
                .replace("{z}", String.valueOf((int) home.getZ()));
                
                sender.sendMessage(item);
            }
        });
    }
    
    private void setHomeLimit(CommandSender sender, String playerName, String limitStr) {
        try {
            int limit = Integer.parseInt(limitStr);
            OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
            
            if (target == null || !target.hasPlayedBefore()) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("{player}", playerName);
                sendMessage(sender, "errors.player-not-found", placeholders);
                return;
            }
            
            plugin.getHomeManager().setHomeLimit(target.getUniqueId(), limit);
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("{player}", target.getName());
            placeholders.put("{limit}", String.valueOf(limit));
            sendMessage(sender, "admin.limit-set", placeholders);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid number format!");
        }
    }
    
    private void deletePlayerHome(CommandSender sender, String playerName, String homeName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target == null || !target.hasPlayedBefore()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("{player}", playerName);
            sendMessage(sender, "errors.player-not-found", placeholders);
            return;
        }
        
        plugin.getHomeManager().deleteHome(target.getUniqueId(), homeName).thenAccept(success -> {
            if (success) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("{player}", target.getName());
                placeholders.put("{home}", homeName);
                sendMessage(sender, "success.admin-home-deleted", placeholders);
            } else {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("{home}", homeName);
                sendMessage(sender, "errors.home-not-found", placeholders);
            }
        });
    }
    
    private void openAdminGUI(Player player) {
        plugin.getHomeManager().getAllHomes().thenAccept(homes -> {
            if (homes.isEmpty()) {
                plugin.getLanguageManager().sendMessage(player, "errors.no-homes");
                return;
            }
            
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                new AdminHomesGUI(plugin, player, homes).open();
            });
        });
    }
    
    // Helper methods for sending messages
    private void sendMessage(CommandSender sender, String path) {
        if (sender instanceof Player) {
            plugin.getLanguageManager().sendMessage((Player) sender, path);
        } else {
            // For console, remove color codes
            String message = plugin.getLanguageManager().getMessage("en", path);
            sender.sendMessage(stripColor(message));
        }
    }
    
    private void sendMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        if (sender instanceof Player) {
            plugin.getLanguageManager().sendMessage((Player) sender, path, placeholders);
        } else {
            // For console, remove color codes
            String message = plugin.getLanguageManager().getMessage("en", path);
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace(entry.getKey(), entry.getValue());
            }
            sender.sendMessage(stripColor(message));
        }
    }
    
    private String stripColor(String text) {
        return text.replaceAll("&[0-9a-fk-or]", "");
    }
}
