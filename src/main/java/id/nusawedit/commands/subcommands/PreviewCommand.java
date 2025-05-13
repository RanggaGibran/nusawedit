package id.nusawedit.commands.subcommands;

import id.nusawedit.Plugin;
import id.nusawedit.commands.SubCommand;
import id.nusawedit.visualization.BlockPreview;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PreviewCommand implements SubCommand {
    private final Plugin plugin;
    
    public PreviewCommand(Plugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        
        if (args.length < 1) {
            // If no args, toggle preview mode or show status
            if (plugin.getVisualizationManager().hasActivePreview(player)) {
                // Show preview status
                BlockPreview preview = plugin.getVisualizationManager().getPreview(player);
                String materialName = formatMaterial(preview.getMaterial());
                String operationType = preview.isReplace() ? 
                        "replace " + formatMaterial(preview.getFromMaterial()) + " with " : 
                        "set to ";
                
                player.sendMessage("§aActive preview: §6" + operationType + materialName);
                player.sendMessage("§aAffected blocks: §6" + preview.getBlockCount());
                player.sendMessage("§aUse §6/nwe preview confirm §ato execute or §6/nwe preview cancel §ato cancel.");
            } else {
                player.sendMessage("§cUsage: /nwe preview <set/replace/cancel/confirm>");
                player.sendMessage("§cExample: /nwe preview set STONE");
                player.sendMessage("§cExample: /nwe preview replace DIRT STONE");
            }
            return true;
        }
        
        String action = args[0].toLowerCase();
        
        // Only require holding wand for creating preview or confirming, not for canceling
        if (!action.equals("cancel") && !plugin.getSelectionManager().isHoldingWand(player)) {
            player.sendMessage("§cAnda harus memegang tongkat NusaWEdit untuk menggunakan perintah ini!");
            return false;
        }
        
        switch (action) {
            case "set":
                return handleSetPreview(player, args);
                
            case "replace":
                return handleReplacePreview(player, args);
                
            case "cancel":
                if (plugin.getVisualizationManager().cancelPreview(player)) {
                    player.sendMessage("§aPreview cancelled.");
                } else {
                    player.sendMessage("§cNo active preview to cancel.");
                }
                return true;
                
            case "confirm":
                // Check for holding wand here too for consistency
                if (!plugin.getSelectionManager().isHoldingWand(player)) {
                    player.sendMessage("§cAnda harus memegang tongkat NusaWEdit untuk menggunakan perintah ini!");
                    return false;
                }
                
                if (plugin.getVisualizationManager().confirmPreview(player)) {
                    player.sendMessage("§aOperation executed!");
                } else {
                    player.sendMessage("§cFailed to execute operation. No active preview or insufficient materials.");
                }
                return true;
                
            default:
                player.sendMessage("§cUnknown preview action: " + action);
                player.sendMessage("§cUsage: /nwe preview <set/replace/cancel/confirm>");
                return false;
        }
    }
    
    private boolean handleSetPreview(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /nwe preview set <material>");
            return false;
        }
        
        String materialName = args[1].toUpperCase();
        Material material;
        
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cInvalid material: " + materialName);
            return false;
        }
        
        // Start preview
        if (plugin.getVisualizationManager().startPreview(player, material, false, null)) {
            BlockPreview preview = plugin.getVisualizationManager().getPreview(player);
            player.sendMessage("§aPreview started: §6Set " + preview.getBlockCount() + " blocks to " + formatMaterial(material));
            player.sendMessage("§aUse §6/nwe preview confirm §ato execute or §6/nwe preview cancel §ato cancel.");
            
            // Check if player has enough resources
            if (!plugin.getInventoryManager().hasMaterial(player, material, preview.getBlockCount())) {
                player.sendMessage("§c§lWarning: §eYou don't have enough materials! You need §6" + 
                    preview.getBlockCount() + " " + formatMaterial(material) + "§e!");
            }
            
            return true;
        } else {
            player.sendMessage("§cCouldn't create preview. Do you have a valid selection?");
            return false;
        }
    }
    
    private boolean handleReplacePreview(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /nwe preview replace <from_material> <to_material>");
            return false;
        }
        
        String fromMaterialName = args[1].toUpperCase();
        String toMaterialName = args[2].toUpperCase();
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
        
        // Start preview
        if (plugin.getVisualizationManager().startPreview(player, toMaterial, true, fromMaterial)) {
            BlockPreview preview = plugin.getVisualizationManager().getPreview(player);
            player.sendMessage("§aPreview started: §6Replace " + preview.getBlockCount() + " " + 
                formatMaterial(fromMaterial) + " blocks with " + formatMaterial(toMaterial));
            player.sendMessage("§aUse §6/nwe preview confirm §ato execute or §6/nwe preview cancel §ato cancel.");
            
            // Check if player has enough resources
            if (!plugin.getInventoryManager().hasMaterial(player, toMaterial, preview.getBlockCount())) {
                player.sendMessage("§c§lWarning: §eYou don't have enough materials! You need §6" + 
                    preview.getBlockCount() + " " + formatMaterial(toMaterial) + "§e!");
            }
            
            return true;
        } else {
            player.sendMessage("§cCouldn't create preview. Do you have a valid selection?");
            return false;
        }
    }
    
    /**
     * Format material name for display
     * @param material Material
     * @return Formatted name
     */
    private String formatMaterial(Material material) {
        return material.name().replace('_', ' ').toLowerCase();
    }
    
    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("nusawedit.preview");
    }
    
    @Override
    public String getDescription() {
        return "Preview block operations before executing them";
    }
    
    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}