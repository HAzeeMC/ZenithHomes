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

import java.util.List;
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
            sender.sendMessage(plugin.getLanguageManager().getMessage((sender instanceof Player) ? (Player) sender : null, "errors.no-permission"));
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
                    sender.sendMessage("Usage: /homesadmin setlimit <player> <limit>");
                }
                break;
            case "delete":
                if (args.length >= 3) {
                    deletePlayerHome(sender, args[1], args[2]);
                } else {
                    sender.sendMessage("Usage: /homesadmin delete <player> <home>");
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
        sender.sendMessage("§6=== ZenithHomes Admin Commands ===");
        sender.sendMessage("§e/homesadmin reload §7- Reload configuration");
        sender.sendMessage("§e/homesadmin list [player] §7- List all homes or player's homes");
        sender.sendMessage("§e/homesadmin setlimit <player> <limit> §7- Set player's home limit");
        sender.sendMessage("§e/homesadmin delete <player> <home> §7- Delete player's home");
        sender.sendMessage("§e/homesadmin gui §7- Open admin homes GUI");
    }
    
    private void reloadConfig(CommandSender sender) {
        plugin.getConfigManager().reloadConfigs();
        plugin.getLanguageManager().loadLanguages();
        sender.sendMessage("§aConfiguration reloaded!");
    }
    
    private void listAllHomes(CommandSender sender) {
        plugin.getHomeManager().getAllHomes().thenAccept(homes -> {
            sender.sendMessage("§6=== All Homes (" + homes.size() + ") ===");
            for (Home home : homes) {
                sender.sendMessage("§e" + home.getPlayerName() + " §7- §a" + home.getName() + 
                        " §7(" + home.getWorldName() + ", " + (int)home.getX() + ", " + (int)home.getY() + ", " + (int)home.getZ() + ")");
            }
        });
    }
    
    private void listPlayerHomes(CommandSender sender, String playerName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage("§cPlayer not found!");
            return;
        }
        
        plugin.getHomeManager().getHomes(target.getUniqueId().toString()).thenAccept(homes -> {
            sender.sendMessage("§6=== " + target.getName() + "'s Homes (" + homes.size() + ") ===");
            for (Home home : homes) {
                sender.sendMessage("§a" + home.getName() + " §7(" + home.getWorldName() + ", " + 
                        (int)home.getX() + ", " + (int)home.getY() + ", " + (int)home.getZ() + ")");
            }
        });
    }
    
    private void setHomeLimit(CommandSender sender, String playerName, String limitStr) {
        try {
            int limit = Integer.parseInt(limitStr);
            OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
            
            if (target == null || !target.hasPlayedBefore()) {
                sender.sendMessage("§cPlayer not found!");
                return;
            }
            
            plugin.getHomeManager().setHomeLimit(target.getUniqueId(), limit);
            sender.sendMessage("§aSet home limit for " + target.getName() + " to " + limit);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid number format!");
        }
    }
    
    private void deletePlayerHome(CommandSender sender, String playerName, String homeName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage("§cPlayer not found!");
            return;
        }
        
        plugin.getHomeManager().deleteHome(target.getUniqueId(), homeName).thenAccept(success -> {
            if (success) {
                sender.sendMessage("§aDeleted home '" + homeName + "' for " + target.getName());
            } else {
                sender.sendMessage("§cHome not found!");
            }
        });
    }
    
    private void openAdminGUI(Player player) {
        plugin.getHomeManager().getAllHomes().thenAccept(homes -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                new AdminHomesGUI(plugin, player, homes).open();
            });
        });
    }
}