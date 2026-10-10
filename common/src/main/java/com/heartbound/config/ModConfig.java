package com.heartbound.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * All tunable numbers of the mod. Plain fields so the JSON file maps onto them directly.
 * Pure Java: no Minecraft dependencies. {@link #sanitize()} keeps hand-edited values in a sane range.
 */
public final class ModConfig {

    /** Bumped when defaults change; older config files are reset to the new defaults. */
    public static final int CURRENT_VERSION = 3;

    public int configVersion = CURRENT_VERSION;

    public int giftCooldownTicks = 1200;
    public int petCooldownTicks = 600;
    public int petGain = 3;
    public int maxFollowers = 3;
    public int breakupAffinity = 300;
    public double followTeleportDistance = 24.0;
    public int morningBonusSeconds = 30;
    public int morningBonusXp = 15;
    public int gestureCooldownTicks = 200;
    public int holdHandsSeconds = 30;
    /** Life intensity: 0 off, 1 rare, 2 normal, 3 lively. */
    public int lifeIntensity = 2;
    public int gesturePenalty = 3;

    /**
     * Optional gift overrides. Key: mob id ("fox") or mob id and gender ("piglin/female").
     * Value: gift name ("bouquet", "heart_charm") to affinity gain.
     */
    public Map<String, Map<String, Integer>> giftGains = new LinkedHashMap<>();

    public ModConfig sanitize() {
        giftCooldownTicks = clamp(giftCooldownTicks, 0, 6000);
        petCooldownTicks = clamp(petCooldownTicks, 0, 6000);
        petGain = clamp(petGain, 0, 1000);
        maxFollowers = clamp(maxFollowers, 1, 20);
        breakupAffinity = clamp(breakupAffinity, 0, 1000);
        followTeleportDistance = Math.max(8.0, Math.min(128.0, followTeleportDistance));
        morningBonusSeconds = clamp(morningBonusSeconds, 0, 600);
        morningBonusXp = clamp(morningBonusXp, 0, 1000);
        gestureCooldownTicks = clamp(gestureCooldownTicks, 0, 6000);
        gesturePenalty = clamp(gesturePenalty, 0, 1000);
        holdHandsSeconds = clamp(holdHandsSeconds, 5, 600);
        lifeIntensity = clamp(lifeIntensity, 0, 3);

        if (giftGains == null) {
            giftGains = new LinkedHashMap<>();
        }
        giftGains.values().removeIf(gains -> gains == null);
        for (Map<String, Integer> gains : giftGains.values()) {
            gains.replaceAll((gift, value) -> value == null ? 0 : clamp(value, -1000, 1000));
        }
        return this;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
