package com.heartbound.relationship;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-pair cooldowns measured in game ticks. Pure Java. */
public final class CooldownTracker {

    private record Key(UUID a, UUID b) {
    }

    private final Map<Key, Long> lastUse = new HashMap<>();

    /** Returns true and records the use if the cooldown has passed, false otherwise. */
    public boolean tryUse(UUID a, UUID b, long now, long cooldownTicks) {
        Key key = new Key(a, b);
        Long last = lastUse.get(key);
        // now < last means the world time went backwards (e.g. another world); allow it
        if (last != null && now >= last && now - last < cooldownTicks) {
            return false;
        }
        lastUse.put(key, now);
        return true;
    }

    public void clear() {
        lastUse.clear();
    }
}
