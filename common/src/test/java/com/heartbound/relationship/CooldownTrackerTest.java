package com.heartbound.relationship;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CooldownTrackerTest {

    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();

    @Test
    void blocksUntilCooldownPasses() {
        CooldownTracker tracker = new CooldownTracker();
        assertTrue(tracker.tryUse(a, b, 100, 50));
        assertFalse(tracker.tryUse(a, b, 120, 50));
        assertFalse(tracker.tryUse(a, b, 149, 50));
        assertTrue(tracker.tryUse(a, b, 150, 50));
    }

    @Test
    void pairsAreIndependent() {
        CooldownTracker tracker = new CooldownTracker();
        assertTrue(tracker.tryUse(a, b, 100, 50));
        assertTrue(tracker.tryUse(a, UUID.randomUUID(), 100, 50));
    }

    @Test
    void timeGoingBackwardsDoesNotBlock() {
        CooldownTracker tracker = new CooldownTracker();
        assertTrue(tracker.tryUse(a, b, 1000, 50));
        assertTrue(tracker.tryUse(a, b, 10, 50));
    }
}
