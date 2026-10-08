package com.heartbound.relationship;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** All pair memories. Pure Java. */
public final class MemoryBook {

    private final Map<AffinityTable.Key, PairMemory> memories = new HashMap<>();

    /** The memory of this pair, or null if nothing happened yet. */
    public PairMemory get(UUID mob, UUID player) {
        return memories.get(new AffinityTable.Key(mob, player));
    }

    public PairMemory getOrCreate(UUID mob, UUID player) {
        return memories.computeIfAbsent(new AffinityTable.Key(mob, player), key -> new PairMemory());
    }

    public void removeMob(UUID mob) {
        memories.keySet().removeIf(key -> key.mob().equals(mob));
    }

    public Map<AffinityTable.Key, PairMemory> entries() {
        return Collections.unmodifiableMap(memories);
    }
}
