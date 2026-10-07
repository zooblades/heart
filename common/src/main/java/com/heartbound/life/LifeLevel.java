package com.heartbound.life;

/** Numbers behind the "life intensity" setting (0 off, 1 rare, 2 normal, 3 lively). Pure Java. */
public final class LifeLevel {

    private LifeLevel() {
    }

    public static int clamp(int level) {
        return Math.max(0, Math.min(3, level));
    }

    /** How many mobs may be doing an activity at once around one player. */
    public static int maxActive(int level) {
        return switch (clamp(level)) {
            case 0 -> 0;
            case 1 -> 2;
            case 2 -> 4;
            default -> 6;
        };
    }

    /** Chance per second (percent) that an idle, free mob starts something. */
    public static int startChancePercent(int level) {
        return switch (clamp(level)) {
            case 0 -> 0;
            case 1 -> 8;
            case 2 -> 15;
            default -> 25;
        };
    }

    /** Minimum pause after an activity, in ticks. */
    public static int pauseTicks(int level) {
        return switch (clamp(level)) {
            case 1 -> 1200;
            case 2 -> 600;
            case 3 -> 300;
            default -> 1200;
        };
    }
}
