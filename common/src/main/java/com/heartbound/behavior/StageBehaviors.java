package com.heartbound.behavior;

import com.heartbound.config.HeartboundConfig;
import com.heartbound.life.LifeDirector;
import com.heartbound.relationship.Home;
import com.heartbound.relationship.RelationshipData;
import com.heartbound.relationship.RelationshipStage;
import com.heartbound.relationship.RomanceableMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Behaviour that depends on the relationship. Driven by a single server tick hook (no mixins,
 * no custom goals), run every few ticks:
 * - Acquaintances and above look at you when you are near;
 * - Friends and above can follow you (toggled in the window);
 * - Close and above defend you against mobs that recently hurt you;
 * - Partners can wait at a home point, and a night slept next to your partner gives a morning bonus.
 */
public final class StageBehaviors {

    public static final int INTERVAL_TICKS = 10;
    public static final double NEAR_RADIUS = 6.0D;
    public static final double FOLLOW_STOP_DISTANCE_SQR = 3.0D * 3.0D;
    public static final double HOME_RADIUS_SQR = 6.0D * 6.0D;
    public static final double HOME_TELEPORT_DISTANCE_SQR = 48.0D * 48.0D;
    public static final double SLEEP_NEAR_DISTANCE_SQR = 10.0D * 10.0D;
    public static final int DEFEND_WINDOW_TICKS = 100;

    /** Player to partner mob, for players who are asleep with their partner nearby. */
    private static final Map<UUID, UUID> SLEPT_NEAR_PARTNER = new HashMap<>();

    /** Mobs currently holding hands with a player (transient, not saved). */
    private static final Map<UUID, HandHold> HAND_HOLDS = new HashMap<>();

    private record HandHold(UUID player, long untilTick) {
    }

    private StageBehaviors() {
    }

    public static boolean isHoldingHands(UUID mob) {
        return HAND_HOLDS.containsKey(mob);
    }

    public static void releaseHands(UUID mob) {
        HAND_HOLDS.remove(mob);
    }

    /** Makes the mob walk right beside the player until the given game tick. */
    public static void holdHands(UUID mob, UUID player, long untilTick) {
        HAND_HOLDS.put(mob, new HandHold(player, untilTick));
    }

    public static void tick(MinecraftServer server) {
        FreezeManager.tick(server);
        if (server.getTickCount() % INTERVAL_TICKS != 0) {
            return;
        }
        RelationshipData data = RelationshipData.get(server);
        followTick(server, data);
        homeTick(server, data);
        handTick(server);
        partnerTick(server, data);
        if (server.getTickCount() % 20 == 0) {
            LifeDirector.tick(server, data);
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            nearbyTick(player, data);
            sleepTick(player, data);
        }
    }

    // ---- following

    private static void followTick(MinecraftServer server, RelationshipData data) {
        for (Map.Entry<UUID, UUID> entry : data.followingSnapshot().entrySet()) {
            UUID mobId = entry.getKey();
            UUID playerId = entry.getValue();

            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                continue; // offline: stays where it is, resumes when the player returns
            }
            Entity found = findEntity(server, mobId);
            if (found == null) {
                continue; // not loaded right now
            }
            if (!(found instanceof Mob mob) || !mob.isAlive()
                    || data.get(mobId, playerId) < RelationshipStage.FRIENDS.threshold()) {
                data.stopFollowing(mobId);
                continue;
            }
            if (mob.level() != player.level() || mob.isPassenger() || isSitting(mob)
                    || FreezeManager.isFrozen(mobId)) {
                continue;
            }

            double distanceSqr = mob.distanceToSqr(player);
            if (distanceSqr > followTeleportSqr()) {
                mob.getNavigation().stop();
                mob.teleportTo(player.getX(), player.getY(), player.getZ());
            } else if (distanceSqr > FOLLOW_STOP_DISTANCE_SQR) {
                moveToward(mob, player);
            }
        }
    }

    // ---- holding hands

    private static void handTick(MinecraftServer server) {
        if (HAND_HOLDS.isEmpty()) {
            return;
        }
        long now = server.overworld().getGameTime();
        for (Map.Entry<UUID, HandHold> entry : new HashMap<>(HAND_HOLDS).entrySet()) {
            HandHold hold = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(hold.player());
            Entity found = findEntity(server, entry.getKey());
            if (player == null || now > hold.untilTick()
                    || !(found instanceof Mob mob) || !mob.isAlive() || mob.isPassenger()
                    || mob.level() != player.level()) {
                HAND_HOLDS.remove(entry.getKey());
                continue;
            }
            if (FreezeManager.isFrozen(entry.getKey())) {
                continue;
            }
            double distanceSqr = mob.distanceToSqr(player);
            if (distanceSqr > 12.0D * 12.0D) {
                mob.getNavigation().stop();
                mob.teleportTo(player.getX(), player.getY(), player.getZ());
            } else if (distanceSqr > 2.0D * 2.0D) {
                moveToward(mob, player);
            }
        }
    }

    // ---- homes

    private static void homeTick(MinecraftServer server, RelationshipData data) {
        for (Map.Entry<UUID, Home> entry : data.homesSnapshot().entrySet()) {
            Entity found = findEntity(server, entry.getKey());
            if (!(found instanceof Mob mob) || !mob.isAlive() || mob.isPassenger() || isSitting(mob)
                    || FreezeManager.isFrozen(entry.getKey())) {
                continue;
            }
            Home home = entry.getValue();
            if (!mob.level().dimension().location().toString().equals(home.dimension())) {
                continue;
            }
            BlockPos pos = BlockPos.of(home.pos());
            double distanceSqr = mob.distanceToSqr(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
            if (distanceSqr > HOME_TELEPORT_DISTANCE_SQR) {
                mob.getNavigation().stop();
                mob.teleportTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
            } else if (distanceSqr > HOME_RADIUS_SQR) {
                moveToPos(mob, pos);
            }
        }
    }

    // ---- partners

    /** If a partner dies, the relationship ends. */
    private static void partnerTick(MinecraftServer server, RelationshipData data) {
        for (Map.Entry<UUID, UUID> entry : data.partnersSnapshot().entrySet()) {
            Entity found = findEntity(server, entry.getKey());
            if (found instanceof LivingEntity living && !living.isAlive()) {
                data.unpairMob(entry.getKey());
                data.clearHome(entry.getKey());
                data.stopFollowing(entry.getKey());
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getValue());
                if (player != null) {
                    player.displayClientMessage(
                            Component.translatable("message.heartbound.partner_died", found.getName()), false);
                }
            }
        }
    }

    /** Sleeping next to your partner and waking up in the morning gives a short bonus. */
    private static void sleepTick(ServerPlayer player, RelationshipData data) {
        UUID playerId = player.getUUID();
        if (player.isSleeping()) {
            UUID mobId = data.getPartnerOfPlayer(playerId);
            if (mobId != null) {
                Entity partner = player.serverLevel().getEntity(mobId);
                if (partner != null && partner.distanceToSqr(player) < SLEEP_NEAR_DISTANCE_SQR) {
                    SLEPT_NEAR_PARTNER.put(playerId, mobId);
                }
            }
        } else {
            UUID mobId = SLEPT_NEAR_PARTNER.remove(playerId);
            if (mobId != null && player.level().isDay()) {
                morning(player, mobId);
            }
        }
    }

    private static void morning(ServerPlayer player, UUID mobId) {
        ServerLevel level = player.serverLevel();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, HeartboundConfig.get().morningBonusSeconds * 20, 1));
        player.giveExperiencePoints(HeartboundConfig.get().morningBonusXp);
        Entity partner = level.getEntity(mobId);
        if (partner != null) {
            level.sendParticles(ParticleTypes.HEART,
                    partner.getX(), partner.getY() + partner.getBbHeight() + 0.2, partner.getZ(), 10, 0.4, 0.3, 0.4, 0.02);
            player.displayClientMessage(Component.translatable("message.heartbound.morning", partner.getName()), true);
        }
    }

    // ---- helpers

    private static double followTeleportSqr() {
        double distance = HeartboundConfig.get().followTeleportDistance;
        return distance * distance;
    }

    private static boolean isSitting(Mob mob) {
        return mob instanceof TamableAnimal tamable && tamable.isOrderedToSit();
    }

    private static Entity findEntity(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    /** Villagers and piglins run on the brain system, everything else on plain navigation. */
    private static void moveToward(Mob mob, Player player) {
        if (usesBrain(mob)) {
            mob.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(player, 0.8F, 2));
        } else {
            mob.getNavigation().moveTo(player, 1.0D);
        }
    }

    private static void moveToPos(Mob mob, BlockPos pos) {
        if (usesBrain(mob)) {
            mob.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(pos, 0.6F, 2));
        } else {
            mob.getNavigation().moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 1.0D);
        }
    }

    private static boolean usesBrain(Mob mob) {
        return mob instanceof Villager || mob instanceof AbstractPiglin;
    }

    // ---- looking and defending

    private static void nearbyTick(ServerPlayer player, RelationshipData data) {
        ServerLevel level = player.serverLevel();
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class,
                player.getBoundingBox().inflate(NEAR_RADIUS), RomanceableMobs::isEligible);
        if (mobs.isEmpty()) {
            return;
        }

        LivingEntity attacker = player.getLastHurtByMob();
        boolean underAttack = attacker != null
                && attacker.isAlive()
                && !(attacker instanceof Player)
                && player.tickCount - player.getLastHurtByMobTimestamp() < DEFEND_WINDOW_TICKS;

        for (Mob mob : mobs) {
            int affinity = data.get(mob.getUUID(), player.getUUID());
            if (affinity >= RelationshipStage.ACQUAINTED.threshold()) {
                mob.getLookControl().setLookAt(player, 30.0F, 30.0F);
            }
            if (underAttack && attacker != mob && affinity >= RelationshipStage.CLOSE.threshold()) {
                defend(mob, attacker);
            }
        }
    }

    private static void defend(Mob mob, LivingEntity attacker) {
        if (mob instanceof AbstractPiglin) {
            mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, attacker);
        } else if (!(mob instanceof Villager) && mob.getTarget() == null) {
            mob.setTarget(attacker);
        }
    }
}
