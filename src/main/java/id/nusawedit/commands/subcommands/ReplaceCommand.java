package id.nusawedit.commands.subcommands;

import id.nusawedit.Plugin;
import id.nusawedit.commands.SubCommand;
import id.nusawedit.selection.Selection;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.block.Block;

public class ReplaceCommand implements SubCommand {
    private final Plugin plugin;
    
    public ReplaceCommand(Plugin plugin) {
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
        
        if (args.length < 2) {
            player.sendMessage("§cUsage: /nwe replace <from_material> <to_material>");
            return false;
        }
        
        String fromMaterialName = args[0].toUpperCase();
        String toMaterialName = args[1].toUpperCase();
        Material fromMaterial, toMaterial;
        
        try {
            fromMaterial = Material.valueOf(fromMaterialName);
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cInvalid material: " + fromMaterialName);
            return false;
        }
        
        try {
            toMaterial = Material.valueOf(toMaterialName);
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cInvalid material: " + toMaterialName);
            return false;
        }
        
        // Lakukan pre-check untuk memastikan operasi bisa dilakukan
        if (!preCheckOperation(player, fromMaterial, toMaterial)) {
            return false; // Gagal pre-check, tidak perlu consume wand
        }
        
        // Precheck lolos, sekarang konsumsi penggunaan tongkat
        if (!plugin.getSelectionManager().consumePlayerWandUse(player)) {
            return false; // Tongkat kehabisan penggunaan
        }
        
        // Execute replace operation
        plugin.getBlockOperationHandler().replaceBlocks(player, fromMaterial, toMaterial);
        return true;
    }
    
    /**
     * Melakukan pre-check untuk operasi replace
     * @param player Player
     * @param fromMaterial Material sumber
     * @param toMaterial Material target
     * @return true jika pre-check berhasil
     */
    private boolean preCheckOperation(Player player, Material fromMaterial, Material toMaterial) {
        // Check if player has a valid selection
        if (!plugin.getSelectionManager().hasCompleteSelection(player)) {
            player.sendMessage("§cYou need to make a complete selection first!");
            return false;
        }
        
        Selection selection = plugin.getSelectionManager().getSelection(player);
        
        // Hitung jumlah blok yang akan diganti
        int toReplace = 0;
        for (int x = selection.getMinX(); x <= selection.getMaxX(); x++) {
            for (int y = selection.getMinY(); y <= selection.getMaxY(); y++) {
                for (int z = selection.getMinZ(); z <= selection.getMaxZ(); z++) {
                    Block block = selection.getWorld().getBlockAt(x, y, z);
                    if (block.getType() == fromMaterial && !plugin.getConfigManager().isBlacklisted(block.getType())) {
                        toReplace++;
                    }
                }
            }
        }
        
        // Check batas blok sesuai rank
        String rank = plugin.getBlockOperationHandler().getRank(player);
        int blockLimit = plugin.getConfigManager().getRankBlockLimit(rank);
        
        if (toReplace > blockLimit) {
            player.sendMessage("§cToo many blocks to replace! Maximum: §6" + blockLimit + " blocks§c, Selected: §6" + toReplace + " blocks");
            return false;
        }
        
        // Check material yang cukup
        if (!plugin.getInventoryManager().hasMaterial(player, toMaterial, toReplace)) {
            player.sendMessage("§cYou don't have enough materials! You need §6" + toReplace + " " + formatMaterial(toMaterial) + "§c!");
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
        return sender.hasPermission("nusawedit.replace");
    }
    
    @Override
    public String getDescription() {
        return "Replace one material with another in your selection";
    }
    
    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}