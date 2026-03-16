# Hytale Inventory & Item System API

> Decompiled from `HytaleServer.jar` using `javap -p` on 2026-02-07.
> All method signatures are **exact** from bytecode. Use these to write compilable code.

---

## Table of Contents

1. [Quick Reference: How to Give a Player an Item](#1-quick-reference-give-player-an-item)
2. [ItemStack — The Core Item Object](#2-itemstack)
3. [Inventory — Player/Entity Inventory](#3-inventory)
4. [ItemContainer — Slot-Based Storage](#4-itemcontainer)
5. [Item (Asset Config) — Item Definitions](#5-item-asset-config)
6. [Equipment & Armor Slots](#6-equipment--armor-slots)
7. [RoleUtils & InventoryHelper — NPC Items](#7-roleutils--inventoryhelper)
8. [Transactions — Inventory Operation Results](#8-transactions)
9. [Events — Inventory Change Detection](#9-events)
10. [GiveCommand — Reference Implementation](#10-givecommand-reference)
11. [Usage Patterns for Dungeon Plugin](#11-usage-patterns)

---

## 1. Quick Reference: Give Player an Item

```java
// Create an ItemStack from an item asset ID
ItemStack sword = new ItemStack("iron_longsword");          // quantity=1
ItemStack arrows = new ItemStack("arrow", 64);              // quantity=64
ItemStack customMeta = new ItemStack("potion", 3, metadata); // with BSON metadata

// Get the player's inventory
Inventory inventory = player.getInventory();  // inherited from LivingEntity

// Add to hotbar specifically
ItemContainer hotbar = inventory.getHotbar();
hotbar.addItemStack(sword);  // returns ItemStackTransaction (check .succeeded())

// Add to any available slot (hotbar first, then storage)
ItemContainer combined = inventory.getCombinedHotbarFirst();
combined.addItemStack(arrows);

// Add to specific slot
hotbar.addItemStackToSlot((short) 0, sword);  // slot 0

// Set a slot directly (replaces whatever was there)
hotbar.setItemStackForSlot((short) 0, sword);

// NPC shortcut: create item via InventoryHelper
ItemStack item = InventoryHelper.createItem("iron_longsword");
```

---

## 2. ItemStack

**Package:** `com.hypixel.hytale.server.core.inventory`

The fundamental item representation. Immutable-ish with `with*()` builder methods.

### Constructors

```java
// From item asset ID (string-based)
public ItemStack(String itemId);                                        // qty=1, no metadata
public ItemStack(String itemId, int quantity);                          // with quantity
public ItemStack(String itemId, int quantity, BsonDocument metadata);   // with metadata
public ItemStack(String itemId, int quantity, double durability, double maxDurability, BsonDocument metadata);
```

### Key Fields

```java
protected String itemId;                    // Asset ID (e.g., "iron_longsword")
protected int quantity;                     // Stack count
protected double durability;               // Current durability
protected double maxDurability;            // Max durability
protected boolean overrideDroppedItemAnimation;
protected BsonDocument metadata;           // Arbitrary BSON metadata (org.bson.BsonDocument)
```

### Key Constants

```java
public static final ItemStack EMPTY;                     // The empty/null item
public static final ItemStack[] EMPTY_ARRAY;
public static final BuilderCodec<ItemStack> CODEC;       // For serialization
```

### Getters

```java
public String getItemId();
public int getQuantity();
public BsonDocument getMetadata();
public boolean isUnbreakable();
public boolean isBroken();
public double getMaxDurability();
public double getDurability();
public boolean isEmpty();
public boolean getOverrideDroppedItemAnimation();
public String getBlockKey();                              // If this is a block item
public Item getItem();                                    // Get the Item asset definition
public boolean isValid();                                 // Whether asset ID resolves to a real item
```

### Builder Methods (return new ItemStack)

```java
public ItemStack withDurability(double durability);
public ItemStack withMaxDurability(double maxDurability);
public ItemStack withIncreasedDurability(double amount);
public ItemStack withRestoredDurability(double amount);
public ItemStack withState(String state);
public ItemStack withQuantity(int quantity);
public ItemStack withMetadata(BsonDocument metadata);
public <T> ItemStack withMetadata(KeyedCodec<T> codec, T value);
public <T> ItemStack withMetadata(String key, Codec<T> codec, T value);
public ItemStack withMetadata(String key, BsonValue value);
```

### Metadata Access

```java
public <T> T getFromMetadataOrNull(KeyedCodec<T> codec);
public <T> T getFromMetadataOrNull(String key, Codec<T> codec);
public <T> T getFromMetadataOrDefault(String key, BuilderCodec<T> codec);
```

### Comparison / Static

```java
public boolean isStackableWith(ItemStack other);
public boolean isEquivalentType(ItemStack other);
public static boolean isEmpty(ItemStack stack);
public static boolean isStackableWith(ItemStack a, ItemStack b);
public static boolean isEquivalentType(ItemStack a, ItemStack b);
public static boolean isSameItemType(ItemStack a, ItemStack b);
public static ItemStack fromPacket(ItemQuantity packet);
```

### ItemStack.Metadata (inner class)

```java
public class ItemStack.Metadata {
    public static final String BLOCK_STATE;    // Metadata key for block state
}
```

---

## 3. Inventory

**Package:** `com.hypixel.hytale.server.core.inventory`

The full inventory of a `LivingEntity` (Player or NPC). Contains multiple **sections** (ItemContainers).

### How to Access

```java
// From any LivingEntity (Player, NPCEntity)
Inventory inventory = livingEntity.getInventory();   // LivingEntity.getInventory()
livingEntity.setInventory(inventory);                // Replace entire inventory
livingEntity.setInventory(inventory, boolean markChanged);
livingEntity.setInventory(inventory, boolean markChanged, List<ItemStack> overflow);
```

### Inventory Sections (Each is an ItemContainer)

```java
public ItemContainer getHotbar();                    // Main hand items (weapon bar)
public ItemContainer getStorage();                   // Backpack/main storage
public ItemContainer getArmor();                     // 4 armor slots (Head, Chest, Hands, Legs)
public ItemContainer getUtility();                   // Utility slots (offhand-like)
public ItemContainer getTools();                     // Tool slots
public ItemContainer getBackpack();                  // Extended backpack storage
```

### Section IDs (for use with getSectionById)

```java
public static final int HOTBAR_SECTION_ID;
public static final int STORAGE_SECTION_ID;
public static final int ARMOR_SECTION_ID;
public static final int UTILITY_SECTION_ID;
public static final int TOOLS_SECTION_ID;
public static final int BACKPACK_SECTION_ID;

public ItemContainer getSectionById(int sectionId);  // Get section by ID
```

### Combined Containers (span multiple sections)

These combine sections for add/remove operations that should span multiple sections:

```java
public CombinedItemContainer getCombinedHotbarFirst();          // Hotbar → Storage
public CombinedItemContainer getCombinedStorageFirst();          // Storage → Hotbar
public CombinedItemContainer getCombinedBackpackStorageHotbar(); // Backpack → Storage → Hotbar
public CombinedItemContainer getCombinedArmorHotbarStorage();    // Armor → Hotbar → Storage
public CombinedItemContainer getCombinedArmorHotbarUtilityStorage();
public CombinedItemContainer getCombinedHotbarUtilityConsumableStorage();
public CombinedItemContainer getCombinedEverything();            // ALL sections
```

### Active Slot Management

```java
public byte getActiveHotbarSlot();
public void setActiveHotbarSlot(byte slot);
public ItemStack getActiveHotbarItem();              // Currently selected hotbar item
public ItemStack getItemInHand();                    // Same as active hotbar item

public byte getActiveUtilitySlot();
public void setActiveUtilitySlot(byte slot);
public ItemStack getUtilityItem();                   // Active utility item

public byte getActiveToolsSlot();
public void setActiveToolsSlot(byte slot);
public ItemStack getToolsItem();                     // Active tool

public void setActiveSlot(int sectionId, byte slot);
public byte getActiveSlot(int sectionId);
```

### Bulk Operations

```java
public void clear();                                 // Clear entire inventory
public List<ItemStack> dropAllItemStacks();           // Remove all, return as list
public void moveItem(int srcSection, int srcSlot, int dstSection, int dstSlot, int quantity);
public void smartMoveItem(int section, int slot, int quantity, SmartMoveType type);
```

### Constructors

```java
public Inventory();                                   // Default capacities
public Inventory(short hotbar, short utility, short tools, short armor, short storage);
public Inventory(ItemContainer storage, ItemContainer armor, ItemContainer hotbar,
                 ItemContainer utility, ItemContainer tools, ItemContainer backpack);
```

### Default Capacities

```java
public static final short DEFAULT_HOTBAR_CAPACITY;    // e.g., 8
public static final short DEFAULT_UTILITY_CAPACITY;
public static final short DEFAULT_TOOLS_CAPACITY;
public static final short DEFAULT_ARMOR_CAPACITY;     // 4 (Head, Chest, Hands, Legs)
public static final short DEFAULT_STORAGE_ROWS;
public static final short DEFAULT_STORAGE_COLUMNS;
public static final short DEFAULT_STORAGE_CAPACITY;
```

### Other

```java
public boolean consumeIsDirty();                      // Network sync flag
public boolean consumeNeedsSaving();
public void markChanged();                            // Force dirty flag
public void setEntity(LivingEntity entity);
public void sortStorage(SortType type);
public boolean containsBrokenItem();
public ItemContainer getContainerForItemPickup(Item item, PlayerSettings settings);
```

---

## 4. ItemContainer

**Package:** `com.hypixel.hytale.server.core.inventory.container`

Abstract base class for all inventory slot storage. The primary implementations:
- **`SimpleItemContainer`** — standard fixed-size container
- **`CombinedItemContainer`** — wraps multiple containers as one logical container
- **`EmptyItemContainer`** — always empty
- **`DelegateItemContainer`** — delegates to another container
- **`ItemStackItemContainer`** — wraps a single ItemStack

### Adding Items

```java
// Add to any available slot (stacks first, then empty slots)
public ItemStackTransaction addItemStack(ItemStack stack);
public ItemStackTransaction addItemStack(ItemStack stack, boolean allOrNothing, boolean filter, boolean fullStacks);

// Add multiple
public ListTransaction<ItemStackTransaction> addItemStacks(List<ItemStack> stacks);
public ListTransaction<ItemStackTransaction> addItemStacks(List<ItemStack> stacks, boolean allOrNothing, boolean filter, boolean fullStacks);

// Add to specific slot
public ItemStackSlotTransaction addItemStackToSlot(short slot, ItemStack stack);
public ItemStackSlotTransaction addItemStackToSlot(short slot, ItemStack stack, boolean allOrNothing, boolean filter);

// Set slot directly (replaces content)
public ItemStackSlotTransaction setItemStackForSlot(short slot, ItemStack stack);
public ItemStackSlotTransaction setItemStackForSlot(short slot, ItemStack stack, boolean sendUpdate);

// Add ordered (preserves insertion order)
public ListTransaction<ItemStackSlotTransaction> addItemStacksOrdered(List<ItemStack> stacks);
public ListTransaction<ItemStackSlotTransaction> addItemStacksOrdered(short startSlot, List<ItemStack> stacks);
```

### Checking Capacity

```java
public boolean canAddItemStack(ItemStack stack);
public boolean canAddItemStack(ItemStack stack, boolean allOrNothing, boolean filter);
public boolean canAddItemStacks(List<ItemStack> stacks);
public boolean canAddItemStacks(List<ItemStack> stacks, boolean allOrNothing, boolean filter);
```

### Reading Items

```java
public ItemStack getItemStack(short slot);            // Get item at slot index
public short getCapacity();                            // Total slots
public boolean isEmpty();
public int countItemStacks(Predicate<ItemStack> predicate);  // Count matching
public boolean containsItemStacksStackableWith(ItemStack stack);
public void forEach(ShortObjectConsumer<ItemStack> consumer); // Iterate all slots
```

### Removing Items

```java
// Remove from specific slot
public SlotTransaction removeItemStackFromSlot(short slot);
public SlotTransaction removeItemStackFromSlot(short slot, boolean allOrNothing);
public ItemStackSlotTransaction removeItemStackFromSlot(short slot, int quantity);
public ItemStackSlotTransaction removeItemStackFromSlot(short slot, int quantity, boolean allOrNothing, boolean filter);
public ItemStackSlotTransaction removeItemStackFromSlot(short slot, ItemStack specific, int quantity);

// Remove by type (searches all slots)
public ItemStackTransaction removeItemStack(ItemStack stack);
public ItemStackTransaction removeItemStack(ItemStack stack, boolean allOrNothing, boolean exactAmount);
public boolean canRemoveItemStack(ItemStack stack);

// Remove multiple
public ListTransaction<ItemStackTransaction> removeItemStacks(List<ItemStack> stacks);
public boolean canRemoveItemStacks(List<ItemStack> stacks);

// Remove all
public ClearTransaction clear();
public List<ItemStack> removeAllItemStacks();
public List<ItemStack> dropAllItemStacks();
public List<ItemStack> dropAllItemStacks(boolean includeCantDrop);
```

### Replacing Items

```java
public ItemStackSlotTransaction replaceItemStackInSlot(short slot, ItemStack expected, ItemStack replacement);
public ListTransaction<ItemStackSlotTransaction> replaceAll(SlotReplacementFunction function);
```

### Moving Items Between Containers

```java
public MoveTransaction<ItemStackTransaction> moveItemStackFromSlot(short slot, ItemContainer target);
public MoveTransaction<ItemStackTransaction> moveItemStackFromSlot(short slot, ItemContainer target, boolean allOrNothing);
public MoveTransaction<SlotTransaction> moveItemStackFromSlotToSlot(short fromSlot, int qty, ItemContainer target, short toSlot);
public ListTransaction<MoveTransaction<ItemStackTransaction>> moveAllItemStacksTo(ItemContainer... targets);
public ListTransaction<MoveTransaction<SlotTransaction>> swapItems(short slot, ItemContainer other, short otherSlot, short quantity);
```

### Filters

```java
public void setGlobalFilter(FilterType filter);
public void setSlotFilter(FilterActionType actionType, short slot, SlotFilter filter);
```

### Change Events

```java
public EventRegistration registerChangeEvent(Consumer<ItemContainerChangeEvent> handler);
public EventRegistration registerChangeEvent(EventPriority priority, Consumer<ItemContainerChangeEvent> handler);
public EventRegistration registerChangeEvent(short slot, Consumer<ItemContainerChangeEvent> handler);
```

### SimpleItemContainer (concrete implementation)

```java
public SimpleItemContainer(short capacity);              // Create with fixed capacity
public static ItemContainer getNewContainer(short cap);  // Factory

// Static helpers for "add or drop on ground if full"
public static boolean addOrDropItemStack(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref,
                                          ItemContainer container, ItemStack stack);
public static boolean addOrDropItemStack(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref,
                                          ItemContainer container, short slot, ItemStack stack);
public static boolean addOrDropItemStacks(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref,
                                           ItemContainer container, List<ItemStack> stacks);
```

---

## 5. Item (Asset Config)

**Package:** `com.hypixel.hytale.server.core.asset.type.item.config`

Represents a registered item definition (loaded from game data JSON). This is the **type** definition, not an instance.

### Looking Up Items by ID

```java
// Static asset map — look up any registered item by string ID
Item.getAssetMap().getAsset("iron_longsword");        // returns Item or null
Item.getAssetMap().getAsset(pack, key);               // by pack + key
Item.getAssetMap().getKeys(path);                     // all item IDs
Item.getAssetMap().getAssetCount();                   // total registered items
Item.getAssetMap().getAssetMap();                     // full Map<String, Item>

// Or from an ItemStack
ItemStack stack = new ItemStack("iron_longsword");
Item itemDef = stack.getItem();                       // resolves to Item asset
```

### Key Properties

```java
public String getId();                                // Asset ID (e.g., "iron_longsword")
public int getMaxStack();                             // Max stack size
public int getItemLevel();                            // Item level/tier
public String getIcon();                              // Icon texture path
public String getModel();                             // 3D model path
public String getTexture();                           // Texture path
public float getScale();                              // Model scale
public double getMaxDurability();                     // Max durability (0 = no durability)
public double getDurabilityLossOnHit();               // Durability loss per use
public double getFuelQuality();                       // Fuel burning value
public boolean isConsumable();                        // Can be consumed
public boolean isVariant();
public boolean hasBlockType();                        // Is also a block
public String getBlockId();                           // Corresponding block ID
public boolean dropsOnDeath();
public String[] getCategories();                      // Item categories

// Sub-type data
public ItemTool getTool();                            // Tool properties (null if not a tool)
public ItemWeapon getWeapon();                        // Weapon properties (null if not a weapon)
public ItemArmor getArmor();                          // Armor properties (null if not armor)
public ItemGlider getGlider();                        // Glider properties
public ItemUtility getUtility();                      // Utility properties
public PortalKey getPortalKey();                      // Portal key properties
public ItemStackContainerConfig getItemStackContainerConfig(); // Container config (bags, chests)
```

---

## 6. Equipment & Armor Slots

### ItemArmorSlot (Enum)

**Package:** `com.hypixel.hytale.protocol`

```java
public enum ItemArmorSlot {
    Head,     // Helmet slot (index 0)
    Chest,    // Chestplate slot (index 1)
    Hands,    // Gauntlets/gloves slot (index 2)
    Legs      // Leggings slot (index 3)
}
```

### Equipment Protocol Object

```java
// com.hypixel.hytale.protocol.Equipment
public class Equipment {
    public String[] armorIds;        // Array of 4 armor item IDs [head, chest, hands, legs]
    public String rightHandItemId;   // Right hand (active hotbar item)
    public String leftHandItemId;    // Left hand (utility/offhand item)
}
```

### Armor in the Inventory

The armor section is an `ItemContainer` with 4 slots (one per `ItemArmorSlot`).
Each slot has an `ArmorSlotAddFilter` that restricts which armor pieces can go in.

```java
// Get armor container
ItemContainer armor = inventory.getArmor();

// Slots map to ItemArmorSlot enum ordinals:
// 0 = Head, 1 = Chest, 2 = Hands, 3 = Legs

// Get current helmet
ItemStack helmet = armor.getItemStack((short) 0);  // Head slot

// Set helmet
armor.setItemStackForSlot((short) 0, new ItemStack("iron_helmet"));

// Set full armor set
armor.setItemStackForSlot((short) 0, new ItemStack("iron_helmet"));    // Head
armor.setItemStackForSlot((short) 1, new ItemStack("iron_chestplate"));// Chest
armor.setItemStackForSlot((short) 2, new ItemStack("iron_gauntlets")); // Hands
armor.setItemStackForSlot((short) 3, new ItemStack("iron_leggings")); // Legs
```

### ArmorSlotAddFilter

```java
// com.hypixel.hytale.server.core.inventory.container.filter.ArmorSlotAddFilter
public class ArmorSlotAddFilter implements ItemSlotFilter {
    public ArmorSlotAddFilter(ItemArmorSlot armorSlot);
    public boolean test(Item item);                      // Returns true if item fits this armor slot
    public ItemArmorSlot getItemArmorSlot();
}
```

### ItemArmor (on the Item asset)

```java
// com.hypixel.hytale.server.core.asset.type.item.config.ItemArmor
public class ItemArmor {
    public ItemArmorSlot getArmorSlot();                 // Which slot this armor piece goes in
    public double getBaseDamageResistance();             // Base armor value
    // ... stat modifiers, resistances, etc.
}
```

---

## 7. RoleUtils & InventoryHelper — NPC Items

### RoleUtils (Quick NPC Equipment)

**Package:** `com.hypixel.hytale.server.npc.role`

```java
public class RoleUtils {
    // Set items in NPC hotbar slots (string array of item IDs)
    public static void setHotbarItems(NPCEntity npc, String[] itemIds);

    // Set items in NPC offhand/utility slots
    public static void setOffHandItems(NPCEntity npc, String[] itemIds);

    // Set single item in NPC's hand (convenience)
    public static void setItemInHand(NPCEntity npc, String itemId);

    // Set NPC armor by search string
    public static void setArmor(NPCEntity npc, String armorId);
}
```

### InventoryHelper (NPC Inventory Utilities)

**Package:** `com.hypixel.hytale.server.npc.util`

```java
public class InventoryHelper {
    // === CONSTANTS ===
    public static final short DEFAULT_NPC_HOTBAR_SLOTS;
    public static final short MAX_NPC_HOTBAR_SLOTS;
    public static final short DEFAULT_NPC_INVENTORY_SLOTS;
    public static final short DEFAULT_NPC_UTILITY_SLOTS;
    public static final short MAX_NPC_UTILITY_SLOTS;
    public static final short DEFAULT_NPC_TOOL_SLOTS;
    public static final short MAX_NPC_INVENTORY_SLOTS;

    // === ITEM CREATION ===
    public static ItemStack createItem(String itemId);        // Create ItemStack from asset ID

    // === ITEM LOOKUP ===
    public static boolean itemKeyExists(String key);          // Check if item ID is valid
    public static boolean itemKeyIsBlockType(String key);     // Is this a block item?
    public static boolean itemDropListKeyExists(String key);  // Does droplist exist?

    // === PATTERN MATCHING ===
    public static boolean matchesItem(String pattern, ItemStack stack);
    public static boolean matchesItem(List<String> patterns, ItemStack stack);

    // === FIND SLOTS ===
    public static byte findHotbarSlotWithItem(Inventory inv, String itemId);
    public static short findHotbarSlotWithItem(Inventory inv, List<String> itemIds);
    public static byte findHotbarEmptySlot(Inventory inv);
    public static short findInventorySlotWithItem(Inventory inv, String itemId);
    public static short findInventorySlotWithItem(Inventory inv, List<String> itemIds);

    // === COUNT ===
    public static int countItems(ItemContainer container, List<String> itemIds);
    public static int countFreeSlots(ItemContainer container);

    // === CHECK CONTAINS ===
    public static boolean hotbarContainsItem(Inventory inv, String itemId);
    public static boolean hotbarContainsItem(Inventory inv, List<String> itemIds);
    public static boolean holdsItem(Inventory inv, String itemId);    // In active hand?
    public static boolean containsItem(Inventory inv, String itemId); // Anywhere in inv?
    public static boolean containsItem(Inventory inv, List<String> itemIds);

    // === SET ITEMS ===
    public static boolean setHotbarItem(Inventory inv, String itemId, byte slot);
    public static boolean setOffHandItem(Inventory inv, String itemId, byte slot);
    public static void setHotbarSlot(Inventory inv, byte slot);       // Set active slot
    public static void setOffHandSlot(Inventory inv, byte slot);

    // === USE/EQUIP ===
    public static boolean useItem(Inventory inv, String itemId, byte slot);
    public static boolean useItem(Inventory inv, String itemId);
    public static boolean useArmor(ItemContainer armorContainer, String armorId);
    public static boolean useArmor(ItemContainer armorContainer, ItemStack armor);

    // === REMOVE ===
    public static boolean clearItemInHand(Inventory inv, byte slot);
    public static void removeItemInHand(Inventory inv, int quantity);
    public static boolean checkHotbarSlot(Inventory inv, byte slot);
    public static boolean checkOffHandSlot(Inventory inv, byte slot);
}
```

---

## 8. Transactions — Inventory Operation Results

All inventory operations return Transaction objects that tell you if they succeeded.

### Transaction (interface)

```java
public interface Transaction {
    boolean succeeded();                               // Did the operation work?
    boolean wasSlotModified(short slot);               // Was this specific slot changed?
}
```

### ActionType (enum)

```java
public enum ActionType {
    SET,        // Slot was set directly
    ADD,        // Item was added
    REMOVE,     // Item was removed
    REPLACE     // Item was replaced
}
```

### ItemStackTransaction

```java
public class ItemStackTransaction implements Transaction {
    public static final ItemStackTransaction FAILED_ADD;

    public boolean succeeded();
    public ActionType getAction();
    public ItemStack getQuery();                       // What was requested
    public ItemStack getRemainder();                   // What didn't fit (overflow)
    public boolean isAllOrNothing();
    public List<ItemStackSlotTransaction> getSlotTransactions();  // Per-slot details
}
```

### SlotTransaction

```java
public class SlotTransaction implements Transaction {
    public boolean succeeded();
    public ActionType getAction();
    public short getSlot();                            // Which slot was affected
    public ItemStack getSlotBefore();                  // What was in the slot before
    public ItemStack getSlotAfter();                   // What's in the slot now
    public ItemStack getOutput();                      // What was produced (removed item)
}
```

### Checking Results

```java
// Always check .succeeded() after inventory operations!
ItemStackTransaction tx = hotbar.addItemStack(new ItemStack("iron_longsword"));
if (tx.succeeded()) {
    // Item was added
} else {
    // Inventory was full or filtered
    ItemStack overflow = tx.getRemainder();  // What didn't fit
}
```

---

## 9. Events — Inventory Change Detection

### LivingEntityInventoryChangeEvent

**Package:** `com.hypixel.hytale.server.core.event.events.entity`

```java
public class LivingEntityInventoryChangeEvent extends EntityEvent<LivingEntity, String> {
    // Constructor
    public LivingEntityInventoryChangeEvent(LivingEntity entity, ItemContainer container, Transaction transaction);

    public ItemContainer getItemContainer();    // Which container changed
    public Transaction getTransaction();        // What happened
    // Inherited: getEntity() → LivingEntity
}
```

### ItemContainer Change Events (per-container)

```java
// Register on any ItemContainer
EventRegistration reg = container.registerChangeEvent(event -> {
    // event is ItemContainerChangeEvent
});

// Register for specific slot only
EventRegistration reg = container.registerChangeEvent((short) 0, event -> {
    // Only fires when slot 0 changes
});
```

---

## 10. GiveCommand — Reference Implementation

The built-in `/give` command shows the canonical way to give items:

```java
public class GiveCommand extends AbstractPlayerCommand {
    // Arguments:
    private RequiredArg<Item> itemArg;           // ITEM_ASSET arg type
    private DefaultArg<Integer> quantityArg;     // Defaults to 1
    private OptionalArg<Double> durabilityArg;   // Optional durability override
    private OptionalArg<String> metadataArg;     // Optional BSON metadata string

    // Execute receives: ctx, store, ref, playerRef, world
    // Internally creates ItemStack and adds to player inventory
}
```

The `/givearmor` command is also available:
```java
public class GiveArmorCommand extends AbstractAsyncCommand {
    // Arguments: optional player, required search string, --set flag
    // Searches item assets for matching armor and gives full set
}
```

---

## 11. Usage Patterns for Dungeon Plugin

### Pattern 1: Give Dungeon Loot to Player

```java
public void giveReward(Player player, String itemId, int quantity) {
    Inventory inventory = player.getInventory();
    ItemStack reward = new ItemStack(itemId, quantity);

    // Try hotbar first, then storage
    ItemContainer combined = inventory.getCombinedHotbarFirst();
    ItemStackTransaction tx = combined.addItemStack(reward);

    if (!tx.succeeded()) {
        // Inventory full — handle overflow
        ItemStack remainder = tx.getRemainder();
        // Option: drop on ground, or show "inventory full" message
    }
}
```

### Pattern 2: Give a Weapon with Custom Durability

```java
public void giveWeapon(Player player, String weaponId) {
    Item itemDef = Item.getAssetMap().getAsset(weaponId);
    if (itemDef == null) return;  // Invalid item

    ItemStack weapon = new ItemStack(weaponId)
        .withDurability(itemDef.getMaxDurability())
        .withMaxDurability(itemDef.getMaxDurability());

    player.getInventory().getHotbar().addItemStack(weapon);
}
```

### Pattern 3: Equip NPC with Dungeon Gear

```java
public void equipDungeonMob(NPCEntity npc) {
    // Quick method via RoleUtils
    RoleUtils.setItemInHand(npc, "iron_longsword");
    RoleUtils.setArmor(npc, "iron");  // Set full iron armor

    // Or manually via inventory
    Inventory inv = npc.getInventory();
    inv.getArmor().setItemStackForSlot((short) 0, new ItemStack("iron_helmet"));
    inv.getArmor().setItemStackForSlot((short) 1, new ItemStack("iron_chestplate"));
    inv.getHotbar().setItemStackForSlot((short) 0, new ItemStack("iron_longsword"));
}
```

### Pattern 4: Check if Player Has a Required Item

```java
public boolean hasKey(Player player, String keyItemId) {
    return InventoryHelper.containsItem(player.getInventory(), keyItemId);
}

public boolean removeKey(Player player, String keyItemId) {
    Inventory inv = player.getInventory();
    ItemContainer combined = inv.getCombinedEverything();
    ItemStackTransaction tx = combined.removeItemStack(new ItemStack(keyItemId));
    return tx.succeeded();
}
```

### Pattern 5: Clear Player Inventory for Dungeon Instance

```java
public List<ItemStack> stashAndClearInventory(Player player) {
    Inventory inventory = player.getInventory();
    List<ItemStack> stashed = inventory.dropAllItemStacks();
    // Save stashed items somewhere for restoration after dungeon
    return stashed;
}

public void restoreInventory(Player player, List<ItemStack> stashed) {
    Inventory inventory = player.getInventory();
    inventory.clear();
    ItemContainer combined = inventory.getCombinedEverything();
    combined.addItemStacks(stashed);
}
```

### Pattern 6: Dungeon Chest / Loot Container

```java
public void openLootChest(Player player, List<ItemStack> loot) {
    // Create a temporary container
    SimpleItemContainer chestContainer = new SimpleItemContainer((short) loot.size());
    for (int i = 0; i < loot.size(); i++) {
        chestContainer.setItemStackForSlot((short) i, loot.get(i));
    }

    // Player can interact via the window system
    // (see PageManager.openCustomPageWithWindows for UI integration)
}
```

### Pattern 7: Validate Item Asset Exists

```java
public boolean isValidDungeonItem(String itemId) {
    return InventoryHelper.itemKeyExists(itemId);
    // OR:
    // return Item.getAssetMap().getAsset(itemId) != null;
}
```

---

## Package Summary

| Class | Full Package |
|-------|-------------|
| `ItemStack` | `com.hypixel.hytale.server.core.inventory` |
| `ItemStack.Metadata` | `com.hypixel.hytale.server.core.inventory` |
| `Inventory` | `com.hypixel.hytale.server.core.inventory` |
| `Inventory.ItemPickupType` | `com.hypixel.hytale.server.core.inventory` |
| `ItemContext` | `com.hypixel.hytale.server.core.inventory` |
| `MaterialQuantity` | `com.hypixel.hytale.server.core.inventory` |
| `ResourceQuantity` | `com.hypixel.hytale.server.core.inventory` |
| `ItemContainer` | `com.hypixel.hytale.server.core.inventory.container` |
| `SimpleItemContainer` | `com.hypixel.hytale.server.core.inventory.container` |
| `CombinedItemContainer` | `com.hypixel.hytale.server.core.inventory.container` |
| `ItemContainerUtil` | `com.hypixel.hytale.server.core.inventory.container` |
| `SortType` | `com.hypixel.hytale.server.core.inventory.container` |
| `Transaction` | `com.hypixel.hytale.server.core.inventory.transaction` |
| `ActionType` | `com.hypixel.hytale.server.core.inventory.transaction` |
| `ItemStackTransaction` | `com.hypixel.hytale.server.core.inventory.transaction` |
| `ItemStackSlotTransaction` | `com.hypixel.hytale.server.core.inventory.transaction` |
| `SlotTransaction` | `com.hypixel.hytale.server.core.inventory.transaction` |
| `Item` | `com.hypixel.hytale.server.core.asset.type.item.config` |
| `ItemArmor` | `com.hypixel.hytale.server.core.asset.type.item.config` |
| `ItemWeapon` | `com.hypixel.hytale.server.core.asset.type.item.config` |
| `ItemTool` | `com.hypixel.hytale.server.core.asset.type.item.config` |
| `ItemQuality` | `com.hypixel.hytale.server.core.asset.type.item.config` |
| `ItemGlider` | `com.hypixel.hytale.server.core.asset.type.item.config` |
| `ItemUtility` | `com.hypixel.hytale.server.core.asset.type.item.config` |
| `ItemArmorSlot` | `com.hypixel.hytale.protocol` |
| `Equipment` | `com.hypixel.hytale.protocol` |
| `InventorySection` | `com.hypixel.hytale.protocol` |
| `InventoryActionType` | `com.hypixel.hytale.protocol` |
| `RoleUtils` | `com.hypixel.hytale.server.npc.role` |
| `InventoryHelper` | `com.hypixel.hytale.server.npc.util` |
| `ArmorSlotAddFilter` | `com.hypixel.hytale.server.core.inventory.container.filter` |
| `LivingEntityInventoryChangeEvent` | `com.hypixel.hytale.server.core.event.events.entity` |
| `LivingEntity` | `com.hypixel.hytale.server.core.entity` |
| `GiveCommand` | `com.hypixel.hytale.server.core.command.commands.player.inventory` |

---

## Key Takeaways

1. **Items are string-based** — item IDs are strings (e.g., `"iron_longsword"`), not numeric IDs.
2. **ItemStack is the unit** — create with `new ItemStack(itemId)` or `InventoryHelper.createItem(itemId)`.
3. **Inventory has 6 sections** — hotbar, storage, armor (4 slots), utility, tools, backpack.
4. **Use CombinedContainers** for cross-section operations — `getCombinedHotbarFirst()` is the typical "give item" target.
5. **Always check transactions** — `addItemStack()` returns a `Transaction` with `.succeeded()` and `.getRemainder()`.
6. **NPC shortcuts exist** — `RoleUtils.setItemInHand()`, `RoleUtils.setArmor()` for quick NPC equipment.
7. **Armor slots are enum-based** — `Head(0)`, `Chest(1)`, `Hands(2)`, `Legs(3)`.
8. **Item assets are statically registered** — `Item.getAssetMap().getAsset(id)` to look up definitions.
9. **BsonDocument for metadata** — ItemStack metadata uses MongoDB's BSON format (`org.bson.BsonDocument`).

---

*Generated 2026-02-07 from HytaleServer.jar decompilation*
