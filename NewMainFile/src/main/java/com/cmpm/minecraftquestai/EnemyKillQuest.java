package com.cmpm.minecraftquestai;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = MinecraftQuestAI.MODID)
public class EnemyKillQuest implements Quest {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnemyKillQuest.class);

    private final String id;
    private final String title;
    private final String description;
    private final String entityId;
    private final int requiredAmount;

    // Track kill count for each player
    private final Map<UUID, Integer> killCount = new HashMap<>();

    // Track completed status for each player - this is critical for proper completion handling
    private final Map<UUID, Boolean> completionStatus = new HashMap<>();

    public EnemyKillQuest(String id, String title, String entityId, int requiredAmount) {
        this.id = id;
        this.title = title;
        this.entityId = entityId;
        this.requiredAmount = requiredAmount;
        this.description = generateDescription();

        LOGGER.info("Created EnemyKillQuest: {}, Entity: {}, Required: {}",
                title, entityId, requiredAmount);
    }

    private String generateDescription() {
        EntityType<?> targetEntity = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(entityId));
        String entityName = targetEntity != null ? targetEntity.getDescription().getString() : "unknown entity";
        return "Kill " + requiredAmount + " " + entityName + "(s)";
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isCompleted(Player player) {
        UUID playerUUID = player.getUUID();

        // First check explicit completion status
        if (completionStatus.getOrDefault(playerUUID, false)) {
            return true;
        }

        // Then check if progress meets or exceeds the requirement
        int progress = getProgress(player);
        boolean completedByProgress = progress >= requiredAmount;

        // If completed by progress, update the completion status
        if (completedByProgress) {
            LOGGER.info("Quest {} is now completed for player {}. Progress: {}/{}",
                    title, player.getName().getString(), progress, requiredAmount);

            // Mark as completed
            completionStatus.put(playerUUID, true);
        }

        return completedByProgress;
    }

    @Override
    public int getProgress(Player player) {
        return killCount.getOrDefault(player.getUUID(), 0);
    }

    @Override
    public int getRequiredAmount() {
        return requiredAmount;
    }

    @Override
    public void reward(Player player) {
        UUID playerUUID = player.getUUID();

        // Explicitly mark as completed
        completionStatus.put(playerUUID, true);

        LOGGER.info("Rewarding player {} for completing kill quest: {}",
                player.getName().getString(), title);

        if (player instanceof ServerPlayer serverPlayer) {
            // Give player XP
            serverPlayer.giveExperiencePoints(50);

            // Inform player
            player.sendSystemMessage(Component.literal("Received quest reward: ")
                    .withStyle(Style.EMPTY.withColor(0x55FF55))
                    .append(Component.literal("50 XP").withStyle(Style.EMPTY.withColor(0xFFAA00))));

            // Once quest is rewarded, reset the kills counter to avoid confusion
            // with duplicated progress messages
            killCount.remove(playerUUID);
        }
    }

    public void onEnemyKilled(Player player, EntityType<?> entityType) {
        UUID playerUUID = player.getUUID();

        // Skip if already completed
        if (completionStatus.getOrDefault(playerUUID, false)) {
            // Important: Never send progress messages for completed quests
            return;
        }

        // Get resource locations for comparison
        ResourceLocation killedEntityKey = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
        ResourceLocation targetEntityKey = new ResourceLocation(entityId);

        // Ensure entity comparison uses string values
        if (killedEntityKey != null && killedEntityKey.toString().equals(targetEntityKey.toString())) {
            // Increment kill count
            int current = killCount.getOrDefault(playerUUID, 0);
            int newCount = current + 1;
            killCount.put(playerUUID, newCount);

            LOGGER.info("Player {} killed {}: {}/{} for quest '{}'",
                    player.getName().getString(),
                    entityType.getDescription().getString(),
                    newCount, requiredAmount, title);

            // Notify player about progress only if not completed
            if (newCount <= requiredAmount) {
                player.sendSystemMessage(Component.literal("[Quest Progress] ")
                        .withStyle(Style.EMPTY.withColor(0xFFAA00))
                        .append(Component.literal(title + ": " + newCount + "/" + requiredAmount)
                                .withStyle(Style.EMPTY.withColor(0xFFFFFF))));
            }

            // Send ready-to-complete message only once when exactly reaching the requirement
            if (newCount == requiredAmount) {
                LOGGER.info("Quest {} is now ready for completion!", title);
                player.sendSystemMessage(Component.literal("[Quest Complete] ")
                        .withStyle(Style.EMPTY.withColor(0x55FF55))
                        .append(Component.literal(title + " - Return to a Quest Block to claim reward!")
                                .withStyle(Style.EMPTY.withColor(0xFFFFFF))));
            }
        }
    }

    /**
     * Global event handler for entity death events
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        Entity killedEntity = event.getEntity();
        Entity sourceEntity = event.getSource().getEntity();

        if (!(sourceEntity instanceof ServerPlayer)) {
            return;
        }

        ServerPlayer player = (ServerPlayer) sourceEntity;
        if (killedEntity instanceof LivingEntity) {
            EntityType<?> entityType = killedEntity.getType();

            // Process all active quests for this player
            List<Quest> playerQuests = MinecraftQuestAI.questManager.getQuestsForPlayer(player);

            for (Quest quest : playerQuests) {
                if (quest instanceof EnemyKillQuest enemyKillQuest && !quest.isCompleted(player)) {
                    enemyKillQuest.onEnemyKilled(player, entityType);
                }
            }
        }
    }
}
