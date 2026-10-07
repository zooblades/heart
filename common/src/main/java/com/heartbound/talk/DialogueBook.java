package com.heartbound.talk;

import com.heartbound.gesture.Personality;

import java.util.ArrayList;
import java.util.List;

/**
 * Names the translation keys of every line a mob can say. The lines themselves live in the language
 * files, so they are easy to extend and are shown in each player's language.
 */
public final class DialogueBook {

    /** Lines per (topic, outcome, character). */
    public static final int VARIANTS = 4;
    /** Extra "about me" lines per supported species. */
    public static final int SPECIES_VARIANTS = 2;
    public static final List<String> SPECIES = List.of("villager", "wolf", "cat", "fox", "piglin");

    private static final String PREFIX = "dialogue.heartbound.";

    private DialogueBook() {
    }

    public static String key(Topic topic, boolean ok, Personality personality, int variant) {
        return PREFIX + topic.key() + "." + (ok ? "ok" : "fail") + "." + personality.key() + "." + variant;
    }

    public static String comfortKey(Personality personality, int variant) {
        return PREFIX + "comfort." + personality.key() + "." + variant;
    }

    public static String tiredKey(Personality personality, int variant) {
        return PREFIX + "tired." + personality.key() + "." + variant;
    }

    public static boolean hasSpecies(String mobPath) {
        return SPECIES.contains(mobPath);
    }

    public static String speciesKey(String mobPath, int variant) {
        return PREFIX + "about_you.species." + mobPath + "." + variant;
    }

    /** Every key that must exist in the language files. */
    public static List<String> allKeys() {
        List<String> keys = new ArrayList<>();
        for (Personality personality : Personality.values()) {
            for (int v = 0; v < VARIANTS; v++) {
                for (Topic topic : Topic.values()) {
                    keys.add(key(topic, true, personality, v));
                    if (topic.canFail()) {
                        keys.add(key(topic, false, personality, v));
                    }
                }
                keys.add(comfortKey(personality, v));
                keys.add(tiredKey(personality, v));
            }
        }
        for (String species : SPECIES) {
            for (int v = 0; v < SPECIES_VARIANTS; v++) {
                keys.add(speciesKey(species, v));
            }
        }
        return keys;
    }
}
