package com.heartbound.config;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;

/**
 * Settings that can be changed in the game with /gamerule (per world, no restart needed):
 * heartboundPartnerTalkSeconds and heartboundDateCooldownSeconds.
 */
public final class ModGameRules {

    public static final int DEFAULT_PARTNER_TALK_SECONDS = 300;
    public static final int DEFAULT_DATE_COOLDOWN_SECONDS = 1200;

    private static GameRules.Key<GameRules.IntegerValue> partnerTalkSeconds;
    private static GameRules.Key<GameRules.IntegerValue> dateCooldownSeconds;

    private ModGameRules() {
    }

    /** Registers the rules; called once when the mod starts, before any world is loaded. */
    public static void register() {
        partnerTalkSeconds = GameRules.register("heartboundPartnerTalkSeconds", GameRules.Category.MOBS,
                GameRules.IntegerValue.create(DEFAULT_PARTNER_TALK_SECONDS));
        dateCooldownSeconds = GameRules.register("heartboundDateCooldownSeconds", GameRules.Category.MOBS,
                GameRules.IntegerValue.create(DEFAULT_DATE_COOLDOWN_SECONDS));
    }

    /** Average pause between conversations the partner starts, in seconds; 0 turns them off. */
    public static int partnerTalkSeconds(MinecraftServer server) {
        return clamp(server.getGameRules().getInt(partnerTalkSeconds), 0, 3600);
    }

    /** Average pause between date invitations, in seconds; 0 turns them off. */
    public static int dateCooldownSeconds(MinecraftServer server) {
        return clamp(server.getGameRules().getInt(dateCooldownSeconds), 0, 86400);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
