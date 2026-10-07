package com.heartbound.relationship;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * How much the player has learned about a mob: 0 nothing, 1 its character, 2 its favourite gift.
 * Pure Java.
 */
public final class KnowledgeTable {

    public static final int MAX_LEVEL = 2;

    private final Map<AffinityTable.Key, Integer> values = new HashMap<>();

    public int get(UUID mob, UUID player) {
        return values.getOrDefault(new AffinityTable.Key(mob, player), 0);
    }

    public int set(UUID mob, UUID player, int level) {
        int clamped = Math.max(0, Math.min(MAX_LEVEL, level));
        AffinityTable.Key key = new AffinityTable.Key(mob, player);
        if (clamped == 0) {
            values.remove(key);
        } else {
            values.put(key, clamped);
        }
        return clamped;
    }

    public void removeMob(UUID mob) {
        values.keySet().removeIf(key -> key.mob().equals(mob));
    }

    public Map<AffinityTable.Key, Integer> entries() {
        return Collections.unmodifiableMap(values);
    }
}
