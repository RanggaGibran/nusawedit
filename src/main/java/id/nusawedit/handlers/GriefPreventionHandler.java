package id.nusawedit.handlers;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.Material;

/**
 * Handles integration with GriefPrevention
 */
public class GriefPreventionHandler {
    private final Plugin plugin;
    private boolean enabled = false;
    private GriefPrevention griefPrevention;

    /**
     * Create a new GriefPrevention handler
     * @param plugin The NusaWEdit plugin
     */
    public GriefPreventionHandler(Plugin plugin) {
        this.plugin = plugin;
        
        // Check if GriefPrevention is available
        Plugin gpPlugin = plugin.getServer().getPluginManager().getPlugin("GriefPrevention");
        if (gpPlugin != null && gpPlugin instanceof GriefPrevention) {
            griefPrevention = (GriefPrevention) gpPlugin;
            enabled = true;
            plugin.getLogger().info("GriefPrevention integration enabled. Claim protection active.");
        } else {
            plugin.getLogger().info("GriefPrevention not found. Claim protection disabled.");
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
            return true; // If GriefPrevention is not enabled, allow by default
        }
        
        try {
            // Get claim at location
            Claim claim = griefPrevention.dataStore.getClaimAt(location, true, null);
            
            // If no claim at this location or player has admin permissions, allow
            if (claim == null || player.hasPermission("nusawedit.admin.bypass")) {
                return true;
            }
            
            // Check if player is the owner or has trust permissions
            String errorMessage = claim.allowBuild(player, Material.AIR);
            return errorMessage == null;
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking GriefPrevention permissions: " + e.getMessage());
            return true; // Default to allowing if there's an error
        }
    }
    
    /**
     * Check if GriefPrevention integration is enabled
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
}