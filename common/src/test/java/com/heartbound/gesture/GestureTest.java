package com.heartbound.gesture;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GestureTest {

    @Test
    void unlockRules() {
        assertFalse(Gesture.HOLD_HANDS.isUnlocked(299, false));
        assertTrue(Gesture.HOLD_HANDS.isUnlocked(300, false));
        assertFalse(Gesture.HUG.isUnlocked(599, false));
        assertTrue(Gesture.HUG.isUnlocked(600, false));
        assertFalse(Gesture.KISS.isUnlocked(1000, false));
        assertTrue(Gesture.KISS.isUnlocked(0, true));
    }

    @Test
    void lieDownIsImplementedAndPartnerOnly() {
        assertTrue(Gesture.LIE_DOWN.implemented());
        assertTrue(Gesture.LIE_DOWN.partnerOnly());
        assertTrue(Gesture.KISS.implemented());
    }

    @Test
    void lookupById() {
        assertEquals(Gesture.HUG, Gesture.byId(Gesture.HUG.ordinal()));
        assertNull(Gesture.byId(-1));
        assertNull(Gesture.byId(99));
    }

    @Test
    void intimacyGrowsWithDepth() {
        assertTrue(Gesture.HOLD_HANDS.intimacy() < Gesture.HUG.intimacy());
        assertTrue(Gesture.HUG.intimacy() < Gesture.KISS.intimacy());
    }
}
