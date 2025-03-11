package com.cmpm.minecraftquestai;

import net.minecraft.world.entity.player.Player;

public interface Quest {
    String getId();
    String getTitle();
    String getDescription();
    boolean isCompleted(Player player);
    int getProgress(Player player);
    int getRequiredAmount();
    void reward(Player player);
}