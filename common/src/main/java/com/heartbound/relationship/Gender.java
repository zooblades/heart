package com.heartbound.relationship;

import java.util.UUID;

/**
 * Gender of a mob. It is derived from the mob's UUID, so it is stable, needs no extra storage or
 * networking, and the client and server always agree.
 */
public enum Gender {
    MALE,
    FEMALE;

    public static Gender of(UUID uuid) {
        return (uuid.getLeastSignificantBits() & 1L) == 0L ? MALE : FEMALE;
    }

    public String key() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
