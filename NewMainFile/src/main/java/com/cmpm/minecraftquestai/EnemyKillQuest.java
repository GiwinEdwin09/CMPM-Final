package com.cmpm.minecraftquestai;

import net.minecraft.core.registries.BuiltInRegistries;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = MinecraftQuestAI.MODID)
public class EnemyKillQuest implements Quest {
    private final String id;
    private final String title;
    private final String description;
    private final String entityId;
    private final int requiredAmount;

    // Track kill count for each player
    private final Map<UUID, Integer> killCount = new HashMap<>();

    // Track completed status for each player
    private final Map<UUID, Boolean> completionStatus = new HashMap<>();

    public EnemyKillQuest(String id, String title, String entityId, int requiredAmount) {
        this.id = id;
        this.title = title;
        this.entityId = entityId;
        this.requiredAmount = requiredAmount;
        this.description = generateDescription();
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
        return completionStatus.getOrDefault(player.getUUID(), false) ||
                getProgress(player) >= requiredAmount;
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
        // Mark this quest as completed for this player
        completionStatus.put(player.getUUID(), true);

        if (player instanceof ServerPlayer serverPlayer) {
            // Give player XP
            serverPlayer.giveExperiencePoints(50); // Adjust XP amount as needed

            // Inform player
            player.sendSystemMessage(Component.literal("Received quest reward: ")
                    .withStyle(Style.EMPTY.withColor(0x55FF55))
                    .append(Component.literal("50 XP").withStyle(Style.EMPTY.withColor(0xFFAA00))));
        }
    }

    /**
     * Process an enemy kill for this quest
     * @param player The player who killed the entity
     * @param entityType The type of entity that was killed
     */
    public void onEnemyKilled(Player player, EntityType<?> entityType) {
        UUID playerUUID = player.getUUID();

        // If quest is already completed for this player, do nothing
        if (completionStatus.getOrDefault(playerUUID, false)) {
            return;
        }

        // Check if the killed entity matches our target entity
        ResourceLocation killedEntityKey = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
        ResourceLocation targetEntityKey = new ResourceLocation(entityId);

        if (killedEntityKey != null && killedEntityKey.toString().equals(targetEntityKey.toString())) {
            // Increment kill count
            int current = killCount.getOrDefault(playerUUID, 0);
            killCount.put(playerUUID, current + 1);

            // Log for debugging
            System.out.println("Player " + player.getName().getString() +
                    " killed " + entityType.getDescription().getString() +
                    " - Progress: " + (current + 1) + "/" + requiredAmount +
                    " for quest " + title);

            // Notify player about progress
            if ((current + 1) % 5 == 0 || (current + 1) == requiredAmount || requiredAmount <= 5) {
                player.sendSystemMessage(Component.literal("[Quest Progress] ")
                        .withStyle(Style.EMPTY.withColor(0xFFAA00))
                        .append(Component.literal(title + ": " + (current + 1) + "/" + requiredAmount)
                                .withStyle(Style.EMPTY.withColor(0xFFFFFF))));
            }
        }
    }

    /**
     * Static event handler for entity death events
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        // Get the entity that died
        Entity killedEntity = event.getEntity();

        // Get the entity that caused the death (the player)
        Entity sourceEntity = event.getSource().getEntity();

        if (sourceEntity instanceof ServerPlayer player) {
            if (killedEntity instanceof LivingEntity) {
                EntityType<?> entityType = killedEntity.getType();

                // Log for debugging
                System.out.println("Entity killed: " + entityType.getDescription().getString() +
                        " by player: " + player.getName().getString());

                // Check if this kill applies to any active quests
                for (Quest quest : MinecraftQuestAI.questManager.getQuestsForPlayer(player)) {
                    if (quest instanceof EnemyKillQuest enemyKillQuest) {
                        enemyKillQuest.onEnemyKilled(player, entityType);
                    }
                }
            }
        }
    }
}
