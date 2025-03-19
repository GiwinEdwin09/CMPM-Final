package com.cmpm.minecraftquestai;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Random;

public class QuestGenerator {

    private static final Random random = new Random();

    // List of possible enemy types for EnemyKillQuest
    private static final List<String> ENEMY_TYPES = List.of(
            "minecraft:zombie",
            "minecraft:skeleton",
            "minecraft:creeper",
            "minecraft:spider"
    );

    // List of possible items for ItemCollectionQuest
    private static final List<String> ITEM_TYPES = List.of(
            "minecraft:iron_ingot",
            "minecraft:gold_ingot",
            "minecraft:diamond",
            "minecraft:emerald"
    );

    public static Quest generateRandomQuest() {
        // Randomly choose between EnemyKillQuest and ItemCollectionQuest
        int questType = random.nextInt(2); // 0 or 1

        if (questType == 0) {
            // Generate an EnemyKillQuest
            String enemyId = ENEMY_TYPES.get(random.nextInt(ENEMY_TYPES.size()));
            int requiredKills = random.nextInt(5) + 1; // Random number between 1 and 5
            String title = "Defeat " + requiredKills + " " + getEntityName(enemyId);
            return new EnemyKillQuest(generateQuestId(), title, enemyId, requiredKills);
        } else {
            // Generate an ItemCollectionQuest
            String itemId = ITEM_TYPES.get(random.nextInt(ITEM_TYPES.size()));
            int requiredItems = random.nextInt(5) + 1; // Random number between 1 and 5
            String title = "Collect " + requiredItems + " " + getItemName(itemId);
            return new ItemCollectionQuest(generateQuestId(), title, itemId, requiredItems);
        }
    }

    private static String generateQuestId() {
        return "quest_" + System.currentTimeMillis() + "_" + random.nextInt(1000);
    }

    private static String getEntityName(String entityId) {
        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(entityId));
        return entityType != null ? entityType.getDescription().getString() : "unknown entity";
    }

    private static String getItemName(String itemId) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
        return item != null ? item.getName(item.getDefaultInstance()).getString() : "unknown item";
    }
}