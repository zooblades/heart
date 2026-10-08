package com.heartbound.relationship;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryTest {

    @Test
    void eachEventIsRecordedOnlyOnce() {
        PairMemory memory = new PairMemory();
        assertTrue(memory.record(EventType.FIRST_GIFT, 3, 100L, 1));
        assertFalse(memory.record(EventType.FIRST_GIFT, 9, 200L, 0));
        assertEquals(1, memory.entries().size());
        assertEquals(3, memory.entries().get(0).day());
        assertEquals(1, memory.entries().get(0).extra());
    }

    @Test
    void eventsKeepTheirOrder() {
        PairMemory memory = new PairMemory();
        memory.record(EventType.FIRST_MEETING, 1, 0, 0);
        memory.record(EventType.STAGE_FRIENDS, 5, 0, 0);
        memory.record(EventType.PROPOSAL, 9, 0, 0);
        assertEquals(EventType.FIRST_MEETING, memory.entries().get(0).type());
        assertEquals(EventType.PROPOSAL, memory.entries().get(2).type());
        assertTrue(memory.has(EventType.STAGE_FRIENDS));
        assertFalse(memory.has(EventType.BREAKUP));
    }

    @Test
    void countersCount() {
        PairMemory memory = new PairMemory();
        memory.increment(PairMemory.Counter.TALKS);
        memory.increment(PairMemory.Counter.TALKS);
        memory.setCount(PairMemory.Counter.GIFTS, -4);
        assertEquals(2, memory.count(PairMemory.Counter.TALKS));
        assertEquals(0, memory.count(PairMemory.Counter.GIFTS));
    }

    @Test
    void bookKeepsPairsApartAndForgetsMobs() {
        MemoryBook book = new MemoryBook();
        UUID mob = UUID.randomUUID();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        assertNull(book.get(mob, a));
        book.getOrCreate(mob, a).record(EventType.FIRST_MEETING, 1, 0, 0);
        assertNull(book.get(mob, b));
        book.removeMob(mob);
        assertNull(book.get(mob, a));
    }

    @Test
    void stagesMapToEvents() {
        assertEquals(EventType.STAGE_CLOSE, EventType.forStage(RelationshipStage.CLOSE));
        assertNull(EventType.forStage(RelationshipStage.STRANGERS));
        assertNull(EventType.forStage(RelationshipStage.PARTNERS));
        assertEquals(EventType.PROPOSAL, EventType.byName("PROPOSAL"));
        assertNull(EventType.byName("nope"));
    }
}
