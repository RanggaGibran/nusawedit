package id.nusawedit;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import id.nusawedit.commands.CommandHandler;
import id.nusawedit.commands.TabCompleterHandler;
import id.nusawedit.config.ConfigManager;
import id.nusawedit.handlers.WorldGuardHandler;
import id.nusawedit.inventory.VirtualInventoryManager;
import id.nusawedit.selection.SelectionManager;
import id.nusawedit.operations.BlockOperationHandler;
import id.nusawedit.visualization.VisualizationManager;
import id.nusawedit.listeners.WandListener;
import id.nusawedit.handlers.SuperiorSkyblockHandler;
import id.nusawedit.handlers.GriefPreventionHandler;
import id.nusawedit.handlers.TownyHandler;
import id.nusawedit.handlers.PlotSquaredHandler;

import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for NusaWEdit (NWE)
 */
public class Plugin extends JavaPlugin {
    private static final Logger LOGGER = Logger.getLogger("nusawedit");
    private static Plugin instance;
    
    // StateFlag defined as static in the main class
    public static StateFlag USE_NWE_WAND;
    
    // Managers
    private ConfigManager configManager;
    private SelectionManager selectionManager;
    private VirtualInventoryManager inventoryManager;
    private BlockOperationHandler blockOperationHandler;
    private VisualizationManager visualizationManager;
    private WorldGuardHandler worldGuardHandler;
    private SuperiorSkyblockHandler superiorSkyblockHandler;
    private GriefPreventionHandler griefPreventionHandler;
    private TownyHandler townyHandler;
    private PlotSquaredHandler plotSquaredHandler;
    
    @Override
    public void onLoad() {
        instance = this;
        
        // Flags must be registered in onLoad(), before WorldGuard is initialized
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            LOGGER.info("Registering WorldGuard flags for NusaWEdit...");
            registerWorldGuardFlag();
        }
    }
    
    /**
     * Register custom flag for WorldGuard
     */
    private void registerWorldGuardFlag() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            // Create our flag (defaults to ALLOW)
            StateFlag flag = new StateFlag("use-nwe-wand", true);
            
            // Register the flag
            registry.register(flag);
            USE_NWE_WAND = flag;
            
            LOGGER.info("Successfully registered WorldGuard flag 'use-nwe-wand'");
        } catch (FlagConflictException e) {
            // Flag might have been registered by another instance somehow
            Flag<?> existing = registry.get("use-nwe-wand");
            if (existing instanceof StateFlag) {
                USE_NWE_WAND = (StateFlag) existing;
                LOGGER.info("Using existing WorldGuard flag for NusaWEdit");
            } else {
                LOGGER.severe("Conflict with WorldGuard flag 'use-nwe-wand' - another plugin is using this name");
            }
        } catch (Exception e) {
            LOGGER.severe("Error registering WorldGuard flag: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void onEnable() {
        instance = this;
        LOGGER.info("Initializing NusaWEdit...");
        
        // Initialize config manager first
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        // Initialize other managers
        selectionManager = new SelectionManager(this);
        inventoryManager = new VirtualInventoryManager(this);
        blockOperationHandler = new BlockOperationHandler(this);
        visualizationManager = new VisualizationManager(this);
        worldGuardHandler = new WorldGuardHandler(this);
        superiorSkyblockHandler = new SuperiorSkyblockHandler(this);
        griefPreventionHandler = new GriefPreventionHandler(this);
        townyHandler = new TownyHandler(this);
        plotSquaredHandler = new PlotSquaredHandler(this);
        
        // Register command handler and tab completer
        CommandHandler commandHandler = new CommandHandler(this);
        TabCompleterHandler tabCompleter = new TabCompleterHandler(this);
        
        getCommand("nwe").setExecutor(commandHandler);
        getCommand("nwe").setTabCompleter(tabCompleter);
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new WandListener(this), this);
        
        // Start inventory cleanup task
        inventoryManager.startCleanupTask();
        
        LOGGER.info("NusaWEdit enabled successfully!");
    }

    @Override
    public void onDisable() {
        // Save data before shutdown
        if (inventoryManager != null) {
            inventoryManager.saveAllInventories();
        }
        
        // Clean up visualization
        if (visualizationManager != null) {
            visualizationManager.shutdown();
        }
        
        LOGGER.info("NusaWEdit disabled");
    }
    
    /**
     * Get the plugin instance
     * @return Plugin instance
     */
    public static Plugin getInstance() {
        return instance;
    }
    
    // Getters for managers
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public SelectionManager getSelectionManager() {
        return selectionManager;
    }
    
    public VirtualInventoryManager getInventoryManager() {
        return inventoryManager;
    }
    
    public BlockOperationHandler getBlockOperationHandler() {
        return blockOperationHandler;
    }
    
    public VisualizationManager getVisualizationManager() {
        return visualizationManager;
    }
    
    public WorldGuardHandler getWorldGuardHandler() {
        return worldGuardHandler;
    }
    
    /**
     * Get the SuperiorSkyblock handler
     * @return SuperiorSkyblockHandler
     */
    public SuperiorSkyblockHandler getSuperiorSkyblockHandler() {
        return superiorSkyblockHandler;
    }
    
    /**
     * Get the GriefPrevention handler
     * @return GriefPreventionHandler
     */
    public GriefPreventionHandler getGriefPreventionHandler() {
        return griefPreventionHandler;
    }
    
    /**
     * Get the Towny handler
     * @return TownyHandler
     */
    public TownyHandler getTownyHandler() {
        return townyHandler;
    }
    
    /**
     * Get the PlotSquared handler
     * @return PlotSquaredHandler
     */
    public PlotSquaredHandler getPlotSquaredHandler() {
        return plotSquaredHandler;
    }
}
