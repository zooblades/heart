package com.heartbound.config;

import com.heartbound.platform.Services;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;

/**
 * Settings that can be changed in the game with /gamerule (per world, no restart needed):
 * heartboundPartnerTalkSeconds and heartboundDateCooldownSeconds.
 */
public final class ModGameRules {

    public static final int DEFAULT_PARTNER_TALK_SECONDS = 300;
    public static final int DEFAULT_DATE_COOLDOWN_SECONDS = 1200;
    public static final int DEFAULT_PARTNER_GIFT_SECONDS = 1500;

    private static GameRules.Key<GameRules.IntegerValue> partnerTalkSeconds;
    private static GameRules.Key<GameRules.IntegerValue> dateCooldownSeconds;
    private static GameRules.Key<GameRules.IntegerValue> partnerGiftSeconds;
    private static GameRules.Key<GameRules.IntegerValue> homeRoutine;

    private ModGameRules() {
    }

    /** Registers the rules; called once when the mod starts, before any world is loaded. */
    public static void register() {
        partnerTalkSeconds = Services.PLATFORM.registerIntGameRule("heartboundPartnerTalkSeconds",
                DEFAULT_PARTNER_TALK_SECONDS);
        dateCooldownSeconds = Services.PLATFORM.registerIntGameRule("heartboundDateCooldownSeconds",
                DEFAULT_DATE_COOLDOWN_SECONDS);
        partnerGiftSeconds = Services.PLATFORM.registerIntGameRule("heartboundPartnerGiftSeconds",
                DEFAULT_PARTNER_GIFT_SECONDS);
        homeRoutine = Services.PLATFORM.registerIntGameRule("heartboundHomeRoutine", 1);
    }

    /** Average pause between conversations the partner starts, in seconds; 0 turns them off. */
    public static int partnerTalkSeconds(MinecraftServer server) {
        return clamp(server.getGameRules().getInt(partnerTalkSeconds), 0, 3600);
    }

    /** Average pause between date invitations, in seconds; 0 turns them off. */
    public static int dateCooldownSeconds(MinecraftServer server) {
        return clamp(server.getGameRules().getInt(dateCooldownSeconds), 0, 86400);
    }

    /** Average pause between presents from the partner, in seconds; 0 turns them off. */
    public static int partnerGiftSeconds(MinecraftServer server) {
        return clamp(server.getGameRules().getInt(partnerGiftSeconds), 0, 86400);
    }

    /** 1: partners with a home follow a daily routine at home; 0: they do not. */
    public static int homeRoutine(MinecraftServer server) {
        return clamp(server.getGameRules().getInt(homeRoutine), 0, 1);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
