package com.howlstudio.dungeons.model;
import java.util.*;

/** Active dungeon run for a group of players */
public class DungeonInstance {
    public enum State { WAITING, ACTIVE, COMPLETE, FAILED }
    public final String instanceId;
    public final DungeonDef def;
    public final Set<UUID> playerUuids = new HashSet<>();
    public State state = State.WAITING;
    public int currentRoom = 0;
    public long startTime;

    public DungeonInstance(String instanceId, DungeonDef def) {
        this.instanceId = instanceId; this.def = def;
    }
    public boolean isFull() { return playerUuids.size() >= def.maxPlayers; }
    public void addPlayer(UUID uuid) { playerUuids.add(uuid); }
    public void removePlayer(UUID uuid) { playerUuids.remove(uuid); }
    public void start() { state = State.ACTIVE; startTime = System.currentTimeMillis(); }
    public void complete() { state = State.COMPLETE; }
    public void fail() { state = State.FAILED; }
}
