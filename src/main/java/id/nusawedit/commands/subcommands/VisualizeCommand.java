package id.nusawedit.commands.subcommands;

import id.nusawedit.Plugin;
import id.nusawedit.commands.SubCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VisualizeCommand implements SubCommand {
    private final Plugin plugin;
    
    public VisualizeCommand(Plugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        
        // Check if player is holding a wand (optional, but consistent)
        if (!plugin.getSelectionManager().isHoldingWand(player)) {
            player.sendMessage(plugin.getMessageManager().getMessage("selection.wand-required"));
            return false;
        }
        
        boolean enabled = plugin.getVisualizationManager().toggleVisualization(player);
        
        if (enabled) {
            player.sendMessage(plugin.getMessageManager().getMessage("visualize.enabled"));
        } else {
            player.sendMessage(plugin.getMessageManager().getMessage("visualize.disabled"));
        }
        
        return true;
    }
    
    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("nusawedit.visualize");
    }
    
    @Override
    public String getDescription() {
        return "Toggle visualization of selections";
    }
    
    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}