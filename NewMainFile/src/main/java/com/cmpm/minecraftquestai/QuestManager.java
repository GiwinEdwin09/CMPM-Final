package com.cmpm.minecraftquestai;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class QuestManager {
    private final List<Quest> quests = new ArrayList<>();

    public void registerQuest(Quest quest) {
        quests.add(quest);
    }

    public List<Quest> getQuests() {
        return quests;
    }

    public boolean hasQuests() {
        return !quests.isEmpty();
    }

    public void checkAndRewardCompletedQuests(Player player) {
        for (Quest quest : quests) {
            if (!quest.isCompleted(player) && quest.getProgress(player) >= quest.getRequiredAmount()) {
                quest.reward(player);
                player.sendSystemMessage(Component.literal("Quest completed: " + quest.getTitle())
                        .withStyle(Style.EMPTY.withColor(0x55FF55)));
            }
        }
    }
}
