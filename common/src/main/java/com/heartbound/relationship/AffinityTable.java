package com.heartbound.relationship;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Pure-Java storage of affinity values per (mob, player) pair. No Minecraft dependencies,
 * so it can be unit tested and ported freely.
 *
 * A value of 0 is the default and is never stored.
 */
public final class AffinityTable {

    public record Key(UUID mob, UUID player) {
    }

    private final Map<Key, Integer> values = new HashMap<>();

    public static int clamp(long value) {
        return (int) Math.max(0L, Math.min(RelationshipStage.MAX_AFFINITY, value));
    }

    public int get(UUID mob, UUID player) {
        return values.getOrDefault(new Key(mob, player), 0);
    }

    /** Sets the value (clamped to 0..MAX_AFFINITY) and returns the stored value. */
    public int set(UUID mob, UUID player, long value) {
        int clamped = clamp(value);
        Key key = new Key(mob, player);
        if (clamped == 0) {
            values.remove(key);
        } else {
            values.put(key, clamped);
        }
        return clamped;
    }

    /** Adds a (possibly negative) delta and returns the new stored value. */
    public int add(UUID mob, UUID player, int delta) {
        return set(mob, player, (long) get(mob, player) + delta);
    }

    /** Forgets everything about the given mob. */
    public void removeMob(UUID mob) {
        values.keySet().removeIf(key -> key.mob().equals(mob));
    }

    public Map<Key, Integer> entries() {
        return Collections.unmodifiableMap(values);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }
}
