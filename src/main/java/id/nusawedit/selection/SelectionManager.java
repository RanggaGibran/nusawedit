package id.nusawedit.selection;

import id.nusawedit.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Location;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Manages player selections and wand usage
 */
public class SelectionManager {
    private final Plugin plugin;
    private final Map<UUID, Selection> playerSelections = new HashMap<>();
    private final NamespacedKey wandKey;
    private final NamespacedKey usesKey;
    
    public SelectionManager(Plugin plugin) {
        this.plugin = plugin;
        this.wandKey = new NamespacedKey(plugin, "nwe_wand");
        this.usesKey = new NamespacedKey(plugin, "nwe_uses");
    }
    
    /**
     * Get or create a selection for a player
     * @param player Player
     * @return Selection
     */
    public Selection getSelection(Player player) {
        return playerSelections.computeIfAbsent(player.getUniqueId(), k -> new Selection());
    }
    
    /**
     * Check if a player has a complete selection
     * @param player Player
     * @return true if selection is complete
     */
    public boolean hasCompleteSelection(Player player) {
        Selection selection = playerSelections.get(player.getUniqueId());
        return selection != null && selection.isComplete();
    }
    
    /**
     * Calculate the volume of a player's selection
     * @param player Player
     * @return Volume in blocks, or -1 if selection incomplete
     */
    public int calculateSelectionVolume(Player player) {
        Selection selection = playerSelections.get(player.getUniqueId());
        if (selection == null || !selection.isComplete()) {
            return -1;
        }
        return selection.getVolume();
    }
    
    /**
     * Set first position in selection
     * @param player Player
     * @param location Location
     */
    public void setFirstPosition(Player player, Location location) {
        Selection selection = getSelection(player);
        selection.setPos1(location);
        player.sendMessage(plugin.getMessageManager().getFormattedMessage("selection.position-1-set", formatLocation(location)));
    }
    
    /**
     * Set second position in selection
     * @param player Player
     * @param location Location
     */
    public void setSecondPosition(Player player, Location location) {
        Selection selection = getSelection(player);
        selection.setPos2(location);
        player.sendMessage(plugin.getMessageManager().getFormattedMessage("selection.position-2-set", formatLocation(location)));
        
        // If selection is now complete, show volume info
        if (selection.isComplete()) {
            player.sendMessage(plugin.getMessageManager().getFormattedMessage("selection.selection-complete", selection.getVolume()));
        }
    }
    
    /**
     * Create a selection wand with limited uses
     * @param uses Number of uses
     * @return Wand item
     */
    public ItemStack createWand(int uses) {
        ItemStack wand = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = wand.getItemMeta();
        
        meta.setDisplayName("§e§lNusaWEdit Wand");
        meta.setLore(java.util.Arrays.asList(
            "§7Right-click to set position 1",
            "§7Left-click to set position 2",
            "§7Remaining uses: §6" + uses
        ));
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(wandKey, PersistentDataType.BYTE, (byte) 1);
        container.set(usesKey, PersistentDataType.INTEGER, uses);
        
        wand.setItemMeta(meta);
        return wand;
    }
    
    /**
     * Check if an item is a selection wand
     * @param item Item to check
     * @return true if wand
     */
    public boolean isWand(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.has(wandKey, PersistentDataType.BYTE);
    }
    
    /**
     * Get remaining uses for a wand
     * @param item Wand item
     * @return Remaining uses, or 0 if not a valid wand
     */
    public int getWandUses(ItemStack item) {
        if (!isWand(item)) {
            return 0;
        }
        
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.getOrDefault(usesKey, PersistentDataType.INTEGER, 0);
    }
    
    /**
     * Consume one use of wand
     * @param item Wand item
     * @return true if wand still has uses left, false if depleted
     */
    public boolean consumeWandUse(ItemStack item) {
        if (!isWand(item)) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        
        int uses = container.getOrDefault(usesKey, PersistentDataType.INTEGER, 0) - 1;
        
        if (uses <= 0) {
            return false; // Wand depleted
        }
        
        container.set(usesKey, PersistentDataType.INTEGER, uses);
        
        // Update lore with new uses count
        meta.setLore(java.util.Arrays.asList(
            "§7Right-click to set position 1",
            "§7Left-click to set position 2",
            "§7Remaining uses: §6" + uses
        ));
        
        item.setItemMeta(meta);
        return true;
    }
    
    /**
     * Consume one use of the wand the player is holding
     * @param player Player holding the wand
     * @return true if wand still has uses, false if depleted/not holding wand
     */
    public boolean consumePlayerWandUse(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        
        // Check main hand first
        if (isWand(mainHand)) {
            boolean success = consumeWandUse(mainHand);
            if (!success) {
                // Remove depleted wand
                player.getInventory().setItemInMainHand(null);
                player.playSound(player.getLocation(), "entity.item.break", 1.0f, 1.0f);
                player.sendMessage(plugin.getMessageManager().getMessage("selection.wand-depleted"));
            }
            return success;
        }
        
        // Check off hand if main hand isn't holding a wand
        if (isWand(offHand)) {
            boolean success = consumeWandUse(offHand);
            if (!success) {
                // Remove depleted wand
                player.getInventory().setItemInOffHand(null);
                player.playSound(player.getLocation(), "entity.item.break", 1.0f, 1.0f);
                player.sendMessage(plugin.getMessageManager().getMessage("selection.wand-depleted"));
            }
            return success;
        }
        
        // Player isn't holding a wand
        return true; // Return true so operations can continue without a wand
    }
    
    /**
     * Check if player is holding a wand in either hand
     * @param player Player to check
     * @return true if player is holding a wand
     */
    public boolean isHoldingWand(Player player) {
        // Check main hand
        if (isWand(player.getInventory().getItemInMainHand())) {
            return true;
        }
        
        // Check off hand
        if (isWand(player.getInventory().getItemInOffHand())) {
            return true;
        }
        
        return false;
    }
    
    
    /**
     * Format location for display
     * @param location Location
     * @return Formatted string
     */
    private String formatLocation(Location location) {
        return location.getBlockX() + ", " + 
               location.getBlockY() + ", " + 
               location.getBlockZ();
    }
}