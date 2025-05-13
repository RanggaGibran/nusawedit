package id.nusawedit.inventory;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Represents a player's virtual inventory for materials
 */
public class VirtualInventory {
    private final String title;
    private final Inventory inventory;
    
    /**
     * Create a new virtual inventory
     * @param title Inventory title
     */
    public VirtualInventory(String title) {
        this.title = title;
        this.inventory = Bukkit.createInventory(null, 54, title); // 6 rows
    }
    
    /**
     * Get the inventory title
     * @return Title
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Get the Bukkit inventory
     * @return Inventory
     */
    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * Try to add an item to the inventory
     * @param item Item to add
     * @return true if added successfully
     */
    public boolean addItem(ItemStack item) {
        HashMap<Integer, ItemStack> leftover = inventory.addItem(item);
        return leftover.isEmpty(); // True if all items were added
    }
    
    /**
     * Count how much of a specific material is in the inventory
     * @param material Material to count
     * @return Amount of material
     */
    public int countMaterial(Material material) {
        int count = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }
    
    /**
     * Remove an amount of a specific material
     * @param material Material to remove
     * @param amount Amount to remove
     * @return true if removed successfully
     */
    public boolean removeMaterial(Material material, int amount) {
        if (countMaterial(material) < amount) {
            return false;
        }
        
        int remainingToRemove = amount;
        
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() == material) {
                if (item.getAmount() <= remainingToRemove) {
                    // Remove entire stack
                    remainingToRemove -= item.getAmount();
                    inventory.setItem(i, null);
                } else {
                    // Remove partial stack
                    item.setAmount(item.getAmount() - remainingToRemove);
                    remainingToRemove = 0;
                }
                
                if (remainingToRemove <= 0) {
                    break;
                }
            }
        }
        
        return remainingToRemove <= 0;
    }
    
    /**
     * Serialize inventory contents for storage
     * @return Serialized contents
     */
    public Map<String, Object> serializeContents() {
        Map<String, Object> result = new HashMap<>();
        ItemStack[] contents = inventory.getContents();
        
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                result.put(Integer.toString(i), contents[i]);
            }
        }
        
        return result;
    }
    
    /**
     * Load inventory contents from serialized data
     * @param serialized Serialized inventory contents
     */
    @SuppressWarnings("unchecked")
    public void loadFromSerialized(Map<String, Object> serialized) {
        for (Map.Entry<String, Object> entry : serialized.entrySet()) {
            int slot = Integer.parseInt(entry.getKey());
            if (slot >= 0 && slot < inventory.getSize()) {
                if (entry.getValue() instanceof ItemStack) {
                    inventory.setItem(slot, (ItemStack) entry.getValue());
                } else if (entry.getValue() instanceof Map) {
                    // Handle older format where items were serialized as maps
                    try {
                        ItemStack item = ItemStack.deserialize((Map<String, Object>) entry.getValue());
                        inventory.setItem(slot, item);
                    } catch (Exception e) {
                        // Ignore invalid items
                    }
                }
            }
        }
    }
}