package id.nusawedit.commands.subcommands;

import id.nusawedit.Plugin;
import id.nusawedit.commands.SubCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CancelCommand implements SubCommand {
    private final Plugin plugin;
    
    public CancelCommand(Plugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        
        // Check if player is holding a wand
        if (!plugin.getSelectionManager().isHoldingWand(player)) {
            player.sendMessage("§cAnda harus memegang tongkat NusaWEdit untuk menggunakan perintah ini!");
            return false;
        }
        
        boolean cancelled = plugin.getBlockOperationHandler().cancelOperations(player);
        
        if (cancelled) {
            player.sendMessage("§aActive operations have been cancelled.");
        } else {
            player.sendMessage("§cYou don't have any active operations to cancel.");
        }
        
        return true;
    }
    
    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("nusawedit.cancel");
    }
    
    @Override
    public String getDescription() {
        return "Cancel any active block operations";
    }
    
    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}