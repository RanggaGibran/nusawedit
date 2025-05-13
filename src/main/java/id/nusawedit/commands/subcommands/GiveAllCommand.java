package id.nusawedit.commands.subcommands;

import id.nusawedit.Plugin;
import id.nusawedit.commands.SubCommand;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GiveAllCommand implements SubCommand {
    private final Plugin plugin;
    
    public GiveAllCommand(Plugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /nwe giveall wand <uses>");
            return false;
        }
        
        // Check if giving wand
        String itemType = args[0].toLowerCase();
        if (!itemType.equals("wand")) {
            sender.sendMessage("§cInvalid item type. Only 'wand' is supported.");
            return false;
        }
        
        // Parse uses
        int uses;
        try {
            uses = Integer.parseInt(args[1]);
            if (uses <= 0) {
                sender.sendMessage("§cUses must be a positive number!");
                return false;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid number of uses: " + args[1]);
            return false;
        }
        
        // Count online players
        int playerCount = Bukkit.getOnlinePlayers().size();
        if (playerCount == 0) {
            sender.sendMessage("§cNo players online to give wands to!");
            return false;
        }
        
        // Give wands to all online players
        int givenCount = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            // Create wand for each player
            ItemStack wand = plugin.getSelectionManager().createWand(uses);
            target.getInventory().addItem(wand);
            
            // Notify player
            target.sendMessage("§aYou received a NusaWEdit wand with §6" + uses + " uses§a!");
            givenCount++;
        }
        
        // Notify command sender
        sender.sendMessage("§aGave wands with §6" + uses + " uses §ato §6" + givenCount + "§a players!");
        return true;
    }
    
    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("nusawedit.admin.giveall");
    }
    
    @Override
    public String getDescription() {
        return "Give selection wands to all online players";
    }
    
    @Override
    public boolean isPlayerOnly() {
        return false;
    }
}