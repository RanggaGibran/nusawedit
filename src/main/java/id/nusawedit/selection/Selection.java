package id.nusawedit.selection;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * Represents a cuboid selection between two points
 */
public class Selection {
    private Location pos1;
    private Location pos2;
    
    /**
     * Check if selection is complete (both positions set)
     * @return true if complete
     */
    public boolean isComplete() {
        return pos1 != null && pos2 != null && pos1.getWorld().equals(pos2.getWorld());
    }
    
    /**
     * Get the volume of the selection
     * @return Volume in blocks, or -1 if incomplete
     */
    public int getVolume() {
        if (!isComplete()) {
            return -1;
        }
        
        // Calculate dimensions
        int xSize = Math.abs(pos2.getBlockX() - pos1.getBlockX()) + 1;
        int ySize = Math.abs(pos2.getBlockY() - pos1.getBlockY()) + 1;
        int zSize = Math.abs(pos2.getBlockZ() - pos1.getBlockZ()) + 1;
        
        return xSize * ySize * zSize;
    }
    
    /**
     * Get minimum X coordinate
     * @return Min X
     */
    public int getMinX() {
        return Math.min(pos1.getBlockX(), pos2.getBlockX());
    }
    
    /**
     * Get maximum X coordinate
     * @return Max X
     */
    public int getMaxX() {
        return Math.max(pos1.getBlockX(), pos2.getBlockX());
    }
    
    /**
     * Get minimum Y coordinate
     * @return Min Y
     */
    public int getMinY() {
        return Math.min(pos1.getBlockY(), pos2.getBlockY());
    }
    
    /**
     * Get maximum Y coordinate
     * @return Max Y
     */
    public int getMaxY() {
        return Math.max(pos1.getBlockY(), pos2.getBlockY());
    }
    
    /**
     * Get minimum Z coordinate
     * @return Min Z
     */
    public int getMinZ() {
        return Math.min(pos1.getBlockZ(), pos2.getBlockZ());
    }
    
    /**
     * Get maximum Z coordinate
     * @return Max Z
     */
    public int getMaxZ() {
        return Math.max(pos1.getBlockZ(), pos2.getBlockZ());
    }
    
    /**
     * Get the world of the selection
     * @return World, or null if incomplete
     */
    public World getWorld() {
        return isComplete() ? pos1.getWorld() : null;
    }
    
    /**
     * Get first position
     * @return Position 1
     */
    public Location getPos1() {
        return pos1;
    }
    
    /**
     * Set first position
     * @param pos1 Position 1
     */
    public void setPos1(Location pos1) {
        this.pos1 = pos1.clone();
    }
    
    /**
     * Get second position
     * @return Position 2
     */
    public Location getPos2() {
        return pos2;
    }
    
    /**
     * Set second position
     * @param pos2 Position 2
     */
    public void setPos2(Location pos2) {
        this.pos2 = pos2.clone();
    }
}