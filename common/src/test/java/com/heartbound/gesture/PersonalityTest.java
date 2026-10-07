package com.heartbound.gesture;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonalityTest {

    @Test
    void stableForTheSameUuid() {
        UUID id = UUID.randomUUID();
        assertEquals(Personality.of(id), Personality.of(id));
    }

    @Test
    void allCharactersOccur() {
        Set<Personality> seen = EnumSet.noneOf(Personality.class);
        for (int i = 0; i < 500; i++) {
            seen.add(Personality.of(UUID.randomUUID()));
        }
        assertEquals(Personality.values().length, seen.size());
    }

    @Test
    void shyIsHurtMoreByDeeperGestures() {
        assertTrue(Personality.SHY.modifier(3) < Personality.SHY.modifier(1));
        assertEquals(0, Personality.NEUTRAL.modifier(4));
    }
}
