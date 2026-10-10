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

    private static PartnerTalkRules.Context ctx(boolean upset, boolean hurt, Prompt event, Prompt situation) {
        return new PartnerTalkRules.Context(Period.DAY, upset, hurt, event, situation);
    }

    @Test
    void recentEventComesBeforeSituationAndTimeOfDay() {
        assertEquals(Prompt.GIFT, PartnerTalkRules.choose(ctx(false, false, Prompt.GIFT, Prompt.RAIN), null, 10));
        assertEquals(Prompt.COMFORT, PartnerTalkRules.choose(ctx(true, false, Prompt.GIFT, null), null, 10));
        assertEquals(Prompt.WORRY, PartnerTalkRules.choose(ctx(false, true, Prompt.GIFT, null), null, 10));
        assertNotEquals(Prompt.GIFT, PartnerTalkRules.choose(ctx(false, false, Prompt.GIFT, null), Prompt.GIFT, 10));
    }

    @Test
    void situationIsUsedAboutHalfTheTime() {
        assertEquals(Prompt.CAVE, PartnerTalkRules.choose(ctx(false, false, null, Prompt.CAVE), null, 10));
        assertNotEquals(Prompt.CAVE, PartnerTalkRules.choose(ctx(false, false, null, Prompt.CAVE), null, 80));
        assertNotEquals(Prompt.CAVE, PartnerTalkRules.choose(ctx(false, false, null, Prompt.CAVE), Prompt.CAVE, 10));
    }

    @Test
    void continuationsDependOnTheOutcome() {
        assertEquals(Prompt.FOLLOW_GOOD, PartnerTalkRules.followUp(Outcome.GOOD, 10));
        assertNull(PartnerTalkRules.followUp(Outcome.GOOD, 90));
        assertEquals(Prompt.FOLLOW_BAD, PartnerTalkRules.followUp(Outcome.BAD, 10));
        assertNull(PartnerTalkRules.followUp(Outcome.NEUTRAL, 0));
    }

    @Test
    void continuationsAreNeverChosenOnTheirOwn() {
        for (Period period : Period.values()) {
            for (int roll = 0; roll < 100; roll += 5) {
                Prompt pick = PartnerTalkRules.choose(period, false, false, null, roll);
                assertNotEquals(Prompt.FOLLOW_GOOD, pick);
                assertNotEquals(Prompt.FOLLOW_BAD, pick);
            }
        }
    }

    @Test
    void dateInvitationComesAfterEventsAndBeforeSituations() {
        PartnerTalkRules.Context invite = new PartnerTalkRules.Context(Period.DAY, false, false, null, Prompt.RAIN, Prompt.DATE_WALK);
        assertEquals(Prompt.DATE_WALK, PartnerTalkRules.choose(invite, null, 10));
        assertNotEquals(Prompt.DATE_WALK, PartnerTalkRules.choose(invite, Prompt.DATE_WALK, 90));
        PartnerTalkRules.Context withEvent = new PartnerTalkRules.Context(Period.DAY, false, false, Prompt.GIFT, null, Prompt.DATE_WALK);
        assertEquals(Prompt.GIFT, PartnerTalkRules.choose(withEvent, null, 10));
        PartnerTalkRules.Context hurt = new PartnerTalkRules.Context(Period.DAY, false, true, null, null, Prompt.DATE_WALK);
        assertEquals(Prompt.WORRY, PartnerTalkRules.choose(hurt, null, 10));
    }

    @Test
    void invitationsMapToDateTypes() {
        for (com.heartbound.date.DateRules.DateType type : com.heartbound.date.DateRules.DateType.values()) {
            assertEquals(type, Prompt.forDate(type).dateType());
        }
        assertNull(Prompt.GLAD.dateType());
        assertNull(Prompt.DATE_DONE.dateType());
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
