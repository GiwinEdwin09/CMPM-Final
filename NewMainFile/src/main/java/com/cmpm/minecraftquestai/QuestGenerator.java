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

    /**
     * Generates a random enemy kill quest with the specified difficulty.
     *
     * @param difficultyFactor The difficulty factor to use.
     * @return A new enemy kill quest.
     */
    public static Quest generateRandomEnemyKillQuest(int difficultyFactor) {
        // Get a random enemy type, weighted by difficulty
        String enemyId;
        if (difficultyFactor > 3 && random.nextDouble() < 0.3) {
            // 30% chance to get a more difficult enemy for higher difficulty levels
            List<String> hardEnemies = List.of(
                    "minecraft:witch",
                    "minecraft:enderman",
                    "minecraft:blaze",
                    "minecraft:slime"
            );
            enemyId = hardEnemies.get(random.nextInt(hardEnemies.size()));
        } else {
            enemyId = ENEMY_TYPES.get(random.nextInt(ENEMY_TYPES.size()));
        }

        // Calculate required kills based on difficulty
        int baseRequiredKills = 1 + random.nextInt(3); // Base range: 1-3
        int requiredKills = baseRequiredKills + (difficultyFactor - 1);

        // Cap the required kills to a reasonable amount
        requiredKills = Math.min(requiredKills, 15);

        String title = "Defeat " + requiredKills + " " + getEntityName(enemyId);
        return new EnemyKillQuest(generateQuestId(), title, enemyId, requiredKills);
    }

    /**
     * Generates a random item collection quest with the specified difficulty.
     *
     * @param difficultyFactor The difficulty factor to use.
     * @return A new item collection quest.
     */
    public static Quest generateRandomItemCollectionQuest(int difficultyFactor) {
        // Determine which items to request based on difficulty
        String itemId;
        if (difficultyFactor > 2 && random.nextDouble() < 0.4) {
            // 40% chance to get rarer items for higher difficulty levels
            List<String> rareItems = List.of(
                    "minecraft:diamond",
                    "minecraft:emerald",
                    "minecraft:ender_pearl",
                    "minecraft:blaze_rod"
            );
            itemId = rareItems.get(random.nextInt(rareItems.size()));
        } else {
            itemId = ITEM_TYPES.get(random.nextInt(ITEM_TYPES.size()));
        }

        // Calculate required items based on difficulty and rarity
        int baseRequiredItems = 1 + random.nextInt(3); // Base range: 1-3

        // Adjust based on item rarity
        if (itemId.equals("minecraft:diamond") || itemId.equals("minecraft:emerald")) {
            // Diamonds and emeralds are rare, so require fewer
            baseRequiredItems = Math.max(1, baseRequiredItems - 1);
        } else if (itemId.equals("minecraft:iron_ingot") || itemId.equals("minecraft:gold_ingot")) {
            // Iron and gold are common, so potentially require more
            baseRequiredItems += 1;
        }

        // Apply difficulty factor
        int requiredItems = baseRequiredItems + (int)((difficultyFactor - 1) * 0.5);

        // Cap the required items to a reasonable amount
        requiredItems = Math.min(requiredItems, 10);

        String title = "Collect " + requiredItems + " " + getItemName(itemId);
        return new ItemCollectionQuest(generateQuestId(), title, itemId, requiredItems);
    }

    /**
     * Generate a random quest with a specific type and difficulty.
     *
     * @param questType The type of quest to generate (0 for enemy kill, 1 for item collection).
     * @param difficultyFactor The difficulty factor to use.
     * @return A new quest of the specified type.
     */
    public static Quest generateQuestByType(int questType, int difficultyFactor) {
        if (questType == 0) {
            return generateRandomEnemyKillQuest(difficultyFactor);
        } else {
            return generateRandomItemCollectionQuest(difficultyFactor);
        }
    }

}