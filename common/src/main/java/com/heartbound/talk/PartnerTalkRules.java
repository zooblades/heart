package com.heartbound.talk;

import com.heartbound.gesture.Personality;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Rules for conversations that the partner starts: what it says depends on the time of day and the
 * situation, the player picks one of three answers (warm, playful, cold) and the partner reacts to the
 * answer according to its character. Pure Java, no Minecraft dependencies; the lines themselves are in the
 * language files.
 */
public final class PartnerTalkRules {

    public static final int OPEN_VARIANTS = 2;
    public static final int REACT_VARIANTS = 2;

    private static final String PREFIX = "chat.heartbound.";

    private PartnerTalkRules() {
    }

    public enum Period {
        MORNING,
        DAY,
        EVENING,
        NIGHT;

        /** By the Minecraft day time (0 is sunrise). */
        public static Period of(long dayTime) {
            long t = Math.floorMod(dayTime, 24000L);
            if (t < 3000L || t >= 23000L) {
                return MORNING;
            }
            if (t < 12000L) {
                return DAY;
            }
            return t < 17000L ? EVENING : NIGHT;
        }
    }

    public enum Prompt {
        MORNING,
        PLANS,
        EVENING,
        NIGHT,
        /** The player is hurt. */
        WORRY,
        /** The partner itself is hurt. */
        COMFORT,
        /** Nothing special, just warm words. */
        GLAD;

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Prompt forPeriod(Period period) {
            return switch (period) {
                case MORNING -> MORNING;
                case DAY -> PLANS;
                case EVENING -> EVENING;
                case NIGHT -> NIGHT;
            };
        }
    }

    /** What the player answers; the position in the list of answers. */
    public enum Tone {
        WARM,
        PLAYFUL,
        COLD;

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Tone byId(int id) {
            Tone[] all = values();
            return id >= 0 && id < all.length ? all[id] : null;
        }
    }

    public enum Outcome {
        GOOD,
        NEUTRAL,
        BAD;

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * Picks what to talk about. A hurt partner or a hurt player always come first; otherwise mostly the
     * time of day, sometimes just warm words, and never the same subject twice in a row.
     *
     * @param roll a random number from 0 to 99
     */
    public static Prompt choose(Period period, boolean partnerUpset, boolean playerHurt, Prompt last, int roll) {
        Prompt byPeriod = Prompt.forPeriod(period);
        if (partnerUpset) {
            return Prompt.COMFORT;
        }
        if (playerHurt && last != Prompt.WORRY) {
            return Prompt.WORRY;
        }
        Prompt pick = roll < 70 ? byPeriod : Prompt.GLAD;
        if (pick == last) {
            pick = pick == Prompt.GLAD ? byPeriod : Prompt.GLAD;
        }
        return pick;
    }

    /** How the partner takes the answer. A joke can fall flat, depending on the character. */
    public static Outcome resolve(Tone tone, Personality personality, int roll) {
        return switch (tone) {
            case WARM -> Outcome.GOOD;
            case PLAYFUL -> roll < jokeChance(personality) ? Outcome.GOOD : Outcome.NEUTRAL;
            case COLD -> Outcome.BAD;
        };
    }

    public static int jokeChance(Personality personality) {
        return switch (personality) {
            case SHY -> 40;
            case NEUTRAL -> 60;
            case BOLD -> 70;
            case PLAYFUL -> 90;
        };
    }

    /** Affinity change for an answer. */
    public static int gain(Tone tone, Outcome outcome) {
        return switch (outcome) {
            case GOOD -> tone == Tone.WARM ? 4 : 3;
            case NEUTRAL -> 0;
            case BAD -> -3;
        };
    }

    public static String openKey(Prompt prompt, int variant) {
        return PREFIX + prompt.key() + ".open." + variant;
    }

    public static String answerKey(Prompt prompt, Tone tone) {
        return PREFIX + prompt.key() + ".answer." + tone.key();
    }

    public static String reactKey(Outcome outcome, Personality personality, int variant) {
        return PREFIX + "react." + outcome.key() + "." + personality.key() + "." + variant;
    }

    /** Every key that must exist in the language files. */
    public static List<String> allKeys() {
        List<String> keys = new ArrayList<>();
        for (Prompt prompt : Prompt.values()) {
            for (int v = 0; v < OPEN_VARIANTS; v++) {
                keys.add(openKey(prompt, v));
            }
            for (Tone tone : Tone.values()) {
                keys.add(answerKey(prompt, tone));
            }
        }
        for (Outcome outcome : Outcome.values()) {
            for (Personality personality : Personality.values()) {
                for (int v = 0; v < REACT_VARIANTS; v++) {
                    keys.add(reactKey(outcome, personality, v));
                }
            }
        }
        return keys;
    }
}
