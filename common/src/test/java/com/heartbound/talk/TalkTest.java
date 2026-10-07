package com.heartbound.talk;

import com.heartbound.gesture.Personality;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TalkTest {

    @Test
    void easyTopicsNeverFail() {
        for (Personality personality : Personality.values()) {
            assertEquals(100, TalkRules.chance(Topic.HOW_ARE_YOU, 0, false, personality));
            assertEquals(100, TalkRules.chance(Topic.ABOUT_YOU, 0, false, personality));
        }
        assertFalse(Topic.HOW_ARE_YOU.canFail());
        assertTrue(Topic.FLIRT.canFail());
    }

    @Test
    void jokesLandBetterWithPlayfulMobs() {
        assertTrue(TalkRules.chance(Topic.JOKE, 100, false, Personality.PLAYFUL)
                > TalkRules.chance(Topic.JOKE, 100, false, Personality.SHY));
    }

    @Test
    void flirtingIsHarderForShyAndEasierForPartners() {
        assertTrue(TalkRules.chance(Topic.FLIRT, 600, false, Personality.PLAYFUL)
                > TalkRules.chance(Topic.FLIRT, 600, false, Personality.SHY));
        assertTrue(TalkRules.chance(Topic.FLIRT, 600, true, Personality.NEUTRAL)
                > TalkRules.chance(Topic.FLIRT, 600, false, Personality.NEUTRAL));
    }

    @Test
    void chanceStaysInRange() {
        for (Topic topic : Topic.values()) {
            for (Personality personality : Personality.values()) {
                for (int affinity : new int[]{0, 300, 1000}) {
                    int chance = TalkRules.chance(topic, affinity, true, personality);
                    assertTrue(chance >= 5 && chance <= 100);
                }
            }
        }
    }

    @Test
    void flirtNeedsFriends() {
        assertEquals(300, Topic.FLIRT.minAffinity());
        assertEquals(0, Topic.COMPLIMENT.minAffinity());
    }

    @Test
    void talkingGetsLessRewardingThroughTheDay() {
        assertEquals(100, TalkRules.gainFactorPercent(0));
        assertEquals(100, TalkRules.gainFactorPercent(2));
        assertEquals(50, TalkRules.gainFactorPercent(3));
        assertEquals(0, TalkRules.gainFactorPercent(6));
    }

    @Test
    void limiterCountsPerDay() {
        TalkLimiter limiter = new TalkLimiter();
        UUID mob = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        assertEquals(0, limiter.countToday(mob, player, 100));
        limiter.record(mob, player, 100);
        limiter.record(mob, player, 500);
        assertEquals(2, limiter.countToday(mob, player, 1000));
        assertEquals(0, limiter.countToday(mob, player, TalkRules.TICKS_PER_DAY + 5));
        assertEquals(0, limiter.countToday(mob, UUID.randomUUID(), 1000));
    }

    @Test
    void lookupById() {
        assertEquals(Topic.JOKE, Topic.byId(Topic.JOKE.ordinal()));
        assertEquals(null, Topic.byId(42));
    }
}
