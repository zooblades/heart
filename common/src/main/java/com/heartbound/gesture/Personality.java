package com.heartbound.gesture;

import java.util.Locale;
import java.util.UUID;

/**
 * Character of a mob, derived from its UUID (like gender), so it is stable and needs no storage.
 * It changes how willing the mob is to accept closer gestures.
 */
public enum Personality {
    SHY,
    NEUTRAL,
    BOLD,
    PLAYFUL;

    public static Personality of(UUID uuid) {
        Personality[] all = values();
        return all[(int) ((uuid.getLeastSignificantBits() >>> 1) & 3L)];
    }

    /** Change of the acceptance chance (percentage points) for a gesture of the given intimacy. */
    public int modifier(int intimacy) {
        return switch (this) {
            case SHY -> -7 * intimacy;
            case NEUTRAL -> 0;
            case BOLD -> 3 * intimacy;
            case PLAYFUL -> 5 * intimacy;
        };
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
