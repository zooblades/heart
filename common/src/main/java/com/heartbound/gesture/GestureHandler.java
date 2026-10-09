package com.heartbound.gesture;

import com.heartbound.behavior.FreezeManager;
import com.heartbound.behavior.RestManager;
import com.heartbound.behavior.StageBehaviors;
import com.heartbound.config.HeartboundConfig;
import com.heartbound.network.RadialInfoPayload;
import com.heartbound.relationship.CooldownTracker;
import com.heartbound.relationship.EventType;
import com.heartbound.relationship.MemoryRecorder;
import com.heartbound.relationship.PairMemory;
import com.heartbound.relationship.RelationshipData;
import com.heartbound.relationship.RomanceableMobs;
import com.heartbound.talk.PartnerTalk;
import com.heartbound.talk.PartnerTalkRules;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import java.util.Locale;
import java.util.UUID;

/**
 * Server side of the gesture menu: answers "what can I do with this mob" and performs gestures.
 * The mob can refuse, depending on affinity, partner status and its character; repeated refusals
 * offend it for a while. Everything is validated here: the client only asks.
 */
public final class GestureHandler {

    private static final double MAX_DISTANCE_SQR = 6.0D * 6.0D;

    private static final CooldownTracker COOLDOWNS = new CooldownTracker();
    private static final MoodTracker MOODS = new MoodTracker();

    private GestureHandler() {
    }

    private static Entity find(ServerPlayer player, int entityId) {
        Entity entity = player.serverLevel().getEntity(entityId);
        if (entity == null || player.isSpectator() || !RomanceableMobs.isEligible(entity)
                || player.distanceToSqr(entity) > MAX_DISTANCE_SQR) {
            return null;
        }
        return entity;
    }

    /** Sends the client what it needs to draw the menu. */
    public static void requestRadial(ServerPlayer player, int entityId) {
        Entity entity = find(player, entityId);
        if (entity == null || player.getServer() == null) {
            return;
        }
        RelationshipData data = RelationshipData.get(player.getServer());
        int affinity = data.get(entity.getUUID(), player.getUUID());
        boolean partner = data.isPartner(entity.getUUID(), player.getUUID());
        FreezeManager.freezeUntil(entity.getUUID(), player.serverLevel().getGameTime() + 80L);
        player.connection.send(new ClientboundCustomPayloadPacket(
                new RadialInfoPayload(entity.getId(), affinity, partner,
                        StageBehaviors.isHoldingHands(entity.getUUID()))));
    }

    public static void perform(ServerPlayer player, int entityId, int gestureId) {
        Entity entity = find(player, entityId);
        Gesture gesture = Gesture.byId(gestureId);
        if (entity == null || gesture == null || player.getServer() == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        RelationshipData data = RelationshipData.get(player.getServer());
        UUID mobId = entity.getUUID();
        UUID playerId = player.getUUID();
        FreezeManager.freezeUntil(mobId, level.getGameTime() + 40L);
        if (gesture == Gesture.HOLD_HANDS && StageBehaviors.isHoldingHands(mobId)) {
            StageBehaviors.releaseHands(mobId);
            player.displayClientMessage(Component.translatable("gesture.heartbound.released", entity.getName()), true);
            return;
        }
        if (gesture == Gesture.LIE_DOWN && RestManager.getUp(entity)) {
            player.displayClientMessage(Component.translatable("gesture.heartbound.got_up", entity.getName()), true);
            return;
        }
        int affinity = data.get(mobId, playerId);
        boolean partner = data.isPartner(mobId, playerId);

        if (!gesture.implemented() || !gesture.isUnlocked(affinity, partner)) {
            player.displayClientMessage(Component.translatable("gesture.heartbound.locked"), true);
            return;
        }
        long now = level.getGameTime();
        if (MOODS.isOffended(mobId, playerId, now)) {
            player.displayClientMessage(Component.translatable("gesture.heartbound.offended", entity.getName()), true);
            return;
        }
        if (gesture == Gesture.LIE_DOWN) {
            RestManager.Check check = RestManager.check(player, entity);
            if (check != RestManager.Check.OK) {
                player.displayClientMessage(Component.translatable(
                        "gesture.heartbound.lie_down." + check.name().toLowerCase(Locale.ROOT), entity.getName()), true);
                return;
            }
        }
        if (!COOLDOWNS.tryUse(playerId, mobId, now, HeartboundConfig.get().gestureCooldownTicks)) {
            return;
        }

        Personality personality = Personality.of(mobId);
        int chance = GestureRules.chance(gesture, affinity, partner, personality);
        if (level.random.nextInt(100) < chance) {
            succeed(level, player, entity, data, gesture, now);
        } else {
            refuse(level, player, entity, data, personality, now);
        }
    }

    private static void succeed(ServerLevel level, ServerPlayer player, Entity entity,
                                RelationshipData data, Gesture gesture, long now) {
        UUID mobId = entity.getUUID();
        UUID playerId = player.getUUID();
        if (gesture == Gesture.LIE_DOWN) {
            Component problem = entity instanceof Mob rester ? RestManager.start(level, player, rester)
                    : Component.translatable("gesture.heartbound.lie_down.busy", entity.getName());
            if (problem != null) {
                player.displayClientMessage(problem, true);
                return;
            }
        }
        MOODS.recordSuccess(mobId, playerId);
        boolean comfort = MobMood.isUpset(entity) && gesture != Gesture.LIE_DOWN;
        int before = data.get(mobId, playerId);
        int after = data.add(mobId, playerId, gesture.gain() * (comfort ? 2 : 1));
        MemoryRecorder.onAffinityChanged(player, entity, before, after);
        MemoryRecorder.count(player, mobId, PairMemory.Counter.GESTURES);
        if (gesture == Gesture.LIE_DOWN) {
            PartnerTalk.note(mobId, PartnerTalkRules.Prompt.TOGETHER, now);
        } else if (gesture != Gesture.HOLD_HANDS) {
            PartnerTalk.note(mobId, PartnerTalkRules.Prompt.CLOSE, now);
        }
        if (gesture == Gesture.HUG) {
            MemoryRecorder.record(player, mobId, entity.blockPosition().asLong(), EventType.FIRST_HUG, 0);
        } else if (gesture == Gesture.KISS) {
            MemoryRecorder.record(player, mobId, entity.blockPosition().asLong(), EventType.FIRST_KISS, 0);
        } else if (gesture == Gesture.LIE_DOWN) {
            MemoryRecorder.record(player, mobId, entity.blockPosition().asLong(), EventType.FIRST_LIE_DOWN, 0);
        }

        if (entity instanceof Mob mob) {
            mob.getLookControl().setLookAt(player, 30.0F, 30.0F);
            if (gesture == Gesture.HUG || gesture == Gesture.KISS) {
                mob.getJumpControl().jump();
            }
        }
        int hearts = switch (gesture) {
            case HOLD_HANDS -> 4;
            case HUG -> 10;
            case CHEEK_KISS -> 8;
            case KISS -> 16;
            case LIE_DOWN -> 6;
            default -> 3;
        };
        burst(level, entity, ParticleTypes.HEART, hearts);
        sound(level, entity, gesture == Gesture.KISS ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.VILLAGER_YES);

        if (gesture == Gesture.KISS) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
        }
        if (gesture == Gesture.HOLD_HANDS) {
            StageBehaviors.holdHands(mobId, playerId, now + HeartboundConfig.get().holdHandsSeconds * 20L);
        }
        player.displayClientMessage(Component.translatable(
                comfort ? "gesture.heartbound.comfort" : "gesture.heartbound.success." + gesture.key(),
                entity.getName()), true);
    }

    private static void refuse(ServerLevel level, ServerPlayer player, Entity entity,
                               RelationshipData data, Personality personality, long now) {
        UUID mobId = entity.getUUID();
        UUID playerId = player.getUUID();
        MOODS.recordRefusal(mobId, playerId, now);
        PartnerTalk.note(mobId, PartnerTalkRules.Prompt.QUARREL, now);
        data.add(mobId, playerId, -HeartboundConfig.get().gesturePenalty);
        burst(level, entity, ParticleTypes.SMOKE, 5);
        player.displayClientMessage(
                Component.translatable("gesture.heartbound.refused." + personality.key(), entity.getName()), true);
    }

    private static void burst(ServerLevel level, Entity entity, ParticleOptions particle, int count) {
        level.sendParticles(particle,
                entity.getX(), entity.getY() + entity.getBbHeight() + 0.2, entity.getZ(),
                count, 0.3, 0.2, 0.3, 0.02);
    }

    private static void sound(ServerLevel level, Entity entity, SoundEvent sound) {
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.NEUTRAL, 1.0F, 1.1F);
    }
}
