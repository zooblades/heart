package com.heartbound.life;

import com.heartbound.gesture.Personality;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LifeTest {

    @Test
    void everyCharacterHasThingsToDo() {
        for (Personality personality : Personality.values()) {
            assertFalse(Activity.forPersonality(personality).isEmpty());
        }
    }

    @Test
    void charactersDoDifferentThings() {
        assertTrue(Activity.forPersonality(Personality.PLAYFUL).contains(Activity.PLAY));
        assertTrue(Activity.forPersonality(Personality.SHY).contains(Activity.WATCH_FROM_AFAR));
        assertTrue(Activity.forPersonality(Personality.BOLD).contains(Activity.PATROL));
    }

    @Test
    void livelierLevelsMeanMoreLife() {
        assertEquals(0, LifeLevel.maxActive(0));
        assertTrue(LifeLevel.maxActive(1) < LifeLevel.maxActive(2));
        assertTrue(LifeLevel.maxActive(2) < LifeLevel.maxActive(3));
        assertTrue(LifeLevel.startChancePercent(1) < LifeLevel.startChancePercent(3));
        assertTrue(LifeLevel.pauseTicks(1) > LifeLevel.pauseTicks(3));
    }

    @Test
    void outOfRangeLevelsAreClamped() {
        assertEquals(LifeLevel.maxActive(3), LifeLevel.maxActive(99));
        assertEquals(0, LifeLevel.maxActive(-4));
    }
}
