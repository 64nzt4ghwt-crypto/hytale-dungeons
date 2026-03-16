package com.howlstudio.dungeons.commands.sub;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Interface for /dungeon subcommands.
 * 
 * Each subcommand receives the parsed player context and a string
 * containing any remaining arguments after the subcommand name.
 */
public interface SubCommand {

    /**
     * Executes this subcommand.
     *
     * @param ctx       command context for sending messages
     * @param store     entity store
     * @param ref       entity ref
     * @param playerRef player reference (for username, UUID, etc.)
     * @param player    the player entity
     * @param world     the player's current world
     * @param args      remaining argument string after subcommand name
     */
    void execute(
        CommandContext ctx,
        Store<EntityStore> store,
        Ref<EntityStore> ref,
        PlayerRef playerRef,
        Player player,
        World world,
        String args
    );
}
