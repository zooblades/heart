package com.heartbound.partnergift;

import com.heartbound.partnergift.PartnerGiftRules.Gift;
import com.heartbound.partnergift.PartnerGiftRules.Occasion;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartnerGiftRulesTest {

    private static final String[] MOBS = {"villager", "wolf", "cat", "fox", "piglin", "something_else"};

    @Test
    void hurtPlayerGetsCareNotAnItem() {
        for (String mob : MOBS) {
            assertNull(PartnerGiftRules.choose(mob, Occasion.HURT, 900, 0, 0));
        }
    }

    @Test
    void everyOtherOccasionGivesAValidItem() {
        for (String mob : MOBS) {
            for (Occasion occasion : new Occasion[]{Occasion.RANDOM, Occasion.HUNGRY, Occasion.DATE}) {
                for (int affinity : new int[]{100, 800}) {
                    for (int a = 0; a < 100; a += 9) {
                        for (int b = 0; b < 100; b += 11) {
                            Gift gift = PartnerGiftRules.choose(mob, occasion, affinity, a, b);
                            assertNotNull(gift);
                            assertTrue(gift.itemId().startsWith("minecraft:"), gift.itemId());
                            assertTrue(gift.count() >= 1 && gift.count() <= 3, "count " + gift.count());
                        }
                    }
                }
            }
        }
    }

    @Test
    void rarePresentsNeedAStrongRelationship() {
        assertEquals("minecraft:emerald", PartnerGiftRules.choose("villager", Occasion.RANDOM, 800, 5, 0).itemId());
        assertEquals("minecraft:gold_ingot", PartnerGiftRules.choose("piglin", Occasion.RANDOM, 800, 5, 0).itemId());
        assertEquals("minecraft:amethyst_shard", PartnerGiftRules.choose("cat", Occasion.DATE, 800, 5, 0).itemId());
        assertTrue(!PartnerGiftRules.choose("villager", Occasion.RANDOM, 300, 5, 0).itemId().equals("minecraft:emerald"));
    }

    @Test
    void foodWhenHungryIsNeverARarePresent() {
        for (int roll = 0; roll < 100; roll++) {
            String item = PartnerGiftRules.choose("wolf", Occasion.HUNGRY, 1000, roll, 0).itemId();
            assertTrue(item.contains("bread") || item.contains("cooked") || item.contains("apple") || item.contains("potato"), item);
        }
    }

    @Test
    void keysAreUnique() {
        List<String> keys = PartnerGiftRules.allKeys();
        assertEquals(keys.size(), keys.stream().distinct().count());
    }

    @Test
    void everyLineExistsInBothLanguages() throws IOException {
        for (String lang : new String[]{"en_us", "ru_ru"}) {
            String text;
            try (InputStream in = PartnerGiftRulesTest.class.getResourceAsStream("/assets/heartbound/lang/" + lang + ".json")) {
                assertTrue(in != null, "missing language file " + lang);
                text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            List<String> missing = new ArrayList<>();
            for (String key : PartnerGiftRules.allKeys()) {
                if (!text.contains("\"" + key + "\"")) {
                    missing.add(key);
                }
            }
            for (String period : new String[]{"morning", "day", "evening", "night"}) {
                for (int v = 0; v < 2; v++) {
                    String key = "home.heartbound.greet." + period + "." + v;
                    if (!text.contains("\"" + key + "\"")) {
                        missing.add(key);
                    }
                }
            }
            assertTrue(missing.isEmpty(), lang + " is missing: " + missing);
        }
    }
}
