package id.nusawedit.operations;

import id.nusawedit.Plugin;
import id.nusawedit.selection.Selection;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

/**
 * Handles block operations (set, replace, undo)
 */
public class BlockOperationHandler {
    private final Plugin plugin;
    private final Map<UUID, Stack<UndoOperation>> undoHistory = new HashMap<>();
    private final int MAX_UNDO_HISTORY = 10;
    private AsyncBlockOperationHandler asyncHandler;
    
    public BlockOperationHandler(Plugin plugin) {
        this.plugin = plugin;
        // Create async handler after this handler is initialized
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            this.asyncHandler = new AsyncBlockOperationHandler(plugin, this);
        }, 1L);
    }
    
    /**
     * Check if an operation can be performed based on selection size, rank limits, etc.
     * @param player Player
     * @param material Target material (for inventory check)
     * @return true if operation can be performed
     */
    public boolean canPerformOperation(Player player, Material material) {
        // Check if player has a valid selection
        if (!plugin.getSelectionManager().hasCompleteSelection(player)) {
            player.sendMessage("§cYou need to make a complete selection first!");
            return false;
        }
        
        Selection selection = plugin.getSelectionManager().getSelection(player);
        int volume = selection.getVolume();
        
        // Check if player has permission for this many blocks
        String rank = getRank(player);
        int blockLimit = plugin.getConfigManager().getRankBlockLimit(rank);
        
        if (volume > blockLimit) {
            player.sendMessage("§cYour selection is too large! Maximum: §6" + blockLimit + " blocks§c, Selected: §6" + volume + " blocks");
            return false;
        }
        
        return true;
    }
    
    /**
     * Set blocks in player's selection to a specific material
     * @param player Player
     * @param material Material to set
     * @return true if operation was successful
     */
    public boolean setBlocks(Player player, Material material) {
        // Use async handler for large selections
        Selection selection = plugin.getSelectionManager().getSelection(player);
        int volume = selection != null ? selection.getVolume() : 0;
        
        // Use async handler for selections above a threshold 
        if (volume > 1000) { 
            asyncHandler.setBlocksAsync(player, material);
            return true; // Operation started
        }
        
        // For smaller selections, use synchronous approach
        // Check if player has a valid selection
        if (!plugin.getSelectionManager().hasCompleteSelection(player)) {
            player.sendMessage("§cYou need to make a complete selection first!");
            return false;
        }
        
        // Check if player has permission for this many blocks
        String rank = getRank(player);
        int blockLimit = plugin.getConfigManager().getRankBlockLimit(rank);
        
        if (volume > blockLimit) {
            player.sendMessage("§cYour selection is too large! Maximum: §6" + blockLimit + " blocks§c, Selected: §6" + volume + " blocks");
            return false;
        }
        
        // Check if player has enough materials
        if (!plugin.getInventoryManager().hasMaterial(player, material, volume)) {
            player.sendMessage("§cYou don't have enough materials! You need §6" + volume + " " + formatMaterial(material) + "§c!");
            return false;
        }
        
        // Create undo operation
        UndoOperation undoOp = new UndoOperation(player.getUniqueId());
        
        // Process the blocks
        int affected = 0;
        for (int x = selection.getMinX(); x <= selection.getMaxX(); x++) {
            for (int y = selection.getMinY(); y <= selection.getMaxY(); y++) {
                for (int z = selection.getMinZ(); z <= selection.getMaxZ(); z++) {
                    Block block = selection.getWorld().getBlockAt(x, y, z);
                    
                    // Skip blacklisted blocks
                    if (plugin.getConfigManager().isBlacklisted(block.getType())) {
                        continue;
                    }
                    
                    // Store block for undo
                    undoOp.addBlock(block.getLocation(), block.getBlockData());
                    
                    // Change the block
                    block.setType(material);
                    affected++;
                }
            }
        }
        
        // Remove materials from player's inventory
        plugin.getInventoryManager().removeMaterial(player, material, affected);
        
        // Add undo operation to history
        addUndoOperation(player, undoOp);
        
        // Notify player
        player.sendMessage("§aSuccessfully changed §6" + affected + " blocks §ato §6" + formatMaterial(material) + "§a!");
        return true;
    }
    
    /**
     * Replace specific blocks in player's selection with another material
     * @param player Player
     * @param fromMaterial Material to replace
     * @param toMaterial Material to replace with
     * @return true if operation was successful
     */
    public boolean replaceBlocks(Player player, Material fromMaterial, Material toMaterial) {
        // Use async handler for large selections
        Selection selection = plugin.getSelectionManager().getSelection(player);
        int volume = selection != null ? selection.getVolume() : 0;
        
        if (volume > 1000) {
            asyncHandler.replaceBlocksAsync(player, fromMaterial, toMaterial);
            return true; // Operation started
        }
        
        // For smaller selections, use synchronous approach
        // Check if player has a valid selection
        if (!plugin.getSelectionManager().hasCompleteSelection(player)) {
            player.sendMessage("§cYou need to make a complete selection first!");
            return false;
        }
        
        // Count blocks to be replaced
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
        
        // Check if player has permission for this many blocks
        String rank = getRank(player);
        int blockLimit = plugin.getConfigManager().getRankBlockLimit(rank);
        
        if (toReplace > blockLimit) {
            player.sendMessage("§cToo many blocks to replace! Maximum: §6" + blockLimit + " blocks§c, Selected: §6" + toReplace + " blocks");
            return false;
        }
        
        // Check if player has enough materials
        if (!plugin.getInventoryManager().hasMaterial(player, toMaterial, toReplace)) {
            player.sendMessage("§cYou don't have enough materials! You need §6" + toReplace + " " + formatMaterial(toMaterial) + "§c!");
            return false;
        }
        
        // Create undo operation
        UndoOperation undoOp = new UndoOperation(player.getUniqueId());
        
        // Process the blocks
        int affected = 0;
        for (int x = selection.getMinX(); x <= selection.getMaxX(); x++) {
            for (int y = selection.getMinY(); y <= selection.getMaxY(); y++) {
                for (int z = selection.getMinZ(); z <= selection.getMaxZ(); z++) {
                    Block block = selection.getWorld().getBlockAt(x, y, z);
                    
                    // Only replace matching blocks
                    if (block.getType() == fromMaterial && !plugin.getConfigManager().isBlacklisted(block.getType())) {
                        // Store block for undo
                        undoOp.addBlock(block.getLocation(), block.getBlockData());
                        
                        // Change the block
                        block.setType(toMaterial);
                        affected++;
                    }
                }
            }
        }
        
        // Remove new materials from player's inventory
        plugin.getInventoryManager().removeMaterial(player, toMaterial, affected);
        
        // Add old materials to player's inventory
        plugin.getInventoryManager().addMaterial(player, fromMaterial, affected);
        
        // Add undo operation to history
        addUndoOperation(player, undoOp);
        
        // Notify player
        player.sendMessage("§aSuccessfully replaced §6" + affected + " " + formatMaterial(fromMaterial) + 
                " §awith §6" + formatMaterial(toMaterial) + "§a!");
        return true;
    }
    
    /**
     * Undo the last operation performed by a player
     * @param player Player
     * @return true if undo was successful
     */
    public boolean undoLastOperation(Player player) {
        // Cancel any running operations first
        if (asyncHandler != null && asyncHandler.hasActiveOperation(player)) {
            asyncHandler.cancelOperations(player);
            return true;
        }
        
        Stack<UndoOperation> history = undoHistory.get(player.getUniqueId());
        
        if (history == null || history.isEmpty()) {
            player.sendMessage("§cNo operations to undo!");
            return false;
        }
        
        UndoOperation undoOp = history.pop();
        Map<Material, Integer> materials = new HashMap<>();
        
        // Restore all blocks
        for (Map.Entry<Location, BlockData> entry : undoOp.getBlocks().entrySet()) {
            Location location = entry.getKey();
            BlockData oldData = entry.getValue();
            Block block = location.getBlock();
            
            // Count materials for returning to inventory
            Material currentType = block.getType();
            if (currentType != Material.AIR) {
                materials.put(currentType, materials.getOrDefault(currentType, 0) + 1);
            }
            
            // Restore original state
            block.setBlockData(oldData);
        }
        
        // Return materials to player's inventory
        for (Map.Entry<Material, Integer> entry : materials.entrySet()) {
            plugin.getInventoryManager().addMaterial(player, entry.getKey(), entry.getValue());
        }
        
        player.sendMessage("§aSuccessfully undid the last operation! (§6" + undoOp.getBlocks().size() + " blocks§a)");
        return true;
    }
    
    /**
     * Add an undo operation to a player's history
     * @param player Player
     * @param operation Operation to add
     */
    public void addUndoOperation(Player player, UndoOperation operation) {
        UUID playerId = player.getUniqueId();
        Stack<UndoOperation> history = undoHistory.computeIfAbsent(playerId, k -> new Stack<>());
        
        // Ensure history doesn't grow too large
        while (history.size() >= MAX_UNDO_HISTORY) {
            history.remove(0);
        }
        
        history.push(operation);
    }
    
    /**
     * Get a player's rank
     * @param player Player
     * @return Rank name (defaults to "Aetherian")
     */
    public String getRank(Player player) {
        // Check permissions from highest to lowest rank
        if (player.hasPermission("nusawedit.rank.sovereign")) return "Sovereign";
        if (player.hasPermission("nusawedit.rank.nebula")) return "Nebula";
        if (player.hasPermission("nusawedit.rank.skyforge")) return "Skyforge";
        if (player.hasPermission("nusawedit.rank.skymason")) return "Skymason";
        return "Aetherian";
    }
    
    /**
     * Format material name for display
     * @param material Material
     * @return Formatted name
     */
    public String formatMaterial(Material material) {
        return material.name().replace('_', ' ').toLowerCase();
    }
    
    /**
     * Get the async handler
     * @return AsyncBlockOperationHandler
     */
    public AsyncBlockOperationHandler getAsyncHandler() {
        return asyncHandler;
    }
    
    /**
     * Cancel any active operations for a player
     * @param player Player
     * @return true if any operation was cancelled
     */
    public boolean cancelOperations(Player player) {
        if (asyncHandler != null && asyncHandler.hasActiveOperation(player)) {
            asyncHandler.cancelOperations(player);
            return true;
        }
        return false;
    }
}