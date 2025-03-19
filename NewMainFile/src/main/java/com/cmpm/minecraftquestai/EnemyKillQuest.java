// src/main/java/com/cmpm/minecraftquestai/EnemyKillQuest.java
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
import net.minecraft.world.item.ItemStack;
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
        return killCount.getOrDefault(player.getUUID(), 0) >= requiredAmount;
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
        killCount.put(player.getUUID(), requiredAmount);

        if (player instanceof ServerPlayer serverPlayer) {
            // Give player XP
            serverPlayer.giveExperiencePoints(50); // Adjust XP amount as needed

            // Inform player
            player.sendSystemMessage(Component.literal("Received quest reward: ")
                    .withStyle(Style.EMPTY.withColor(0x55FF55))
                    .append(Component.literal("50 XP").withStyle(Style.EMPTY.withColor(0xFFAA00))));
        }
    }

    public void onEnemyKilled(Player player, EntityType<?> entityType) {
        ResourceLocation entityKey = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        if (entityKey != null && entityKey.toString().equals(entityId)) {
            UUID playerUUID = player.getUUID();
            killCount.put(playerUUID, killCount.getOrDefault(playerUUID, 0) + 1);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        Entity source = event.getSource().getEntity();
        if (source instanceof ServerPlayer player) {
            Entity target = event.getEntity();
            if (target instanceof LivingEntity livingEntity) {
                EntityType<?> entityType = livingEntity.getType();
                for (Quest quest : MinecraftQuestAI.questManager.getQuests()) {
                    if (quest instanceof EnemyKillQuest enemyKillQuest) {
                        enemyKillQuest.onEnemyKilled(player, entityType);
                    }
                }
            }
        }
    }
}