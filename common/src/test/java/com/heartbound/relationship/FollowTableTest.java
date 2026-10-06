package com.heartbound.relationship;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FollowTableTest {

    private final UUID mob = UUID.randomUUID();
    private final UUID player = UUID.randomUUID();

    @Test
    void setGetClear() {
        FollowTable table = new FollowTable();
        assertNull(table.get(mob));
        table.set(mob, player);
        assertEquals(player, table.get(mob));
        assertTrue(table.clear(mob));
        assertFalse(table.clear(mob));
        assertNull(table.get(mob));
    }

    @Test
    void aMobFollowsOnlyOnePlayer() {
        FollowTable table = new FollowTable();
        UUID other = UUID.randomUUID();
        table.set(mob, player);
        table.set(mob, other);
        assertEquals(other, table.get(mob));
        assertEquals(0, table.count(player));
        assertEquals(1, table.count(other));
    }

    @Test
    void countsPerPlayer() {
        FollowTable table = new FollowTable();
        table.set(UUID.randomUUID(), player);
        table.set(UUID.randomUUID(), player);
        table.set(UUID.randomUUID(), UUID.randomUUID());
        assertEquals(2, table.count(player));
    }

    @Test
    void snapshotIsACopy() {
        FollowTable table = new FollowTable();
        table.set(mob, player);
        var copy = table.snapshot();
        table.clear(mob);
        assertEquals(1, copy.size());
        assertTrue(table.isEmpty());
    }
}
