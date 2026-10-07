package com.heartbound.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
                ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                current = (loaded == null ? new ModConfig() : loaded).sanitize();
                Constants.LOG.info("Loaded config {}", file);
            } catch (Exception e) {
                current = new ModConfig();
                Constants.LOG.error("Could not read {}, using defaults", file, e);
            }
        } else {
            current = new ModConfig();
            try {
                Files.createDirectories(configDir);
                try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                    GSON.toJson(current, writer);
                }
                Constants.LOG.info("Wrote default config {}", file);
            } catch (IOException e) {
                Constants.LOG.error("Could not write default config {}", file, e);
            }
        }
    }
}
