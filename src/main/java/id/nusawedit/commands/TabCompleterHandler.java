package id.nusawedit.commands;

import id.nusawedit.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * Handles tab completion for NusaWEdit commands
 */
public class TabCompleterHandler implements TabCompleter {
    private final Plugin plugin;
    private final List<String> SUBCOMMANDS = Arrays.asList(
            "inventory", "set", "replace", "undo", "reload", "give", "giveall",
            "visualize", "preview", "cancel");
    
    private final List<String> ADMIN_COMMANDS = Arrays.asList("reload", "give", "giveall");
    private final List<String> PLAYER_COMMANDS = Arrays.asList(
            "inventory", "set", "replace", "undo", "visualize", "preview", "cancel");
    
    private final List<String> PREVIEW_ACTIONS = Arrays.asList("set", "replace", "cancel", "confirm");
    
    public TabCompleterHandler(Plugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Suggest subcommands based on permissions
            if (sender.hasPermission("nusawedit.admin")) {
                return suggestByStart(SUBCOMMANDS, args[0]);
            } else {
                return suggestByStart(PLAYER_COMMANDS, args[0]);
            }
        } else if (args.length > 1) {
            // Process subcommand-specific completions
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "set":
                    if (args.length == 2 && sender.hasPermission("nusawedit.set")) {
                        // Suggest block materials
                        return suggestMaterials(args[1], false);
                    }
                    break;
                    
                case "replace":
                    if (sender.hasPermission("nusawedit.replace")) {
                        if (args.length == 2) {
                            // Suggest source material
                            return suggestMaterials(args[1], false);
                        } else if (args.length == 3) {
                            // Suggest target material
                            return suggestMaterials(args[2], false);
                        }
                    }
                    break;
                    
                case "give":
                    if (sender.hasPermission("nusawedit.admin.give")) {
                        if (args.length == 2) {
                            // Suggest online players
                            return suggestPlayers(args[1]);
                        } else if (args.length == 3) {
                            // Suggest item types (currently only wand)
                            return suggestByStart(Collections.singletonList("wand"), args[2]);
                        } else if (args.length == 4) {
                            // Suggest common uses quantities
                            return suggestByStart(Arrays.asList("5", "10", "25", "50", "100"), args[3]);
                        }
                    }
                    break;
                    
                case "giveall":
                    if (sender.hasPermission("nusawedit.admin.giveall")) {
                        if (args.length == 2) {
                            // Suggest item types (currently only wand)
                            return suggestByStart(Collections.singletonList("wand"), args[1]);
                        } else if (args.length == 3) {
                            // Suggest common uses quantities
                            return suggestByStart(Arrays.asList("5", "10", "25", "50", "100"), args[2]);
                        }
                    }
                    break;
                    
                case "preview":
                    if (sender.hasPermission("nusawedit.preview")) {
                        if (args.length == 2) {
                            // Suggest preview actions
                            return suggestByStart(PREVIEW_ACTIONS, args[1]);
                        } else if (args.length == 3) {
                            // Material suggestions based on action
                            if (args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("replace")) {
                                return suggestMaterials(args[2], false);
                            }
                        } else if (args.length == 4 && args[1].equalsIgnoreCase("replace")) {
                            // Suggest target material for replace
                            return suggestMaterials(args[3], false);
                        }
                    }
                    break;
            }
        }
        
        return completions;
    }
    
    /**
     * Suggest completions that start with a given input
     * @param options List of possible options
     * @param input User input to filter by
     * @return Filtered suggestions
     */
    private List<String> suggestByStart(List<String> options, String input) {
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
    
    /**
     * Suggest player names that match the input
     * @param input User input to filter by
     * @return Filtered player name suggestions
     */
    private List<String> suggestPlayers(String input) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
    
    /**
     * Suggest materials that match the input
     * @param input User input to filter by
     * @param includeAll Whether to include non-block materials
     * @return Filtered material name suggestions
     */
    private List<String> suggestMaterials(String input, boolean includeAll) {
        Stream<Material> materialStream = Arrays.stream(Material.values());
        
        if (!includeAll) {
            // Only include solid blocks, exclude technical blocks
            materialStream = materialStream.filter(material -> 
                    material.isBlock() && 
                    material.isSolid() && 
                    !material.name().contains("COMMAND") &&
                    !material.name().equals("BARRIER") &&
                    !material.name().equals("STRUCTURE_VOID") &&
                    !material.name().equals("LIGHT"));
        }
        
        return materialStream
                .map(Material::name)
                .filter(name -> name.startsWith(input.toUpperCase()))
                .collect(Collectors.toList());
    }
}