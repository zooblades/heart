package com.heartbound.config;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModConfigTest {

    @Test
    void defaultsAreAlreadyValid() {
        ModConfig config = new ModConfig();
        int cooldown = config.giftCooldownTicks;
        int followers = config.maxFollowers;
        config.sanitize();
        assertEquals(cooldown, config.giftCooldownTicks);
        assertEquals(followers, config.maxFollowers);
    }

    @Test
    void sanitizeClampsOutOfRangeValues() {
        ModConfig config = new ModConfig();
        config.petGain = 99999;
        config.maxFollowers = 0;
        config.giftCooldownTicks = -5;
        config.breakupAffinity = 5000;
        config.followTeleportDistance = 1;
        config.morningBonusXp = -1;
        config.sanitize();
        assertEquals(1000, config.petGain);
        assertEquals(1, config.maxFollowers);
        assertEquals(0, config.giftCooldownTicks);
        assertEquals(1000, config.breakupAffinity);
        assertEquals(8.0, config.followTeleportDistance);
        assertEquals(0, config.morningBonusXp);
    }

    @Test
    void sanitizeFixesBrokenGiftTable() {
        ModConfig config = new ModConfig();
        Map<String, Integer> gains = new HashMap<>();
        gains.put("bouquet", 5000);
        gains.put("heart_charm", null);
        config.giftGains.put("fox", gains);
        config.giftGains.put("wolf", null);
        config.sanitize();
        assertNotNull(config.giftGains.get("fox"));
        assertEquals(1000, config.giftGains.get("fox").get("bouquet"));
        assertEquals(0, config.giftGains.get("fox").get("heart_charm"));
        assertTrue(!config.giftGains.containsKey("wolf"));
    }

    @Test
    void nullGiftTableBecomesEmpty() {
        ModConfig config = new ModConfig();
        config.giftGains = null;
        config.sanitize();
        assertNotNull(config.giftGains);
    }
}
