package com.heartbound.behavior;

import com.heartbound.config.HeartboundConfig;
import com.heartbound.gesture.RestRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The "lie down" gesture: the partner lies down next to the player.
 * - Animals (wolf, cat, fox) lie on their side (the sleeping pose), no bed needed.
 * - Villagers and piglins lie on a free bed within a few blocks; villagers only at night, because their
 *   own brain wakes them up by day.
 * - By day the partner gets up after {@code restSeconds}. At night the partner stays until morning and
 *   then the morning bonus (hearts, regeneration, experience) is given.
 * The partner also gets up when hurt, when the player walks away or leaves, or when asked again.
 * Transient: nothing is saved. All other behaviours skip the mob while it rests (it is frozen).
 */
public final class RestManager {

    public static final int BED_RADIUS = 8;
    public static final double MAX_DISTANCE_SQR = 24.0D * 24.0D;

    public enum Check {
        OK,
        BUSY,
        NO_BED,
        TOO_EARLY
    }

    private record Rest(UUID player, long endTick, boolean night, BlockPos bed) {
    }

    private static final Map<UUID, Rest> RESTS = new HashMap<>();

    private RestManager() {
    }

    public static boolean isResting(UUID mob) {
        return RESTS.containsKey(mob);
    }

    private static boolean usesBed(Mob mob) {
        return mob instanceof Villager || mob instanceof AbstractPiglin;
    }

    /** Whether this mob can lie down right now; never changes anything. */
    public static Check check(Entity entity) {
        if (!(entity instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) {
            return Check.BUSY;
        }
        if (mob.isPassenger() || mob.isSleeping() || RESTS.containsKey(mob.getUUID())) {
            return Check.BUSY;
        }
        if (usesBed(mob)) {
            if (mob instanceof Villager && !RestRules.isNightTime(level.getDayTime())) {
                return Check.TOO_EARLY;
            }
            if (findBed(level, mob) == null) {
                return Check.NO_BED;
            }
        }
        return Check.OK;
    }

    /** Makes the mob lie down. Returns false if it cannot (for example the bed is gone). */
    public static boolean start(ServerLevel level, ServerPlayer player, Mob mob) {
        boolean night = RestRules.isNightTime(level.getDayTime()) && !level.dimensionType().hasFixedTime();
        long now = level.getGameTime();
        BlockPos bed = null;
        mob.getNavigation().stop();
        if (usesBed(mob)) {
            bed = findBed(level, mob);
            if (bed == null) {
                return false;
            }
            mob.startSleeping(bed);
        } else {
            mob.setPose(Pose.SLEEPING);
        }
        long end = night ? Long.MAX_VALUE : now + HeartboundConfig.get().restSeconds * 20L;
        RESTS.put(mob.getUUID(), new Rest(player.getUUID(), end, night, bed));
        FreezeManager.freezeUntil(mob.getUUID(), now + 30L);
        return true;
    }

    /** Gets the mob up if it is resting. Returns true if it was. */
    public static boolean getUp(Entity entity) {
        Rest rest = RESTS.remove(entity.getUUID());
        if (rest == null) {
            return false;
        }
        if (entity instanceof Mob mob) {
            wake(mob, rest);
        }
        return true;
    }

    public static void tick(MinecraftServer server) {
        if (RESTS.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, Rest> entry : new HashMap<>(RESTS).entrySet()) {
            UUID mobId = entry.getKey();
            Rest rest = entry.getValue();
            Entity found = find(server, mobId);
            if (!(found instanceof Mob mob) || !mob.isAlive() || !(mob.level() instanceof ServerLevel level)) {
                RESTS.remove(mobId);
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(rest.player());
            if (player == null || player.level() != level || mob.distanceToSqr(player) > MAX_DISTANCE_SQR
                    || mob.hurtTime > 0 || mob.isPassenger()) {
                RESTS.remove(mobId);
                wake(mob, rest);
                continue;
            }
            if (rest.bed() != null && !mob.isSleeping()) {
                RESTS.remove(mobId); // something else woke it up: nothing to restore
                continue;
            }

            long now = level.getGameTime();
            if (rest.night()) {
                if (!RestRules.isNightTime(level.getDayTime())) {
                    RESTS.remove(mobId);
                    wake(mob, rest);
                    StageBehaviors.morningBonus(player, mobId);
                    continue;
                }
            } else if (now >= rest.endTick()) {
                RESTS.remove(mobId);
                wake(mob, rest);
                level.sendParticles(ParticleTypes.HEART, mob.getX(), mob.getY() + mob.getBbHeight() + 0.2, mob.getZ(),
                        3, 0.3, 0.2, 0.3, 0.02);
                continue;
            }

            if (rest.bed() == null && mob.getPose() != Pose.SLEEPING) {
                mob.setPose(Pose.SLEEPING);
            }
            FreezeManager.freezeUntil(mobId, now + 30L);
        }
    }

    private static void wake(Mob mob, Rest rest) {
        if (rest.bed() != null) {
            if (mob.isSleeping()) {
                mob.stopSleeping();
            }
        } else if (mob.getPose() == Pose.SLEEPING) {
            mob.setPose(Pose.STANDING);
        }
    }

    /** The nearest free bed (head part) around the mob that no other resting mob has taken, or null. */
    private static BlockPos findBed(ServerLevel level, Mob mob) {
        BlockPos origin = mob.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-BED_RADIUS, -3, -BED_RADIUS), origin.offset(BED_RADIUS, 3, BED_RADIUS))) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof BedBlock)
                    || state.getValue(BedBlock.PART) != BedPart.HEAD
                    || state.getValue(BedBlock.OCCUPIED)
                    || isTaken(pos)) {
                continue;
            }
            double distance = pos.distSqr(origin);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos.immutable();
            }
        }
        return best;
    }

    private static boolean isTaken(BlockPos pos) {
        for (Rest rest : RESTS.values()) {
            if (pos.equals(rest.bed())) {
                return true;
            }
        }
        return false;
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
