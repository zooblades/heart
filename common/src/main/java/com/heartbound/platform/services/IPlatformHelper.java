package com.heartbound.platform.services;

public interface IPlatformHelper {

    /** Name of the current platform (Fabric / NeoForge). */
    String getPlatformName();

    /** Whether a mod with the given id is loaded. */
    boolean isModLoaded(String modId);

    /** Whether the game is running in a development environment. */
    boolean isDevelopmentEnvironment();

    default String getEnvironmentName() {
        return isDevelopmentEnvironment() ? "development" : "production";
    }
}
