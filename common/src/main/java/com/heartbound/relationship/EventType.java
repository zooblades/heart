package com.heartbound.relationship;

import java.util.Locale;

/** Significant moments a mob remembers about a player (each is recorded once). Pure data. */
public enum EventType {
    FIRST_MEETING,
    FIRST_GIFT,
    STAGE_ACQUAINTED,
    STAGE_FRIENDS,
    STAGE_CLOSE,
    FIRST_HUG,
    FIRST_KISS,
    PROPOSAL,
    FIRST_MORNING,
    BREAKUP,
    FIRST_LIE_DOWN,
    FIRST_DATE;

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static EventType byName(String name) {
        for (EventType type : values()) {
            if (type.name().equals(name)) {
                return type;
            }
        }
        return null;
    }

    public static EventType byOrdinal(int ordinal) {
        EventType[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
    }

    /** The event for reaching a stage, or null for stages that have none. */
    public static EventType forStage(RelationshipStage stage) {
        return switch (stage) {
            case ACQUAINTED -> STAGE_ACQUAINTED;
            case FRIENDS -> STAGE_FRIENDS;
            case CLOSE -> STAGE_CLOSE;
            default -> null;
        };
    }
}
