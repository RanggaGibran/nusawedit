package id.nusawedit.handlers;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import id.nusawedit.Plugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Handles integration with SuperiorSkyblock
 */
public class SuperiorSkyblockHandler {
    private final Plugin plugin;
    private boolean enabled = false;

    /**
     * Create a new SuperiorSkyblock handler
     * @param plugin The NusaWEdit plugin
     */
    public SuperiorSkyblockHandler(Plugin plugin) {
        this.plugin = plugin;
        
        // Check if SuperiorSkyblock is available
        if (plugin.getServer().getPluginManager().getPlugin("SuperiorSkyblock2") != null) {
            enabled = true;
            plugin.getLogger().info("SuperiorSkyblock integration enabled. Island protection active.");
        } else {
            plugin.getLogger().info("SuperiorSkyblock not found. Island protection disabled.");
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
            return true; // If SuperiorSkyblock is not enabled, allow by default
        }
        
        try {
            // Get the island at the location
            Island island = SuperiorSkyblockAPI.getGrid().getIslandAt(location);
            
            // If location is not on an island, allow usage
            if (island == null) {
                return true;
            }
            
            // Get the SuperiorPlayer wrapper for the player
            SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(player.getUniqueId());
            
            // Check if player is a member of the island or has admin bypass
            return island.isMember(superiorPlayer) || player.hasPermission("nusawedit.admin.bypass");
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking SuperiorSkyblock permissions: " + e.getMessage());
            return true; // Default to allowing if there's an error
        }
    }
    
    /**
     * Check if SuperiorSkyblock integration is enabled
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
}