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

public class QuestBlock extends Block {

    public QuestBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            // Display quest information to the player
            player.sendSystemMessage(Component.literal("[Quest] ").withStyle(Style.EMPTY.withColor(0xFFAA00))
                    .append(Component.literal("Welcome to the quest system!").withStyle(Style.EMPTY.withColor(0xFFFFFF))));

            // Access quest manager to show available quests
            if (MinecraftQuestAI.questManager.hasQuests()) {
                player.sendSystemMessage(Component.literal("[Quest] ").withStyle(Style.EMPTY.withColor(0xFFAA00))
                        .append(Component.literal("Available quests:").withStyle(Style.EMPTY.withColor(0xFFFFFF))));

                for (Quest quest : MinecraftQuestAI.questManager.getQuests()) {
                    String status = quest.isCompleted(player) ? "[Completed]" : "[In Progress]";
                    player.sendSystemMessage(Component.literal(status).withStyle(Style.EMPTY.withColor(0xFFFF55))
                            .append(Component.literal(" " + quest.getTitle()).withStyle(Style.EMPTY.withColor(0xFFFFFF))));

                    if (!quest.isCompleted(player)) {
                        player.sendSystemMessage(Component.literal("  " + quest.getDescription())
                                .withStyle(Style.EMPTY.withColor(0xAAAAAA)));
                        player.sendSystemMessage(Component.literal("  Progress: " + quest.getProgress(player) + "/" + quest.getRequiredAmount())
                                .withStyle(Style.EMPTY.withColor(0xAAAAAA)));
                    }
                }

                // Check for completed quests and give rewards
                if (player instanceof ServerPlayer serverPlayer) {
                    MinecraftQuestAI.questManager.checkAndRewardCompletedQuests(serverPlayer);
                }
            } else {
                player.sendSystemMessage(Component.literal("[Quest] ").withStyle(Style.EMPTY.withColor(0xFFAA00))
                        .append(Component.literal("No quests available.").withStyle(Style.EMPTY.withColor(0xFF5555))));
            }
        }

        return InteractionResult.SUCCESS;
    }
}