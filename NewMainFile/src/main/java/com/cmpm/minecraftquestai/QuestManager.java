package com.cmpm.minecraftquestai;

import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import java.util.Map;

import java.util.ArrayList;
import java.util.List;

public class QuestManager {
    private final List<Quest> quests = new ArrayList<>();
    private final QLearning qLearning = new QLearning();
    private final PlayerStats playerStats = new PlayerStats();
    private GameState currentState = new GameState(0, 0, 0, 20, 1); // Initial state

    public void registerQuest(Quest quest) {
        if (quests.size() < 2) {
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

    public boolean hasQuests() {
        return !quests.isEmpty();
    }

    public void checkAndRewardCompletedQuests(ServerPlayer player) {
        for (int i = 0; i < quests.size(); i++) {
            Quest quest = quests.get(i);
            if (!quest.isCompleted(player) && quest.getProgress(player) >= quest.getRequiredAmount()) {
                quest.reward(player);
                playerStats.incrementQuestsCompleted();

                // Update state and reward
                currentState.setMobsKilled(getTotalMobsKilled(player));
                //currentState.setItemsCollected(playerStats.getItemsCollected());
                currentState.setQuestsCompleted(currentState.getQuestsCompleted() + 1);

                QuestAction action = qLearning.chooseAction(currentState);
                adjustQuestParameters(action);

                double reward = calculateReward(currentState, action);
                GameState nextState = simulateQuest(currentState, action);
                qLearning.updateQValue(currentState, action, reward, nextState);

                currentState = nextState;

                // Replace completed quest
                quests.remove(i);
                quests.add(QuestGenerator.generateRandomQuest());
            }
        }
    }

    private void adjustQuestParameters(QuestAction action) {
        // Adjust quest parameters based on RL action
        switch (action) {
            case INCREASE_MOBS -> QuestGenerator.increaseDifficulty();
            case DECREASE_MOBS -> QuestGenerator.decreaseDifficulty();
            case INCREASE_ITEMS -> QuestGenerator.increaseItemRequirement();
            case DECREASE_ITEMS -> QuestGenerator.decreaseItemRequirement();
        }
    }

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
}