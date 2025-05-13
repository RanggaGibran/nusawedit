package id.nusawedit.commands.subcommands;

import id.nusawedit.Plugin;
import id.nusawedit.commands.SubCommand;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GiveCommand implements SubCommand {
    private final Plugin plugin;
    
    public GiveCommand(Plugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /nwe give <player> wand <uses>");
            return false;
        }
        
        // Get target player
        String playerName = args[0];
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: " + playerName);
            return false;
        }
        
        // Check if giving wand
        String itemType = args[1].toLowerCase();
        if (!itemType.equals("wand")) {
            sender.sendMessage("§cInvalid item type. Only 'wand' is supported.");
            return false;
        }
        
        // Parse uses
        int uses;
        try {
            uses = Integer.parseInt(args[2]);
            if (uses <= 0) {
                sender.sendMessage("§cUses must be a positive number!");
                return false;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid number of uses: " + args[2]);
            return false;
        }
        
        // Create and give wand
        ItemStack wand = plugin.getSelectionManager().createWand(uses);
        target.getInventory().addItem(wand);
        
        target.sendMessage("§aYou received a NusaWEdit wand with §6" + uses + " uses§a!");
        sender.sendMessage("§aGave wand with §6" + uses + " uses §ato §6" + target.getName() + "§a!");
        return true;
    }
    
    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("nusawedit.admin.give");
    }
    
    @Override
    public String getDescription() {
        return "Give a selection wand to a player";
    }
    
    @Override
    public boolean isPlayerOnly() {
        return false;
    }
}