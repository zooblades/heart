package com.heartbound.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * All tunable numbers of the mod. Plain fields so the JSON file maps onto them directly.
 * Pure Java: no Minecraft dependencies. {@link #sanitize()} keeps hand-edited values in a sane range.
 */
public final class ModConfig {

    public int giftCooldownTicks = 20;
    public int petCooldownTicks = 100;
    public int petGain = 3;
    public int maxFollowers = 3;
    public int breakupAffinity = 300;
    public double followTeleportDistance = 24.0;
    public int morningBonusSeconds = 30;
    public int morningBonusXp = 15;

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
