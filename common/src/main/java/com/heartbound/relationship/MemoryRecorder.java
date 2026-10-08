package com.heartbound.relationship;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

/** Server-side helpers that write significant moments and counters into the pair memory. */
public final class MemoryRecorder {

    private static final long TICKS_PER_DAY = 24000L;

    private MemoryRecorder() {
    }

    private static long dayOf(ServerPlayer player) {
        return player.serverLevel().getDayTime() / TICKS_PER_DAY;
    }

    public static void record(ServerPlayer player, UUID mobId, long pos, EventType type, int extra) {
        if (player.getServer() == null) {
            return;
        }
        RelationshipData.get(player.getServer()).recordEvent(mobId, player.getUUID(), type, dayOf(player), pos, extra);
    }

    public static void count(ServerPlayer player, UUID mobId, PairMemory.Counter counter) {
        if (player.getServer() == null) {
            return;
        }
        RelationshipData.get(player.getServer()).incrementCounter(mobId, player.getUUID(), counter);
    }

    /** Call after the affinity changed: records the first meeting and any stage that was just reached. */
    public static void onAffinityChanged(ServerPlayer player, Entity mob, int before, int after) {
        long pos = mob.blockPosition().asLong();
        record(player, mob.getUUID(), pos, EventType.FIRST_MEETING, 0);
        for (RelationshipStage stage : RelationshipStage.values()) {
            EventType type = EventType.forStage(stage);
            if (type != null && before < stage.threshold() && after >= stage.threshold()) {
                record(player, mob.getUUID(), pos, type, 0);
            }
        }
    }
}
