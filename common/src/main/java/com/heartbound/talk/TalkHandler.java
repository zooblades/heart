package com.heartbound.talk;

import com.heartbound.gesture.MobMood;
import com.heartbound.gesture.Personality;
import com.heartbound.relationship.CooldownTracker;
import com.heartbound.relationship.RelationshipData;
import com.heartbound.relationship.RomanceableMobs;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

/**
 * Server side of conversations. The client only says "this topic"; everything is decided here:
 * topics can be locked, answers depend on the mob's character, flirting and jokes can fail,
 * talking gives less each time during a day, and a wounded mob is comforted by "How are you?".
 * Replies are translation keys, so every player reads them in their own language.
 */
public final class TalkHandler {

    private static final double MAX_DISTANCE_SQR = 6.0D * 6.0D;
    private static final int COOLDOWN_TICKS = 40;
    private static final int COMFORT_GAIN = 6;
    private static final int FLIRT_FAIL_PENALTY = 2;

    private static final CooldownTracker COOLDOWNS = new CooldownTracker();
    private static final TalkLimiter LIMITER = new TalkLimiter();

    private TalkHandler() {
    }

    public static void perform(ServerPlayer player, int entityId, int topicId) {
        Topic topic = Topic.byId(topicId);
        Entity entity = player.serverLevel().getEntity(entityId);
        if (topic == null || entity == null || player.isSpectator() || player.getServer() == null
                || !RomanceableMobs.isEligible(entity) || player.distanceToSqr(entity) > MAX_DISTANCE_SQR) {
            return;
        }
        ServerLevel level = player.serverLevel();
        RelationshipData data = RelationshipData.get(player.getServer());
        UUID mobId = entity.getUUID();
        UUID playerId = player.getUUID();
        long now = level.getGameTime();
        int affinity = data.get(mobId, playerId);
        boolean partner = data.isPartner(mobId, playerId);
        Personality personality = Personality.of(mobId);
        int variant = level.random.nextInt(DialogueBook.VARIANTS);

        if (affinity < topic.minAffinity()) {
            player.displayClientMessage(Component.translatable("message.heartbound.talk_locked"), true);
            return;
        }
        if (!COOLDOWNS.tryUse(playerId, mobId, now, COOLDOWN_TICKS)) {
            return;
        }

        int factor = TalkRules.gainFactorPercent(LIMITER.countToday(mobId, playerId, now));
        if (factor == 0) {
            say(player, entity, DialogueBook.tiredKey(personality, variant));
            return;
        }
        LIMITER.record(mobId, playerId, now);

        boolean ok = true;
        int gain;
        String key;
        if (topic == Topic.HOW_ARE_YOU && MobMood.isUpset(entity)) {
            key = DialogueBook.comfortKey(personality, variant);
            gain = COMFORT_GAIN;
        } else {
            ok = level.random.nextInt(100) < TalkRules.chance(topic, affinity, partner, personality);
            key = DialogueBook.key(topic, ok, personality, variant);
            String mobPath = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
            if (ok && topic == Topic.ABOUT_YOU && DialogueBook.hasSpecies(mobPath) && level.random.nextBoolean()) {
                key = DialogueBook.speciesKey(mobPath, level.random.nextInt(DialogueBook.SPECIES_VARIANTS));
            }
            gain = ok ? topic.gain() : (topic == Topic.FLIRT ? -FLIRT_FAIL_PENALTY : 0);
        }
        if (gain > 0) {
            gain = Math.max(1, gain * factor / 100);
        }
        if (gain != 0) {
            data.add(mobId, playerId, gain);
        }

        if (ok && topic == Topic.ABOUT_YOU) {
            int known = data.getKnowledge(mobId, playerId);
            if (known < 2) {
                data.setKnowledge(mobId, playerId, known + 1);
                player.displayClientMessage(Component.translatable("message.heartbound.learned", entity.getName()), true);
            }
        }
        if (ok && (topic == Topic.FLIRT || topic == Topic.COMPLIMENT)) {
            level.sendParticles(ParticleTypes.HEART,
                    entity.getX(), entity.getY() + entity.getBbHeight() + 0.2D, entity.getZ(), 3, 0.3D, 0.2D, 0.3D, 0.02D);
        }
        say(player, entity, key);
    }

    private static void say(ServerPlayer player, Entity entity, String key) {
        player.sendSystemMessage(Component.translatable("dialogue.heartbound.format",
                entity.getName().copy().withStyle(ChatFormatting.YELLOW), Component.translatable(key)));
    }
}
