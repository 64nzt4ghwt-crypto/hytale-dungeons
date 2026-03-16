package com.howlstudio.dungeons.commands;

import java.util.Collection;

import com.howlstudio.dungeons.config.DungeonTemplate;
import com.howlstudio.dungeons.manager.DungeonManager;
import com.howlstudio.dungeons.util.Msg;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class DungeonListCommand extends AbstractPlayerCommand {
    
    private final DungeonManager dungeonManager;
    
    public DungeonListCommand(DungeonManager manager) {
        super("dlist", "List available dungeon templates", false);
        this.dungeonManager = manager;
    }
    
    @Override
    protected void execute(
            @Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        Collection<String> templateIds = dungeonManager.getAvailableTemplates();
        
        if (templateIds.isEmpty()) {
            ctx.sendMessage(Msg.error("No dungeon templates configured"));
            ctx.sendMessage(Msg.hint("Add JSON files to Server/DungeonConfigs/"));
            return;
        }
        
        ctx.sendMessage(Msg.header("Available Dungeons"));
        ctx.sendMessage(Msg.text("", Msg.DARK));
        
        for (String id : templateIds) {
            DungeonTemplate template = dungeonManager.getTemplate(id);
            if (template == null) continue;
            
            String name = template.getName();
            int rooms = template.getRoomCount();
            String players = template.getMinPlayers() + "-" + template.getMaxPlayers();
            int timeMin = template.getTimeLimitSeconds() / 60;
            
            ctx.sendMessage(Msg.pair("  " + id + " ", "— " + name, Msg.AMBER, Msg.WHITE));
            ctx.sendMessage(Msg.text("    " + rooms + " rooms · " + players + " players · " + timeMin + " min", Msg.GRAY));
        }
        
        ctx.sendMessage(Msg.text("", Msg.DARK));
        ctx.sendMessage(Msg.divider());
        
        int active = dungeonManager.getActiveInstanceCount();
        if (active > 0) {
            ctx.sendMessage(Msg.text("  Active instances: " + active, Msg.GRAY));
        }
        
        ctx.sendMessage(Msg.hint("Create a dungeon: /dcreate <template>"));
    }
}
