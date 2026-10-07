package com.heartbound.life;

import com.heartbound.behavior.StageBehaviors;
import com.heartbound.config.HeartboundConfig;
import com.heartbound.gesture.Personality;
import com.heartbound.relationship.RelationshipData;
import com.heartbound.relationship.RelationshipStage;
import com.heartbound.relationship.RomanceableMobs;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The "life director": once a second it gives mobs that know a nearby player a small activity
 * (wander, stay close, play, watch from afar, patrol) chosen by their character. How much happens at
 * once is limited by the life intensity in the config. Mobs that follow, hold hands, sleep, fight or
 * have a home are left alone, and everything is transient (nothing is saved).
 */
public final class LifeDirector {

    public static final double RADIUS = 16.0D;

    private record Task(Activity activity, UUID player, long until) {
    }

    private static final Map<UUID, Task> TASKS = new HashMap<>();
    private static final Map<UUID, Long> NEXT_ALLOWED = new HashMap<>();

    private LifeDirector() {
    }

    public static void tick(MinecraftServer server, RelationshipData data) {
        int level = LifeLevel.clamp(HeartboundConfig.get().lifeIntensity);
        if (level == 0) {
            TASKS.clear();
            return;
        }
        long now = server.overworld().getGameTime();

        for (Map.Entry<UUID, Task> entry : new HashMap<>(TASKS).entrySet()) {
            UUID mobId = entry.getKey();
            Task task = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(task.player());
            Entity found = player == null ? null : player.serverLevel().getEntity(mobId);
            if (player == null || !(found instanceof Mob mob) || !mob.isAlive()
                    || now >= task.until() || !allowed(mob, data, player)) {
                TASKS.remove(mobId);
                int pause = LifeLevel.pauseTicks(level);
                NEXT_ALLOWED.put(mobId, now + pause + server.overworld().random.nextInt(pause / 2 + 1));
                continue;
            }
            step(player, mob, task, data);
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            startTasks(player, data, now, level);
        }
    }

    private static boolean allowed(Mob mob, RelationshipData data, ServerPlayer player) {
        UUID id = mob.getUUID();
        return !mob.isSleeping()
                && !mob.isPassenger()
                && mob.getTarget() == null
                && data.getFollowTarget(id) == null
                && data.getHome(id) == null
                && !StageBehaviors.isHoldingHands(id)
                && data.get(id, player.getUUID()) >= RelationshipStage.ACQUAINTED.threshold()
                && !(mob instanceof TamableAnimal tamable && tamable.isOrderedToSit())
                && !(mob instanceof Villager && mob.level().isNight());
    }

    private static void startTasks(ServerPlayer player, RelationshipData data, long now, int level) {
        int max = LifeLevel.maxActive(level);
        int active = 0;
        for (Task task : TASKS.values()) {
            if (task.player().equals(player.getUUID())) {
                active++;
            }
        }
        if (active >= max) {
            return;
        }
        ServerLevel serverLevel = player.serverLevel();
        List<Mob> candidates = serverLevel.getEntitiesOfClass(Mob.class,
                player.getBoundingBox().inflate(RADIUS), RomanceableMobs::isEligible);
        for (Mob mob : candidates) {
            if (active >= max) {
                break;
            }
            UUID id = mob.getUUID();
            if (TASKS.containsKey(id) || now < NEXT_ALLOWED.getOrDefault(id, 0L) || !allowed(mob, data, player)) {
                continue;
            }
            if (serverLevel.random.nextInt(100) >= LifeLevel.startChancePercent(level)) {
                continue;
            }
            List<Activity> options = Activity.forPersonality(Personality.of(id));
            Activity activity = options.get(serverLevel.random.nextInt(options.size()));
            TASKS.put(id, new Task(activity, player.getUUID(), now + 200 + serverLevel.random.nextInt(200)));
            active++;
        }
    }

    private static void step(ServerPlayer player, Mob mob, Task task, RelationshipData data) {
        RandomSource random = mob.getRandom();
        double distanceSqr = mob.distanceToSqr(player);
        switch (task.activity()) {
            case WANDER -> {
                if (idle(mob)) {
                    walkAround(mob, player, 3.0D, 7.0D, random);
                }
            }
            case STAY_CLOSE -> {
                if (distanceSqr > 9.0D && idle(mob)) {
                    walkAround(mob, player, 1.5D, 2.5D, random);
                } else {
                    mob.getLookControl().setLookAt(player, 30.0F, 30.0F);
                }
                if (data.get(mob.getUUID(), player.getUUID()) >= RelationshipStage.CLOSE.threshold()
                        && random.nextInt(8) == 0) {
                    particle(mob, ParticleTypes.HEART);
                }
            }
            case PLAY -> {
                if (idle(mob)) {
                    walkAround(mob, player, 2.0D, 6.0D, random);
                }
                if (random.nextInt(3) == 0) {
                    mob.getJumpControl().jump();
                }
                if (random.nextInt(4) == 0) {
                    particle(mob, ParticleTypes.NOTE);
                }
            }
            case WATCH_FROM_AFAR -> {
                mob.getLookControl().setLookAt(player, 30.0F, 30.0F);
                if (distanceSqr < 12.0D && idle(mob)) {
                    walkAround(mob, player, 5.0D, 6.5D, random);
                } else if (distanceSqr > 64.0D && idle(mob)) {
                    walkAround(mob, player, 4.0D, 5.0D, random);
                }
            }
            case PATROL -> {
                if (idle(mob)) {
                    walkAround(mob, player, 5.0D, 9.0D, random);
                }
            }
        }
    }

    private static void particle(Mob mob, net.minecraft.core.particles.ParticleOptions particle) {
        if (mob.level() instanceof ServerLevel level) {
            level.sendParticles(particle, mob.getX(), mob.getY() + mob.getBbHeight() + 0.2D, mob.getZ(),
                    1, 0.2D, 0.1D, 0.2D, 0.0D);
        }
    }

    private static boolean usesBrain(Mob mob) {
        return mob instanceof Villager || mob instanceof AbstractPiglin;
    }

    /** True when the mob is not busy walking somewhere. */
    private static boolean idle(Mob mob) {
        return usesBrain(mob)
                ? !mob.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)
                : mob.getNavigation().isDone();
    }

    private static void walkAround(Mob mob, ServerPlayer player, double minRadius, double maxRadius, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double radius = minRadius + random.nextDouble() * (maxRadius - minRadius);
        moveTo(mob, player.getX() + Math.cos(angle) * radius, mob.getY(), player.getZ() + Math.sin(angle) * radius);
    }

    private static void moveTo(Mob mob, double x, double y, double z) {
        if (usesBrain(mob)) {
            mob.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new Vec3(x, y, z), 0.6F, 1));
        } else {
            mob.getNavigation().moveTo(x, y, z, 1.0D);
        }
    }
}
