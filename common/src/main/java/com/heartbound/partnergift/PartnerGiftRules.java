package com.heartbound.partnergift;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What the partner gives the player and when. Items are registry ids (strings) so this stays pure Java.
 * Rarer presents appear only when the relationship is strong. The lines are in the language files.
 */
public final class PartnerGiftRules {

    public static final int RARE_AFFINITY = 700;
    public static final int LINE_VARIANTS = 2;
    public static final String GIVE_MESSAGE_KEY = "gift.heartbound.partner";
    public static final String EVENT_KEY = "event.heartbound.first_partner_gift";

    private static final String PREFIX = "gift.heartbound.give.";

    private PartnerGiftRules() {
    }

    public enum Occasion {
        /** A small present out of nowhere. */
        RANDOM,
        /** The player is hungry: food. */
        HUNGRY,
        /** After a successful date: a flower. */
        DATE,
        /** The player is hurt: care instead of an item. */
        HURT;

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public record Gift(String itemId, int count) {
    }

    private static final List<String> FLOWERS = List.of("minecraft:poppy", "minecraft:dandelion",
            "minecraft:azure_bluet", "minecraft:cornflower", "minecraft:lily_of_the_valley", "minecraft:blue_orchid",
            "minecraft:allium", "minecraft:red_tulip");
    private static final List<String> FOOD = List.of("minecraft:bread", "minecraft:baked_potato",
            "minecraft:cooked_salmon", "minecraft:cooked_cod", "minecraft:cooked_porkchop", "minecraft:apple",
            "minecraft:cooked_chicken");

    /** Presents by mob (registry path), common ones first. */
    private static List<String> common(String mob) {
        return switch (mob) {
            case "villager" -> List.of("minecraft:bread", "minecraft:carrot", "minecraft:potato", "minecraft:wheat",
                    "minecraft:poppy", "minecraft:dandelion");
            case "wolf" -> List.of("minecraft:bone", "minecraft:stick", "minecraft:cooked_beef", "minecraft:bone");
            case "cat" -> List.of("minecraft:cod", "minecraft:salmon", "minecraft:string", "minecraft:feather");
            case "fox" -> List.of("minecraft:sweet_berries", "minecraft:glow_berries", "minecraft:apple",
                    "minecraft:sweet_berries");
            case "piglin" -> List.of("minecraft:gold_nugget", "minecraft:cooked_porkchop", "minecraft:gold_nugget");
            default -> List.of("minecraft:poppy", "minecraft:dandelion", "minecraft:apple");
        };
    }

    private static String rare(String mob) {
        return switch (mob) {
            case "villager" -> "minecraft:emerald";
            case "piglin" -> "minecraft:gold_ingot";
            default -> "minecraft:amethyst_shard";
        };
    }

    /**
     * Picks the present; null when the occasion has no item (HURT). Rolls are random numbers from 0 to 99.
     */
    public static Gift choose(String mob, Occasion occasion, int affinity, int rollKind, int rollCount) {
        if (occasion == Occasion.HURT) {
            return null;
        }
        boolean rare = affinity >= RARE_AFFINITY && rollKind < (occasion == Occasion.DATE ? 35 : 20);
        List<String> pool = switch (occasion) {
            case HUNGRY -> FOOD;
            case DATE -> FLOWERS;
            default -> common(mob);
        };
        if (rare && occasion != Occasion.HUNGRY) {
            return new Gift(rare(mob), 1);
        }
        String item = pool.get(Math.floorMod(rollKind * 7 + rollCount, pool.size()));
        int count = item.endsWith("nugget") || item.equals("minecraft:sweet_berries") ? 1 + rollCount % 3 : 1 + rollCount % 2;
        return new Gift(item, count);
    }

    public static String lineKey(Occasion occasion, int variant) {
        return PREFIX + occasion.key() + "." + variant;
    }

    /** Every key that must exist in the language files. */
    public static List<String> allKeys() {
        List<String> keys = new ArrayList<>();
        for (Occasion occasion : Occasion.values()) {
            for (int v = 0; v < LINE_VARIANTS; v++) {
                keys.add(lineKey(occasion, v));
            }
        }
        keys.add(GIVE_MESSAGE_KEY);
        keys.add(EVENT_KEY);
        return keys;
    }
}
