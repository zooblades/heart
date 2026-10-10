package com.heartbound.date;

import com.heartbound.date.DateRules.DateType;
import com.heartbound.date.DateRules.FailReason;
import com.heartbound.talk.PartnerTalkRules.Period;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateRulesTest {

    @Test
    void outdoorDatesOnlyByDayWithoutRainInTheOverworld() {
        assertEquals(DateType.WALK, DateRules.chooseType(Period.DAY, false, false, true, 10));
        assertEquals(DateType.PLACE, DateRules.chooseType(Period.MORNING, false, false, true, 80));
        assertNull(DateRules.chooseType(Period.EVENING, false, false, true, 10));
        assertNull(DateRules.chooseType(Period.NIGHT, false, false, true, 10));
        assertNull(DateRules.chooseType(Period.DAY, true, false, true, 10));
        assertNull(DateRules.chooseType(Period.DAY, false, false, false, 10));
    }

    @Test
    void homeDatesWhenThereIsAHomeAndOutdoorsDoesNotFit() {
        assertEquals(DateType.HOME, DateRules.chooseType(Period.EVENING, false, true, true, 90));
        assertEquals(DateType.HOME, DateRules.chooseType(Period.NIGHT, false, true, true, 90));
        assertEquals(DateType.HOME, DateRules.chooseType(Period.DAY, true, true, true, 90));
        assertEquals(DateType.HOME, DateRules.chooseType(Period.DAY, false, true, true, 10));
        assertEquals(DateType.WALK, DateRules.chooseType(Period.DAY, false, true, true, 40));
        assertEquals(DateType.HOME, DateRules.chooseType(Period.DAY, false, true, false, 90));
    }

    @Test
    void aWalkNeedsMostOfTheTimeTogether() {
        assertTrue(DateRules.walkSucceeded(1800, 1800));
        assertTrue(DateRules.walkSucceeded(1260, 1800));
        assertFalse(DateRules.walkSucceeded(1240, 1800));
        assertFalse(DateRules.walkSucceeded(0, 0));
    }

    @Test
    void onlyAbandoningCostsAffinity() {
        assertTrue(DateRules.failGain(FailReason.ABANDONED) < 0);
        assertEquals(0, DateRules.failGain(FailReason.LOST));
        assertEquals(0, DateRules.failGain(FailReason.HURT));
        for (DateType type : DateType.values()) {
            assertTrue(DateRules.successGain(type) > 0);
        }
    }

    @Test
    void keysAreUnique() {
        List<String> keys = DateRules.allKeys();
        assertEquals(keys.size(), keys.stream().distinct().count());
    }

    @Test
    void everyLineExistsInBothLanguages() throws IOException {
        for (String lang : new String[]{"en_us", "ru_ru"}) {
            String text;
            try (InputStream in = DateRulesTest.class.getResourceAsStream("/assets/heartbound/lang/" + lang + ".json")) {
                assertTrue(in != null, "missing language file " + lang);
                text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            List<String> missing = new ArrayList<>();
            for (String key : DateRules.allKeys()) {
                if (!text.contains("\"" + key + "\"")) {
                    missing.add(key);
                }
            }
            assertTrue(missing.isEmpty(), lang + " is missing: " + missing);
        }
    }
}
