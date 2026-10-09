package com.heartbound.behavior;

import com.heartbound.gesture.RestRules;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The "lie down" gesture. Only at night, and it needs two free beds close to each other: the player lies
 * down on one and the partner on the other. The partner gets up when the player gets up (or when hurt).
 * The morning bonus (hearts, regeneration, experience) comes from the normal "slept next to your partner"
 * rule in {@link StageBehaviors}: the partner's bed is within a few blocks of the player's.
 * Transient: nothing is saved. The partner is frozen while lying (other behaviours skip it).
 */
public final class RestManager {

    public static final int BED_RADIUS = 8;
    /** The partner's bed must be at most this far from the player's bed (blocks). */
    public static final double PAIR_DISTANCE_SQR = 6.0D * 6.0D;

    public enum Check {
        OK,
        BUSY,
        /** Not night. */
        TOO_EARLY,
        /** No two free beds next to each other. */
        NO_BED
    }

    private record Rest(UUID player) {
    }

    private static final Map<UUID, Rest> RESTS = new HashMap<>();

    private RestManager() {
    }

    public static boolean isResting(UUID mob) {
        return RESTS.containsKey(mob);
    }

    private static boolean isNight(ServerLevel level) {
        return RestRules.isNightTime(level.getDayTime()) && !level.dimensionType().hasFixedTime();
    }

    /** Whether the gesture can be done right now; never changes anything. */
    public static Check check(ServerPlayer player, Entity entity) {
        if (!(entity instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) {
            return Check.BUSY;
        }
        if (mob.isPassenger() || mob.isSleeping() || player.isSleeping() || RESTS.containsKey(mob.getUUID())) {
            return Check.BUSY;
        }
        if (!isNight(level)) {
            return Check.TOO_EARLY;
        }
        return findBeds(level, player) == null ? Check.NO_BED : Check.OK;
    }

    /**
     * The player lies down on the first bed and the mob on the second. Returns null on success, or the
     * message to show to the player if it did not work (the player was refused by the game, no beds).
     */
    public static Component start(ServerLevel level, ServerPlayer player, Mob mob) {
        BlockPos[] beds = findBeds(level, player);
        if (beds == null) {
            return Component.translatable("gesture.heartbound.lie_down.no_bed", mob.getName());
        }
        var result = player.startSleepInBed(beds[0]);
        if (result.left().isPresent()) {
            Component message = result.left().get().getMessage();
            return message != null ? message
                    : Component.translatable("gesture.heartbound.lie_down.busy", mob.getName());
        }
        mob.getNavigation().stop();
        mob.startSleeping(beds[1]);
        RESTS.put(mob.getUUID(), new Rest(player.getUUID()));
        FreezeManager.freezeUntil(mob.getUUID(), level.getGameTime() + 30L);
        return null;
    }

    /** Gets the mob up if it is lying. Returns true if it was. */
    public static boolean getUp(Entity entity) {
        if (RESTS.remove(entity.getUUID()) == null) {
            return false;
        }
        if (entity instanceof Mob mob && mob.isSleeping()) {
            mob.stopSleeping();
        }
        return true;
    }

    public static void tick(MinecraftServer server) {
        if (RESTS.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, Rest> entry : new HashMap<>(RESTS).entrySet()) {
            UUID mobId = entry.getKey();
            Entity found = find(server, mobId);
            if (!(found instanceof Mob mob) || !mob.isAlive() || !(mob.level() instanceof ServerLevel level)) {
                RESTS.remove(mobId);
                continue;
            }
            if (!mob.isSleeping()) {
                RESTS.remove(mobId); // something else woke it up
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getValue().player());
            if (player == null || !player.isSleeping() || player.level() != level || mob.hurtTime > 0) {
                RESTS.remove(mobId);
                mob.stopSleeping();
                continue;
            }
            FreezeManager.freezeUntil(mobId, level.getGameTime() + 30L);
        }
    }

    /**
     * Two different free beds (head parts) near the player: the nearest one to the player, and the nearest
     * other one to it that is within PAIR_DISTANCE_SQR. Null if there is no such pair.
     */
    private static BlockPos[] findBeds(ServerLevel level, ServerPlayer player) {
        BlockPos origin = player.blockPosition();
        List<BlockPos> beds = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-BED_RADIUS, -3, -BED_RADIUS), origin.offset(BED_RADIUS, 3, BED_RADIUS))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BedBlock
                    && state.getValue(BedBlock.PART) == BedPart.HEAD
                    && !state.getValue(BedBlock.OCCUPIED)) {
                beds.add(pos.immutable());
            }
        }
        if (beds.size() < 2) {
            return null;
        }
        beds.sort((a, b) -> Double.compare(a.distSqr(origin), b.distSqr(origin)));
        BlockPos first = beds.get(0);
        BlockPos second = null;
        double best = Double.MAX_VALUE;
        for (int i = 1; i < beds.size(); i++) {
            double distance = beds.get(i).distSqr(first);
            if (distance <= PAIR_DISTANCE_SQR && distance < best) {
                best = distance;
                second = beds.get(i);
            }
        }
        return second == null ? null : new BlockPos[]{first, second};
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
