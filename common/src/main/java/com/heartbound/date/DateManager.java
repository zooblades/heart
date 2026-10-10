package com.heartbound.date;

import com.heartbound.behavior.FreezeManager;
import com.heartbound.date.DateRules.DateType;
import com.heartbound.date.DateRules.FailReason;
import com.heartbound.relationship.EventType;
import com.heartbound.relationship.Home;
import com.heartbound.relationship.MemoryRecorder;
import com.heartbound.relationship.RelationshipData;
import com.heartbound.talk.PartnerTalk;
import com.heartbound.talk.PartnerTalkRules;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Runs the dates after the player accepted an invitation. A walk: the partner walks with the player for a
 * while and it counts if they stayed together. A place or home date: the partner goes to a viewpoint or
 * to the home point (waiting for the player when it gets ahead), then they stay there together for a
 * while. A success improves the relationship and starts new conversations; if the player goes away the
 * relationship suffers a little. Transient: nothing is saved. Other behaviours skip the mob on a date.
 */
public final class DateManager {

    private static final class Date {
        final UUID player;
        final DateType type;
        final BlockPos target;
        final long start;
        int together;
        int total;
        boolean arrived;
        int stay;
        int lines;

        Date(UUID player, DateType type, BlockPos target, long start) {
            this.player = player;
            this.type = type;
            this.target = target;
            this.start = start;
        }
    }

    private static final Map<UUID, Date> DATES = new HashMap<>();

    private DateManager() {
    }

    public static boolean isOnDate(UUID mob) {
        return DATES.containsKey(mob);
    }

    /** Starts a date; returns false if there is nowhere to go. */
    public static boolean start(ServerLevel level, ServerPlayer player, Mob mob, DateType type, RelationshipData data) {
        if (DATES.containsKey(mob.getUUID())) {
            return false;
        }
        BlockPos target = null;
        if (type == DateType.PLACE) {
            target = findViewpoint(level, mob);
        } else if (type == DateType.HOME) {
            target = homeOf(level, data, mob);
        }
        if (type != DateType.WALK && target == null) {
            return false;
        }
        DATES.put(mob.getUUID(), new Date(player.getUUID(), type, target, level.getGameTime()));
        player.sendSystemMessage(Component.translatable(DateRules.startKey(type), mob.getName())
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        return true;
    }

    /** Called about once a second. */
    public static void tick(MinecraftServer server, RelationshipData data) {
        if (DATES.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, Date> entry : new HashMap<>(DATES).entrySet()) {
            UUID mobId = entry.getKey();
            Date date = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(date.player);
            Entity found = find(server, mobId);
            if (!(found instanceof Mob mob) || !mob.isAlive() || player == null) {
                DATES.remove(mobId);
                continue;
            }
            ServerLevel level = (ServerLevel) mob.level();
            if (player.level() != level || mob.isPassenger() || mob.isSleeping()) {
                finish(data, player, mob, date, FailReason.LOST);
                continue;
            }
            if (mob.hurtTime > 0) {
                finish(data, player, mob, date, FailReason.HURT);
                continue;
            }
            double distanceSqr = mob.distanceToSqr(player);
            if (distanceSqr > square(DateRules.ABANDON_DISTANCE)) {
                finish(data, player, mob, date, FailReason.ABANDONED);
                continue;
            }
            long now = level.getGameTime();
            long age = now - date.start;
            boolean near = distanceSqr <= square(DateRules.TOGETHER_DISTANCE);
            date.total += 20;
            if (near) {
                date.together += 20;
            }
            if (age > DateRules.TIMEOUT_TICKS) {
                finish(data, player, mob, date, date.arrived ? FailReason.ABANDONED : FailReason.LOST);
                continue;
            }
            if (date.type == DateType.WALK) {
                walk(data, level, player, mob, date, distanceSqr, age);
            } else {
                visit(data, level, player, mob, date, distanceSqr, now);
            }
        }
    }

    private static void walk(RelationshipData data, ServerLevel level, ServerPlayer player, Mob mob, Date date,
                             double distanceSqr, long age) {
        if (age >= DateRules.WALK_TICKS) {
            finish(data, player, mob, date,
                    DateRules.walkSucceeded(date.together, date.total) ? null : FailReason.ABANDONED);
            return;
        }
        if (distanceSqr > 25.0D) {
            mob.getNavigation().moveTo(player, 1.0D);
        } else {
            mob.getNavigation().stop();
        }
        if (date.lines < DateRules.WALK_LINES && age >= (date.lines + 1) * 500L) {
            talk(player, mob, DateRules.walkKey(date.lines));
            date.lines++;
        }
    }

    private static void visit(RelationshipData data, ServerLevel level, ServerPlayer player, Mob mob, Date date,
                              double distanceSqr, long now) {
        BlockPos target = date.target;
        if (!date.arrived) {
            double toTarget = mob.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
            double playerToTarget = player.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
            if (toTarget <= square(DateRules.ARRIVE_MOB_DISTANCE)
                    && playerToTarget <= square(DateRules.ARRIVE_PLAYER_DISTANCE)) {
                date.arrived = true;
                mob.getNavigation().stop();
                talk(player, mob, DateRules.arriveKey(date.type));
            } else if (distanceSqr > square(DateRules.WAIT_DISTANCE)) {
                mob.getNavigation().stop(); // wait for the player to catch up
            } else {
                mob.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 1.0D);
            }
            return;
        }
        FreezeManager.freezeUntil(mob.getUUID(), now + 30L);
        if (distanceSqr <= square(DateRules.TOGETHER_DISTANCE)) {
            date.stay += 20;
        }
        if (date.stay >= DateRules.STAY_TICKS) {
            finish(data, player, mob, date, null);
        }
    }

    /** reason == null: success. */
    private static void finish(RelationshipData data, ServerPlayer player, Mob mob, Date date, FailReason reason) {
        UUID mobId = mob.getUUID();
        DATES.remove(mobId);
        ServerLevel level = (ServerLevel) mob.level();
        int gain = reason == null ? DateRules.successGain(date.type) : DateRules.failGain(reason);
        if (gain != 0) {
            int before = data.get(mobId, player.getUUID());
            int after = data.add(mobId, player.getUUID(), gain);
            MemoryRecorder.onAffinityChanged(player, mob, before, after);
        }
        if (reason == null) {
            MemoryRecorder.record(player, mobId, mob.blockPosition().asLong(), EventType.FIRST_DATE, date.type.ordinal());
            PartnerTalk.note(mobId, PartnerTalkRules.Prompt.DATE_DONE, level.getGameTime());
            level.sendParticles(ParticleTypes.HEART, mob.getX(), mob.getY() + mob.getBbHeight() + 0.2D, mob.getZ(),
                    8, 0.4D, 0.3D, 0.4D, 0.02D);
            talk(player, mob, DateRules.successKey(date.type));
        } else {
            player.sendSystemMessage(Component.translatable(DateRules.failKey(reason), mob.getName())
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }

    private static void talk(ServerPlayer player, Mob mob, String key) {
        player.sendSystemMessage(Component.translatable("dialogue.heartbound.format",
                mob.getName().copy().withStyle(ChatFormatting.YELLOW), Component.translatable(key)));
    }

    private static double square(double value) {
        return value * value;
    }

    private static BlockPos homeOf(ServerLevel level, RelationshipData data, Mob mob) {
        Home home = data.getHome(mob.getUUID());
        if (home == null || !home.dimension().equals(level.dimension().location().toString())) {
            return null;
        }
        return BlockPos.of(home.pos());
    }

    /** The highest solid spot found by sampling places 20 to 45 blocks away; null if none is loaded. */
    private static BlockPos findViewpoint(ServerLevel level, Mob mob) {
        BlockPos origin = mob.blockPosition();
        BlockPos best = null;
        for (int i = 0; i < 24; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            int distance = 20 + level.random.nextInt(26);
            int x = origin.getX() + (int) Math.round(Math.cos(angle) * distance);
            int z = origin.getZ() + (int) Math.round(Math.sin(angle) * distance);
            if (!level.hasChunkAt(new BlockPos(x, origin.getY(), z))) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            BlockPos below = pos.below();
            if (!level.getFluidState(below).isEmpty() || !level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                continue;
            }
            if (best == null || y > best.getY()) {
                best = pos;
            }
        }
        return best;
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
