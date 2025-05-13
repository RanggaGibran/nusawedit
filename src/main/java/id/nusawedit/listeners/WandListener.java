package id.nusawedit.listeners;

import id.nusawedit.Plugin;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Listener for wand interactions
 */
public class WandListener implements Listener {
    private final Plugin plugin;
    
    public WandListener(Plugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        // Check if player is holding a wand
        if (item == null || !plugin.getSelectionManager().isWand(item)) {
            return;
        }
        
        // Cancel the event to prevent normal interaction
        event.setCancelled(true);
        
        // Get the clicked block
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }
        
        // Check WorldGuard permissions - prevent usage in protected regions
        if (plugin.getWorldGuardHandler().isEnabled()) {
            if (!plugin.getWorldGuardHandler().canUseWand(player, clickedBlock.getLocation())) {
                player.sendMessage("§cAnda tidak diizinkan menggunakan tongkat NusaWEdit di region ini!");
                return;
            }
        }
        
        // Check SuperiorSkyblock permissions - prevent usage on other islands
        if (plugin.getSuperiorSkyblockHandler().isEnabled()) {
            if (!plugin.getSuperiorSkyblockHandler().canUseWand(player, clickedBlock.getLocation())) {
                player.sendMessage("§cAnda tidak diizinkan menggunakan tongkat NusaWEdit di island orang lain!");
                return;
            }
        }
        
        // Check GriefPrevention permissions - prevent usage in others' claims
        if (plugin.getGriefPreventionHandler().isEnabled()) {
            if (!plugin.getGriefPreventionHandler().canUseWand(player, clickedBlock.getLocation())) {
                player.sendMessage("§cAnda tidak diizinkan menggunakan tongkat NusaWEdit di claim orang lain!");
                return;
            }
        }
        
        // Check Towny permissions
        if (plugin.getTownyHandler().isEnabled()) {
            if (!plugin.getTownyHandler().canUseWand(player, clickedBlock.getLocation())) {
                player.sendMessage("§cAnda tidak diizinkan menggunakan tongkat NusaWEdit di area town ini!");
                return;
            }
        }
        
        // Handle different actions
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Set position 1
            plugin.getSelectionManager().setFirstPosition(player, clickedBlock.getLocation());
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            // Set position 2
            plugin.getSelectionManager().setSecondPosition(player, clickedBlock.getLocation());
        }
    }
}