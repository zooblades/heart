package com.heartbound.relationship;

import java.util.Map;

/**
 * How much affinity each mob gains from each gift. Mobs are identified by their registry path
 * ("villager", "wolf", ...). A gender-specific entry ("piglin/female") wins over the plain mob entry,
 * which wins over the defaults. Pure Java, easy to test; will become data driven (JSON) later.
 */
public final class GiftPreferences {

    private static final Map<GiftKind, Integer> DEFAULT_GAINS = Map.of(
            GiftKind.BOUQUET, 20,
            GiftKind.HEART_CHARM, 60
    );

    private static final Map<String, Map<GiftKind, Integer>> TABLE = Map.of(
            "villager", Map.of(GiftKind.BOUQUET, 25, GiftKind.HEART_CHARM, 60),
            "wolf", Map.of(GiftKind.BOUQUET, 12, GiftKind.HEART_CHARM, 80),
            "cat", Map.of(GiftKind.BOUQUET, 20, GiftKind.HEART_CHARM, 70),
            "fox", Map.of(GiftKind.BOUQUET, 30, GiftKind.HEART_CHARM, 55),
            "piglin/female", Map.of(GiftKind.BOUQUET, 30, GiftKind.HEART_CHARM, 55),
            "piglin/male", Map.of(GiftKind.BOUQUET, 10, GiftKind.HEART_CHARM, 85)
    );

    private GiftPreferences() {
    }

    public static int gain(String mobId, Gender gender, GiftKind kind) {
        return gain(mobId, gender, kind, Map.of());
    }

    /**
     * Same as above, but entries in {@code overrides} (from the config) win. Override keys are
     * "mob/gender" or "mob", override values map the gift name ("bouquet") to the gain.
     */
    public static int gain(String mobId, Gender gender, GiftKind kind, Map<String, Map<String, Integer>> overrides) {
        String giftName = kind.name().toLowerCase(java.util.Locale.ROOT);
        Integer fromConfig = override(overrides, mobId + "/" + gender.key(), giftName);
        if (fromConfig == null) {
            fromConfig = override(overrides, mobId, giftName);
        }
        if (fromConfig != null) {
            return fromConfig;
        }
        Integer specific = lookup(mobId + "/" + gender.key(), kind);
        if (specific != null) {
            return specific;
        }
        Integer general = lookup(mobId, kind);
        if (general != null) {
            return general;
        }
        return DEFAULT_GAINS.get(kind);
    }

    /** The gift this mob likes best (the first one on a tie). */
    public static GiftKind favorite(String mobId, Gender gender, Map<String, Map<String, Integer>> overrides) {
        GiftKind best = GiftKind.values()[0];
        int bestGain = Integer.MIN_VALUE;
        for (GiftKind kind : GiftKind.values()) {
            int gain = gain(mobId, gender, kind, overrides);
            if (gain > bestGain) {
                bestGain = gain;
                best = kind;
            }
        }
        return best;
    }

    private static Integer override(Map<String, Map<String, Integer>> overrides, String key, String giftName) {
        if (overrides == null) {
            return null;
        }
        Map<String, Integer> gains = overrides.get(key);
        return gains == null ? null : gains.get(giftName);
    }

    private static Integer lookup(String key, GiftKind kind) {
        Map<GiftKind, Integer> gains = TABLE.get(key);
        return gains == null ? null : gains.get(kind);
    }
}
