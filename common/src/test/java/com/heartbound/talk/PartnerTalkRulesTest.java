package com.heartbound.talk;

import com.heartbound.gesture.Personality;
import com.heartbound.talk.PartnerTalkRules.Outcome;
import com.heartbound.talk.PartnerTalkRules.Period;
import com.heartbound.talk.PartnerTalkRules.Prompt;
import com.heartbound.talk.PartnerTalkRules.Tone;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartnerTalkRulesTest {

    @Test
    void periodsFollowTheDay() {
        assertEquals(Period.MORNING, Period.of(0));
        assertEquals(Period.MORNING, Period.of(23500));
        assertEquals(Period.DAY, Period.of(6000));
        assertEquals(Period.EVENING, Period.of(13000));
        assertEquals(Period.NIGHT, Period.of(18000));
        assertEquals(Period.DAY, Period.of(24000L * 7 + 6000));
    }

    @Test
    void hurtPartnerAlwaysComesFirst() {
        assertEquals(Prompt.COMFORT, PartnerTalkRules.choose(Period.DAY, true, true, Prompt.COMFORT, 10));
    }

    @Test
    void hurtPlayerIsWorriedAboutButNotTwiceInARow() {
        assertEquals(Prompt.WORRY, PartnerTalkRules.choose(Period.DAY, false, true, null, 10));
        assertNotEquals(Prompt.WORRY, PartnerTalkRules.choose(Period.DAY, false, true, Prompt.WORRY, 10));
    }

    @Test
    void usuallyTheTimeOfDayAndSometimesWarmWords() {
        assertEquals(Prompt.EVENING, PartnerTalkRules.choose(Period.EVENING, false, false, null, 10));
        assertEquals(Prompt.GLAD, PartnerTalkRules.choose(Period.EVENING, false, false, null, 90));
    }

    @Test
    void neverTheSameSubjectTwice() {
        for (Period period : Period.values()) {
            for (Prompt last : Prompt.values()) {
                for (int roll = 0; roll < 100; roll += 7) {
                    assertNotEquals(last, PartnerTalkRules.choose(period, false, false, last, roll));
                }
            }
        }
    }

    @Test
    void answersDecideTheOutcome() {
        for (Personality personality : Personality.values()) {
            assertEquals(Outcome.GOOD, PartnerTalkRules.resolve(Tone.WARM, personality, 99));
            assertEquals(Outcome.BAD, PartnerTalkRules.resolve(Tone.COLD, personality, 0));
        }
        assertEquals(Outcome.GOOD, PartnerTalkRules.resolve(Tone.PLAYFUL, Personality.PLAYFUL, 80));
        assertEquals(Outcome.NEUTRAL, PartnerTalkRules.resolve(Tone.PLAYFUL, Personality.SHY, 80));
    }

    @Test
    void gainsHaveTheRightSign() {
        assertEquals(4, PartnerTalkRules.gain(Tone.WARM, Outcome.GOOD));
        assertEquals(3, PartnerTalkRules.gain(Tone.PLAYFUL, Outcome.GOOD));
        assertEquals(0, PartnerTalkRules.gain(Tone.PLAYFUL, Outcome.NEUTRAL));
        assertTrue(PartnerTalkRules.gain(Tone.COLD, Outcome.BAD) < 0);
    }

    @Test
    void toneLookup() {
        assertEquals(Tone.COLD, Tone.byId(2));
        assertNull(Tone.byId(3));
        assertNull(Tone.byId(-1));
    }

    private static String read(String lang) throws IOException {
        try (InputStream in = PartnerTalkRulesTest.class.getResourceAsStream("/assets/heartbound/lang/" + lang + ".json")) {
            assertTrue(in != null, "missing language file " + lang);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void everyLineExistsInBothLanguages() throws IOException {
        for (String lang : new String[]{"en_us", "ru_ru"}) {
            String text = read(lang);
            List<String> missing = new ArrayList<>();
            for (String key : PartnerTalkRules.allKeys()) {
                if (!text.contains("\"" + key + "\"")) {
                    missing.add(key);
                }
            }
            assertTrue(missing.isEmpty(), lang + " is missing: " + missing);
        }
    }

    @Test
    void keysAreUnique() {
        List<String> keys = PartnerTalkRules.allKeys();
        assertEquals(keys.size(), keys.stream().distinct().count());
    }
}
