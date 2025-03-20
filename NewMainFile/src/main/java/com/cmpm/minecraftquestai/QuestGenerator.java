package com.cmpm.minecraftquestai;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Random;

public class QuestGenerator {

    private static final Random random = new Random();
    private static int difficultyLevel = 1; // Default difficulty level

    // List of possible enemy types for EnemyKillQuest
    private static final List<String> ENEMY_TYPES = List.of(
            "minecraft:zombie",
            "minecraft:skeleton",
            "minecraft:creeper",
            "minecraft:spider"
    );

    // List of possible items for ItemCollectionQuest
    private static final List<String> ITEM_TYPES = List.of(
            "minecraft:iron_ingot",
            "minecraft:gold_ingot",
            "minecraft:diamond",
            "minecraft:emerald"
    );

    /**
     * Generates a random quest based on the current difficulty level.
     * @return A randomly generated quest.
     */
    public static Quest generateRandomQuest() {
        int questType = random.nextInt(2); // 0 or 1

        if (questType == 0) {
            // Generate an EnemyKillQuest
            String enemyId = ENEMY_TYPES.get(random.nextInt(ENEMY_TYPES.size()));
            int requiredKills = getScaledAmount(1, 5); // Random number between 1 and 5, scaled by difficulty
            String title = "Defeat " + requiredKills + " " + getEntityName(enemyId);
            return new EnemyKillQuest(generateQuestId(), title, enemyId, requiredKills);
        } else {
            // Generate an ItemCollectionQuest
            String itemId = ITEM_TYPES.get(random.nextInt(ITEM_TYPES.size()));
            int requiredItems = getScaledAmount(1, 5); // Random number between 1 and 5, scaled by difficulty
            String title = "Collect " + requiredItems + " " + getItemName(itemId);
            return new ItemCollectionQuest(generateQuestId(), title, itemId, requiredItems);
        }
    }

    /**
     * Increases the difficulty level for quest generation.
     */
    public static void increaseDifficulty() {
        difficultyLevel++;
    }

    /**
     * Decreases the difficulty level for quest generation.
     */
    public static void decreaseDifficulty() {
        if (difficultyLevel > 1) {
            difficultyLevel--;
        }
    }

    /**
     * Increases the item requirement for collection quests.
     */
    public static void increaseItemRequirement() {
        // You can add logic here to increase item requirements if needed
    }

    /**
     * Decreases the item requirement for collection quests.
     */
    public static void decreaseItemRequirement() {
        // You can add logic here to decrease item requirements if needed
    }

    /**
     * Gets the current difficulty level.
     * @return The current difficulty level.
     */
    public static int getDifficultyLevel() {
        return difficultyLevel;
    }

    /**
     * Generates a unique quest ID.
     * @return A unique quest ID.
     */
    private static String generateQuestId() {
        return "quest_" + System.currentTimeMillis() + "_" + random.nextInt(1000);
    }

    /**
     * Gets the display name of an entity.
     * @param entityId The entity ID.
     * @return The display name of the entity.
     */
    private static String getEntityName(String entityId) {
        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(entityId));
        return entityType != null ? entityType.getDescription().getString() : "unknown entity";
    }

    /**
     * Gets the display name of an item.
     * @param itemId The item ID.
     * @return The display name of the item.
     */
    private static String getItemName(String itemId) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
        return item != null ? item.getName(item.getDefaultInstance()).getString() : "unknown item";
    }

    /**
     * Scales a value based on the current difficulty level.
     * @param min The minimum value.
     * @param max The maximum value.
     * @return A scaled value between min and max, adjusted by difficulty.
     */
    private static int getScaledAmount(int min, int max) {
        return min + random.nextInt(max) * difficultyLevel;
    }
}