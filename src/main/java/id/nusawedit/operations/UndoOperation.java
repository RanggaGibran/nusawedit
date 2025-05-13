package id.nusawedit.operations;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;

/**
 * Represents an operation that can be undone
 */
public class UndoOperation {
    private final UUID playerId;
    private final long timestamp;
    private final Map<Location, BlockData> blocks = new HashMap<>();
    
    /**
     * Create a new undo operation
     * @param playerId Player UUID
     */
    public UndoOperation(UUID playerId) {
        this.playerId = playerId;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Add a block to the undo operation
     * @param location Block location
     * @param data Original block data
     */
    public void addBlock(Location location, BlockData data) {
        blocks.put(location.clone(), data);
    }
    
    /**
     * Get the player UUID
     * @return Player UUID
     */
    public UUID getPlayerId() {
        return playerId;
    }
    
    /**
     * Get the timestamp when this operation was created
     * @return Timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Get all blocks in this undo operation
     * @return Map of locations to block data
     */
    public Map<Location, BlockData> getBlocks() {
        return blocks;
    }
}