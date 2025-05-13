package id.nusawedit.commands;

import org.bukkit.command.CommandSender;

public interface SubCommand {
    /**
     * Execute the command
     * @param sender Command sender
     * @param args Command arguments
     * @return true if command was successful
     */
    boolean execute(CommandSender sender, String[] args);
    
    /**
     * Check if sender has permission to use this command
     * @param sender Command sender
     * @return true if has permission
     */
    boolean hasPermission(CommandSender sender);
    
    /**
     * Get command description for help menu
     * @return Command description
     */
    String getDescription();
    
    /**
     * Check if command can only be used by players
     * @return true if player-only
     */
    boolean isPlayerOnly();
}