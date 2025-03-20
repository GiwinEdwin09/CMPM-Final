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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class QuestManager {
    private final List<Quest> quests = new ArrayList<>();
    private final QLearning qLearning = new QLearning();
    private final PlayerStats playerStats = new PlayerStats();
    private GameState currentState = new GameState(0, 0, 0, 20, 1); // Initial state

    // Track player-specific quest progress
    private final Map<UUID, List<Quest>> playerQuests = new HashMap<>();

    // Track cooldowns between quest generations to avoid spamming
    private final Map<UUID, Long> questGenerationCooldowns = new HashMap<>();
    private static final long QUEST_GENERATION_COOLDOWN_MS = 60000; // 1 minute cooldown

    public QuestManager() {
        // Initialize with some default quests
        quests.add(QuestGenerator.generateRandomEnemyKillQuest(1));
        quests.add(QuestGenerator.generateRandomItemCollectionQuest(1));
    }

    public void registerQuest(Quest quest) {
        if (quests.size() < 5) { // Allow more quests in the pool
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
    public void initializePlayerQuests(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        if (!playerQuests.containsKey(playerUUID) || playerQuests.get(playerUUID).isEmpty()) {
            List<Quest> newPlayerQuests = new ArrayList<>();

            // Create a mix of quest types based on starter difficulty
            newPlayerQuests.add(QuestGenerator.generateRandomEnemyKillQuest(1));
            newPlayerQuests.add(QuestGenerator.generateRandomItemCollectionQuest(1));

            // Store the quests for this player
            playerQuests.put(playerUUID, newPlayerQuests);

            // Reset cooldown for this player so they can get quests right away
            updateQuestGenerationCooldown(playerUUID);
        }
    }

    public boolean hasQuests() {
        return !quests.isEmpty();
    }

    /**
     * Generate a new quest for a player after they complete a quest
     */
    public Quest generateNewQuestAfterCompletion(ServerPlayer player, Quest completedQuest) {
        // Get player's current statistics to determine quest type and difficulty
        int mobsKilled = getTotalMobsKilled(player);
        int playerLevel = player.experienceLevel;
        int questsCompleted = currentState.getQuestsCompleted();

        // Calculate a quest difficulty factor based on player's progress
        int baseDifficulty = QuestGenerator.getDifficultyLevel();
        int adjustedDifficulty = Math.max(1, baseDifficulty);

        // Determine if we should increase difficulty
        boolean increaseDifficulty = false;
        if (questsCompleted > 0 && questsCompleted % 5 == 0) {
            // Every 5 quests, consider increasing difficulty
            increaseDifficulty = true;
        }

        // Adjust quest generation parameters based on completed quest type
        if (completedQuest instanceof EnemyKillQuest) {
            // Player completed a kill quest, adjust accordingly
            if (increaseDifficulty) {
                QuestGenerator.increaseDifficulty();
                notifyPlayerOfDifficultyChange(player, true);
            }

            // Randomly decide whether to give a different type of quest
            // or a more challenging enemy kill quest
            if (Math.random() < 0.7) {
                // 70% chance to switch to an item collection quest
                return QuestGenerator.generateRandomItemCollectionQuest(adjustedDifficulty);
            } else {
                // 30% chance to generate another kill quest, possibly harder
                return QuestGenerator.generateRandomEnemyKillQuest(adjustedDifficulty);
            }
        } else if (completedQuest instanceof ItemCollectionQuest) {
            // Player completed an item collection quest
            if (increaseDifficulty && Math.random() < 0.5) {
                QuestGenerator.increaseDifficulty();
                notifyPlayerOfDifficultyChange(player, true);
            }

            // Similar logic for item quests
            if (Math.random() < 0.7) {
                // 70% chance to switch to an enemy kill quest
                return QuestGenerator.generateRandomEnemyKillQuest(adjustedDifficulty);
            } else {
                // 30% chance to generate another item collection quest
                return QuestGenerator.generateRandomItemCollectionQuest(adjustedDifficulty);
            }
        }

        // Default fallback - generate a random quest
        return QuestGenerator.generateRandomQuest();
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
        return currentTime - lastGeneration >= QUEST_GENERATION_COOLDOWN_MS;
    }

    /**
     * Update the quest generation cooldown for a player
     */
    private void updateQuestGenerationCooldown(UUID playerUUID) {
        questGenerationCooldowns.put(playerUUID, System.currentTimeMillis());
    }

    /**
     * Check for completed quests and reward the player
     */
    public void checkAndRewardCompletedQuests(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        List<Quest> playerQuestList = playerQuests.computeIfAbsent(playerUUID, k -> new ArrayList<>(quests));

        // Create a list to track quests that need to be removed (completed quests)
        List<Quest> completedQuests = new ArrayList<>();

        for (Quest quest : playerQuestList) {
            if (!quest.isCompleted(player) && quest.getProgress(player) >= quest.getRequiredAmount()) {
                // Quest is complete - reward the player
                quest.reward(player);
                playerStats.incrementQuestsCompleted();

                // Add to completed quests list for later removal
                completedQuests.add(quest);

                // Update state
                currentState.setMobsKilled(getTotalMobsKilled(player));
                currentState.setQuestsCompleted(currentState.getQuestsCompleted() + 1);

                // Use reinforcement learning to decide next action
                QuestAction action = qLearning.chooseAction(currentState);
                adjustQuestParameters(action);

                // Calculate reward and update Q-learning values
                double reward = calculateReward(currentState, action);
                GameState nextState = simulateQuest(currentState, action);
                qLearning.updateQValue(currentState, action, reward, nextState);
                currentState = nextState;

                // Inform the player of quest completion
                player.sendSystemMessage(Component.literal("[Quest Completed] ")
                        .withStyle(Style.EMPTY.withColor(0x55FF55))
                        .append(Component.literal(quest.getTitle())
                                .withStyle(Style.EMPTY.withColor(0xFFFFFF))));
            }
        }

        // Process completed quests and generate new ones
        if (!completedQuests.isEmpty()) {
            // Check if player can receive new quests now
            if (canGenerateQuestForPlayer(playerUUID)) {
                // Remove all completed quests
                playerQuestList.removeAll(completedQuests);

                // Generate new quests to replace completed ones
                for (Quest completedQuest : completedQuests) {
                    Quest newQuest = generateNewQuestAfterCompletion(player, completedQuest);
                    playerQuestList.add(newQuest);

                    // Notify the player about the new quest
                    player.sendSystemMessage(Component.literal("[New Quest] ")
                            .withStyle(Style.EMPTY.withColor(0x55FF55))
                            .append(Component.literal(newQuest.getTitle())
                                    .withStyle(Style.EMPTY.withColor(0xFFFFFF))));

                    player.sendSystemMessage(Component.literal(newQuest.getDescription())
                            .withStyle(Style.EMPTY.withColor(0xAAAAAA)));
                }

                // Update cooldown
                updateQuestGenerationCooldown(playerUUID);
            } else {
                player.sendSystemMessage(Component.literal("[Quest System] ")
                        .withStyle(Style.EMPTY.withColor(0xFFAA00))
                        .append(Component.literal("You'll receive new quests soon!")
                                .withStyle(Style.EMPTY.withColor(0xFFFFFF))));
            }
        }

        // Save player's quest list
        playerQuests.put(playerUUID, playerQuestList);
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
