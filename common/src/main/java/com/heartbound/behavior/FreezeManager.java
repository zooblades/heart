package com.heartbound.behavior;

import com.heartbound.menu.RelationshipMenu;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps a mob standing still (and looking at the player) while the player talks to it: as long as the
 * relationship window is open, and for a short time after the gesture menu is opened or a gesture is
 * performed. Runs every tick; all other behaviours (follow, home, life director) skip frozen mobs.
 * Transient: nothing is saved, and the mob's AI is never switched off, only held each tick.
 */
public final class FreezeManager {

    /** Mobs held because a window is open (recomputed every tick). */
    private static final Set<UUID> MENU = new HashSet<>();
    /** Mobs held for a limited time: mob to game tick until which it is held. */
    private static final Map<UUID, Long> TIMED = new HashMap<>();
    private static long lastNow;

    private FreezeManager() {
    }

    public static boolean isFrozen(UUID mob) {
        Long until = TIMED.get(mob);
        return MENU.contains(mob) || (until != null && lastNow < until);
    }

    /** Holds the mob until the given game tick. */
    public static void freezeUntil(UUID mob, long untilTick) {
        TIMED.merge(mob, untilTick, Math::max);
    }

    public static void tick(MinecraftServer server) {
        lastNow = server.overworld().getGameTime();

        MENU.clear();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof RelationshipMenu menu
                    && menu.getTargetEntity() instanceof Mob mob && mob.isAlive()) {
                MENU.add(mob.getUUID());
                hold(mob, player);
            }
        }

        if (TIMED.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Long>> iterator = TIMED.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (lastNow >= entry.getValue()) {
                iterator.remove();
                continue;
            }
            Entity found = find(server, entry.getKey());
            if (found instanceof Mob mob && mob.isAlive()) {
                ServerPlayer nearest = nearestPlayer(server, mob);
                hold(mob, nearest);
            }
        }
    }

    private static ServerPlayer nearestPlayer(MinecraftServer server, Mob mob) {
        ServerPlayer best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() != mob.level()) {
                continue;
            }
            double distance = mob.distanceToSqr(player);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = player;
            }
        }
        return best;
    }

    private static void hold(Mob mob, ServerPlayer player) {
        mob.getNavigation().stop();
        mob.setZza(0.0F);
        mob.setXxa(0.0F);
        mob.setSpeed(0.0F);
        mob.setDeltaMovement(0.0D, mob.getDeltaMovement().y, 0.0D);
        mob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        if (player != null) {
            mob.getLookControl().setLookAt(player, 30.0F, 30.0F);
        }
    }

    private static Entity find(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }
}
