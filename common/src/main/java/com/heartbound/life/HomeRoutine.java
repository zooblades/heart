package com.heartbound.life;

import com.heartbound.behavior.FreezeManager;
import com.heartbound.behavior.RestManager;
import com.heartbound.behavior.StageBehaviors;
import com.heartbound.config.ModGameRules;
import com.heartbound.date.DateManager;
import com.heartbound.relationship.Home;
import com.heartbound.relationship.RelationshipData;
import com.heartbound.talk.PartnerTalkRules.Period;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The daily routine of a partner that has a home point. By day it does its own things around the home
 * (small walks), in the evening it stays close to the home point, and at night it goes to a free bed near
 * the home and sleeps until the morning (villagers keep their own night routine; the partner stays up while
 * the player is awake nearby). When the player comes home, the partner greets them according to the time
 * of day. Transient: nothing is saved. Can be turned off with the game rule heartboundHomeRoutine.
 */
public final class HomeRoutine {

    private static final double WORK_RADIUS = 5.0D;
    private static final int BED_RADIUS = 5;
    private static final int BED_PATIENCE = 45;
    private static final long GREET_PAUSE_TICKS = 6000L;
    private static final double GREET_DISTANCE_SQR = 12.0D * 12.0D;

    private static final Map<UUID, BlockPos> ASLEEP = new HashMap<>();
    private static final Map<UUID, Integer> BED_TRIES = new HashMap<>();
    private static final Map<UUID, Long> NEXT_CHORE = new HashMap<>();
    private static final Map<UUID, Long> LAST_GREET = new HashMap<>();
    private static final Map<UUID, Boolean> PLAYER_AT_HOME = new HashMap<>();

    private HomeRoutine() {
    }

    public static boolean isAsleep(UUID mob) {
        return ASLEEP.containsKey(mob);
    }

    /** The bed the partner sleeps in because of the routine, or null. */
    public static BlockPos bedOf(UUID mob) {
        return ASLEEP.get(mob);
    }

    /** Wakes the partner if the routine put it to bed. */
    public static void wake(Entity entity) {
        UUID id = entity.getUUID();
        BED_TRIES.remove(id);
        if (ASLEEP.remove(id) != null && entity instanceof Mob mob && mob.isSleeping()) {
            mob.stopSleeping();
        }
    }

    /** Called about once a second. */
    public static void tick(MinecraftServer server, RelationshipData data) {
        if (ModGameRules.homeRoutine(server) == 0) {
            for (UUID id : new ArrayList<>(ASLEEP.keySet())) {
                Entity entity = find(server, id);
                if (entity != null) {
                    wake(entity);
                } else {
                    ASLEEP.remove(id);
                }
            }
            return;
        }
        for (Map.Entry<UUID, UUID> partner : data.partnersSnapshot().entrySet()) {
            UUID mobId = partner.getKey();
            Home home = data.getHome(mobId);
            if (home == null) {
                continue;
            }
            Entity found = find(server, mobId);
            if (!(found instanceof Mob mob) || !mob.isAlive() || !(mob.level() instanceof ServerLevel level)) {
                continue;
            }
            if (!level.dimension().location().toString().equals(home.dimension())) {
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(partner.getValue());
            routine(level, mob, home, player, data);
            if (player != null && player.level() == level) {
                greet(level, mob, home, player);
            }
        }
    }

    private static boolean busy(Mob mob, RelationshipData data) {
        UUID id = mob.getUUID();
        return mob.isPassenger() || FreezeManager.isFrozen(id) || DateManager.isOnDate(id)
                || StageBehaviors.isHoldingHands(id) || data.getFollowTarget(id) != null
                || RestManager.isResting(id) || (mob instanceof TamableAnimal tamable && tamable.isOrderedToSit());
    }

    private static void routine(ServerLevel level, Mob mob, Home home, ServerPlayer player, RelationshipData data) {
        UUID id = mob.getUUID();
        Period period = Period.of(level.getDayTime());
        boolean night = period == Period.NIGHT && !level.dimensionType().hasFixedTime();
        if (ASLEEP.containsKey(id)) {
            if (!night || !mob.isSleeping() || mob.hurtTime > 0) {
                wake(mob);
            }
            return;
        }
        if (!night) {
            BED_TRIES.remove(id);
        }
        if (busy(mob, data) || mob.isSleeping()) {
            return;
        }
        BlockPos homePos = BlockPos.of(home.pos());
        Vec3 center = Vec3.atBottomCenterOf(homePos);
        boolean playerAwakeNear = player != null && player.level() == level && !player.isSleeping()
                && player.distanceToSqr(mob) < 16.0D * 16.0D;
        if (night && mob instanceof Villager) {
            return; // villagers keep their own night routine
        }
        if (night && !playerAwakeNear) {
            goToBed(level, mob, homePos, center);
        } else if (night || period == Period.EVENING) {
            stayNear(mob, center);
        } else {
            chores(level, mob, homePos);
        }
    }

    private static void goToBed(ServerLevel level, Mob mob, BlockPos homePos, Vec3 center) {
        UUID id = mob.getUUID();
        BlockPos bed = findBed(level, homePos);
        if (bed == null) {
            stayNear(mob, center);
            return;
        }
        double distanceSqr = mob.distanceToSqr(bed.getX() + 0.5D, bed.getY(), bed.getZ() + 0.5D);
        int tries = BED_TRIES.merge(id, 1, Integer::sum);
        if (distanceSqr <= 4.0D || tries > BED_PATIENCE) {
            mob.getNavigation().stop();
            mob.startSleeping(bed);
            ASLEEP.put(id, bed);
            BED_TRIES.remove(id);
        } else if (LifeDirector.idle(mob)) {
            LifeDirector.moveTo(mob, bed.getX() + 0.5D, bed.getY(), bed.getZ() + 0.5D);
        }
    }

    private static void stayNear(Mob mob, Vec3 center) {
        if (mob.distanceToSqr(center) > 9.0D && LifeDirector.idle(mob)) {
            LifeDirector.moveTo(mob, center.x, center.y, center.z);
        }
    }

    private static void chores(ServerLevel level, Mob mob, BlockPos homePos) {
        UUID id = mob.getUUID();
        long now = level.getGameTime();
        if (now < NEXT_CHORE.getOrDefault(id, 0L) || !LifeDirector.idle(mob)) {
            return;
        }
        double dx = (level.random.nextDouble() * 2.0D - 1.0D) * WORK_RADIUS;
        double dz = (level.random.nextDouble() * 2.0D - 1.0D) * WORK_RADIUS;
        LifeDirector.moveTo(mob, homePos.getX() + 0.5D + dx, homePos.getY(), homePos.getZ() + 0.5D + dz);
        NEXT_CHORE.put(id, now + 300L + level.random.nextInt(500));
    }

    /** The nearest free bed (head part) around the home point that no other partner sleeps in, or null. */
    private static BlockPos findBed(ServerLevel level, BlockPos homePos) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(homePos.offset(-BED_RADIUS, -3, -BED_RADIUS),
                homePos.offset(BED_RADIUS, 3, BED_RADIUS))) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof BedBlock) || state.getValue(BedBlock.PART) != BedPart.HEAD
                    || state.getValue(BedBlock.OCCUPIED) || ASLEEP.containsValue(pos)) {
                continue;
            }
            double distance = pos.distSqr(homePos);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos.immutable();
            }
        }
        return best;
    }

    /** When the player comes home the partner notices and greets them (at most every five minutes). */
    private static void greet(ServerLevel level, Mob mob, Home home, ServerPlayer player) {
        UUID id = mob.getUUID();
        BlockPos homePos = BlockPos.of(home.pos());
        boolean near = player.distanceToSqr(Vec3.atCenterOf(homePos)) < GREET_DISTANCE_SQR;
        boolean was = PLAYER_AT_HOME.getOrDefault(id, false);
        PLAYER_AT_HOME.put(id, near);
        long now = level.getGameTime();
        if (!near || was || mob.isSleeping() || mob.isPassenger() || FreezeManager.isFrozen(id)
                || DateManager.isOnDate(id) || now - LAST_GREET.getOrDefault(id, -GREET_PAUSE_TICKS) < GREET_PAUSE_TICKS) {
            return;
        }
        LAST_GREET.put(id, now);
        Period period = Period.of(level.getDayTime());
        int variant = level.random.nextInt(2);
        player.sendSystemMessage(Component.translatable("dialogue.heartbound.format",
                mob.getName().copy().withStyle(ChatFormatting.YELLOW),
                Component.translatable(greetKey(period, variant))));
        mob.getLookControl().setLookAt(player, 30.0F, 30.0F);
        LifeDirector.moveTo(mob, player.getX(), player.getY(), player.getZ());
        level.sendParticles(ParticleTypes.HEART, mob.getX(), mob.getY() + mob.getBbHeight() + 0.2D, mob.getZ(),
                2, 0.3D, 0.2D, 0.3D, 0.02D);
    }

    public static String greetKey(Period period, int variant) {
        return "home.heartbound.greet." + period.name().toLowerCase(java.util.Locale.ROOT) + "." + variant;
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
