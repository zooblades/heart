package com.heartbound.relationship;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Which mob follows which player. A mob follows at most one player. Pure Java. */
public final class FollowTable {

    private final Map<UUID, UUID> followers = new HashMap<>();

    /** The player the mob follows, or null. */
    public UUID get(UUID mob) {
        return followers.get(mob);
    }

    public void set(UUID mob, UUID player) {
        followers.put(mob, player);
    }

    /** Returns true if the mob was following someone. */
    public boolean clear(UUID mob) {
        return followers.remove(mob) != null;
    }

    public int count(UUID player) {
        int result = 0;
        for (UUID followed : followers.values()) {
            if (followed.equals(player)) {
                result++;
            }
        }
        return result;
    }

    /** A copy, safe to iterate while the table changes. */
    public Map<UUID, UUID> snapshot() {
        return new HashMap<>(followers);
    }

    public boolean isEmpty() {
        return followers.isEmpty();
    }
}
