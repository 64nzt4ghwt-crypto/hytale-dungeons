package com.howlstudio.dungeons;

import com.howlstudio.dungeons.commands.DungeonCommand;
import com.howlstudio.dungeons.components.DungeonData;
import com.howlstudio.dungeons.events.DungeonEventListener;
import com.howlstudio.dungeons.manager.DungeonManager;
import com.howlstudio.dungeons.portal.PortalManager;
import com.howlstudio.dungeons.spawning.MobSpawner;
import com.howlstudio.dungeons.systems.DungeonTickSystem;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.command.system.CommandRegistry;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Main entry point for the Dungeon Plugin.
 * Sets up ECS components, commands, event listeners, and the tick system.
 * 
 * Architecture: clean separation of concerns
 * - Manager handles instance lifecycle + world registration + teleportation
 * - Systems handle per-tick logic via ECS
 * - Components store persistent player data
 * - Commands provide player interface via /dungeon root command
 */
public class DungeonPlugin extends JavaPlugin {

    private static ComponentType<EntityStore, DungeonData> dungeonDataType;
    private static MobSpawner mobSpawnerInstance;
    
    private DungeonManager dungeonManager;
    private DungeonTickSystem tickSystem;

    public DungeonPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void start() {
        System.out.println("[Dungeons] Loading dungeon plugin v" + getManifest().getVersion());
        
        // Initialize the dungeon manager (singleton pattern)
        this.dungeonManager = DungeonManager.getInstance();
        this.dungeonManager.setDataDirectory(getDataDirectory().toFile());
        
        // Register persistent component for player dungeon data
        dungeonDataType = getEntityStoreRegistry().registerComponent(
            DungeonData.class,
            "DungeonData",
            DungeonData.CODEC
        );
        
        // Pass component type to manager for data access
        dungeonManager.setDungeonDataType(dungeonDataType);
        
        // Create mob spawner (singleton, shared across commands + tick system)
        MobSpawner mobSpawner = new MobSpawner();
        mobSpawnerInstance = mobSpawner;
        
        // Wire mob spawner into the manager for room advancement
        dungeonManager.setMobSpawner(mobSpawner);
        
        // Create portal manager for proximity-based dungeon entry
        PortalManager portalManager = new PortalManager(dungeonManager);
        
        // Register the tick system that processes active dungeons each frame
        this.tickSystem = new DungeonTickSystem(dungeonDataType);
        tickSystem.setDungeonManager(dungeonManager);
        tickSystem.setMobSpawner(mobSpawner);
        tickSystem.setPortalManager(portalManager);
        getEntityStoreRegistry().registerSystem(tickSystem);
        
        // Register the single root command: /dungeon <subcommand> [args...]
        CommandRegistry commands = getCommandRegistry();
        commands.registerCommand(new DungeonCommand(dungeonManager, mobSpawner));
        
        // Register standalone shortcut commands
        commands.registerCommand(new com.howlstudio.dungeons.commands.DungeonListCommand(dungeonManager));
        commands.registerCommand(new com.howlstudio.dungeons.commands.DungeonCreateCommand(dungeonManager, mobSpawner));
        commands.registerCommand(new com.howlstudio.dungeons.commands.DungeonJoinCommand(dungeonManager));
        commands.registerCommand(new com.howlstudio.dungeons.commands.DungeonLeaveCommand(dungeonManager));
        commands.registerCommand(new com.howlstudio.dungeons.commands.DungeonInfoCommand(dungeonManager));
        commands.registerCommand(new com.howlstudio.dungeons.commands.DungeonBrowseCommand(dungeonManager));
        commands.registerCommand(new com.howlstudio.dungeons.commands.DungeonStatusCommand(dungeonManager));
        
        // Register event listeners
        EventRegistry events = getEventRegistry();
        DungeonEventListener listener = new DungeonEventListener(dungeonManager);
        events.registerGlobal(com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent.class, 
            listener::onPlayerReady);
        
        System.out.println("[Dungeons] Plugin loaded successfully");
        System.out.println("[Dungeons] Loaded " + dungeonManager.getAvailableTemplates().size() + " templates");
    }
    
    /**
     * Gets the registered dungeon data component type.
     */
    public static ComponentType<EntityStore, DungeonData> getDungeonDataType() {
        return dungeonDataType;
    }
    
    /**
     * Gets the shared mob spawner instance.
     */
    public static MobSpawner getMobSpawner() {
        return mobSpawnerInstance;
    }
    
    /**
     * Gets the dungeon manager instance.
     */
    public DungeonManager getDungeonManager() {
        return dungeonManager;
    }
}
