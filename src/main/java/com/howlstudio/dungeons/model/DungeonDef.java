package com.howlstudio.dungeons.model;
import java.util.*;

/** Defines a dungeon: name, difficulty, tiers, rewards */
public class DungeonDef {
    public final String id;
    public final String displayName;
    public final int minLevel;
    public final int maxPlayers;
    public final List<String> rewardPool; // item ID strings

    public DungeonDef(String id, String displayName, int minLevel, int maxPlayers, List<String> rewards) {
        this.id = id; this.displayName = displayName; this.minLevel = minLevel;
        this.maxPlayers = maxPlayers; this.rewardPool = rewards;
    }
}
