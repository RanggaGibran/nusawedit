package id.nusawedit.visualization;

import id.nusawedit.Plugin;
import id.nusawedit.selection.Selection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Manages visualization of selections and operation previews
 */
public class VisualizationManager {
    private final Plugin plugin;
    private BukkitTask particleTask;
    
    // Players who have visualization enabled
    private final Set<UUID> visualizationEnabled = new HashSet<>();
    
    // Track active previews
    private final Map<UUID, BlockPreview> activePreview = new HashMap<>();
    
    // Particles configuration
    private final DustOptions SELECTION_PARTICLES = new DustOptions(Color.fromRGB(255, 255, 0), 1.0f); // Yellow
    private final DustOptions PREVIEW_PARTICLES = new DustOptions(Color.fromRGB(0, 255, 255), 1.0f); // Cyan
    
    public VisualizationManager(Plugin plugin) {
        this.plugin = plugin;
        startVisualizationTask();
    }
    
    /**
     * Start the visualization task for showing particles
     */
    private void startVisualizationTask() {
        // Cancel existing task if it exists
        if (particleTask != null) {
            particleTask.cancel();
        }
        
        // Run particle task every half second
        particleTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            // Show selection particles for online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID playerId = player.getUniqueId();
                
                // Only show for players who have visualization enabled
                if (!visualizationEnabled.contains(playerId)) {
                    continue;
                }
                
                // Show selection boundaries
                Selection selection = plugin.getSelectionManager().getSelection(player);
                if (selection != null && selection.isComplete()) {
                    showSelectionBoundaries(player, selection);
                }
                
                // Show preview if active
                BlockPreview preview = activePreview.get(playerId);
                if (preview != null) {
                    showPreviewBoundaries(player, preview);
                }
            }
        }, 10L, 10L); // Every half second (10 ticks)
    }
    
    /**
     * Clean up resources
     */
    public void shutdown() {
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
        
        activePreview.clear();
        visualizationEnabled.clear();
    }
    
    /**
     * Toggle visualization for a player
     * @param player Player
     * @return New visualization state (true = enabled)
     */
    public boolean toggleVisualization(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (visualizationEnabled.contains(playerId)) {
            visualizationEnabled.remove(playerId);
            return false;
        } else {
            visualizationEnabled.add(playerId);
            return true;
        }
    }
    
    /**
     * Check if a player has visualization enabled
     * @param player Player
     * @return true if enabled
     */
    public boolean hasVisualizationEnabled(Player player) {
        return visualizationEnabled.contains(player.getUniqueId());
    }
    
    /**
     * Show selection boundaries for a player
     * @param player Player to show particles to
     * @param selection Selection to visualize
     */
    private void showSelectionBoundaries(Player player, Selection selection) {
        if (!selection.isComplete()) return;
        
        int minX = selection.getMinX();
        int minY = selection.getMinY();
        int minZ = selection.getMinZ();
        int maxX = selection.getMaxX();
        int maxY = selection.getMaxY();
        int maxZ = selection.getMaxZ();
        
        // Calculate edge points with reduced density
        Set<Location> edgePoints = new HashSet<>();
        
        // How many blocks apart to place particles (higher = less particles)
        int spacing = Math.max(1, (int)Math.ceil((maxX - minX + maxY - minY + maxZ - minZ) / 100.0));
        
        // Add edge points for each edge of the cuboid
        for (int x = minX; x <= maxX; x += spacing) {
            addEdgePoint(edgePoints, x, minY, minZ, selection);
            addEdgePoint(edgePoints, x, minY, maxZ, selection);
            addEdgePoint(edgePoints, x, maxY, minZ, selection);
            addEdgePoint(edgePoints, x, maxY, maxZ, selection);
        }
        
        for (int y = minY; y <= maxY; y += spacing) {
            addEdgePoint(edgePoints, minX, y, minZ, selection);
            addEdgePoint(edgePoints, minX, y, maxZ, selection);
            addEdgePoint(edgePoints, maxX, y, minZ, selection);
            addEdgePoint(edgePoints, maxX, y, maxZ, selection);
        }
        
        for (int z = minZ; z <= maxZ; z += spacing) {
            addEdgePoint(edgePoints, minX, minY, z, selection);
            addEdgePoint(edgePoints, minX, maxY, z, selection);
            addEdgePoint(edgePoints, maxX, minY, z, selection);
            addEdgePoint(edgePoints, maxX, maxY, z, selection);
        }
        
        // Spawn particles
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Location loc : edgePoints) {
                player.spawnParticle(Particle.REDSTONE, loc.add(0.5, 0.5, 0.5), 1, 0, 0, 0, 0, SELECTION_PARTICLES);
            }
        });
    }
    
    /**
     * Safely add an edge point
     * @param points Set to add to
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param selection Selection for world reference
     */
    private void addEdgePoint(Set<Location> points, int x, int y, int z, Selection selection) {
        if (selection.getWorld() != null) {
            points.add(new Location(selection.getWorld(), x, y, z));
        }
    }
    
    /**
     * Start a block operation preview
     * @param player Player
     * @param material Target material
     * @param isReplace Whether this is a replace operation
     * @param fromMaterial Source material for replace operation
     * @return true if preview was created
     */
    public boolean startPreview(Player player, Material material, boolean isReplace, Material fromMaterial) {
        UUID playerId = player.getUniqueId();
        
        // Cancel existing preview
        cancelPreview(player);
        
        // Check if selection is complete
        Selection selection = plugin.getSelectionManager().getSelection(player);
        if (selection == null || !selection.isComplete()) {
            return false;
        }
        
        // Create new preview
        BlockPreview preview = new BlockPreview(selection, material, isReplace, fromMaterial);
        
        // Calculate affected blocks for preview
        int minX = selection.getMinX();
        int minY = selection.getMinY();
        int minZ = selection.getMinZ();
        int maxX = selection.getMaxX();
        int maxY = selection.getMaxY();
        int maxZ = selection.getMaxZ();
        
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = selection.getWorld().getBlockAt(x, y, z);
                    
                    // Skip blacklisted blocks
                    if (plugin.getConfigManager().isBlacklisted(block.getType())) {
                        continue;
                    }
                    
                    if (!isReplace || block.getType() == fromMaterial) {
                        preview.addBlock(block.getLocation());
                    }
                }
            }
        }
        
        // Store preview
        activePreview.put(playerId, preview);
        
        // Enable visualization for this player if not already enabled
        if (!visualizationEnabled.contains(playerId)) {
            visualizationEnabled.add(playerId);
        }
        
        return true;
    }
    
    /**
     * Show preview boundaries for a player
     * @param player Player to show particles to
     * @param preview Preview to visualize
     */
    private void showPreviewBoundaries(Player player, BlockPreview preview) {
        // Calculate particles to show based on density
        Set<Location> previewPoints = new HashSet<>();
        
        // Limit total particles shown
        int maxParticles = 200;
        int particleSpacing = Math.max(1, preview.getBlocks().size() / maxParticles);
        
        int counter = 0;
        for (Location loc : preview.getBlocks()) {
            if (counter % particleSpacing == 0) {
                previewPoints.add(loc);
            }
            counter++;
        }
        
        // Spawn particles
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Location loc : previewPoints) {
                player.spawnParticle(Particle.REDSTONE, loc.add(0.5, 0.5, 0.5), 1, 0, 0, 0, 0, PREVIEW_PARTICLES);
            }
        });
    }
    
    /**
     * Cancel an active preview
     * @param player Player
     * @return true if a preview was cancelled
     */
    public boolean cancelPreview(Player player) {
        UUID playerId = player.getUniqueId();
        BlockPreview preview = activePreview.remove(playerId);
        return preview != null;
    }
    
    /**
     * Check if player has an active preview
     * @param player Player
     * @return true if preview is active
     */
    public boolean hasActivePreview(Player player) {
        return activePreview.containsKey(player.getUniqueId());
    }
    
    /**
     * Confirm and execute a preview operation
     * @param player Player
     * @return true if operation was executed
     */
    public boolean confirmPreview(Player player) {
        UUID playerId = player.getUniqueId();
        BlockPreview preview = activePreview.get(playerId);
        
        if (preview == null) {
            return false;
        }
        
        // Execute the operation
        boolean success;
        if (preview.isReplace()) {
            success = plugin.getBlockOperationHandler().replaceBlocks(player, preview.getFromMaterial(), preview.getMaterial());
        } else {
            success = plugin.getBlockOperationHandler().setBlocks(player, preview.getMaterial());
        }
        
        // Remove the preview if successful
        if (success) {
            activePreview.remove(playerId);
        }
        
        return success;
    }
    
    /**
     * Get the preview for a player
     * @param player Player
     * @return Block preview or null if none
     */
    public BlockPreview getPreview(Player player) {
        return activePreview.get(player.getUniqueId());
    }
}