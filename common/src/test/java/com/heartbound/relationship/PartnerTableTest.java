package com.heartbound.relationship;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartnerTableTest {

    private final UUID mob = UUID.randomUUID();
    private final UUID player = UUID.randomUUID();

    @Test
    void pairsBothWays() {
        PartnerTable table = new PartnerTable();
        assertTrue(table.pair(mob, player));
        assertEquals(player, table.partnerOfMob(mob));
        assertEquals(mob, table.partnerOfPlayer(player));
        assertTrue(table.isPair(mob, player));
    }

    @Test
    void noPolygamy() {
        PartnerTable table = new PartnerTable();
        table.pair(mob, player);
        assertFalse(table.pair(UUID.randomUUID(), player));
        assertFalse(table.pair(mob, UUID.randomUUID()));
        assertEquals(1, table.snapshot().size());
    }

    @Test
    void unpairFromEitherSide() {
        PartnerTable table = new PartnerTable();
        table.pair(mob, player);
        assertEquals(mob, table.unpairPlayer(player));
        assertNull(table.partnerOfMob(mob));
        assertTrue(table.pair(mob, player));
        assertEquals(player, table.unpairMob(mob));
        assertNull(table.partnerOfPlayer(player));
        assertNull(table.unpairMob(mob));
    }
}
