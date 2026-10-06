package com.heartbound.relationship;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GiftPreferencesTest {

    @Test
    void mobsHaveDifferentTastes() {
        assertTrue(GiftPreferences.gain("fox", GiftKind.BOUQUET) > GiftPreferences.gain("wolf", GiftKind.BOUQUET));
        assertTrue(GiftPreferences.gain("wolf", GiftKind.HEART_CHARM) > GiftPreferences.gain("fox", GiftKind.HEART_CHARM));
    }

    @Test
    void unknownMobFallsBackToDefault() {
        assertEquals(20, GiftPreferences.gain("axolotl", GiftKind.BOUQUET));
        assertEquals(60, GiftPreferences.gain("axolotl", GiftKind.HEART_CHARM));
    }

    @Test
    void charmIsAlwaysWorthMoreThanBouquet() {
        for (String mob : new String[]{"villager", "wolf", "cat", "fox", "other"}) {
            assertTrue(GiftPreferences.gain(mob, GiftKind.HEART_CHARM) > GiftPreferences.gain(mob, GiftKind.BOUQUET));
        }
    }
}
