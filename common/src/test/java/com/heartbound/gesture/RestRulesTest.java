package com.heartbound.gesture;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestRulesTest {

    @Test
    void nightWindowMatchesBedTime() {
        assertFalse(RestRules.isNightTime(0));
        assertFalse(RestRules.isNightTime(6000));
        assertFalse(RestRules.isNightTime(12541));
        assertTrue(RestRules.isNightTime(12542));
        assertTrue(RestRules.isNightTime(18000));
        assertTrue(RestRules.isNightTime(23459));
        assertFalse(RestRules.isNightTime(23460));
    }

    @Test
    void nightWindowRepeatsEveryDay() {
        assertTrue(RestRules.isNightTime(24000L * 5 + 15000));
        assertFalse(RestRules.isNightTime(24000L * 5 + 3000));
    }

    @Test
    void eveningNightAndMorningShareOneIndex() {
        long evening = 24000L * 3 + 13000;
        long lateNight = 24000L * 3 + 23000;
        long morning = 24000L * 4 + 100;
        long nextEvening = 24000L * 4 + 13000;
        assertEquals(RestRules.nightIndex(evening), RestRules.nightIndex(lateNight));
        assertEquals(RestRules.nightIndex(evening), RestRules.nightIndex(morning));
        assertTrue(RestRules.nightIndex(nextEvening) > RestRules.nightIndex(evening));
    }
}
