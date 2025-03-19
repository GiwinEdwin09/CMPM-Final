package com.cmpm.minecraftquestai;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class QuestManager {
    private final List<Quest> quests = new ArrayList<>();

    /**
     * Registers a new quest, ensuring that no more than 2 quests are active at a time.
     * @param quest The quest to register.
     */
    public void registerQuest(Quest quest) {
        if (quests.size() < 2) {
            quests.add(quest);
        }
    }

    /**
     * Returns the list of active quests.
     * @return The list of active quests.
     */
    public List<Quest> getQuests() {
        return quests;
    }

    /**
     * Checks if there are any active quests.
     * @return True if there are active quests, false otherwise.
     */
    public boolean hasQuests() {
        return !quests.isEmpty();
    }

    /**
     * Checks and rewards completed quests for a player.
     * If a quest is completed, it is replaced with a new random quest.
     * @param player The player to check and reward.
     */
    public void checkAndRewardCompletedQuests(Player player) {
        for (int i = 0; i < quests.size(); i++) {
            Quest quest = quests.get(i);
            if (!quest.isCompleted(player) && quest.getProgress(player) >= quest.getRequiredAmount()) {
                // Reward the player
                quest.reward(player);
                player.sendSystemMessage(Component.literal("Quest completed: " + quest.getTitle())
                        .withStyle(Style.EMPTY.withColor(0x55FF55)));

                // Remove the completed quest and generate a new one
                quests.remove(i);
                quests.add(QuestGenerator.generateRandomQuest());
            }
        }
    }
}