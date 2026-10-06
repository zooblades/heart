package com.heartbound.relationship;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenderTest {

    @Test
    void isStableForTheSameUuid() {
        UUID id = UUID.randomUUID();
        assertEquals(Gender.of(id), Gender.of(id));
    }

    @Test
    void bothGendersOccurRoughlyEvenly() {
        int males = 0;
        for (int i = 0; i < 2000; i++) {
            if (Gender.of(UUID.randomUUID()) == Gender.MALE) {
                males++;
            }
        }
        assertTrue(males > 800 && males < 1200, "males: " + males);
    }
}
