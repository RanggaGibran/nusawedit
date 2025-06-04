package id.nusawedit.commands.subcommands;

import id.nusawedit.Plugin;
import id.nusawedit.commands.SubCommand;
import id.nusawedit.selection.Selection;
import id.nusawedit.operations.BlockPattern;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class SetCommand implements SubCommand {
    private final Plugin plugin;
    
    public SetCommand(Plugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        
        // Check if set command is enabled in this world
        String worldName = player.getWorld().getName();
        if (!plugin.getConfigManager().isFeatureEnabledInWorld(worldName, "set-enabled")) {
            player.sendMessage(plugin.getMessageManager().getMessage("protection.feature-disabled-world"));
            return false;
        }
        
        // Check if player is holding a wand
        if (!plugin.getSelectionManager().isHoldingWand(player)) {
            player.sendMessage(plugin.getMessageManager().getMessage("selection.wand-required"));
            return false;
        }
        
        if (args.length < 1) {
            player.sendMessage(plugin.getMessageManager().getMessage("usage.set"));
            player.sendMessage(plugin.getMessageManager().getMessage("usage.set-example-1"));
            player.sendMessage(plugin.getMessageManager().getMessage("usage.set-example-2"));
            player.sendMessage(plugin.getMessageManager().getMessage("usage.set-example-3"));
            return false;
        }
        
        // Parse pattern
        BlockPattern pattern = parsePattern(args[0]);
        
        if (pattern.size() == 0) {
            player.sendMessage("§cInvalid material pattern! Example: stone,cobblestone or 30%stone,50%cobblestone");
            return false;
        }
        
        // Lakukan pre-check untuk memastikan operasi bisa dilakukan
        if (!preCheckOperation(player, pattern)) {
            return false; // Gagal pre-check, tidak perlu consume wand
        }
        
        // Pre-check berhasil, sekarang konsumsi penggunaan tongkat
        if (!plugin.getSelectionManager().consumePlayerWandUse(player)) {
            return false; // Tongkat kehabisan penggunaan
        }
        
        // Execute set operation
        plugin.getBlockOperationHandler().setBlocksPattern(player, pattern);
        return true;
    }
    
    /**
     * Parse a pattern string into a BlockPattern
     * @param patternStr Pattern string, e.g. "stone,cobblestone" or "30%stone,50%cobblestone"
     * @return BlockPattern object
     */
    private BlockPattern parsePattern(String patternStr) {
        BlockPattern pattern = new BlockPattern();
        
        // Split by comma
        String[] parts = patternStr.split(",");
        
        for (String part : parts) {
            part = part.trim();
            
            try {
                // Check if it has a percentage
                if (part.contains("%")) {
                    // Format: 30%stone
                    String[] percentParts = part.split("%");
                    if (percentParts.length != 2) {
                        continue; // Invalid format
                    }
                    
                    int weight = Integer.parseInt(percentParts[0]);
                    String materialName = percentParts[1].toUpperCase();
                    Material material = Material.valueOf(materialName);
                    pattern.addMaterial(material, weight);
                } else {
                    // No percentage, use equal weights (100 is arbitrary)
                    String materialName = part.toUpperCase();
                    Material material = Material.valueOf(materialName);
                    pattern.addMaterial(material, 100);
                }
            } catch (NumberFormatException e) {
                // Invalid number
                continue;
            } catch (IllegalArgumentException e) {
                // Invalid material name
                continue;
            }
        }
        
        return pattern;
    }
    
    /**
     * Melakukan pre-check untuk operasi set
     * @param player Player
     * @param pattern Pattern blok untuk set
     * @return true jika pre-check berhasil
     */
    private boolean preCheckOperation(Player player, BlockPattern pattern) {
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
        
        // Calculate material requirements
        Map<Material, Integer> requirements = pattern.calculateRequirements(volume);
        
        // Check if player has enough of each material
        for (Map.Entry<Material, Integer> entry : requirements.entrySet()) {
            if (!plugin.getInventoryManager().hasMaterial(player, entry.getKey(), entry.getValue())) {
                player.sendMessage("§cYou don't have enough materials! You need approximately §6" + entry.getValue() + 
                        " " + formatMaterial(entry.getKey()) + "§c!");
                return false;
            }
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