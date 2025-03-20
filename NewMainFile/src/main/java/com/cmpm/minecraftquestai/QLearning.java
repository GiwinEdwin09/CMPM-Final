package com.cmpm.minecraftquestai;

import java.util.HashMap;
import java.util.Map;

public class QLearning {
    private final Map<GameState, Map<QuestAction, Double>> qTable = new HashMap<>();
    private final double learningRate = 0.1;
    private final double discountFactor = 0.9;
    private final double explorationRate = 0.2;

    public QuestAction chooseAction(GameState state) {
        if (Math.random() < explorationRate) {
            return QuestAction.values()[(int) (Math.random() * QuestAction.values().length)];
        } else {
            return getBestAction(state);
        }
    }

    public void updateQValue(GameState state, QuestAction action, double reward, GameState nextState) {
        double oldQValue = qTable.getOrDefault(state, new HashMap<>()).getOrDefault(action, 0.0);
        double maxFutureQValue = getMaxQValue(nextState);
        double newQValue = oldQValue + learningRate * (reward + discountFactor * maxFutureQValue - oldQValue);
        qTable.computeIfAbsent(state, k -> new HashMap<>()).put(action, newQValue);
    }

    private QuestAction getBestAction(GameState state) {
        return qTable.getOrDefault(state, new HashMap<>()).entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(QuestAction.INCREASE_MOBS);
    }

    private double getMaxQValue(GameState state) {
        return qTable.getOrDefault(state, new HashMap<>()).values().stream()
                .max(Double::compare)
                .orElse(0.0);
    }
}