package com.heartbound.gesture;

import org.junit.jupiter.api.Test;

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
}
