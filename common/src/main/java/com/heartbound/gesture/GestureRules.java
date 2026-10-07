package com.heartbound.gesture;

/** Chance that a mob accepts a gesture. Pure Java. */
public final class GestureRules {

    public static final int MIN_CHANCE = 5;
    public static final int MAX_CHANCE = 95;

    private GestureRules() {
    }

    public static int chance(Gesture gesture, int affinity, boolean partner, Personality personality) {
        int bonus = Math.min(15, Math.max(0, (affinity - gesture.minAffinity()) / 20));
        int chance = gesture.baseChance()
                + bonus
                + personality.modifier(gesture.intimacy())
                + (partner ? 10 : 0);
        return Math.max(MIN_CHANCE, Math.min(MAX_CHANCE, chance));
    }
}
