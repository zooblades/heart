package com.heartbound.relationship;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GiftPreferencesTest {

    @Test
    void mobsHaveDifferentTastes() {
        assertTrue(GiftPreferences.gain("fox", Gender.MALE, GiftKind.BOUQUET)
                > GiftPreferences.gain("wolf", Gender.MALE, GiftKind.BOUQUET));
        assertTrue(GiftPreferences.gain("wolf", Gender.MALE, GiftKind.HEART_CHARM)
                > GiftPreferences.gain("fox", Gender.MALE, GiftKind.HEART_CHARM));
    }

    @Test
    void genderCanChangeTastes() {
        assertNotEquals(
                GiftPreferences.gain("piglin", Gender.MALE, GiftKind.BOUQUET),
                GiftPreferences.gain("piglin", Gender.FEMALE, GiftKind.BOUQUET));
    }

    @Test
    void genderDoesNotMatterWhereNotConfigured() {
        assertEquals(
                GiftPreferences.gain("fox", Gender.MALE, GiftKind.BOUQUET),
                GiftPreferences.gain("fox", Gender.FEMALE, GiftKind.BOUQUET));
    }

    @Test
    void unknownMobFallsBackToDefault() {
        assertEquals(8, GiftPreferences.gain("axolotl", Gender.FEMALE, GiftKind.BOUQUET));
        assertEquals(25, GiftPreferences.gain("axolotl", Gender.MALE, GiftKind.HEART_CHARM));
    }

    @Test
    void charmIsAlwaysWorthMoreThanBouquet() {
        for (String mob : new String[]{"villager", "wolf", "cat", "fox", "piglin", "other"}) {
            for (Gender gender : Gender.values()) {
                assertTrue(GiftPreferences.gain(mob, gender, GiftKind.HEART_CHARM)
                        > GiftPreferences.gain(mob, gender, GiftKind.BOUQUET));
            }
        }
    }
}
