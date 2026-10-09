package com.heartbound.gesture;

/** Time rules for the "lie down" gesture. Pure Java, no Minecraft dependencies. */
public final class RestRules {

    public static final long DAY_LENGTH = 24000L;
    /** Same window in which a player may sleep in a bed. */
    public static final long NIGHT_START = 12542L;
    public static final long NIGHT_END = 23460L;

    private RestRules() {
    }

    public static boolean isNightTime(long dayTime) {
        long t = Math.floorMod(dayTime, DAY_LENGTH);
        return t >= NIGHT_START && t < NIGHT_END;
    }

    /**
     * Index of the "night" a moment belongs to: constant from noon to the next noon, so the evening, the
     * night and the following morning share one value. Used to give the morning bonus once per night.
     */
    public static long nightIndex(long dayTime) {
        return Math.floorDiv(dayTime + 12000L, DAY_LENGTH);
    }
}
