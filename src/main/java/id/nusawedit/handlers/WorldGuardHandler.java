package id.nusawedit.handlers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import id.nusawedit.Plugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Handles the integration with WorldGuard
 */
public class WorldGuardHandler {
    private final Plugin plugin;
    private boolean worldGuardEnabled = false;

    /**
     * Create a new WorldGuard handler
     * @param plugin The NusaWEdit plugin
     */
    public WorldGuardHandler(Plugin plugin) {
        this.plugin = plugin;
        
        // Check if WorldGuard is available and flag was registered
        if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null && Plugin.USE_NWE_WAND != null) {
            worldGuardEnabled = true;
            plugin.getLogger().info("WorldGuard integration enabled with flag 'use-nwe-wand'");
        } else if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            plugin.getLogger().warning("WorldGuard found, but flag was not registered correctly. Protection features disabled.");
        } else {
            plugin.getLogger().info("WorldGuard not found. WorldGuard integration disabled.");
        }
    }
    
    /**
     * Check if a player can use the wand at a specific location
     * @param player Player to check
     * @param location Location to check
     * @return true if player can use the wand
     */
    public boolean canUseWand(Player player, Location location) {
        if (!worldGuardEnabled || Plugin.USE_NWE_WAND == null) {
            return true; // If WorldGuard is not enabled or flag not registered, allow by default
        }
        
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            
            // Konversi lokasi Bukkit ke format WorldGuard
            com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(location);
            
            // Konversi Player Bukkit ke LocalPlayer WorldGuard
            com.sk89q.worldguard.LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            
            // Periksa flag langsung dengan localPlayer
            return query.testState(loc, localPlayer, Plugin.USE_NWE_WAND);
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking WorldGuard flags: " + e.getMessage());
            return true; // Default to allowing if there's an error
        }
    }
    
    /**
     * Check if WorldGuard integration is enabled and flag is registered
     * @return true if enabled
     */
    public boolean isEnabled() {
        return worldGuardEnabled && Plugin.USE_NWE_WAND != null;
    }
}