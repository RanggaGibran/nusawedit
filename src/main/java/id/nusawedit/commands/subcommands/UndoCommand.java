package id.nusawedit.commands.subcommands;

import id.nusawedit.Plugin;
import id.nusawedit.commands.SubCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UndoCommand implements SubCommand {
    private final Plugin plugin;
    
    public UndoCommand(Plugin plugin) {
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
        
        // Konsumsi penggunaan tongkat (method ini sudah memeriksa tangan yang memegang)
        if (!plugin.getSelectionManager().consumePlayerWandUse(player)) {
            return false; // Tongkat kehabisan penggunaan
        }
        
        plugin.getBlockOperationHandler().undoLastOperation(player);
        return true;
    }
    
    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("nusawedit.undo");
    }
    
    @Override
    public String getDescription() {
        return "Undo your last operation";
    }
    
    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}