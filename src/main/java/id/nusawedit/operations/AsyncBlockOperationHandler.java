package id.nusawedit.operations;

import id.nusawedit.Plugin;
import id.nusawedit.selection.Selection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Handles block operations asynchronously with chunk-based processing
 * to reduce server lag
 */
public class AsyncBlockOperationHandler {
    private final Plugin plugin;
    private final BlockOperationHandler standardHandler;
    
    // How many blocks to process per batch
    private static final int BATCH_SIZE = 500;
    
    // Delay between batches in ticks (1 tick = 1/20 second)
    private static final int BATCH_DELAY = 1;
    
    // Track active operations
    private final Map<UUID, BukkitTask> activeOperations = new HashMap<>();
    
    public AsyncBlockOperationHandler(Plugin plugin, BlockOperationHandler standardHandler) {
        this.plugin = plugin;
        this.standardHandler = standardHandler;
    }
    
    /**
     * Cancel any pending operations for a player
     * @param player Player
     */
    public void cancelOperations(Player player) {
        UUID playerId = player.getUniqueId();
        BukkitTask task = activeOperations.remove(playerId);
        if (task != null) {
            task.cancel();
            player.sendMessage(plugin.getMessageManager().getMessage("cancel.all-cancelled"));
        }
    }
    
    /**
     * Check if player has an active operation
     * @param player Player
     * @return true if player has an active operation
     */
    public boolean hasActiveOperation(Player player) {
        return activeOperations.containsKey(player.getUniqueId());
    }
    
    /**
     * Set blocks in player's selection to a specific material with batched processing
     * @param player Player
     * @param material Material to set
     * @return CompletableFuture that completes when operation is done
     */
    public CompletableFuture<Boolean> setBlocksAsync(Player player, Material material) {
        // Check for existing operations
        if (hasActiveOperation(player)) {
            player.sendMessage(plugin.getMessageManager().getMessage("async.operation-in-progress"));
            return CompletableFuture.completedFuture(false);
        }
        
        // Run preliminary checks synchronously
        if (!standardHandler.canPerformOperation(player, material)) {
            return CompletableFuture.completedFuture(false);
        }
        
        Selection selection = plugin.getSelectionManager().getSelection(player);
        int volume = selection.getVolume();
        
        // Create result future
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        
        // Create list of blocks to process
        List<Location> blocksToProcess = new ArrayList<>();
        
        // Collect blocks to process (done synchronously for now)
        for (int x = selection.getMinX(); x <= selection.getMaxX(); x++) {
            for (int y = selection.getMinY(); y <= selection.getMaxY(); y++) {
                for (int z = selection.getMinZ(); z <= selection.getMaxZ(); z++) {
                    Block block = selection.getWorld().getBlockAt(x, y, z);
                    
                    // Skip blacklisted blocks
                    if (plugin.getConfigManager().isBlacklisted(block.getType())) {
                        continue;
                    }
                    
                    blocksToProcess.add(block.getLocation());
                }
            }
        }
        
        // Check if there are blocks to process
        if (blocksToProcess.isEmpty()) {
            player.sendMessage(plugin.getMessageManager().getMessage("operations.no-applicable-blocks"));
            return CompletableFuture.completedFuture(false);
        }
        
        // Check if player has enough materials
        int totalBlocks = blocksToProcess.size();
        if (!plugin.getInventoryManager().hasMaterial(player, material, totalBlocks)) {
            player.sendMessage(plugin.getMessageManager().getFormattedMessage(
                "operations.not-enough-materials", totalBlocks, formatMaterial(material)));
            return CompletableFuture.completedFuture(false);
        }
        
        // Create undo operation
        UndoOperation undoOp = new UndoOperation(player.getUniqueId());
        
        // Remove materials from player's inventory before starting
        plugin.getInventoryManager().removeMaterial(player, material, totalBlocks);
        
        // Start progress message
        player.sendMessage(plugin.getMessageManager().getMessage("async.operation-starting"));
        player.sendMessage(plugin.getMessageManager().getMessage("async.operation-may-take-time"));
        
        // Process blocks in batches
        processBatchedSetOperation(player, blocksToProcess, material, undoOp, 0, totalBlocks, result);
        
        return result;
    }
    
    /**
     * Process blocks in batches for set operation
     */
    private void processBatchedSetOperation(Player player, List<Location> blocks, Material material, 
                                       UndoOperation undoOp, int processed, int total, 
                                       CompletableFuture<Boolean> result) {
    
    final int[] currentIndex = {processed};
    final int[] lastReportedPercentage = {0}; // Track last reported percentage
    
    // Start batch processing task
    BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
        @Override
        public void run() {
            int batchCount = 0;
            
            // Process a batch of blocks
            while (currentIndex[0] < blocks.size() && batchCount < BATCH_SIZE) {
                Location loc = blocks.get(currentIndex[0]);
                Block block = loc.getBlock();
                
                // Store original block for undo
                undoOp.addBlock(loc, block.getBlockData());
                
                // Set the new block
                block.setType(material, false); // false = don't apply physics
                
                currentIndex[0]++;
                batchCount++;
            }
            
            // Report progress at specified intervals
            int currentPercentage = (int) ((double) currentIndex[0] / total * 100);
            if (currentPercentage - lastReportedPercentage[0] >= 5) {
                player.sendMessage(plugin.getMessageManager().getFormattedMessage(
                    "async.operation-progress", currentPercentage));
                lastReportedPercentage[0] = currentPercentage;
            }
            
            // Check if done
            if (currentIndex[0] >= blocks.size()) {
                // Cancel task
                Bukkit.getScheduler().cancelTask(activeOperations.remove(player.getUniqueId()).getTaskId());
                
                // Add undo operation to history
                standardHandler.addUndoOperation(player, undoOp);
                
                // Notify player
                player.sendMessage(plugin.getMessageManager().getFormattedMessage(
                    "operations.set-success", total, formatMaterial(material)));
                
                // Complete future
                result.complete(true);
            }
        }
    }, 0L, BATCH_DELAY);
    
    // Store the task
    activeOperations.put(player.getUniqueId(), task);
}
    
    /**
     * Replace blocks in player's selection asynchronously
     * @param player Player
     * @param fromMaterial Material to replace
     * @param toMaterial Material to replace with
     * @return CompletableFuture that completes when operation is done
     */
    public CompletableFuture<Boolean> replaceBlocksAsync(Player player, Material fromMaterial, Material toMaterial) {
        // Check for existing operations
        if (hasActiveOperation(player)) {
            player.sendMessage("§cYou already have an operation in progress. Please wait or use /nwe cancel.");
            return CompletableFuture.completedFuture(false);
        }
        
        // Run preliminary checks synchronously
        if (!plugin.getSelectionManager().hasCompleteSelection(player)) {
            player.sendMessage("§cYou need to make a complete selection first!");
            return CompletableFuture.completedFuture(false);
        }
        
        Selection selection = plugin.getSelectionManager().getSelection(player);
        
        // First, scan to find blocks to replace
        CompletableFuture<List<Location>> scanResult = new CompletableFuture<>();
        
        // Start scan task asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Location> blocksToReplace = new ArrayList<>();
            
            // Count blocks to be replaced
            for (int x = selection.getMinX(); x <= selection.getMaxX(); x++) {
                for (int y = selection.getMinY(); y <= selection.getMaxY(); y++) {
                    for (int z = selection.getMinZ(); z <= selection.getMaxZ(); z++) {
                        // Need to switch to sync for checking block types
                        int finalX = x;
                        int finalY = y;
                        int finalZ = z;
                        
                        // Queue block check
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Block block = selection.getWorld().getBlockAt(finalX, finalY, finalZ);
                            if (block.getType() == fromMaterial && !plugin.getConfigManager().isBlacklisted(block.getType())) {
                                blocksToReplace.add(block.getLocation());
                            }
                        });
                    }
                }
            }
            
            // Continue after all blocks are checked
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                scanResult.complete(blocksToReplace);
            }, 5L); // Wait a bit to ensure all checks are processed
        });
        
        // Process result of scan
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        
        scanResult.thenAccept(blocksToReplace -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Check if any blocks found
                if (blocksToReplace.isEmpty()) {
                    player.sendMessage("§cNo blocks of type §6" + formatMaterial(fromMaterial) + " §cfound in the selection.");
                    result.complete(false);
                    return;
                }
                
                int toReplace = blocksToReplace.size();
                
                // Check if player has permission for this many blocks
                String rank = standardHandler.getRank(player);
                int blockLimit = plugin.getConfigManager().getRankBlockLimit(rank);
                
                if (toReplace > blockLimit) {
                    player.sendMessage("§cToo many blocks to replace! Maximum: §6" + blockLimit + " blocks§c, Selected: §6" + toReplace + " blocks");
                    result.complete(false);
                    return;
                }
                
                // Check if player has enough materials
                if (!plugin.getInventoryManager().hasMaterial(player, toMaterial, toReplace)) {
                    player.sendMessage("§cYou don't have enough materials! You need §6" + toReplace + " " + formatMaterial(toMaterial) + "§c!");
                    result.complete(false);
                    return;
                }
                
                // Create undo operation
                UndoOperation undoOp = new UndoOperation(player.getUniqueId());
                
                // Remove new materials from player's inventory
                plugin.getInventoryManager().removeMaterial(player, toMaterial, toReplace);
                
                // Start progress message
                player.sendMessage("§aBeginning replace operation. Please wait...");
                player.sendMessage("§7This may take a moment for large selections.");
                
                // Process blocks in batches
                processBatchedReplaceOperation(player, blocksToReplace, fromMaterial, toMaterial, undoOp, 0, toReplace, result);
            });
        });
        
        return result;
    }
    
    /**
     * Process blocks in batches for replace operation
     */
    private void processBatchedReplaceOperation(Player player, List<Location> blocks, Material fromMaterial, 
                                               Material toMaterial, UndoOperation undoOp, int processed, 
                                               int total, CompletableFuture<Boolean> result) {
        
        // Start batch processing task
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private int currentIndex = 0;
            private int totalProcessed = processed;
            
            @Override
            public void run() {
                // Process a batch of blocks
                int batchCount = 0;
                
                while (currentIndex < blocks.size() && batchCount < BATCH_SIZE) {
                    Location location = blocks.get(currentIndex);
                    Block block = location.getBlock();
                    
                    // Double-check block type (it might have changed)
                    if (block.getType() == fromMaterial) {
                        // Store block for undo
                        undoOp.addBlock(location, block.getBlockData());
                        
                        // Change the block
                        block.setType(toMaterial);
                        totalProcessed++;
                    }
                    
                    currentIndex++;
                    batchCount++;
                }
                
                // Send progress update every 10% or at the end
                int progressPercent = (currentIndex * 100) / blocks.size();
                if (progressPercent % 10 == 0 || currentIndex == blocks.size()) {
                    player.sendMessage("§7Progress: §e" + progressPercent + "% §7(§e" + currentIndex + "§7/§e" + blocks.size() + "§7 blocks)");
                }
                
                // Check if we're done
                if (currentIndex >= blocks.size()) {
                    // Clean up and complete
                    activeOperations.remove(player.getUniqueId());
                    ((BukkitTask) activeOperations.get(player.getUniqueId())).cancel();
                    
                    // Add old materials to player's inventory
                    plugin.getInventoryManager().addMaterial(player, fromMaterial, totalProcessed);
                    
                    // Add undo operation to history
                    standardHandler.addUndoOperation(player, undoOp);
                    
                    // Complete the future
                    player.sendMessage("§aOperation complete! Replaced §6" + totalProcessed + " " + formatMaterial(fromMaterial) + 
                            " §awith §6" + formatMaterial(toMaterial) + "§a!");
                    result.complete(true);
                }
            }
        }, 0L, BATCH_DELAY);
        
        // Store the task
        activeOperations.put(player.getUniqueId(), task);
    }
    
    /**
     * Set blocks in player's selection to a pattern of materials with batched processing
     * @param player Player
     * @param pattern Pattern of materials
     * @return CompletableFuture that completes when operation is done
     */
    public CompletableFuture<Boolean> setBlocksPatternAsync(Player player, BlockPattern pattern) {
        // Check if player has a valid selection
        if (!plugin.getSelectionManager().hasCompleteSelection(player)) {
            player.sendMessage("§cYou need to make a complete selection first!");
            return CompletableFuture.completedFuture(false);
        }
        
        // Check if an operation is already running
        if (activeOperations.containsKey(player.getUniqueId())) {
            player.sendMessage("§cYou already have an operation in progress. Use /nwe cancel to cancel it.");
            return CompletableFuture.completedFuture(false);
        }
        
        Selection selection = plugin.getSelectionManager().getSelection(player);
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        
        // Create list of blocks to process
        List<Location> blocksToProcess = new ArrayList<>();
        
        // Collect blocks to process
        for (int x = selection.getMinX(); x <= selection.getMaxX(); x++) {
            for (int y = selection.getMinY(); y <= selection.getMaxY(); y++) {
                for (int z = selection.getMinZ(); z <= selection.getMaxZ(); z++) {
                    Block block = selection.getWorld().getBlockAt(x, y, z);
                    
                    // Skip blacklisted blocks
                    if (plugin.getConfigManager().isBlacklisted(block.getType())) {
                        continue;
                    }
                    
                    blocksToProcess.add(block.getLocation());
                }
            }
        }
        
        // Check if there are blocks to process
        if (blocksToProcess.isEmpty()) {
            player.sendMessage("§cNo applicable blocks found in the selection!");
            return CompletableFuture.completedFuture(false);
        }
        
        int totalBlocks = blocksToProcess.size();
        
        // Calculate material requirements
        Map<Material, Integer> materialEstimates = pattern.calculateRequirements(totalBlocks);
        
        // Check if player has enough of each material
        for (Map.Entry<Material, Integer> entry : materialEstimates.entrySet()) {
            if (!plugin.getInventoryManager().hasMaterial(player, entry.getKey(), entry.getValue())) {
                player.sendMessage("§cYou don't have enough materials! You need approximately §6" + entry.getValue() + 
                        " " + formatMaterial(entry.getKey()) + "§c!");
                return CompletableFuture.completedFuture(false);
            }
        }
        
        // Create undo operation
        UndoOperation undoOp = new UndoOperation(player.getUniqueId());
        
        // Remove materials from player's inventory before starting
        for (Map.Entry<Material, Integer> entry : materialEstimates.entrySet()) {
            plugin.getInventoryManager().removeMaterial(player, entry.getKey(), entry.getValue());
        }
        
        // Start progress message
        player.sendMessage(plugin.getMessageManager().getMessage("async.operation-starting"));
        player.sendMessage(plugin.getMessageManager().getMessage("async.operation-may-take-time"));
        
        // Process blocks in batches
        processBatchedSetPatternOperation(player, blocksToProcess, pattern, undoOp, 0, totalBlocks, materialEstimates, result);
        
        return result;
    }
    
    /**
     * Process blocks in batches for set pattern operation
     */
    private void processBatchedSetPatternOperation(Player player, List<Location> blocks, BlockPattern pattern, 
                                               UndoOperation undoOp, int processed, int total, 
                                               Map<Material, Integer> materialEstimates, CompletableFuture<Boolean> result) {
        
        // Start batch processing task
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private int currentIndex = 0;
            private int totalProcessed = processed;
            private final Map<Material, Integer> materialsUsed = new HashMap<>();
            
            @Override
            public void run() {
                // Process a batch of blocks
                int batchCount = 0;
                
                while (currentIndex < blocks.size() && batchCount < BATCH_SIZE) {
                    Location location = blocks.get(currentIndex);
                    Block block = location.getBlock();
                    
                    // Store block for undo
                    undoOp.addBlock(location, block.getBlockData());
                    
                    // Get random material from pattern
                    Material material = pattern.getRandomMaterial();
                    
                    // Change the block
                    block.setType(material);
                    
                    // Count the material used
                    materialsUsed.put(material, materialsUsed.getOrDefault(material, 0) + 1);
                    
                    currentIndex++;
                    totalProcessed++;
                    batchCount++;
                }
                
                // Send progress update every 10% or at the end
                int progressPercent = (totalProcessed * 100) / total;
                if (progressPercent % 10 == 0 || totalProcessed == total) {
                    player.sendMessage("§7Progress: §e" + progressPercent + "% §7(§e" + totalProcessed + "§7/§e" + total + "§7 blocks)");
                }
                
                // Check if we're done
                if (currentIndex >= blocks.size()) {
                    // Clean up and complete
                    activeOperations.remove(player.getUniqueId());
                    ((BukkitTask) activeOperations.get(player.getUniqueId())).cancel();
                    
                    // Add undo operation to history
                    standardHandler.addUndoOperation(player, undoOp);
                    
                    // Return any unused materials
                    for (Map.Entry<Material, Integer> entry : materialEstimates.entrySet()) {
                        int returned = entry.getValue() - materialsUsed.getOrDefault(entry.getKey(), 0);
                        if (returned > 0) {
                            plugin.getInventoryManager().addMaterial(player, entry.getKey(), returned);
                        }
                    }
                    
                    // Complete the future
                    if (pattern.size() == 1) {
                        player.sendMessage("§aOperation complete! Changed §6" + total + " blocks §ato §6" + 
                                formatMaterial(pattern.getMaterials().get(0)) + "§a!");
                    } else {
                        player.sendMessage("§aOperation complete! Changed §6" + total + " blocks §ato mixed materials!");
                    }
                    result.complete(true);
                }
            }
        }, 0L, BATCH_DELAY);
        
        // Store the task
        activeOperations.put(player.getUniqueId(), task);
    }
    
    /**
     * Format material name for display
     * @param material Material
     * @return Formatted name
     */
    private String formatMaterial(Material material) {
        return material.name().replace('_', ' ').toLowerCase();
    }
}