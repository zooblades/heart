package com.heartbound.relationship;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeTableTest {

    private final UUID mob = UUID.randomUUID();
    private final UUID player = UUID.randomUUID();

    @Test
    void startsAtZeroAndClamps() {
        KnowledgeTable table = new KnowledgeTable();
        assertEquals(0, table.get(mob, player));
        assertEquals(KnowledgeTable.MAX_LEVEL, table.set(mob, player, 9));
        assertEquals(0, table.set(mob, player, -3));
        assertTrue(table.entries().isEmpty());
    }

    @Test
    void removeMobForgetsEverything() {
        KnowledgeTable table = new KnowledgeTable();
        table.set(mob, player, 1);
        table.removeMob(mob);
        assertEquals(0, table.get(mob, player));
    }

    @Test
    void favoriteGiftFollowsTheTable() {
        assertEquals(GiftKind.HEART_CHARM, GiftPreferences.favorite("wolf", Gender.MALE, java.util.Map.of()));
        java.util.Map<String, java.util.Map<String, Integer>> overrides =
                java.util.Map.of("wolf", java.util.Map.of("bouquet", 99));
        assertEquals(GiftKind.BOUQUET, GiftPreferences.favorite("wolf", Gender.MALE, overrides));
    }
}
