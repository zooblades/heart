package com.heartbound.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.heartbound.Constants;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Holds the active config. {@link #load} reads {@code heartbound.json} from the loader's config folder
 * and writes a default one if it does not exist. A broken file falls back to the defaults.
 */
public final class HeartboundConfig {

    public static final String FILE_NAME = "heartbound.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfig current = new ModConfig();

    private HeartboundConfig() {
    }

    public static ModConfig get() {
        return current;
    }

    public static void load(Path configDir) {
        Path file = configDir.resolve(FILE_NAME);
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                int version = json.has("configVersion") ? json.get("configVersion").getAsInt() : 0;
                ModConfig loaded = GSON.fromJson(json, ModConfig.class);
                if (loaded == null) {
                    loaded = new ModConfig();
                }
                if (version < ModConfig.CURRENT_VERSION) {
                    // defaults changed: take the new numbers, keep the player's gift overrides
                    ModConfig fresh = new ModConfig();
                    fresh.giftGains = loaded.giftGains;
                    current = fresh.sanitize();
                    write(configDir, file);
                    Constants.LOG.info("Config {} was from an older version and was reset to the new defaults", file);
                } else {
                    current = loaded.sanitize();
                    Constants.LOG.info("Loaded config {}", file);
                }
            } catch (Exception e) {
                current = new ModConfig();
                Constants.LOG.error("Could not read {}, using defaults", file, e);
            }
        } else {
            current = new ModConfig();
            write(configDir, file);
        }
    }

    private static void write(Path configDir, Path file) {
        try {
            Files.createDirectories(configDir);
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(current, writer);
            }
        } catch (IOException e) {
            Constants.LOG.error("Could not write config {}", file, e);
        }
    }
}
