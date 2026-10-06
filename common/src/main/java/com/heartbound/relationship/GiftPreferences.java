package com.heartbound.relationship;

import java.util.Map;

/**
 * How much affinity each mob gains from each gift. Mobs are identified by their registry path
 * ("villager", "wolf", ...), so this class stays free of Minecraft types and is easy to test.
 * This will become data driven (JSON) later.
 */
public final class GiftPreferences {

    private static final Map<GiftKind, Integer> DEFAULT_GAINS = Map.of(
            GiftKind.BOUQUET, 20,
            GiftKind.HEART_CHARM, 60
    );

    private static final Map<String, Map<GiftKind, Integer>> PER_MOB = Map.of(
            "villager", Map.of(GiftKind.BOUQUET, 30, GiftKind.HEART_CHARM, 60),
            "wolf", Map.of(GiftKind.BOUQUET, 10, GiftKind.HEART_CHARM, 80),
            "cat", Map.of(GiftKind.BOUQUET, 20, GiftKind.HEART_CHARM, 70),
            "fox", Map.of(GiftKind.BOUQUET, 40, GiftKind.HEART_CHARM, 50)
    );

    private GiftPreferences() {
    }

    public static int gain(String mobId, GiftKind kind) {
        Map<GiftKind, Integer> gains = PER_MOB.get(mobId);
        if (gains != null && gains.containsKey(kind)) {
            return gains.get(kind);
        }
        return DEFAULT_GAINS.get(kind);
    }
}
