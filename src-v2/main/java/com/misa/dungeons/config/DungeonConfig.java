package com.misa.dungeons.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Top-level dungeon configuration, deserialized from JSON.
 * Each config corresponds to one dungeon template that can be instantiated.
 */
public class DungeonConfig {

    private String id;
    private String displayName;
    private String instanceTemplate;
    private int maxPlayers;
    private int minPlayers;
    private int timeLimit;
    private String difficulty;
    private boolean allowRejoin;
    private List<RoomConfig> rooms;

    public DungeonConfig() {
        this.rooms = new ArrayList<>();
        this.maxPlayers = 4;
        this.minPlayers = 1;
        this.timeLimit = 1800;
        this.difficulty = "normal";
        this.allowRejoin = true;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getInstanceTemplate() {
        return instanceTemplate;
    }

    public void setInstanceTemplate(String instanceTemplate) {
        this.instanceTemplate = instanceTemplate;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public boolean isAllowRejoin() {
        return allowRejoin;
    }

    public void setAllowRejoin(boolean allowRejoin) {
        this.allowRejoin = allowRejoin;
    }

    public List<RoomConfig> getRooms() {
        return rooms;
    }

    public void setRooms(List<RoomConfig> rooms) {
        this.rooms = rooms;
    }

    public int getRoomCount() {
        return rooms.size();
    }

    public RoomConfig getRoom(int index) {
        if (index < 0 || index >= rooms.size()) {
            return null;
        }
        return rooms.get(index);
    }

    /**
     * Validates this config for obvious issues.
     * @return null if valid, error message otherwise
     */
    public String validate() {
        if (id == null || id.isEmpty()) {
            return "Dungeon id is required";
        }
        if (instanceTemplate == null || instanceTemplate.isEmpty()) {
            return "Instance template is required for dungeon: " + id;
        }
        if (rooms.isEmpty()) {
            return "Dungeon must have at least one room: " + id;
        }
        for (int i = 0; i < rooms.size(); i++) {
            RoomConfig room = rooms.get(i);
            if (room.getWaves() == null || room.getWaves().isEmpty()) {
                return "Room " + i + " in dungeon " + id + " has no waves";
            }
        }
        if (maxPlayers < 1) {
            return "maxPlayers must be >= 1 for dungeon: " + id;
        }
        if (minPlayers < 1 || minPlayers > maxPlayers) {
            return "minPlayers must be between 1 and maxPlayers for dungeon: " + id;
        }
        return null;
    }

    @Override
    public String toString() {
        return "DungeonConfig{id='" + id + "', name='" + displayName
                + "', rooms=" + rooms.size() + ", difficulty=" + difficulty + "}";
    }
}
