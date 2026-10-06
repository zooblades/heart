package com.heartbound.relationship;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelationshipStageTest {

    @Test
    void thresholds() {
        assertEquals(RelationshipStage.STRANGERS, RelationshipStage.forAffinity(0));
        assertEquals(RelationshipStage.STRANGERS, RelationshipStage.forAffinity(99));
        assertEquals(RelationshipStage.ACQUAINTED, RelationshipStage.forAffinity(100));
        assertEquals(RelationshipStage.FRIENDS, RelationshipStage.forAffinity(300));
        assertEquals(RelationshipStage.CLOSE, RelationshipStage.forAffinity(600));
    }

    @Test
    void partnersAreNeverReachedByAffinityAlone() {
        assertEquals(RelationshipStage.CLOSE, RelationshipStage.forAffinity(1000));
        assertEquals(RelationshipStage.CLOSE, RelationshipStage.forAffinity(99999));
    }

    @Test
    void outOfRangeIsClamped() {
        assertEquals(RelationshipStage.STRANGERS, RelationshipStage.forAffinity(-5));
    }

    @Test
    void proposalNeedsEnoughAffinity() {
        assertFalse(RelationshipStage.canPropose(799));
        assertTrue(RelationshipStage.canPropose(800));
    }
}
