package com.heartbound.gesture;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoodTrackerTest {

    private final UUID mob = UUID.randomUUID();
    private final UUID player = UUID.randomUUID();

    @Test
    void oneRefusalDoesNotOffend() {
        MoodTracker tracker = new MoodTracker();
        tracker.recordRefusal(mob, player, 100);
        assertFalse(tracker.isOffended(mob, player, 101));
    }

    @Test
    void twoQuickRefusalsOffendForAWhile() {
        MoodTracker tracker = new MoodTracker();
        tracker.recordRefusal(mob, player, 100);
        tracker.recordRefusal(mob, player, 200);
        assertTrue(tracker.isOffended(mob, player, 300));
        assertTrue(tracker.isOffended(mob, player, 200 + MoodTracker.OFFENDED_TICKS - 1));
        assertFalse(tracker.isOffended(mob, player, 200 + MoodTracker.OFFENDED_TICKS));
    }

    @Test
    void slowRefusalsDoNotAddUp() {
        MoodTracker tracker = new MoodTracker();
        tracker.recordRefusal(mob, player, 0);
        tracker.recordRefusal(mob, player, MoodTracker.WINDOW_TICKS + 10);
        assertFalse(tracker.isOffended(mob, player, MoodTracker.WINDOW_TICKS + 11));
    }

    @Test
    void successForgivesEarlierRefusals() {
        MoodTracker tracker = new MoodTracker();
        tracker.recordRefusal(mob, player, 100);
        tracker.recordSuccess(mob, player);
        tracker.recordRefusal(mob, player, 200);
        assertFalse(tracker.isOffended(mob, player, 201));
    }
}
