package com.heartbound.relationship;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AffinityTableTest {

    private final UUID mob = UUID.randomUUID();
    private final UUID player = UUID.randomUUID();

    @Test
    void defaultIsZero() {
        assertEquals(0, new AffinityTable().get(mob, player));
    }

    @Test
    void clampsToRange() {
        AffinityTable table = new AffinityTable();
        assertEquals(RelationshipStage.MAX_AFFINITY, table.set(mob, player, 5000));
        assertEquals(0, table.set(mob, player, -10));
        assertEquals(RelationshipStage.MAX_AFFINITY, table.add(mob, player, Integer.MAX_VALUE));
        assertEquals(0, table.add(mob, player, Integer.MIN_VALUE));
    }

    @Test
    void zeroIsNotStored() {
        AffinityTable table = new AffinityTable();
        table.set(mob, player, 50);
        table.set(mob, player, 0);
        assertTrue(table.isEmpty());
    }

    @Test
    void pairsAreIndependent() {
        AffinityTable table = new AffinityTable();
        UUID other = UUID.randomUUID();
        table.set(mob, player, 100);
        table.set(mob, other, 700);
        assertEquals(100, table.get(mob, player));
        assertEquals(700, table.get(mob, other));
    }

    @Test
    void removeMobForgetsAllPlayers() {
        AffinityTable table = new AffinityTable();
        table.set(mob, player, 100);
        table.set(mob, UUID.randomUUID(), 200);
        table.removeMob(mob);
        assertTrue(table.isEmpty());
    }
}
