package id.nusawedit.config;

import id.nusawedit.Plugin;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Manages configuration for NusaWEdit
 */
public class ConfigManager {
    private final Plugin plugin;
    private FileConfiguration config;
    private File configFile;
    
    // Cache for frequently accessed config values
    private final Map<String, Integer> rankLimits = new HashMap<>();
    private final Set<Material> blacklistedMaterials = new HashSet<>();
    
    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Load configuration from file
     */
    public void loadConfig() {
        // Create config if it doesn't exist
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        
        // Load rank limits
        loadRankLimits();
        
        // Load blacklisted blocks
        loadBlacklistedBlocks();
    }
    
    /**
     * Reload configuration from file
     */
    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        // Clear and reload caches
        rankLimits.clear();
        blacklistedMaterials.clear();
        
        loadRankLimits();
        loadBlacklistedBlocks();
    }
    
    /**
     * Load block limits for each rank
     */
    private void loadRankLimits() {
        if (!config.isConfigurationSection("ranks")) {
            // Set default values
            config.set("ranks.Aetherian", 100);
            config.set("ranks.Skymason", 200);
            config.set("ranks.Skyforge", 300);
            config.set("ranks.Nebula", 350);
            config.set("ranks.Sovereign", 400);
            plugin.saveConfig();
        }
        
        // Load values into cache
        for (String rank : config.getConfigurationSection("ranks").getKeys(false)) {
            rankLimits.put(rank, config.getInt("ranks." + rank));
        }
    }
    
    /**
     * Load blacklisted blocks
     */
    private void loadBlacklistedBlocks() {
        if (!config.isList("blacklisted-blocks")) {
            // Set default values
            config.set("blacklisted-blocks", List.of(
                "BEDROCK", 
                "END_PORTAL_FRAME", 
                "COMMAND_BLOCK", 
                "BARRIER"
            ));
            plugin.saveConfig();
        }
        
        // Load values into cache
        for (String materialName : config.getStringList("blacklisted-blocks")) {
            try {
                blacklistedMaterials.add(Material.valueOf(materialName));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material in blacklist: " + materialName);
            }
        }
    }
    
    /**
     * Get block limit for a specific rank
     * @param rank Rank name
     * @return Block limit for the rank, or default limit if not found
     */
    public int getRankBlockLimit(String rank) {
        return rankLimits.getOrDefault(rank, 100); // Default to 100 if not found
    }
    
    /**
     * Check if a material is blacklisted
     * @param material Material to check
     * @return true if blacklisted, false otherwise
     */
    public boolean isBlacklisted(Material material) {
        return blacklistedMaterials.contains(material);
    }
    
    /**
     * Get the inventory cleanup period in hours
     * @return Cleanup period in hours (default 24)
     */
    public int getInventoryCleanupHours() {
        return config.getInt("inventory-cleanup-hours", 24);
    }
    
    /**
     * Get the warning time in minutes before inventory cleanup
     * @return Warning time in minutes (default 30)
     */
    public int getInventoryWarningMinutes() {
        return config.getInt("inventory-warning-minutes", 30);
    }
    
    /**
     * Get the batch size for async operations
     * @return Batch size
     */
    public int getAsyncBatchSize() {
        return config.getInt("async.batch-size", 500); // Default to 500
    }

    /**
     * Get the batch delay for async operations
     * @return Batch delay in ticks
     */
    public int getAsyncBatchDelay() {
        return config.getInt("async.batch-delay", 1); // Default to 1 tick
    }

    /**
     * Get how often to report progress during async operations
     * @return Progress report interval percentage
     */
    public int getAsyncProgressReportInterval() {
        return config.getInt("async.progress-report-interval", 10); // Default to 10%
    }

    /**
     * Get block limit multiplier for a specific world
     * @param worldName Name of the world
     * @return Block limit multiplier, defaults to 1.0
     */
    public double getWorldBlockLimitMultiplier(String worldName) {
        String path = "worlds." + worldName + ".block-limit-multiplier";
        return config.isSet(path) ? config.getDouble(path) : 1.0;
    }

    /**
     * Check if a feature is enabled in a specific world
     * @param worldName Name of the world
     * @param feature Feature name
     * @return true if enabled, defaults to true
     */
    public boolean isFeatureEnabledInWorld(String worldName, String feature) {
        String path = "worlds." + worldName + ".features." + feature;
        return config.isSet(path) ? config.getBoolean(path) : true;
    }

    /**
     * Get block limit for a specific rank and world
     * @param rank Rank name
     * @param worldName World name
     * @return Block limit for the rank in the specified world
     */
    public int getWorldRankBlockLimit(String rank, String worldName) {
        int baseLimit = getRankBlockLimit(rank);
        double multiplier = getWorldBlockLimitMultiplier(worldName);
        return (int) Math.ceil(baseLimit * multiplier);
    }
}