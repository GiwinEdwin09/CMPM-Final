package com.cmpm.minecraftquestai;

public class GameState {
    private int mobsKilled;
    private int itemsCollected;
    private int questsCompleted;
    private int playerHealth;
    private int currentDifficultyLevel;

    // Constructor
    public GameState(int mobsKilled, int itemsCollected, int questsCompleted, int playerHealth, int currentDifficultyLevel) {
        this.mobsKilled = mobsKilled;
        this.itemsCollected = itemsCollected;
        this.questsCompleted = questsCompleted;
        this.playerHealth = playerHealth;
        this.currentDifficultyLevel = currentDifficultyLevel;
    }

    // Getters
    public int getMobsKilled() {
        return mobsKilled;
    }

    public int getItemsCollected() {
        return itemsCollected;
    }

    public int getQuestsCompleted() {
        return questsCompleted;
    }

    public int getPlayerHealth() {
        return playerHealth;
    }

    public int getCurrentDifficultyLevel() {
        return currentDifficultyLevel;
    }

    // Setters
    public void setMobsKilled(int mobsKilled) {
        this.mobsKilled = mobsKilled;
    }

    public void setItemsCollected(int itemsCollected) {
        this.itemsCollected = itemsCollected;
    }

    public void setQuestsCompleted(int questsCompleted) {
        this.questsCompleted = questsCompleted;
    }

    public void setPlayerHealth(int playerHealth) {
        this.playerHealth = playerHealth;
    }

    public void setCurrentDifficultyLevel(int currentDifficultyLevel) {
        this.currentDifficultyLevel = currentDifficultyLevel;
    }

    // Override toString() for debugging
    @Override
    public String toString() {
        return "GameState{" +
                "mobsKilled=" + mobsKilled +
                ", itemsCollected=" + itemsCollected +
                ", questsCompleted=" + questsCompleted +
                ", playerHealth=" + playerHealth +
                ", currentDifficultyLevel=" + currentDifficultyLevel +
                '}';
    }
}