package com.heartbound.platform.services;

public interface IPlatformHelper {

    /** Name of the current platform (Fabric / NeoForge). */
    String getPlatformName();

    /** Whether a mod with the given id is loaded. */
    boolean isModLoaded(String modId);

    /** Whether the game is running in a development environment. */
    boolean isDevelopmentEnvironment();

    /** The loader's config folder. */
    java.nio.file.Path getConfigDir();

    /**
     * Registers an integer game rule (the factory for it is not public in vanilla, so each loader does it
     * its own way).
     */
    net.minecraft.world.level.GameRules.Key<net.minecraft.world.level.GameRules.IntegerValue> registerIntGameRule(
            String name, int defaultValue);

    default String getEnvironmentName() {
        return isDevelopmentEnvironment() ? "development" : "production";
    }
}
