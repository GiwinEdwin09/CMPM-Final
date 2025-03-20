package com.cmpm.minecraftquestai;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

public class QuestGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuestGenerator.class);
    private static final Random random = new Random();
    private static int difficultyLevel = 1; // Default difficulty level
    private static int itemRequirementModifier = 0; // Modifier for item requirements
    private static int enemyRequirementModifier = 0; // Modifier for enemy requirements

    // Store the last action taken by the QLearning system
    private static QuestAction lastAction = null;

    // Performance tracking for adaptive difficulty
    private static int questsCompletedAtCurrentDifficulty = 0;
    private static int questsFailedAtCurrentDifficulty = 0;

    // List of possible enemy types for EnemyKillQuest
    private static final List<String> ENEMY_TYPES = List.of(
            "minecraft:zombie",
            "minecraft:skeleton",
            "minecraft:creeper",
            "minecraft:spider"
    );

    // More challenging enemy types
    private static final List<String> HARD_ENEMY_TYPES = List.of(
            "minecraft:witch",
            "minecraft:enderman",
            "minecraft:blaze",
            "minecraft:slime"
    );

    // Very challenging enemy types
    private static final List<String> VERY_HARD_ENEMY_TYPES = List.of(
            "minecraft:wither_skeleton",
            "minecraft:ghast",
            "minecraft:ravager",
            "minecraft:evoker"
    );

    // List of possible items for ItemCollectionQuest
    private static final List<String> ITEM_TYPES = List.of(
            "minecraft:iron_ingot",
            "minecraft:gold_ingot",
            "minecraft:diamond",
            "minecraft:emerald"
    );

    // More valuable/rare items
    private static final List<String> RARE_ITEM_TYPES = List.of(
            "minecraft:diamond",
            "minecraft:emerald",
            "minecraft:ender_pearl",
            "minecraft:blaze_rod"
    );

    // Very rare items
    private static final List<String> VERY_RARE_ITEM_TYPES = List.of(
            "minecraft:netherite_ingot",
            "minecraft:nether_star",
            "minecraft:dragon_breath",
            "minecraft:heart_of_the_sea"
    );

    /**
     * Generates a random quest based on the current difficulty level.
     * @return A randomly generated quest.
     */
    public static Quest generateRandomQuest() {
        int questType = random.nextInt(2); // 0 or 1
        return generateQuestByType(questType, difficultyLevel);
    }

    /**
     * Applies the given quest action to adjust quest generation parameters.
     * @param action The QLearning action to apply.
     */
    public static void applyRLAction(QuestAction action) {
        LOGGER.info("Applying RL action: {}", action);
        lastAction = action;

        switch (action) {
            case INCREASE_MOBS -> {
                increaseDifficulty();
                questsCompletedAtCurrentDifficulty = 0;
                enemyRequirementModifier += 1;
            }
            case DECREASE_MOBS -> {
                decreaseDifficulty();
                questsCompletedAtCurrentDifficulty = 0;
                enemyRequirementModifier = Math.max(0, enemyRequirementModifier - 1);
            }
            case INCREASE_ITEMS -> {
                increaseDifficulty();
                questsCompletedAtCurrentDifficulty = 0;
                itemRequirementModifier += 1;
            }
            case DECREASE_ITEMS -> {
                decreaseDifficulty();
                questsCompletedAtCurrentDifficulty = 0;
                itemRequirementModifier = Math.max(0, itemRequirementModifier - 1);
            }
        }
    }

    /**
     * Increases the difficulty level for quest generation.
     */
    public static void increaseDifficulty() {
        difficultyLevel++;
        LOGGER.info("Increased quest difficulty to level {}", difficultyLevel);
    }

    /**
     * Decreases the difficulty level for quest generation.
     */
    public static void decreaseDifficulty() {
        if (difficultyLevel > 1) {
            difficultyLevel--;
            LOGGER.info("Decreased quest difficulty to level {}", difficultyLevel);
        }
    }

    /**
     * Increases the item requirement for collection quests.
     */
    public static void increaseItemRequirement() {
        itemRequirementModifier++;
        LOGGER.info("Increased item requirement modifier to {}", itemRequirementModifier);
    }

    /**
     * Decreases the item requirement for collection quests.
     */
    public static void decreaseItemRequirement() {
        if (itemRequirementModifier > 0) {
            itemRequirementModifier--;
            LOGGER.info("Decreased item requirement modifier to {}", itemRequirementModifier);
        }
    }

    /**
     * Record a quest completion for adaptive difficulty.
     */
    public static void recordQuestCompletion() {
        questsCompletedAtCurrentDifficulty++;

        // If player is completing quests too easily, automatically increase difficulty
        if (questsCompletedAtCurrentDifficulty >= 3 && questsFailedAtCurrentDifficulty == 0) {
            LOGGER.info("Player completed {} quests easily, increasing difficulty",
                    questsCompletedAtCurrentDifficulty);
            increaseDifficulty();
            questsCompletedAtCurrentDifficulty = 0;
        }
    }

    /**
     * Record a quest failure (abandoned or expired) for adaptive difficulty.
     */
    public static void recordQuestFailure() {
        questsFailedAtCurrentDifficulty++;

        // If player is struggling with quests, automatically decrease difficulty
        if (questsFailedAtCurrentDifficulty >= 2) {
            LOGGER.info("Player struggled with {} quests, decreasing difficulty",
                    questsFailedAtCurrentDifficulty);
            decreaseDifficulty();
            questsFailedAtCurrentDifficulty = 0;
        }
    }

    /**
     * Gets the current difficulty level.
     * @return The current difficulty level.
     */
    public static int getDifficultyLevel() {
        return difficultyLevel;
    }

    /**
     * Gets the last action taken by the RL system.
     * @return The last QuestAction taken.
     */
    public static QuestAction getLastAction() {
        return lastAction;
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
        // Apply a smoother scaling that grows with difficulty but not too quickly
        return min + (int)Math.ceil(random.nextInt(max) * (1 + (difficultyLevel - 1) * 0.5));
    }

    /**
     * Generates a random enemy kill quest with the specified difficulty.
     *
     * @param difficultyFactor The difficulty factor to use.
     * @return A new enemy kill quest.
     */
    public static Quest generateRandomEnemyKillQuest(int difficultyFactor) {
        // Select enemy type based on difficulty
        String enemyId;
        double difficultyRoll = random.nextDouble();

        if (difficultyFactor > 4 && difficultyRoll < 0.3) {
            // Very hard enemies at high difficulty
            enemyId = VERY_HARD_ENEMY_TYPES.get(random.nextInt(VERY_HARD_ENEMY_TYPES.size()));
            LOGGER.info("Selected very hard enemy: {}", enemyId);
        } else if (difficultyFactor > 2 && difficultyRoll < 0.4) {
            // Hard enemies at medium difficulty
            enemyId = HARD_ENEMY_TYPES.get(random.nextInt(HARD_ENEMY_TYPES.size()));
            LOGGER.info("Selected hard enemy: {}", enemyId);
        } else {
            // Standard enemies
            enemyId = ENEMY_TYPES.get(random.nextInt(ENEMY_TYPES.size()));
            LOGGER.info("Selected standard enemy: {}", enemyId);
        }

        // Calculate required kills based on difficulty and RL modifiers
        int baseRequiredKills = 1 + random.nextInt(3); // Base range: 1-3
        int difficultyBonus = (int)(difficultyFactor * 0.7); // Smoother scaling
        int requiredKills = baseRequiredKills + difficultyBonus + enemyRequirementModifier;

        // Cap the required kills based on enemy difficulty
        int cap = 15;
        if (VERY_HARD_ENEMY_TYPES.contains(enemyId)) {
            cap = 5; // Fewer required for very hard enemies
        } else if (HARD_ENEMY_TYPES.contains(enemyId)) {
            cap = 8; // Fewer required for hard enemies
        }

        requiredKills = Math.min(requiredKills, cap);

        // Ensure minimum of 1
        requiredKills = Math.max(1, requiredKills);

        String title = "Defeat " + requiredKills + " " + getEntityName(enemyId);
        LOGGER.info("Generated kill quest: {}", title);
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
        double difficultyRoll = random.nextDouble();

        if (difficultyFactor > 4 && difficultyRoll < 0.25) {
            // Very rare items at high difficulty
            itemId = VERY_RARE_ITEM_TYPES.get(random.nextInt(VERY_RARE_ITEM_TYPES.size()));
            LOGGER.info("Selected very rare item: {}", itemId);
        } else if (difficultyFactor > 2 && difficultyRoll < 0.4) {
            // Rare items at medium difficulty
            itemId = RARE_ITEM_TYPES.get(random.nextInt(RARE_ITEM_TYPES.size()));
            LOGGER.info("Selected rare item: {}", itemId);
        } else {
            // Standard items
            itemId = ITEM_TYPES.get(random.nextInt(ITEM_TYPES.size()));
            LOGGER.info("Selected standard item: {}", itemId);
        }

        // Calculate required items based on difficulty, rarity, and RL modifiers
        int baseRequiredItems = 1 + random.nextInt(3); // Base range: 1-3

        // Adjust based on item rarity
        if (VERY_RARE_ITEM_TYPES.contains(itemId)) {
            // Very rare items, require fewer
            baseRequiredItems = 1;
        } else if (RARE_ITEM_TYPES.contains(itemId)) {
            // Rare items, require slightly fewer
            baseRequiredItems = Math.max(1, baseRequiredItems - 1);
        } else if (itemId.equals("minecraft:iron_ingot") || itemId.equals("minecraft:gold_ingot")) {
            // Common items, require more
            baseRequiredItems += 1;
        }

        // Apply difficulty factor with smooth scaling
        int difficultyBonus = (int)(difficultyFactor * 0.5);
        int requiredItems = baseRequiredItems + difficultyBonus + itemRequirementModifier;

        // Cap the required items based on item rarity
        int cap = 10;
        if (VERY_RARE_ITEM_TYPES.contains(itemId)) {
            cap = 2; // Very few for extremely rare items
        } else if (RARE_ITEM_TYPES.contains(itemId)) {
            cap = 5; // Fewer for rare items
        }

        requiredItems = Math.min(requiredItems, cap);

        // Ensure minimum of 1
        requiredItems = Math.max(1, requiredItems);

        String title = "Collect " + requiredItems + " " + getItemName(itemId);
        LOGGER.info("Generated collection quest: {}", title);
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

    /**
     * Generate a quest that follows from a completed quest, using RL to decide difficulty.
     *
     * @param completedQuest The quest that was just completed.
     * @param gameState The current game state.
     * @param rl The QLearning instance to use for decision making.
     * @return A new quest appropriate for the player's progress.
     */
    public static Quest generateFollowUpQuest(Quest completedQuest, GameState gameState, QLearning rl) {
        // Use RL to decide on the next action
        QuestAction action = rl.chooseAction(gameState);
        LOGGER.info("RL system chose action: {} for follow-up quest", action);

        // Apply the action to adjust quest parameters
        applyRLAction(action);

        // Record the quest completion for adaptive difficulty
        recordQuestCompletion();

        // Generate a quest of the opposite type from what was just completed
        if (completedQuest instanceof EnemyKillQuest) {
            // Player completed a kill quest, give an item collection quest
            return generateRandomItemCollectionQuest(difficultyLevel);
        } else {
            // Player completed an item quest, give a kill quest
            return generateRandomEnemyKillQuest(difficultyLevel);
        }
    }
}
