package com.heartbound.talk;

import java.util.Locale;

/** Conversation topics. Pure data, no Minecraft dependencies. */
public enum Topic {
    HOW_ARE_YOU(0, 2, 100),
    ABOUT_YOU(0, 3, 100),
    COMPLIMENT(0, 4, 75),
    JOKE(0, 3, 60),
    FLIRT(300, 6, 55);

    private final int minAffinity;
    private final int gain;
    private final int baseChance;

    Topic(int minAffinity, int gain, int baseChance) {
        this.minAffinity = minAffinity;
        this.gain = gain;
        this.baseChance = baseChance;
    }

    public int minAffinity() {
        return minAffinity;
    }

    /** Affinity gained on a good answer. */
    public int gain() {
        return gain;
    }

    public int baseChance() {
        return baseChance;
    }

    /** Whether the mob can answer badly (the first two topics always go well). */
    public boolean canFail() {
        return this == COMPLIMENT || this == JOKE || this == FLIRT;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Topic byId(int id) {
        Topic[] all = values();
        return id >= 0 && id < all.length ? all[id] : null;
    }
}
