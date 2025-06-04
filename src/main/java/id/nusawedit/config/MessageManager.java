package id.nusawedit.config;

import id.nusawedit.Plugin;

import java.io.File;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Manages messages for NusaWEdit
 */
public class MessageManager {
    private final Plugin plugin;
    private FileConfiguration messages;
    private File messagesFile;
    
    // Cache for frequently accessed messages
    private final Map<String, String> messageCache = new HashMap<>();
    
    public MessageManager(Plugin plugin) {
        this.plugin = plugin;
        loadMessages();
    }
    
    /**
     * Load messages from file
     */
    public void loadMessages() {
        // Create messages file if it doesn't exist
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Clear and rebuild cache
        messageCache.clear();
    }
    
    /**
     * Reload messages from file
     */
    public void reloadMessages() {
        if (messagesFile == null) {
            messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        }
        
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Clear and rebuild cache
        messageCache.clear();
    }
    
    /**
     * Get a message from the configuration
     * @param path Path to the message
     * @param defaultMessage Default message if not found
     * @return The formatted message
     */
    public String getMessage(String path, String defaultMessage) {
        // Check cache first
        if (messageCache.containsKey(path)) {
            return messageCache.get(path);
        }
        
        // Get from config
        String message = messages.getString(path, defaultMessage);
        
        // Apply color codes
        message = ChatColor.translateAlternateColorCodes('&', message);
        
        // Cache and return
        messageCache.put(path, message);
        return message;
    }
    
    /**
     * Get a message from the configuration
     * @param path Path to the message
     * @return The formatted message, or path if not found
     */
    public String getMessage(String path) {
        return getMessage(path, "§cMissing message: " + path);
    }
    
    /**
     * Get a message with formatted parameters
     * @param path Path to the message
     * @param args Arguments to format into the message
     * @return The formatted message
     */
    public String getFormattedMessage(String path, Object... args) {
        String message = getMessage(path);
        try {
            return MessageFormat.format(message, args);
        } catch (Exception e) {
            plugin.getLogger().warning("Error formatting message " + path + ": " + e.getMessage());
            return message;
        }
    }
}