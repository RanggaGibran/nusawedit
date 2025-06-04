package id.nusawedit.commands;

import id.nusawedit.Plugin;
import id.nusawedit.commands.subcommands.*;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandler implements CommandExecutor {
    private final Plugin plugin;
    private final Map<String, SubCommand> subcommands = new HashMap<>();
    
    public CommandHandler(Plugin plugin) {
        this.plugin = plugin;
        
        // Register subcommands
        registerSubcommand("inventory", new InventoryCommand(plugin));
        registerSubcommand("set", new SetCommand(plugin));
        registerSubcommand("replace", new ReplaceCommand(plugin));
        registerSubcommand("undo", new UndoCommand(plugin));
        registerSubcommand("reload", new ReloadCommand(plugin));
        registerSubcommand("give", new GiveCommand(plugin));
        registerSubcommand("giveall", new GiveAllCommand(plugin)); // New command
        
        // Visualization commands
        registerSubcommand("visualize", new VisualizeCommand(plugin));
        registerSubcommand("preview", new PreviewCommand(plugin));
        
        // Operation control commands
        registerSubcommand("cancel", new CancelCommand(plugin));
    }
    
    private void registerSubcommand(String name, SubCommand command) {
        subcommands.put(name.toLowerCase(), command);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        
        String subcommandName = args[0].toLowerCase();
        SubCommand subcommand = subcommands.get(subcommandName);
        
        if (subcommand == null) {
            sender.sendMessage(plugin.getMessageManager().getFormattedMessage("general.unknown-command", subcommandName));
            showHelp(sender);
            return true;
        }
        
        if (subcommand.isPlayerOnly() && !(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("general.player-only"));
            return true;
        }
        
        if (!subcommand.hasPermission(sender)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("general.no-permission"));
            return true;
        }
        
        // Execute subcommand
        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, subArgs.length);
        
        return subcommand.execute(sender, subArgs);
    }
    
    private void showHelp(CommandSender sender) {
        sender.sendMessage(plugin.getMessageManager().getMessage("general.help-header"));
        
        // For players, show commands they have permission to use
        for (Map.Entry<String, SubCommand> entry : subcommands.entrySet()) {
            SubCommand subcommand = entry.getValue();
            if (subcommand.hasPermission(sender)) {
                sender.sendMessage(plugin.getMessageManager().getFormattedMessage(
                    "general.help-format", entry.getKey(), subcommand.getDescription()));
            }
        }
    }
}