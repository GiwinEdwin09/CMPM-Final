package com.cmpm.minecraftquestai;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ItemCollectionQuest implements Quest {
    private final String id;
    private final String title;
    private final String description;
    private final String itemId;
    private final int requiredAmount;

    // Track completion status for each player
    private final Map<UUID, Boolean> completionStatus = new HashMap<>();

    public ItemCollectionQuest(String id, String title, String itemId, int requiredAmount) {
        this.id = id;
        this.title = title;
        this.itemId = itemId;
        this.requiredAmount = requiredAmount;
        this.description = generateDescription();
    }

    private String generateDescription() {
        Item targetItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
        String itemName = targetItem != null ? targetItem.getName(new ItemStack(targetItem)).getString() : "unknown item";
        return "Collect " + requiredAmount + " " + itemName + "(s)";
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
        return completionStatus.getOrDefault(player.getUUID(), false);
    }

    @Override
    public int getProgress(Player player) {
        Item targetItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
        if (targetItem == null) {
            return 0;
        }

        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == targetItem) {
                count += stack.getCount();
            }
        }

        return count;
    }

    @Override
    public int getRequiredAmount() {
        return requiredAmount;
    }

    @Override
    public void reward(Player player) {
        // Mark quest as completed
        completionStatus.put(player.getUUID(), true);

        if (player instanceof ServerPlayer serverPlayer) {
            // Give player rewards
            ItemStack reward = new ItemStack(MinecraftQuestAI.QUEST_TOKEN.get(), 1);
            if (!serverPlayer.getInventory().add(reward)) {
                // If inventory is full, drop at player's feet
                serverPlayer.drop(reward, false);
            }

            // Remove the required items
            Item targetItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
            if (targetItem != null) {
                int remaining = requiredAmount;
                for (int i = 0; i < serverPlayer.getInventory().getContainerSize() && remaining > 0; i++) {
                    ItemStack stack = serverPlayer.getInventory().getItem(i);
                    if (!stack.isEmpty() && stack.getItem() == targetItem) {
                        int toRemove = Math.min(stack.getCount(), remaining);
                        stack.shrink(toRemove);
                        remaining -= toRemove;
                    }
                }
            }

            // Inform player
            player.sendSystemMessage(Component.literal("Received quest reward: ")
                    .withStyle(Style.EMPTY.withColor(0x55FF55))
                    .append(Component.literal("Quest Token").withStyle(Style.EMPTY.withColor(0xFFAA00))));
        }
    }
}