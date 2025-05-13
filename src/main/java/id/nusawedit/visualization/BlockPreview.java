package id.nusawedit.visualization;

import id.nusawedit.selection.Selection;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;

/**
 * Represents a preview of a block operation
 */
public class BlockPreview {
    private final Selection selection;
    private final Material material;
    private final boolean isReplace;
    private final Material fromMaterial;
    private final Set<Location> blocks = new HashSet<>();
    
    /**
     * Create a new block preview
     * @param selection The selection
     * @param material Target material
     * @param isReplace Whether this is a replace operation
     * @param fromMaterial Source material for replace operation
     */
    public BlockPreview(Selection selection, Material material, boolean isReplace, Material fromMaterial) {
        this.selection = selection;
        this.material = material;
        this.isReplace = isReplace;
        this.fromMaterial = fromMaterial;
    }
    
    /**
     * Add a block to the preview
     * @param location Block location
     */
    public void addBlock(Location location) {
        blocks.add(location.clone());
    }
    
    /**
     * Get all blocks in this preview
     * @return Set of locations
     */
    public Set<Location> getBlocks() {
        return blocks;
    }
    
    /**
     * Get the target material
     * @return Material
     */
    public Material getMaterial() {
        return material;
    }
    
    /**
     * Check if this is a replace operation
     * @return true if replace
     */
    public boolean isReplace() {
        return isReplace;
    }
    
    /**
     * Get the source material for replace
     * @return Material or null if not a replace
     */
    public Material getFromMaterial() {
        return fromMaterial;
    }
    
    /**
     * Get selection
     * @return Selection
     */
    public Selection getSelection() {
        return selection;
    }
    
    /**
     * Get number of blocks in preview
     * @return Block count
     */
    public int getBlockCount() {
        return blocks.size();
    }
}