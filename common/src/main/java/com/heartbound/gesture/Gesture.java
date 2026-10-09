package com.heartbound.gesture;

import java.util.Locale;

/**
 * Closeness gestures, from light to deep. Pure data, no Minecraft dependencies.
 * Everything stays implied: the deepest implemented gesture is a kiss.
 */
public enum Gesture {
    HOLD_HANDS(300, false, 1, 85, 5, true),
    HUG(600, false, 2, 75, 8, true),
    CHEEK_KISS(600, false, 2, 65, 10, true),
    KISS(0, true, 3, 70, 15, true),
    LIE_DOWN(0, true, 4, 60, 10, true);

    private final int minAffinity;
    private final boolean partnerOnly;
    private final int intimacy;
    private final int baseChance;
    private final int gain;
    private final boolean implemented;

    Gesture(int minAffinity, boolean partnerOnly, int intimacy, int baseChance, int gain, boolean implemented) {
        this.minAffinity = minAffinity;
        this.partnerOnly = partnerOnly;
        this.intimacy = intimacy;
        this.baseChance = baseChance;
        this.gain = gain;
        this.implemented = implemented;
    }

    public int minAffinity() {
        return minAffinity;
    }

    public boolean partnerOnly() {
        return partnerOnly;
    }

    /** 1 (light) to 4 (deep); shy characters are put off more by higher values. */
    public int intimacy() {
        return intimacy;
    }

    public int baseChance() {
        return baseChance;
    }

    /** Affinity gained on success. */
    public int gain() {
        return gain;
    }

    public boolean implemented() {
        return implemented;
    }

    public boolean isUnlocked(int affinity, boolean partner) {
        return partnerOnly ? partner : affinity >= minAffinity;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The gesture with this ordinal, or null. */
    public static Gesture byId(int id) {
        Gesture[] all = values();
        return id >= 0 && id < all.length ? all[id] : null;
    }
}
