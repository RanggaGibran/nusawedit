package id.nusawedit.inventory;

import id.nusawedit.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

/**
 * Manages virtual inventories for block storage
 */
public class VirtualInventoryManager {
    private final Plugin plugin;
    private final Map<UUID, VirtualInventory> playerInventories = new ConcurrentHashMap<>();
    private final Map<UUID, Long> inventoryExpiryTimes = new ConcurrentHashMap<>();
    private BukkitTask cleanupTask;
    private BukkitTask warningTask;
    
    public VirtualInventoryManager(Plugin plugin) {
        this.plugin = plugin;
        loadInventories();
    }
    
    /**
     * Start the inventory cleanup task
     */
    public void startCleanupTask() {
        // Run cleanup check every hour
        this.cleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkExpiringInventories, 
                20 * 60 * 60, // 1 hour delay
                20 * 60 * 60  // 1 hour period
        );
        
        // Run warning check every minute
        int warningMinutes = plugin.getConfigManager().getInventoryWarningMinutes();
        this.warningTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkWarningInventories,
                20 * 60,     // 1 minute delay
                20 * 60      // 1 minute period
        );
    }
    
    /**
     * Check for inventories that will expire soon and warn players
     */
    private void checkWarningInventories() {
        int warningMinutes = plugin.getConfigManager().getInventoryWarningMinutes();
        long warningThreshold = System.currentTimeMillis() + (warningMinutes * 60 * 1000);
        
        for (Map.Entry<UUID, Long> entry : inventoryExpiryTimes.entrySet()) {
            UUID playerId = entry.getKey();
            long expiryTime = entry.getValue();
            
            // Check if inventory expires within warning period
            if (expiryTime < warningThreshold && expiryTime > System.currentTimeMillis()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    long minutesLeft = (expiryTime - System.currentTimeMillis()) / (60 * 1000);
                    player.sendMessage(plugin.getMessageManager().getFormattedMessage(
    "inventory.expiry-warning", minutesLeft));
                }
            }
        }
    }
    
    /**
     * Check for expired inventories and clean them up
     */
    private void checkExpiringInventories() {
        long currentTime = System.currentTimeMillis();
        
        // Find expired inventories
        inventoryExpiryTimes.entrySet().removeIf(entry -> {
            if (entry.getValue() < currentTime) {
                UUID playerId = entry.getKey();
                playerInventories.remove(playerId);
                
                // Notify player if online
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    player.sendMessage(plugin.getMessageManager().getMessage("inventory.expired"));
                }
                
                return true; // Remove from map
            }
            return false;
        });
        
        // Save changes
        saveInventories();
    }
    
    /**
     * Open virtual inventory for a player
     * @param player Player
     */
    public void openInventory(Player player) {
        UUID playerId = player.getUniqueId();
        VirtualInventory virtualInv = playerInventories.computeIfAbsent(playerId, 
                id -> new VirtualInventory(player.getName() + "'s NusaWEdit Materials"));
        
        // Update expiry time - 24 hours from now
        updateExpiryTime(playerId);
        
        // Open the inventory
        player.openInventory(virtualInv.getInventory());
        player.sendMessage(plugin.getMessageManager().getMessage("inventory.opened"));
    }
    
    /**
     * Update inventory expiry time for a player
     * @param playerId Player UUID
     */
    private void updateExpiryTime(UUID playerId) {
        int cleanupHours = plugin.getConfigManager().getInventoryCleanupHours();
        long expiryTime = System.currentTimeMillis() + (cleanupHours * 60 * 60 * 1000);
        inventoryExpiryTimes.put(playerId, expiryTime);
    }
    
    /**
     * Add materials to player's virtual inventory
     * @param player Player
     * @param material Material type
     * @param amount Amount to add
     * @return true if added successfully
     */
    public boolean addMaterial(Player player, Material material, int amount) {
        UUID playerId = player.getUniqueId();
        VirtualInventory virtualInv = playerInventories.computeIfAbsent(playerId, 
                id -> new VirtualInventory(player.getName() + "'s NusaWEdit Materials"));
        
        boolean result = virtualInv.addItem(new ItemStack(material, amount));
        updateExpiryTime(playerId);
        return result;
    }
    
    /**
     * Check if player has enough of a material
     * @param player Player
     * @param material Material type
     * @param amount Amount needed
     * @return true if player has enough
     */
    public boolean hasMaterial(Player player, Material material, int amount) {
        VirtualInventory virtualInv = playerInventories.get(player.getUniqueId());
        if (virtualInv == null) {
            return false;
        }
        return virtualInv.countMaterial(material) >= amount;
    }
    
    /**
     * Remove materials from player's virtual inventory
     * @param player Player
     * @param material Material type
     * @param amount Amount to remove
     * @return true if removed successfully
     */
    public boolean removeMaterial(Player player, Material material, int amount) {
        VirtualInventory virtualInv = playerInventories.get(player.getUniqueId());
        if (virtualInv == null) {
            return false;
        }
        
        boolean result = virtualInv.removeMaterial(material, amount);
        updateExpiryTime(player.getUniqueId());
        return result;
    }
    
    /**
     * Load all virtual inventories from disk
     */
    @SuppressWarnings("unchecked")
    private void loadInventories() {
        File dataFolder = new File(plugin.getDataFolder(), "inventories");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
            return;
        }
        
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        
        for (File file : files) {
            try {
                String fileName = file.getName();
                UUID playerId = UUID.fromString(fileName.substring(0, fileName.length() - 4));
                
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                
                // Load inventory contents
                VirtualInventory virtualInv = new VirtualInventory(config.getString("name", "NusaWEdit Materials"));
                if (config.contains("contents")) {
                    Object contentsObj = config.get("contents");
                    Map<String, Object> serializedItems;
                    
                    if (contentsObj instanceof org.bukkit.configuration.MemorySection) {
                        // Convert MemorySection to Map
                        org.bukkit.configuration.MemorySection section = 
                                (org.bukkit.configuration.MemorySection) contentsObj;
                        serializedItems = new HashMap<>();
                        for (String key : section.getKeys(false)) {
                            serializedItems.put(key, section.get(key));
                        }
                    } else if (contentsObj instanceof Map) {
                        // Direct cast if it's already a Map
                        serializedItems = (Map<String, Object>) contentsObj;
                    } else {
                        // Skip if it's neither
                        plugin.getLogger().warning("Invalid contents format in inventory: " + file.getName());
                        continue;
                    }
                    
                    virtualInv.loadFromSerialized(serializedItems);
                }
                
                // Load expiry time
                long expiryTime = config.getLong("expiry-time", System.currentTimeMillis() + (24 * 60 * 60 * 1000));
                
                playerInventories.put(playerId, virtualInv);
                inventoryExpiryTimes.put(playerId, expiryTime);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load inventory: " + file.getName());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Save all virtual inventories to disk
     */
    public void saveInventories() {
        File dataFolder = new File(plugin.getDataFolder(), "inventories");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        // Save each player's inventory
        for (Map.Entry<UUID, VirtualInventory> entry : playerInventories.entrySet()) {
            UUID playerId = entry.getKey();
            VirtualInventory virtualInv = entry.getValue();
            
            File file = new File(dataFolder, playerId.toString() + ".yml");
            FileConfiguration config = new YamlConfiguration();
            
            // Save basic info
            config.set("name", virtualInv.getTitle());
            
            // Save contents
            config.set("contents", virtualInv.serializeContents());
            
            // Save expiry time
            config.set("expiry-time", inventoryExpiryTimes.getOrDefault(playerId, 
                    System.currentTimeMillis() + (24 * 60 * 60 * 1000)));
            
            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save inventory: " + playerId);
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Save all inventories on plugin disable
     */
    public void saveAllInventories() {
        cleanupTask.cancel();
        warningTask.cancel();
        saveInventories();
    }

    /**
     * Stop the inventory cleanup and warning tasks
     */
    public void stopTasks() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        if (warningTask != null) {
            warningTask.cancel();
        }
    }
}