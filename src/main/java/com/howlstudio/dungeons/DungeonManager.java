package com.howlstudio.dungeons;

import com.howlstudio.dungeons.model.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Manages dungeon definitions and active instances */
public class DungeonManager {
    private final Map<String, DungeonDef> defs = new LinkedHashMap<>();
    private final Map<String, DungeonInstance> instances = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerInstance = new ConcurrentHashMap<>();
    private int nextId = 1;

    public DungeonManager() { loadDefaultDefs(); }

    private void loadDefaultDefs() {
        defs.put("cave_spider", new DungeonDef("cave_spider", "Cave of Spiders", 1, 4, List.of("iron_sword", "leather_armor", "gold_coin")));
        defs.put("ruined_keep", new DungeonDef("ruined_keep", "Ruined Keep", 5, 4, List.of("steel_sword", "chainmail", "emerald")));
        defs.put("shadow_vault", new DungeonDef("shadow_vault", "Shadow Vault", 10, 5, List.of("enchanted_blade", "void_armor", "shadow_gem")));
        defs.put("void_citadel", new DungeonDef("void_citadel", "Void Citadel", 20, 5, List.of("void_sword", "void_armor", "void_core", "legendary_token")));
    }

    public Collection<DungeonDef> getDefs() { return defs.values(); }
    public DungeonDef getDef(String id) { return defs.get(id.toLowerCase()); }

    public DungeonInstance createInstance(DungeonDef def) {
        String id = "d" + (nextId++);
        DungeonInstance inst = new DungeonInstance(id, def);
        instances.put(id, inst);
        return inst;
    }

    public boolean joinInstance(UUID playerUuid, String instanceId) {
        DungeonInstance inst = instances.get(instanceId);
        if (inst == null || inst.isFull() || inst.state != DungeonInstance.State.WAITING) return false;
        inst.addPlayer(playerUuid);
        playerInstance.put(playerUuid, instanceId);
        return true;
    }

    public void leaveInstance(UUID playerUuid) {
        String iid = playerInstance.remove(playerUuid);
        if (iid == null) return;
        DungeonInstance inst = instances.get(iid);
        if (inst != null) {
            inst.removePlayer(playerUuid);
            if (inst.playerUuids.isEmpty()) instances.remove(iid);
        }
    }

    public Optional<DungeonInstance> getPlayerInstance(UUID uuid) {
        String iid = playerInstance.get(uuid);
        return iid == null ? Optional.empty() : Optional.ofNullable(instances.get(iid));
    }

    public Collection<DungeonInstance> getActiveInstances() { return instances.values(); }

    public String getRandomReward(DungeonDef def) {
        if (def.rewardPool.isEmpty()) return "gold_coin";
        return def.rewardPool.get((int)(Math.random() * def.rewardPool.size()));
    }
}
