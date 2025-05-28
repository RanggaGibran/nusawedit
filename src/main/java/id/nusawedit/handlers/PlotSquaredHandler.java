package id.nusawedit.handlers;

import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.bukkit.util.BukkitUtil;

import id.nusawedit.Plugin;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Handles integration with PlotSquared
 */
public class PlotSquaredHandler {
    private final Plugin plugin;
    private boolean enabled = false;

    /**
     * Create a new PlotSquared handler
     * @param plugin The NusaWEdit plugin
     */
    public PlotSquaredHandler(Plugin plugin) {
        this.plugin = plugin;
        
        // Check if PlotSquared is available
        if (plugin.getServer().getPluginManager().getPlugin("PlotSquared") != null) {
            enabled = true;
            plugin.getLogger().info("PlotSquared integration enabled");
        } else {
            plugin.getLogger().info("PlotSquared not found. PlotSquared integration disabled.");
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
            return true; // If PlotSquared is not enabled, allow by default
        }
        
        try {
            // If player has admin bypass permission, allow
            if (player.hasPermission("nusawedit.admin.bypass")) {
                return true;
            }
            
            // Convert Bukkit Location to PlotSquared Location
            com.plotsquared.core.location.Location plotLocation = BukkitUtil.adapt(location);
            
            // Get the plot at the location
            Plot plot = plotLocation.getPlotAbs();
            
            // If location is not on a plot, check if server allows building in the road
            if (plot == null) {
                // Typically, building in road areas is restricted, so we'll return false
                return false;
            }
            
            // Get the PlotPlayer
            PlotPlayer<?> plotPlayer = PlotPlayer.from(player);
            
            // Check if player is the owner or a trusted member of the plot
            return plot.isOwner(plotPlayer.getUUID()) || 
                   plot.getTrusted().contains(plotPlayer.getUUID()) ||
                   plot.getMembers().contains(plotPlayer.getUUID());
                    
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking PlotSquared permissions: " + e.getMessage());
            return true; // Default to allowing if there's an error
        }
    }
    
    /**
     * Check if PlotSquared integration is enabled
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
}