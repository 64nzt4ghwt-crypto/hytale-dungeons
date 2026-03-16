package com.howlstudio.dungeons.config;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

/**
 * Loads and manages dungeon template configurations from JSON files.
 * Templates live in Server/DungeonConfigs/*.json and define dungeon blueprints.
 * 
 * This supports hot-reloading when configs change (via /dungeon reload if added).
 * 
 * Similar to how Orbis loads race configs, but simpler - no need for complex
 * hierarchies, just straight JSON -> POJO mapping.
 */
public class DungeonConfig {
    
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();
    
    private final Map<String, DungeonTemplate> templates;
    private File configDirectory;
    
    public DungeonConfig() {
        this.templates = new HashMap<>();
    }
    
    /**
     * Sets the config directory and loads all templates.
     * Called once during plugin startup.
     */
    public void loadFromDirectory(File dataDirectory) {
        this.configDirectory = new File(dataDirectory, "DungeonConfigs");
        
        // Create directory if it doesn't exist
        if (!configDirectory.exists()) {
            configDirectory.mkdirs();
            System.out.println("[Dungeons] Created config directory: " + configDirectory.getPath());
        }
        
        // Also check the resources directory for built-in configs
        loadBuiltInConfigs();
        
        // Load user-defined configs (can override built-in)
        loadUserConfigs();
        
        System.out.println("[Dungeons] Loaded " + templates.size() + " dungeon templates");
    }
    
    /**
     * Loads built-in configs from the plugin resources.
     * These are fallback defaults shipped with the plugin.
     */
    private void loadBuiltInConfigs() {
        // Built-ins are embedded in the jar; they get loaded via getResourceAsStream
        // For now we'll just log that we'd check resources
        // In production, you'd iterate through jar resources
    }
    
    /**
     * Loads user-defined configs from the data directory.
     * These can extend or override built-in templates.
     */
    private void loadUserConfigs() {
        File[] files = configDirectory.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            System.out.println("[Dungeons] No dungeon config files found in " + configDirectory.getPath());
            return;
        }
        
        for (File file : files) {
            loadTemplate(file);
        }
    }
    
    private void loadTemplate(File file) {
        try (FileReader reader = new FileReader(file)) {
            DungeonTemplate template = GSON.fromJson(reader, DungeonTemplate.class);
            
            String validationError = template.validate();
            if (validationError != null) {
                System.err.println("[Dungeons] Invalid config " + file.getName() + ": " + validationError);
                return;
            }
            
            // Ensure ID is set (use filename as fallback)
            if (template.getId() == null || template.getId().isEmpty()) {
                String id = file.getName().replace(".json", "");
                template.setId(id);
            }
            
            templates.put(template.getId(), template);
            System.out.println("[Dungeons] Loaded template: " + template.getId() 
                + " (" + template.getRoomCount() + " rooms)");
                
        } catch (JsonParseException e) {
            System.err.println("[Dungeons] JSON parse error in " + file.getName() + ": " + e.getMessage());
        } catch (IOException e) {
            System.err.println("[Dungeons] Failed to read " + file.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Gets a template by ID.
     * @return the template, or null if not found
     */
    public DungeonTemplate getTemplate(String id) {
        return templates.get(id);
    }
    
    /**
     * Checks if a template exists.
     */
    public boolean hasTemplate(String id) {
        return templates.containsKey(id);
    }
    
    /**
     * Gets all loaded template IDs.
     */
    public Collection<String> getTemplateIds() {
        return Collections.unmodifiableSet(templates.keySet());
    }
    
    /**
     * Gets all loaded templates.
     */
    public Collection<DungeonTemplate> getAllTemplates() {
        return Collections.unmodifiableCollection(templates.values());
    }
    
    /**
     * Reloads all configs from disk.
     * Clears current templates and re-loads.
     */
    public void reload() {
        templates.clear();
        loadBuiltInConfigs();
        loadUserConfigs();
    }
    
    /**
     * Gets the config file for a template (for editing/saving).
     */
    public File getConfigFile(String templateId) {
        return new File(configDirectory, templateId + ".json");
    }
}
