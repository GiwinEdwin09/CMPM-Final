package com.cmpm.minecraftquestai;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class QuestManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuestManager.class);

    // Quest storage
    private final List<Quest> globalQuests = new ArrayList<>();
    private final Map<UUID, List<Quest>> playerQuests = new HashMap<>();

    // Performance tracking
    private final PlayerStats playerStats = new PlayerStats();

    // RL and game state tracking
    private final QLearning qLearning = new QLearning();
    private final Map<UUID, GameState> playerGameStates = new HashMap<>();

    // Quest data persistence
    private final Map<UUID, PlayerQuestData> playerQuestData = new HashMap<>();

    // Cooldown and timing
    private final Map<UUID, Long> questGenerationCooldowns = new HashMap<>();
    private static final long QUEST_GENERATION_COOLDOWN_MS = 5000; // 5 seconds for testing

    // List of hostile mob types to track for statistics
    private static final List<EntityType<?>> HOSTILE_MOBS = List.of(
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.ENDERMAN,
            EntityType.WITCH, EntityType.SPIDER, EntityType.SLIME, EntityType.BLAZE
    );

    public QuestManager() {
        // Initialize with default quests
        globalQuests.add(QuestGenerator.generateRandomEnemyKillQuest(1));
        globalQuests.add(QuestGenerator.generateRandomItemCollectionQuest(1));
        LOGGER.info("QuestManager initialized with {} default quests", globalQuests.size());
    }

    /**
     * Register a new global quest
     */
    public void registerQuest(Quest quest) {
        if (globalQuests.size() < 10) {
            globalQuests.add(quest);
            LOGGER.info("New quest registered: {}", quest.getTitle());
        }
    }

    /**
     * Get all quests for a specific player
     */
    public List<Quest> getQuestsForPlayer(Player player) {
        UUID playerUUID = player.getUUID();
        // Initialize player quests if not present
        if (!playerQuests.containsKey(playerUUID) || playerQuests.get(playerUUID).isEmpty()) {
            LOGGER.info("Initializing quests for player: {}", player.getName().getString());
            if (player instanceof ServerPlayer serverPlayer) {
                initializePlayerQuests(serverPlayer);
            }
        }
        return playerQuests.getOrDefault(playerUUID, new ArrayList<>());
    }

    /**
     * Check if there are any quests available
     */
    public boolean hasQuests() {
        return !globalQuests.isEmpty();
    }

    /**
     * Initialize quests for a new player
     */
    public void initializePlayerQuests(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        if (!playerQuests.containsKey(playerUUID) || playerQuests.get(playerUUID).isEmpty()) {
            List<Quest> newPlayerQuests = new ArrayList<>();

            // Add an enemy kill quest
            Quest killQuest = QuestGenerator.generateRandomEnemyKillQuest(1);
            newPlayerQuests.add(killQuest);
            LOGGER.info("Created kill quest for {}: {}", player.getName().getString(), killQuest.getTitle());

            // Add an item collection quest
            Quest itemQuest = QuestGenerator.generateRandomItemCollectionQuest(1);
            newPlayerQuests.add(itemQuest);
            LOGGER.info("Created item quest for {}: {}", player.getName().getString(), itemQuest.getTitle());

            // Store the quests for this player
            playerQuests.put(playerUUID, newPlayerQuests);

            // Initialize game state
            playerGameStates.put(playerUUID, new GameState(0, 0, 0, (int)player.getHealth(), 1));

            // Reset cooldown so they can get new quests
            questGenerationCooldowns.put(playerUUID, 0L);

            LOGGER.info("Initialized {} quests for player {}", newPlayerQuests.size(), player.getName().getString());
        }
    }

    /**
     * Generate a new quest after completion, using RL to decide on quest parameters
     */
    public Quest generateNewQuestAfterCompletion(ServerPlayer player, Quest completedQuest) {
        LOGGER.info("Generating new quest to replace {} for player {}",
                completedQuest.getTitle(), player.getName().getString());

        // Get or create game state for this player
        GameState gameState = getOrCreateGameState(player);

        // Update game state after quest completion
        gameState.setQuestsCompleted(gameState.getQuestsCompleted() + 1);
        gameState.setMobsKilled(getTotalMobsKilled(player));

        // Use RL to generate follow-up quest
        Quest newQuest = QuestGenerator.generateFollowUpQuest(completedQuest, gameState, qLearning);

        // Calculate reward for the RL system based on player performance
        double reward = calculateReward(gameState, QuestGenerator.getLastAction());

        // Simulate next state after action
        GameState nextState = simulateNextState(gameState, QuestGenerator.getLastAction());

        // Update QL values
        qLearning.updateQValue(gameState, QuestGenerator.getLastAction(), reward, nextState);

        // Update player's game state
        playerGameStates.put(player.getUUID(), nextState);

        return newQuest;
    }

    /**
     * Get or create a game state for a player
     */
    private GameState getOrCreateGameState(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        GameState gameState = playerGameStates.get(playerUUID);

        if (gameState == null) {
            // Create new game state based on player stats
            int mobsKilled = getTotalMobsKilled(player);
            int itemsCollected = 0; // Could track this from player's statistics if needed
            int questsCompleted = 0;
            int playerHealth = (int) player.getHealth();
            int currentDifficultyLevel = QuestGenerator.getDifficultyLevel();

            gameState = new GameState(mobsKilled, itemsCollected, questsCompleted,
                    playerHealth, currentDifficultyLevel);

            playerGameStates.put(playerUUID, gameState);
        }

        return gameState;
    }

    /**
     * Check if a player can receive new quests based on cooldown
     */
    private boolean canGenerateQuestForPlayer(UUID playerUUID) {
        long lastGeneration = questGenerationCooldowns.getOrDefault(playerUUID, 0L);
        long currentTime = System.currentTimeMillis();
        boolean canGenerate = currentTime - lastGeneration >= QUEST_GENERATION_COOLDOWN_MS;

        LOGGER.debug("Quest generation cooldown check: lastGeneration={}, currentTime={}, canGenerate={}",
                lastGeneration, currentTime, canGenerate);

        return canGenerate;
    }

    /**
     * Update quest generation cooldown
     */
    private void updateQuestGenerationCooldown(UUID playerUUID) {
        long time = System.currentTimeMillis();
        questGenerationCooldowns.put(playerUUID, time);
        LOGGER.debug("Updated quest generation cooldown for player {} to {}", playerUUID, time);
    }

    /**
     * Calculate reward for RL system based on player performance
     */
    private double calculateReward(GameState state, QuestAction action) {
        double reward = 0.0;

        // Base reward for completing a quest
        reward += 1.0;

        // If player has completed many quests at current difficulty, reward more
        if (state.getQuestsCompleted() > 5 * state.getCurrentDifficultyLevel()) {
            reward += 2.0; // Player is doing well at current difficulty
        }

        // If player health is low, penalize making things harder
        if (state.getPlayerHealth() < 10) {
            if (action == QuestAction.INCREASE_MOBS || action == QuestAction.INCREASE_ITEMS) {
                reward -= 1.0;
            }
        }

        // Reward system for finding the right difficulty
        // Too easy (many quests completed quickly) should lead to increased difficulty
        // Too hard (player health low, few quests completed) should reduce difficulty
        if (state.getQuestsCompleted() > 10 * state.getCurrentDifficultyLevel()) {
            // Too easy - reward increasing difficulty
            if (action == QuestAction.INCREASE_MOBS || action == QuestAction.INCREASE_ITEMS) {
                reward += 3.0;
            }
        } else if (state.getQuestsCompleted() < 3 * state.getCurrentDifficultyLevel()) {
            // Too hard - reward decreasing difficulty
            if (action == QuestAction.DECREASE_MOBS || action == QuestAction.DECREASE_ITEMS) {
                reward += 3.0;
            }
        }

        LOGGER.info("Calculated reward: {} for action: {}", reward, action);
        return reward;
    }

    /**
     * Simulate next game state after applying an action
     */
    private GameState simulateNextState(GameState state, QuestAction action) {
        int newDifficulty = state.getCurrentDifficultyLevel();

        // Update difficulty based on action
        if (action == QuestAction.INCREASE_MOBS || action == QuestAction.INCREASE_ITEMS) {
            newDifficulty++;
        } else if (action == QuestAction.DECREASE_MOBS || action == QuestAction.DECREASE_ITEMS) {
            newDifficulty = Math.max(1, newDifficulty - 1);
        }

        // Create new state with updated values
        return new GameState(
                state.getMobsKilled(),
                state.getItemsCollected(),
                state.getQuestsCompleted() + 1,
                state.getPlayerHealth(),
                newDifficulty
        );
    }

    /**
     * Get total hostile mobs killed by player
     */
    public static int getTotalMobsKilled(ServerPlayer player) {
        return HOSTILE_MOBS.stream()
                .mapToInt(entityType -> player.getStats().getValue(Stats.ENTITY_KILLED.get(entityType)))
                .sum();
    }

    /**
     * Provide information about the current quest system state
     */
    public Component getQuestSystemInfo(Player player) {
        GameState state = playerGameStates.get(player.getUUID());
        if (state == null) {
            return Component.literal("[Quest System] ")
                    .withStyle(Style.EMPTY.withColor(0xFFAA00))
                    .append(Component.literal("No quest data available.")
                            .withStyle(Style.EMPTY.withColor(0xFFFFFF)));
        }

        return Component.literal("[Quest System] ")
                .withStyle(Style.EMPTY.withColor(0xFFAA00))
                .append(Component.literal("Current Difficulty: " + state.getCurrentDifficultyLevel() +
                                ", Quests Completed: " + state.getQuestsCompleted())
                        .withStyle(Style.EMPTY.withColor(0xFFFFFF)));
    }

    /**
     * Check for completed quests and reward the player
     */
    public void checkAndRewardCompletedQuests(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        LOGGER.info("Checking for completed quests for player: {}", player.getName().getString());

        // Get player's quest list, initialize if not present
        List<Quest> playerQuestList = playerQuests.computeIfAbsent(playerUUID, k -> new ArrayList<>());
        if (playerQuestList.isEmpty()) {
            LOGGER.info("Player had no quests, initializing...");
            initializePlayerQuests(player);
            playerQuestList = playerQuests.get(playerUUID);
        }

        // Create copies of the lists to avoid concurrent modification
        List<Quest> completedQuests = new ArrayList<>();
        List<Quest> remainingQuests = new ArrayList<>();

        // First pass: check which quests are completed
        for (Quest quest : playerQuestList) {
            // Force a progress check
            int progress = quest.getProgress(player);
            int required = quest.getRequiredAmount();

            LOGGER.info("Quest check: '{}' - Progress: {}/{}, Completed: {}",
                    quest.getTitle(), progress, required, quest.isCompleted(player));

            if (quest.isCompleted(player)) {
                // Already completed, track in completed list
                completedQuests.add(quest);
                LOGGER.info("Quest '{}' is already completed", quest.getTitle());
            } else if (progress >= required) {
                // Ready for completion
                LOGGER.info("Quest '{}' is ready for completion", quest.getTitle());

                // Reward the player
                quest.reward(player);
                playerStats.incrementQuestsCompleted();

                // Add to completed list
                completedQuests.add(quest);

                // Update game state with RL
                GameState gameState = getOrCreateGameState(player);
                gameState.setQuestsCompleted(gameState.getQuestsCompleted() + 1);

                // Inform player
                player.sendSystemMessage(Component.literal("[Quest Completed] ")
                        .withStyle(Style.EMPTY.withColor(0x55FF55))
                        .append(Component.literal(quest.getTitle())
                                .withStyle(Style.EMPTY.withColor(0xFFFFFF))));
            } else {
                // Not completed, keep in remaining list
                remainingQuests.add(quest);
            }
        }

        // Generate new quests to replace completed ones
        if (!completedQuests.isEmpty()) {
            LOGGER.info("Found {} completed quests for player {}",
                    completedQuests.size(), player.getName().getString());

            // Generate new quests for each completed quest using RL
            for (Quest completedQuest : completedQuests) {
                Quest newQuest = generateNewQuestAfterCompletion(player, completedQuest);
                remainingQuests.add(newQuest);

                // Notify player about difficulty if it changed
                if (QuestGenerator.getLastAction() == QuestAction.INCREASE_MOBS ||
                        QuestGenerator.getLastAction() == QuestAction.INCREASE_ITEMS) {
                    player.sendSystemMessage(Component.literal("[Quest System] ")
                            .withStyle(Style.EMPTY.withColor(0xFFAA00))
                            .append(Component.literal("The quests are getting more challenging!")
                                    .withStyle(Style.EMPTY.withColor(0xFFFF55))));
                } else if (QuestGenerator.getLastAction() == QuestAction.DECREASE_MOBS ||
                        QuestGenerator.getLastAction() == QuestAction.DECREASE_ITEMS) {
                    player.sendSystemMessage(Component.literal("[Quest System] ")
                            .withStyle(Style.EMPTY.withColor(0xFFAA00))
                            .append(Component.literal("The quests are becoming more manageable.")
                                    .withStyle(Style.EMPTY.withColor(0xAAAAAA))));
                }

                // Notify player
                player.sendSystemMessage(Component.literal("[New Quest] ")
                        .withStyle(Style.EMPTY.withColor(0x55FF55))
                        .append(Component.literal(newQuest.getTitle())
                                .withStyle(Style.EMPTY.withColor(0xFFFFFF))));

                player.sendSystemMessage(Component.literal(newQuest.getDescription())
                        .withStyle(Style.EMPTY.withColor(0xAAAAAA)));
            }

            // Update the player's quest list with the new quests
            playerQuests.put(playerUUID, remainingQuests);
            LOGGER.info("Updated player's quest list, now has {} quests", remainingQuests.size());

            // Update cooldown
            updateQuestGenerationCooldown(playerUUID);

            // Show current system information
            player.sendSystemMessage(getQuestSystemInfo(player));
        } else {
            LOGGER.info("No completed quests found for player {}", player.getName().getString());
        }
    }

    /**
     * Quest data storage class
     */
    public static class PlayerQuestData {
        public UUID playerUUID;
        public List<String> questIds = new ArrayList<>();
        public int questsCompleted;
        public long lastQuestGeneration;

        public PlayerQuestData(UUID playerUUID) {
            this.playerUUID = playerUUID;
        }
    }

    /**
     * Save quest data for a player
     */
    public void savePlayerQuestData(Player player) {
        UUID playerUUID = player.getUUID();

        // Create or get existing data
        PlayerQuestData data = playerQuestData.computeIfAbsent(playerUUID, PlayerQuestData::new);

        // Update data fields
        data.questIds.clear();
        List<Quest> playerQuestList = playerQuests.getOrDefault(playerUUID, new ArrayList<>());
        for (Quest quest : playerQuestList) {
            data.questIds.add(quest.getId());
        }

        GameState state = playerGameStates.get(playerUUID);
        if (state != null) {
            data.questsCompleted = state.getQuestsCompleted();
        }

        data.lastQuestGeneration = questGenerationCooldowns.getOrDefault(playerUUID, 0L);

        // Save to player NBT if possible
        if (player instanceof ServerPlayer) {
            CompoundTag persistentData = player.getPersistentData();
            CompoundTag questData = new CompoundTag();

            // Convert to strings for NBT
            questData.putString("playerUUID", playerUUID.toString());
            questData.putString("questIds", String.join(",", data.questIds));
            questData.putInt("questsCompleted", data.questsCompleted);
            questData.putLong("lastQuestGeneration", data.lastQuestGeneration);
            questData.putInt("difficultyLevel", QuestGenerator.getDifficultyLevel());

            // Store in player's persistent data
            persistentData.put(MinecraftQuestAI.MODID + "_questData", questData);
        }

        // Store in our map for runtime access
        playerQuestData.put(playerUUID, data);

        LOGGER.info("Saved quest data for player {} with {} quests",
                player.getName().getString(), data.questIds.size());
    }

    /**
     * Load quest data for a player
     */
    public void loadPlayerQuestData(Player player) {
        UUID playerUUID = player.getUUID();

        // Try to load from player NBT first
        boolean loadedFromNBT = false;
        if (player instanceof ServerPlayer) {
            CompoundTag persistentData = player.getPersistentData();
            if (persistentData.contains(MinecraftQuestAI.MODID + "_questData")) {
                CompoundTag questData = persistentData.getCompound(MinecraftQuestAI.MODID + "_questData");

                PlayerQuestData data = new PlayerQuestData(playerUUID);

                // Only populate if questIds is present and not empty
                String questIdsStr = questData.getString("questIds");
                if (!questIdsStr.isEmpty()) {
                    data.questIds = Arrays.asList(questIdsStr.split(","));
                    data.questsCompleted = questData.getInt("questsCompleted");
                    data.lastQuestGeneration = questData.getLong("lastQuestGeneration");

                    // Set difficulty level if saved
                    if (questData.contains("difficultyLevel")) {
                        // This requires a way to set the difficulty level in QuestGenerator
                        // which isn't implemented yet
                    }

                    // Restore quest list
                    List<Quest> playerQuestList = new ArrayList<>();
                    for (String questId : data.questIds) {
                        // Try to find the quest by ID or generate a new one if not found
                        Quest quest = findQuestById(questId);
                        if (quest != null) {
                            playerQuestList.add(quest);
                        }
                    }

                    // If we loaded from NBT successfully
                    if (!playerQuestList.isEmpty()) {
                        playerQuests.put(playerUUID, playerQuestList);
                        questGenerationCooldowns.put(playerUUID, data.lastQuestGeneration);

                        // Create game state
                        GameState gameState = new GameState(
                                0, // We don't know mobs killed from NBT
                                0, // We don't know items collected from NBT
                                data.questsCompleted,
                                (int) player.getHealth(),
                                QuestGenerator.getDifficultyLevel()
                        );
                        playerGameStates.put(playerUUID, gameState);

                        loadedFromNBT = true;

                        LOGGER.info("Loaded {} quests for player {} from NBT",
                                playerQuestList.size(), player.getName().getString());
                    }
                }
            }
        }

        // If we couldn't load from NBT, initialize with default quests
        if (!loadedFromNBT) {
            if (player instanceof ServerPlayer serverPlayer) {
                initializePlayerQuests(serverPlayer);
                LOGGER.info("Initialized default quests for player {}", player.getName().getString());
            }
        }
    }

    /**
     * Find a quest by its ID
     */
    private Quest findQuestById(String questId) {
        // Check main quest list first
        for (Quest quest : globalQuests) {
            if (quest.getId().equals(questId)) {
                return quest;
            }
        }

        // Check all player quests
        for (List<Quest> playerQuestList : playerQuests.values()) {
            for (Quest quest : playerQuestList) {
                if (quest.getId().equals(questId)) {
                    return quest;
                }
            }
        }

        // If quest not found, use the ID to determine its type and recreate it
        if (questId != null && !questId.isEmpty()) {
            // Simple approach: if ID contains "enemy" or "kill", it's a kill quest
            if (questId.contains("enemy") || questId.contains("kill")) {
                return QuestGenerator.generateRandomEnemyKillQuest(1);
            } else {
                return QuestGenerator.generateRandomItemCollectionQuest(1);
            }
        }

        return null;
    }

    /**
     * Event handlers for player login/logout and other events
     */
    @Mod.EventBusSubscriber(modid = MinecraftQuestAI.MODID)
    public static class QuestDataEventHandler {
        @SubscribeEvent
        public static void onPlayerLoggedIn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
            Player player = event.getEntity();
            if (player instanceof ServerPlayer serverPlayer) {
                MinecraftQuestAI.questManager.loadPlayerQuestData(serverPlayer);
            }
        }

        @SubscribeEvent
        public static void onPlayerLoggedOut(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
            Player player = event.getEntity();
            if (player instanceof ServerPlayer serverPlayer) {
                MinecraftQuestAI.questManager.savePlayerQuestData(serverPlayer);
            }
        }

        @SubscribeEvent
        public static void onPlayerRespawn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event) {
            Player player = event.getEntity();
            if (player instanceof ServerPlayer serverPlayer) {
                MinecraftQuestAI.questManager.loadPlayerQuestData(serverPlayer);
            }
        }

        @SubscribeEvent
        public static void onPlayerClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
            Player originalPlayer = event.getOriginal();
            Player newPlayer = event.getEntity();

            if (!event.isWasDeath()) {
                return;
            }

            if (originalPlayer instanceof ServerPlayer && newPlayer instanceof ServerPlayer) {
                // Copy NBT data from original player to new player
                CompoundTag originalData = originalPlayer.getPersistentData();
                CompoundTag newData = newPlayer.getPersistentData();

                if (originalData.contains(MinecraftQuestAI.MODID + "_questData")) {
                    CompoundTag questData = originalData.getCompound(MinecraftQuestAI.MODID + "_questData");
                    newData.put(MinecraftQuestAI.MODID + "_questData", questData.copy());
                }
            }
        }
    }
}
