package com.heartbound.gesture;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GestureRulesTest {

    @Test
    void moreAffinityMeansHigherChance() {
        int low = GestureRules.chance(Gesture.HUG, 600, false, Personality.NEUTRAL);
        int high = GestureRules.chance(Gesture.HUG, 900, false, Personality.NEUTRAL);
        assertTrue(high > low);
    }

    @Test
    void partnersAreMoreWilling() {
        assertTrue(GestureRules.chance(Gesture.KISS, 800, true, Personality.NEUTRAL)
                > GestureRules.chance(Gesture.KISS, 800, false, Personality.NEUTRAL));
    }

    @Test
    void characterMatters() {
        int shy = GestureRules.chance(Gesture.KISS, 800, true, Personality.SHY);
        int playful = GestureRules.chance(Gesture.KISS, 800, true, Personality.PLAYFUL);
        assertTrue(playful > shy);
    }

    @Test
    void chanceStaysInRange() {
        for (Gesture gesture : Gesture.values()) {
            for (Personality personality : Personality.values()) {
                for (int affinity : new int[]{0, 500, 1000}) {
                    int chance = GestureRules.chance(gesture, affinity, true, personality);
                    assertTrue(chance >= GestureRules.MIN_CHANCE && chance <= GestureRules.MAX_CHANCE);
                }
            }
        }
        // base 60, no bonus, shy: -7 * intimacy 4 = -28
        assertEquals(32, GestureRules.chance(Gesture.LIE_DOWN, 0, false, Personality.SHY));
    }
}
