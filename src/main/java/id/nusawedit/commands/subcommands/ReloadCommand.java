package id.nusawedit.commands.subcommands;

import id.nusawedit.Plugin;
import id.nusawedit.commands.SubCommand;

import org.bukkit.command.CommandSender;

public class ReloadCommand implements SubCommand {
    private final Plugin plugin;
    
    public ReloadCommand(Plugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        plugin.getConfigManager().reloadConfig();
        sender.sendMessage("§aNusaWEdit configuration reloaded!");
        return true;
    }
    
    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("nusawedit.admin.reload");
    }
    
    @Override
    public String getDescription() {
        return "Reload plugin configuration";
    }
    
    @Override
    public boolean isPlayerOnly() {
        return false;
    }
}