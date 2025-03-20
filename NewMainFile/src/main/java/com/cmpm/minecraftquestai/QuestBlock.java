package com.cmpm.minecraftquestai;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

public class QuestBlock extends Block {

    public QuestBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            // Display quest information to the player
            player.sendSystemMessage(Component.literal("[Quest] ")
                    .withStyle(Style.EMPTY.withColor(0xFFAA00))
                    .append(Component.literal("Welcome to the quest system!")
                            .withStyle(Style.EMPTY.withColor(0xFFFFFF))));

            // Access quest manager to show player's active quests
            if (player instanceof ServerPlayer serverPlayer) {
                // Get quests specific to this player
                List<Quest> playerQuests = MinecraftQuestAI.questManager.getQuestsForPlayer(player);

                if (playerQuests != null && !playerQuests.isEmpty()) {
                    player.sendSystemMessage(Component.literal("[Quest] ")
                            .withStyle(Style.EMPTY.withColor(0xFFAA00))
                            .append(Component.literal("Your active quests:")
                                    .withStyle(Style.EMPTY.withColor(0xFFFFFF))));

                    // Display each quest with its progress
                    for (Quest quest : playerQuests) {
                        boolean completed = quest.isCompleted(player);
                        String status = completed ? "[Completed]" : "[Active]";

                        // Use different colors based on completion status
                        int statusColor = completed ? 0x55FF55 : 0xFFFF55;

                        player.sendSystemMessage(Component.literal(status)
                                .withStyle(Style.EMPTY.withColor(statusColor))
                                .append(Component.literal(" " + quest.getTitle())
                                        .withStyle(Style.EMPTY.withColor(0xFFFFFF))));

                        // Only show details for active quests
                        if (!completed) {
                            player.sendSystemMessage(Component.literal("  " + quest.getDescription())
                                    .withStyle(Style.EMPTY.withColor(0xAAAAAA)));

                            // Show progress
                            int progress = quest.getProgress(player);
                            int required = quest.getRequiredAmount();
                            int progressPercent = (int)((float)progress / required * 100);

                            String progressBar = createProgressBar(progress, required);

                            player.sendSystemMessage(Component.literal("  Progress: " + progressBar + " " +
                                            progress + "/" + required + " (" + progressPercent + "%)")
                                    .withStyle(Style.EMPTY.withColor(getProgressColor(progressPercent))));
                        }
                    }

                    // Check for completed quests and give rewards
                    MinecraftQuestAI.questManager.checkAndRewardCompletedQuests(serverPlayer);
                } else {
                    player.sendSystemMessage(Component.literal("[Quest] ")
                            .withStyle(Style.EMPTY.withColor(0xFFAA00))
                            .append(Component.literal("No quests available. Check back later!")
                                    .withStyle(Style.EMPTY.withColor(0xFF5555))));

                    // Check if we need to initialize quests for this player
                    if (MinecraftQuestAI.questManager.hasQuests()) {
                        MinecraftQuestAI.questManager.initializePlayerQuests(serverPlayer);
                        player.sendSystemMessage(Component.literal("[Quest] ")
                                .withStyle(Style.EMPTY.withColor(0xFFAA00))
                                .append(Component.literal("New quests have been assigned to you! Check the quest block again.")
                                        .withStyle(Style.EMPTY.withColor(0x55FF55))));
                    }
                }
            }
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * Creates a text-based progress bar
     * @param current Current progress
     * @param max Maximum progress
     * @return A string representing a progress bar
     */
    private String createProgressBar(int current, int max) {
        int barLength = 10;
        int filledBars = Math.min(barLength, (int)((float)current / max * barLength));

        StringBuilder progressBar = new StringBuilder("[");
        for (int i = 0; i < barLength; i++) {
            if (i < filledBars) {
                progressBar.append("■"); // Filled square
            } else {
                progressBar.append("□"); // Empty square
            }
        }
        progressBar.append("]");

        return progressBar.toString();
    }

    /**
     * Gets an appropriate color based on progress percentage
     * @param percent Progress percentage
     * @return A color code
     */
    private int getProgressColor(int percent) {
        if (percent < 25) return 0xFF5555; // Red
        if (percent < 50) return 0xFFAA00; // Orange
        if (percent < 75) return 0xFFFF55; // Yellow
        return 0x55FF55; // Green
    }
}
