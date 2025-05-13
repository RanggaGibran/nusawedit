package id.nusawedit.handlers;

import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;

import id.nusawedit.Plugin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Handles integration with Towny
 */
public class TownyHandler {
    private final Plugin plugin;
    private boolean enabled = false;

    /**
     * Create a new Towny handler
     * @param plugin The NusaWEdit plugin
     */
    public TownyHandler(Plugin plugin) {
        this.plugin = plugin;
        
        // Check if Towny is available
        if (plugin.getServer().getPluginManager().getPlugin("Towny") != null) {
            enabled = true;
            plugin.getLogger().info("Towny integration enabled");
        } else {
            plugin.getLogger().info("Towny not found. Towny integration disabled.");
        }
    }
    
    /**
     * Check if a player can use the wand at a specific location
     * @param player Player to check
     * @param location Location to check
     * @return true if player can use the wand
     */
    public boolean canUseWand(Player player, Location location) {
        if (!enabled) {
            return true; // If Towny is not enabled, allow by default
        }
        
        try {
            // If player has admin bypass permission, allow
            if (player.hasPermission("nusawedit.admin.bypass")) {
                return true;
            }
            
            // Check if the location is in wilderness (not claimed by any town)
            if (!com.palmergames.bukkit.towny.TownyAPI.getInstance().isWilderness(location)) {
                // Location is in a town, check building permissions
                Material blockMaterial = location.getBlock().getType();
                return PlayerCacheUtil.getCachePermission(player, location, 
                        blockMaterial, ActionType.BUILD);
            } else {
                // Location is in wilderness, allow usage
                return true;
            }
                    
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking Towny permissions: " + e.getMessage());
            return true; // Default to allowing if there's an error
        }
    }
    
    /**
     * Check if Towny integration is enabled
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
}