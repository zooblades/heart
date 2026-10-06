package com.heartbound.behavior;

import com.heartbound.relationship.RelationshipData;
import com.heartbound.relationship.RelationshipStage;
import com.heartbound.relationship.RomanceableMobs;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Behaviour that depends on the relationship stage. Driven by a single server tick hook (no mixins,
 * no custom goals), run every few ticks:
 * - Acquaintances and above look at you when you are near;
 * - Friends and above can follow you (toggled in the window);
 * - Close and above defend you against mobs that recently hurt you.
 */
public final class StageBehaviors {

    public static final int INTERVAL_TICKS = 10;
    public static final double NEAR_RADIUS = 6.0D;
    public static final double FOLLOW_STOP_DISTANCE_SQR = 3.0D * 3.0D;
    public static final double FOLLOW_TELEPORT_DISTANCE_SQR = 24.0D * 24.0D;
    public static final int DEFEND_WINDOW_TICKS = 100;

    private StageBehaviors() {
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % INTERVAL_TICKS != 0) {
            return;
        }
        RelationshipData data = RelationshipData.get(server);
        followTick(server, data);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            nearbyTick(player, data);
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
            if (mob.level() != player.level() || mob.isPassenger()) {
                continue;
            }
            if (mob instanceof TamableAnimal tamable && tamable.isOrderedToSit()) {
                continue;
            }

            double distanceSqr = mob.distanceToSqr(player);
            if (distanceSqr > FOLLOW_TELEPORT_DISTANCE_SQR) {
                mob.getNavigation().stop();
                mob.teleportTo(player.getX(), player.getY(), player.getZ());
            } else if (distanceSqr > FOLLOW_STOP_DISTANCE_SQR) {
                moveToward(mob, player);
            }
        }
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
        if (mob instanceof Villager || mob instanceof AbstractPiglin) {
            mob.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(player, 0.8F, 2));
        } else {
            mob.getNavigation().moveTo(player, 1.0D);
        }
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
