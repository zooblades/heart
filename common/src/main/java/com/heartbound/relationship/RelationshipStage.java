package com.heartbound.relationship;

/**
 * Relationship stages from the GDD. Pure logic, no Minecraft dependencies.
 *
 * Affinity is a value from 0 to {@link #MAX_AFFINITY}. The PARTNERS stage is never reached
 * automatically: it requires a proposal (ring) and at least {@link #PROPOSAL_MIN_AFFINITY} affinity.
 */
public enum RelationshipStage {
    STRANGERS(0),
    ACQUAINTED(100),
    FRIENDS(300),
    CLOSE(600),
    PARTNERS(800);

    public static final int MAX_AFFINITY = 1000;
    public static final int PROPOSAL_MIN_AFFINITY = 800;

    private final int threshold;

    RelationshipStage(int threshold) {
        this.threshold = threshold;
    }

    public int threshold() {
        return threshold;
    }

    /** Stage reached by affinity alone (never PARTNERS). */
    public static RelationshipStage forAffinity(int affinity) {
        int value = Math.max(0, Math.min(MAX_AFFINITY, affinity));
        RelationshipStage result = STRANGERS;
        for (RelationshipStage stage : values()) {
            if (stage == PARTNERS) {
                continue;
            }
            if (value >= stage.threshold) {
                result = stage;
            }
        }
        return result;
    }

    public static boolean canPropose(int affinity) {
        return affinity >= PROPOSAL_MIN_AFFINITY;
    }
}
