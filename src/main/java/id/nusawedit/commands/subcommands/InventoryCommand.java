package id.nusawedit.commands.subcommands;

import id.nusawedit.Plugin;
import id.nusawedit.commands.SubCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InventoryCommand implements SubCommand {
    private final Plugin plugin;
    
    public InventoryCommand(Plugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        plugin.getInventoryManager().openInventory(player);
        player.sendMessage("§aOpened your NusaWEdit virtual inventory!");
        return true;
    }
    
    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("nusawedit.inventory");
    }
    
    @Override
    public String getDescription() {
        return "Open your virtual material inventory";
    }
    
    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}