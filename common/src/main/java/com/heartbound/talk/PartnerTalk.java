package com.heartbound.talk;

import com.heartbound.behavior.FreezeManager;
import com.heartbound.behavior.RestManager;
import com.heartbound.config.HeartboundConfig;
import com.heartbound.gesture.MobMood;
import com.heartbound.gesture.Personality;
import com.heartbound.relationship.MemoryRecorder;
import com.heartbound.relationship.PairMemory;
import com.heartbound.relationship.RelationshipData;
import com.heartbound.talk.PartnerTalkRules.Outcome;
import com.heartbound.talk.PartnerTalkRules.Period;
import com.heartbound.talk.PartnerTalkRules.Prompt;
import com.heartbound.talk.PartnerTalkRules.Tone;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * The partner starts a conversation by itself: from time to time, when the player is near and not busy,
 * it says a line that fits the time of day and the situation and offers three answers in the chat
 * (clickable, they run {@code /heartbound reply}). The reaction depends on the answer and the character.
 * Everything is decided on the server and nothing is saved (pauses and open questions are transient).
 */
public final class PartnerTalk {

    public static final long ANSWER_TIME_TICKS = 1200L;
    private static final double TALK_DISTANCE_SQR = 10.0D * 10.0D;
    private static final double REPLY_DISTANCE_SQR = 16.0D * 16.0D;

    private record Pending(int token, UUID mob, long expires) {
    }

    /** Open questions by player. */
    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    /** Game tick before which a mob does not start a conversation. */
    private static final Map<UUID, Long> NEXT = new HashMap<>();
    private static final Map<UUID, Prompt> LAST_PROMPT = new HashMap<>();
    private static final Map<UUID, Integer> LAST_VARIANT = new HashMap<>();
    private static int nextToken = 1;

    private PartnerTalk() {
    }

    /** Called about once a second. */
    public static void tick(MinecraftServer server, RelationshipData data) {
        int seconds = HeartboundConfig.get().partnerTalkSeconds;
        if (seconds <= 0) {
            PENDING.clear();
            return;
        }
        long overworldNow = server.overworld().getGameTime();
        Iterator<Map.Entry<UUID, Pending>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            if (overworldNow > iterator.next().getValue().expires()) {
                iterator.remove();
            }
        }

        for (Map.Entry<UUID, UUID> entry : data.partnersSnapshot().entrySet()) {
            UUID mobId = entry.getKey();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getValue());
            if (player == null || PENDING.containsKey(player.getUUID())) {
                continue;
            }
            ServerLevel level = player.serverLevel();
            if (!(level.getEntity(mobId) instanceof Mob mob) || !mob.isAlive()) {
                continue;
            }
            long now = level.getGameTime();
            Long next = NEXT.get(mobId);
            if (next == null) {
                NEXT.put(mobId, now + 600L + level.random.nextInt(1200));
                continue;
            }
            if (now < next || !canTalk(player, mob)) {
                continue;
            }
            start(level, player, mob, now, seconds);
        }
    }

    private static boolean canTalk(ServerPlayer player, Mob mob) {
        return !player.isSpectator() && !player.isSleeping() && player.containerMenu == player.inventoryMenu
                && player.distanceToSqr(mob) < TALK_DISTANCE_SQR
                && !mob.isPassenger() && !mob.isSleeping()
                && !FreezeManager.isFrozen(mob.getUUID()) && !RestManager.isResting(mob.getUUID());
    }

    private static void start(ServerLevel level, ServerPlayer player, Mob mob, long now, int seconds) {
        UUID mobId = mob.getUUID();
        Prompt prompt = PartnerTalkRules.choose(Period.of(level.getDayTime()), MobMood.isUpset(mob),
                player.getHealth() <= 8.0F, LAST_PROMPT.get(mobId), level.random.nextInt(100));
        int variant = level.random.nextInt(PartnerTalkRules.OPEN_VARIANTS);
        if (prompt == LAST_PROMPT.get(mobId) && Integer.valueOf(variant).equals(LAST_VARIANT.get(mobId))) {
            variant = (variant + 1) % PartnerTalkRules.OPEN_VARIANTS;
        }
        LAST_PROMPT.put(mobId, prompt);
        LAST_VARIANT.put(mobId, variant);

        int token = nextToken++;
        PENDING.put(player.getUUID(), new Pending(token, mobId, now + ANSWER_TIME_TICKS));
        NEXT.put(mobId, now + seconds * 20L + level.random.nextInt(Math.max(1, seconds * 10)));
        FreezeManager.freezeUntil(mobId, now + 200L);

        TalkHandler.say(player, mob, PartnerTalkRules.openKey(prompt, variant));
        for (Tone tone : Tone.values()) {
            MutableComponent option = Component.literal("  \u00bb ").withStyle(ChatFormatting.GRAY).append(
                    Component.translatable(PartnerTalkRules.answerKey(prompt, tone)).withStyle(Style.EMPTY
                            .withColor(ChatFormatting.AQUA)
                            .withUnderlined(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/heartbound reply " + token + " " + tone.ordinal()))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Component.translatable("chat.heartbound.click")))));
            player.sendSystemMessage(option);
        }
    }

    /** The player clicked an answer. */
    public static void reply(ServerPlayer player, int token, int toneId) {
        Pending pending = PENDING.get(player.getUUID());
        Tone tone = Tone.byId(toneId);
        if (pending == null || pending.token() != token || tone == null || player.getServer() == null) {
            player.displayClientMessage(Component.translatable("chat.heartbound.expired"), true);
            return;
        }
        PENDING.remove(player.getUUID());
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (now > pending.expires() || !(level.getEntity(pending.mob()) instanceof Mob mob) || !mob.isAlive()
                || player.distanceToSqr(mob) > REPLY_DISTANCE_SQR) {
            player.displayClientMessage(Component.translatable("chat.heartbound.expired"), true);
            return;
        }

        UUID mobId = mob.getUUID();
        Personality personality = Personality.of(mobId);
        Outcome outcome = PartnerTalkRules.resolve(tone, personality, level.random.nextInt(100));
        int gain = PartnerTalkRules.gain(tone, outcome);
        RelationshipData data = RelationshipData.get(player.getServer());
        if (gain != 0) {
            int before = data.get(mobId, player.getUUID());
            int after = data.add(mobId, player.getUUID(), gain);
            MemoryRecorder.onAffinityChanged(player, mob, before, after);
        }
        MemoryRecorder.count(player, mobId, PairMemory.Counter.TALKS);
        FreezeManager.freezeUntil(mobId, now + 60L);

        if (outcome == Outcome.GOOD) {
            level.sendParticles(ParticleTypes.HEART, mob.getX(), mob.getY() + mob.getBbHeight() + 0.2D, mob.getZ(),
                    4, 0.3D, 0.2D, 0.3D, 0.02D);
        } else if (outcome == Outcome.BAD) {
            level.sendParticles(ParticleTypes.SMOKE, mob.getX(), mob.getY() + mob.getBbHeight() + 0.2D, mob.getZ(),
                    4, 0.3D, 0.2D, 0.3D, 0.02D);
        }
        TalkHandler.say(player, mob, PartnerTalkRules.reactKey(outcome, personality,
                level.random.nextInt(PartnerTalkRules.REACT_VARIANTS)));
    }
}
