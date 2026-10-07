package com.heartbound.talk;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Counts conversations per (mob, player) per game day. Not saved. Pure Java. */
public final class TalkLimiter {

    private record Key(UUID mob, UUID player) {
    }

    private record Count(long day, int count) {
    }

    private final Map<Key, Count> counts = new HashMap<>();

    public int countToday(UUID mob, UUID player, long now) {
        Count count = counts.get(new Key(mob, player));
        return count != null && count.day() == TalkRules.dayOf(now) ? count.count() : 0;
    }

    public void record(UUID mob, UUID player, long now) {
        counts.put(new Key(mob, player), new Count(TalkRules.dayOf(now), countToday(mob, player, now) + 1));
    }
}
