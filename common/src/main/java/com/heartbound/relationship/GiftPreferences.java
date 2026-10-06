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
            "villager", Map.of(GiftKind.BOUQUET, 30, GiftKind.HEART_CHARM, 60),
            "wolf", Map.of(GiftKind.BOUQUET, 10, GiftKind.HEART_CHARM, 80),
            "cat", Map.of(GiftKind.BOUQUET, 20, GiftKind.HEART_CHARM, 70),
            "fox", Map.of(GiftKind.BOUQUET, 40, GiftKind.HEART_CHARM, 50),
            "piglin/female", Map.of(GiftKind.BOUQUET, 40, GiftKind.HEART_CHARM, 60),
            "piglin/male", Map.of(GiftKind.BOUQUET, 10, GiftKind.HEART_CHARM, 90)
    );

    private GiftPreferences() {
    }

    public static int gain(String mobId, Gender gender, GiftKind kind) {
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

    private static Integer lookup(String key, GiftKind kind) {
        Map<GiftKind, Integer> gains = TABLE.get(key);
        return gains == null ? null : gains.get(kind);
    }
}
