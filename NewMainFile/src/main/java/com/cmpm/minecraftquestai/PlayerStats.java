package com.cmpm.minecraftquestai;

public class PlayerStats {
    private int mobsKilled;
    private int itemsCollected;
    private int questsCompleted;

    public void incrementMobsKilled() {
        mobsKilled++;
    }

    public void incrementItemsCollected(int amount) {
        itemsCollected += amount;
    }

    public void incrementQuestsCompleted() {
        questsCompleted++;
    }

    // Getters
}