package com.heartbound.gesture;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Remembers refusals per (mob, player). Two refusals in a short time make the mob offended for a while,
 * during which it refuses everything. Not saved: moods pass. Pure Java.
 */
public final class MoodTracker {

    public static final long WINDOW_TICKS = 1200;
    public static final int REFUSALS_TO_OFFEND = 2;
    public static final long OFFENDED_TICKS = 2400;

    private record Key(UUID mob, UUID player) {
    }

    private final Map<Key, Deque<Long>> refusals = new HashMap<>();
    private final Map<Key, Long> offendedUntil = new HashMap<>();

    public boolean isOffended(UUID mob, UUID player, long now) {
        Long until = offendedUntil.get(new Key(mob, player));
        return until != null && now < until;
    }

    public void recordRefusal(UUID mob, UUID player, long now) {
        Key key = new Key(mob, player);
        Deque<Long> recent = refusals.computeIfAbsent(key, k -> new ArrayDeque<>());
        recent.addLast(now);
        while (!recent.isEmpty() && now - recent.peekFirst() > WINDOW_TICKS) {
            recent.removeFirst();
        }
        if (recent.size() >= REFUSALS_TO_OFFEND) {
            offendedUntil.put(key, now + OFFENDED_TICKS);
            recent.clear();
        }
    }

    public void recordSuccess(UUID mob, UUID player) {
        Key key = new Key(mob, player);
        refusals.remove(key);
    }

    public void clear() {
        refusals.clear();
        offendedUntil.clear();
    }
}
