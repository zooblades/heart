package com.heartbound.talk;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards against typos: every dialogue key must exist in both language files. */
class DialogueBookTest {

    private static String read(String lang) throws IOException {
        try (InputStream in = DialogueBookTest.class.getResourceAsStream("/assets/heartbound/lang/" + lang + ".json")) {
            assertTrue(in != null, "missing language file " + lang);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void everyLineExistsInBothLanguages() throws IOException {
        for (String lang : new String[]{"en_us", "ru_ru"}) {
            String text = read(lang);
            List<String> missing = new ArrayList<>();
            for (String key : DialogueBook.allKeys()) {
                if (!text.contains("\"" + key + "\"")) {
                    missing.add(key);
                }
            }
            assertTrue(missing.isEmpty(), lang + " is missing: " + missing);
        }
    }

    @Test
    void keysAreUnique() {
        List<String> keys = DialogueBook.allKeys();
        assertTrue(keys.size() == keys.stream().distinct().count());
    }
}
