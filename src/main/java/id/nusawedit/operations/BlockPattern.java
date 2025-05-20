package id.nusawedit.operations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Material;

/**
 * Represents a pattern of blocks with their weights for random distribution
 */
public class BlockPattern {
    private final List<PatternEntry> entries = new ArrayList<>();
    private int totalWeight = 0;
    private final Random random = new Random();
    
    /**
     * Add a material to the pattern with a specific weight
     * @param material Material to add
     * @param weight Weight of the material (percentage or relative weight)
     */
    public void addMaterial(Material material, int weight) {
        entries.add(new PatternEntry(material, weight));
        totalWeight += weight;
    }
    
    /**
     * Get a random material from the pattern based on weights
     * @return Selected material
     */
    public Material getRandomMaterial() {
        if (entries.isEmpty()) {
            return Material.AIR;
        }
        
        if (entries.size() == 1) {
            return entries.get(0).material;
        }
        
        // Pick based on weight
        int value = random.nextInt(totalWeight);
        int currentWeight = 0;
        
        for (PatternEntry entry : entries) {
            currentWeight += entry.weight;
            if (value < currentWeight) {
                return entry.material;
            }
        }
        
        return entries.get(0).material;
    }
    
    /**
     * Get all materials in the pattern
     * @return List of materials
     */
    public List<Material> getMaterials() {
        List<Material> materials = new ArrayList<>();
        for (PatternEntry entry : entries) {
            materials.add(entry.material);
        }
        return materials;
    }
    
    /**
     * Get the weight of a specific material
     * @param material Material to check
     * @return Weight of the material, or 0 if not found
     */
    public int getWeight(Material material) {
        for (PatternEntry entry : entries) {
            if (entry.material == material) {
                return entry.weight;
            }
        }
        return 0;
    }
    
    /**
     * Get total weight of all materials
     * @return Total weight
     */
    public int getTotalWeight() {
        return totalWeight;
    }
    
    /**
     * Get the number of materials in the pattern
     * @return Number of materials
     */
    public int size() {
        return entries.size();
    }
    
    /**
     * Calculate approximate material requirements based on volume
     * @param volume Total volume to fill
     * @return Map of materials to amounts
     */
    public Map<Material, Integer> calculateRequirements(int volume) {
        Map<Material, Integer> requirements = new HashMap<>();
        for (PatternEntry entry : entries) {
            int amount = (int) Math.ceil((double) entry.weight / totalWeight * volume);
            requirements.put(entry.material, amount);
        }
        return requirements;
    }
    
    /**
     * Internal class representing a material and its weight
     */
    private static class PatternEntry {
        final Material material;
        final int weight;
        
        PatternEntry(Material material, int weight) {
            this.material = material;
            this.weight = weight;
        }
    }
}