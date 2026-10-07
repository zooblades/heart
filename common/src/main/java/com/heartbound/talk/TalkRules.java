package com.heartbound.talk;

import com.heartbound.gesture.Personality;

/** Chances and daily limits for conversations. Pure Java. */
public final class TalkRules {

    public static final long TICKS_PER_DAY = 24000L;

    private TalkRules() {
    }

    public static int chance(Topic topic, int affinity, boolean partner, Personality personality) {
        if (!topic.canFail()) {
            return 100;
        }
        int modifier = switch (topic) {
            case JOKE -> switch (personality) {
                case SHY -> -10;
                case NEUTRAL -> 0;
                case BOLD -> 10;
                case PLAYFUL -> 25;
            };
            case COMPLIMENT -> switch (personality) {
                case SHY -> 10;
                case NEUTRAL -> 0;
                case BOLD -> -5;
                case PLAYFUL -> 5;
            };
            case FLIRT -> switch (personality) {
                case SHY -> -15;
                case NEUTRAL -> 0;
                case BOLD -> 5;
                case PLAYFUL -> 10;
            };
            default -> 0;
        };
        int bonus = topic == Topic.FLIRT
                ? Math.min(20, Math.max(0, (affinity - Topic.FLIRT.minAffinity()) / 20))
                : Math.min(10, Math.max(0, affinity / 100));
        int chance = topic.baseChance() + modifier + bonus + (partner && topic == Topic.FLIRT ? 15 : 0);
        return Math.max(5, Math.min(95, chance));
    }

    /** How much of the normal gain a conversation gives, by how many already happened today. */
    public static int gainFactorPercent(int talksToday) {
        if (talksToday < 3) {
            return 100;
        }
        if (talksToday < 6) {
            return 50;
        }
        return 0;
    }

    public static long dayOf(long gameTime) {
        return Math.floorDiv(gameTime, TICKS_PER_DAY);
    }
}
