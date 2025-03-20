package com.cmpm.minecraftquestai;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class QuestManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuestManager.class);

    private final List<Quest> quests = new ArrayList<>();
    private final QLearning qLearning = new QLearning();
    private final PlayerStats playerStats = new PlayerStats();
    private GameState currentState = new GameState(0, 0, 0, 20, 1); // Initial state

    private final List<Quest> globalQuests = new ArrayList<>();
    // Track player-specific quest progress
    private final Map<UUID, List<Quest>> playerQuests = new HashMap<>();


    // Track cooldowns between quest generations to avoid spamming
    private final Map<UUID, Long> questGenerationCooldowns = new HashMap<>();
    //private static final long QUEST_GENERATION_COOLDOWN_MS = 60000; // 1 minute cooldown
    private static final long QUEST_GENERATION_COOLDOWN_MS = 5000; // Reduced to 5 seconds for testing

    public QuestManager() {
        // Initialize with some default quests
        quests.add(QuestGenerator.generateRandomEnemyKillQuest(1));
        quests.add(QuestGenerator.generateRandomItemCollectionQuest(1));
        LOGGER.info("QuestManager initialized with {} default quests", globalQuests.size());
    }

    public void registerQuest(Quest quest) {
        if (quests.size() < 10) { // Allow more quests in the pool
            quests.add(quest);
        }
    }


    private static final List<EntityType<?>> HOSTILE_MOBS = List.of(
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.ENDERMAN,
            EntityType.WITCH, EntityType.SPIDER, EntityType.SLIME, EntityType.BLAZE
    );

    public static int getTotalMobsKilled(ServerPlayer player) {
        return HOSTILE_MOBS.stream()
                .mapToInt(entityType -> player.getStats().getValue(Stats.ENTITY_KILLED.get(entityType)))
                .sum();
    }

    public List<Quest> getQuests() {
        return quests;
    }

    public List<Quest> getQuestsForPlayer(Player player) {
        return playerQuests.getOrDefault(player.getUUID(), new ArrayList<>());
    }

    /**
     * Initialize quests for a player if they don't have any
     * @param player The player to initialize quests for
     */

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

            // Reset cooldown so they can get new quests
            questGenerationCooldowns.put(playerUUID, 0L);

            LOGGER.info("Initialized {} quests for player {}", newPlayerQuests.size(), player.getName().getString());
        }
    }

    public boolean hasQuests() {
        return !quests.isEmpty();
    }

    /**
     * Generate a new quest for a player after they complete a quest
     */
    public Quest generateNewQuestAfterCompletion(ServerPlayer player, Quest completedQuest) {
        LOGGER.info("Generating new quest to replace {} for player {}",
                completedQuest.getTitle(), player.getName().getString());

        int difficultyLevel = QuestGenerator.getDifficultyLevel();

        // Track what type of quest was completed to alternate types
        Quest newQuest;
        if (completedQuest instanceof EnemyKillQuest) {
            // Player completed a kill quest, give them an item quest
            newQuest = QuestGenerator.generateRandomItemCollectionQuest(difficultyLevel);
            LOGGER.info("Generated item collection quest: {}", newQuest.getTitle());
        } else {
            // Player completed an item quest, give them a kill quest
            newQuest = QuestGenerator.generateRandomEnemyKillQuest(difficultyLevel);
            LOGGER.info("Generated enemy kill quest: {}", newQuest.getTitle());
        }

        return newQuest;
    }
    /**
     * Helper method to notify the player about difficulty changes
     */
    private void notifyPlayerOfDifficultyChange(ServerPlayer player, boolean increase) {
        String message = increase
                ? "The quests are getting more challenging!"
                : "The quests are becoming easier.";

        player.sendSystemMessage(Component.literal("[Quest System] ")
                .withStyle(Style.EMPTY.withColor(0xFFAA00))
                .append(Component.literal(message)
                        .withStyle(Style.EMPTY.withColor(0xFFFFFF))));
    }

    /**
     * Check if a player can receive a new quest based on cooldown
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
     * Update the quest generation cooldown for a player
     */
    private void updateQuestGenerationCooldown(UUID playerUUID) {
        long time = System.currentTimeMillis();
        questGenerationCooldowns.put(playerUUID, time);
        LOGGER.debug("Updated quest generation cooldown for player {} to {}", playerUUID, time);
    }

    /**
     * Check for completed quests and reward players
     * FIXED: This is the key method that was causing issues with quest regeneration
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

                // Update game state
                currentState.setQuestsCompleted(currentState.getQuestsCompleted() + 1);

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

            // Generate new quests for each completed quest
            for (Quest completedQuest : completedQuests) {
                Quest newQuest = generateNewQuestAfterCompletion(player, completedQuest);
                remainingQuests.add(newQuest);

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
        } else {
            LOGGER.info("No completed quests found for player {}", player.getName().getString());
        }
    }


    /**
     * Adjust quest parameters based on RL action
     */
    private void adjustQuestParameters(QuestAction action) {
        // Adjust quest parameters based on RL action
        switch (action) {
            case INCREASE_MOBS -> QuestGenerator.increaseDifficulty();
            case DECREASE_MOBS -> QuestGenerator.decreaseDifficulty();
            case INCREASE_ITEMS -> QuestGenerator.increaseItemRequirement();
            case DECREASE_ITEMS -> QuestGenerator.decreaseItemRequirement();
        }
    }

    /**
     * Calculate reward based on player performance
     */
    private double calculateReward(GameState state, QuestAction action) {
        // Calculate reward based on player performance
        double reward = 0.0;
        if (state.getQuestsCompleted() > 10 * state.getCurrentDifficultyLevel()) {
            reward += 5.0; // Player is doing well
        } else if (state.getQuestsCompleted() < 5 * state.getCurrentDifficultyLevel()) {
            reward -= 5.0; // Player is struggling
        }
        return reward;
    }

    /**
     * Simulate the next state based on the action
     */
    private GameState simulateQuest(GameState state, QuestAction action) {
        // Simulate the next state based on the action
        return new GameState(
                state.getMobsKilled(),
                state.getItemsCollected(),
                state.getQuestsCompleted() + 1,
                state.getPlayerHealth(),
                state.getCurrentDifficultyLevel() + (action == QuestAction.INCREASE_MOBS ? 1 : 0)
        );
    }



    //QUEST DATA PERSISTENCE:

    // Quest data storage class
    public static class PlayerQuestData {
        public UUID playerUUID;
        public List<String> questIds = new ArrayList<>();
        public int questsCompleted;
        public long lastQuestGeneration;

        public PlayerQuestData(UUID playerUUID) {
            this.playerUUID = playerUUID;
        }
    }

    // Field to store player quest data
    private final Map<UUID, PlayerQuestData> playerQuestData = new HashMap<>();

    /**
     * Save quest data for a player
     * @param player The player whose data should be saved
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

        data.questsCompleted = currentState.getQuestsCompleted();
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

            // Store in player's persistent data
            persistentData.put(MinecraftQuestAI.MODID + "_questData", questData);
        }

        // Store in our map for runtime access
        playerQuestData.put(playerUUID, data);
    }

    /**
     * Load quest data for a player
     * @param player The player whose data should be loaded
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
                data.questIds = Arrays.asList(questData.getString("questIds").split(","));
                data.questsCompleted = questData.getInt("questsCompleted");
                data.lastQuestGeneration = questData.getLong("lastQuestGeneration");

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
                    currentState.setQuestsCompleted(data.questsCompleted);
                    loadedFromNBT = true;
                }
            }
        }

        // If we couldn't load from NBT, initialize with default quests
        if (!loadedFromNBT) {
            initializePlayerQuests((ServerPlayer) player);
        }
    }

    /**
     * Find a quest by its ID
     * @param questId The ID of the quest to find
     * @return The quest with the specified ID, or null if not found
     */
    private Quest findQuestById(String questId) {
        // Check main quest list first
        for (Quest quest : quests) {
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
     * Event handler for player login and logout to save/load quest data
     */
    @Mod.EventBusSubscriber(modid = MinecraftQuestAI.MODID)
    public static class QuestDataEventHandler {
        @SubscribeEvent
        public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            Player player = event.getEntity();
            if (player instanceof ServerPlayer serverPlayer) {
                MinecraftQuestAI.questManager.loadPlayerQuestData(serverPlayer);
            }
        }

        @SubscribeEvent
        public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
            Player player = event.getEntity();
            if (player instanceof ServerPlayer serverPlayer) {
                MinecraftQuestAI.questManager.savePlayerQuestData(serverPlayer);
            }
        }

        @SubscribeEvent
        public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
            Player player = event.getEntity();
            if (player instanceof ServerPlayer serverPlayer) {
                MinecraftQuestAI.questManager.loadPlayerQuestData(serverPlayer);
            }
        }

        @SubscribeEvent
        public static void onPlayerClone(PlayerEvent.Clone event) {
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
