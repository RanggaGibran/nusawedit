package id.nusawedit.commands.subcommands;

import id.nusawedit.Plugin;
import id.nusawedit.commands.SubCommand;
import id.nusawedit.selection.Selection;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetCommand implements SubCommand {
    private final Plugin plugin;
    
    public SetCommand(Plugin plugin) {
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
        
        if (args.length < 1) {
            player.sendMessage("§cUsage: /nwe set <material>");
            return false;
        }
        
        String materialName = args[0].toUpperCase();
        Material material;
        
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cInvalid material: " + materialName);
            return false;
        }
        
        // Lakukan pre-check untuk memastikan operasi bisa dilakukan
        if (!preCheckOperation(player, material)) {
            return false; // Gagal pre-check, tidak perlu consume wand
        }
        
        // Pre-check berhasil, sekarang konsumsi penggunaan tongkat
        if (!plugin.getSelectionManager().consumePlayerWandUse(player)) {
            return false; // Tongkat kehabisan penggunaan
        }
        
        // Execute set operation
        plugin.getBlockOperationHandler().setBlocks(player, material);
        return true;
    }
    
    /**
     * Melakukan pre-check untuk operasi set
     * @param player Player
     * @param material Material untuk set
     * @return true jika pre-check berhasil
     */
    private boolean preCheckOperation(Player player, Material material) {
        // Check if player has a valid selection
        if (!plugin.getSelectionManager().hasCompleteSelection(player)) {
            player.sendMessage("§cYou need to make a complete selection first!");
            return false;
        }
        
        Selection selection = plugin.getSelectionManager().getSelection(player);
        int volume = selection.getVolume();
        
        // Check batas blok sesuai rank
        String rank = plugin.getBlockOperationHandler().getRank(player);
        int blockLimit = plugin.getConfigManager().getRankBlockLimit(rank);
        
        if (volume > blockLimit) {
            player.sendMessage("§cYour selection is too large! Maximum: §6" + blockLimit + " blocks§c, Selected: §6" + volume + " blocks");
            return false;
        }
        
        // Check material yang cukup
        if (!plugin.getInventoryManager().hasMaterial(player, material, volume)) {
            player.sendMessage("§cYou don't have enough materials! You need §6" + volume + " " + formatMaterial(material) + "§c!");
            return false;
        }
        
        return true;
    }
    
    /**
     * Format material name for display
     */
    private String formatMaterial(Material material) {
        return material.name().replace('_', ' ').toLowerCase();
    }
    
    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("nusawedit.set");
    }
    
    @Override
    public String getDescription() {
        return "Set all blocks in your selection to a material";
    }
    
    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}