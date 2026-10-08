package com.heartbound.interaction;

import com.heartbound.config.HeartboundConfig;
import com.heartbound.config.ModConfig;
import com.heartbound.item.GiftItem;
import com.heartbound.item.RingItem;
import com.heartbound.menu.RelationshipMenu;
import com.heartbound.relationship.CooldownTracker;
import com.heartbound.relationship.Gender;
import com.heartbound.relationship.EventType;
import com.heartbound.relationship.GiftPreferences;
import com.heartbound.relationship.MemoryRecorder;
import com.heartbound.relationship.PairMemory;
import com.heartbound.relationship.Home;
import com.heartbound.relationship.RelationshipData;
import com.heartbound.relationship.RelationshipStage;
import com.heartbound.relationship.RomanceableMobs;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Locale;
import java.util.UUID;

/**
 * Handles right-clicking a mob:
 * - sneaking with an empty hand: pets the mob;
 * - holding a gift or a ring: gives the gift or proposes.
 * The relationship window itself is opened with a key (see {@link #openFromKey}).
 * Each loader calls {@link #onUseEntity} from its own "player uses entity" event.
 * Returns PASS when the interaction is not ours, so vanilla behaviour is untouched.
 */
public final class InteractionHandler {


    private static ModConfig cfg() {
        return HeartboundConfig.get();
    }

    private static final CooldownTracker GIFT_COOLDOWNS = new CooldownTracker();
    private static final CooldownTracker PET_COOLDOWNS = new CooldownTracker();

    private InteractionHandler() {
    }

    public static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand, Entity target) {
        if (hand != InteractionHand.MAIN_HAND || player.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (!RomanceableMobs.isEligible(target)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof RingItem) {
            if (player instanceof ServerPlayer serverPlayer) {
                propose(serverPlayer, target, stack);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.getItem() instanceof GiftItem gift) {
            if (player instanceof ServerPlayer serverPlayer) {
                giveGift(serverPlayer, target, stack, gift);
            }
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown() && stack.isEmpty()) {
            if (player instanceof ServerPlayer serverPlayer) {
                pet(serverPlayer, target);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /** Opens the window for the entity the player pressed the key at, after checking range and eligibility. */
    public static void openFromKey(ServerPlayer player, int entityId) {
        Entity entity = player.serverLevel().getEntity(entityId);
        if (entity == null || player.isSpectator() || !RomanceableMobs.isEligible(entity)
                || player.distanceToSqr(entity) > 64.0D) {
            return;
        }
        openWindow(player, entity);
    }

    private static void openWindow(ServerPlayer player, Entity target) {
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, p) -> new RelationshipMenu(containerId, inventory, target),
                Component.translatable("screen.heartbound.relationship")));
    }

    /** Gives the gift in the player's main hand (used by the window button). */
    public static boolean giveHeldGift(ServerPlayer player, Entity target) {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof GiftItem gift) {
            return giveGift(player, target, stack, gift);
        }
        player.displayClientMessage(Component.translatable("message.heartbound.need_gift"), true);
        return false;
    }

    public static boolean giveGift(ServerPlayer player, Entity target, ItemStack stack, GiftItem gift) {
        ServerLevel level = player.serverLevel();
        if (!RomanceableMobs.isEligible(target)) {
            return false;
        }
        if (!GIFT_COOLDOWNS.tryUse(player.getUUID(), target.getUUID(), level.getGameTime(), cfg().giftCooldownTicks)) {
            return false;
        }
        String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).getPath();
        int gain = GiftPreferences.gain(mobId, Gender.of(target.getUUID()), gift.kind(), cfg().giftGains);
        applyGain(level, player, target, gain, 6 + gain / 10);
        MemoryRecorder.record(player, target.getUUID(), target.blockPosition().asLong(),
                EventType.FIRST_GIFT, gift.kind().ordinal());
        MemoryRecorder.count(player, target.getUUID(), PairMemory.Counter.GIFTS);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return true;
    }

    public static boolean pet(ServerPlayer player, Entity target) {
        ServerLevel level = player.serverLevel();
        if (!RomanceableMobs.isEligible(target)) {
            return false;
        }
        if (!PET_COOLDOWNS.tryUse(player.getUUID(), target.getUUID(), level.getGameTime(), cfg().petCooldownTicks)) {
            return false;
        }
        applyGain(level, player, target, cfg().petGain, 3);
        return true;
    }

    /** Proposes with the ring in the player's main hand (used by the window button). */
    public static boolean proposeWithHeldRing(ServerPlayer player, Entity target) {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof RingItem) {
            return propose(player, target, stack);
        }
        player.displayClientMessage(Component.translatable("message.heartbound.need_ring"), true);
        return false;
    }

    /** Proposal: needs enough affinity, a free mob and a free player. Consumes the ring on success. */
    public static boolean propose(ServerPlayer player, Entity target, ItemStack ring) {
        ServerLevel level = player.serverLevel();
        if (!RomanceableMobs.isEligible(target) || player.getServer() == null) {
            return false;
        }
        RelationshipData data = RelationshipData.get(player.getServer());
        UUID mobId = target.getUUID();
        UUID playerId = player.getUUID();

        if (data.isPartner(mobId, playerId)) {
            player.displayClientMessage(Component.translatable("message.heartbound.already_together", target.getName()), true);
            return false;
        }
        if (data.getPartnerOfPlayer(playerId) != null) {
            player.displayClientMessage(Component.translatable("message.heartbound.you_have_partner"), true);
            return false;
        }
        if (data.getPartnerOfMob(mobId) != null) {
            player.displayClientMessage(Component.translatable("message.heartbound.mob_taken", target.getName()), true);
            return false;
        }
        if (!RelationshipStage.canPropose(data.get(mobId, playerId))) {
            level.sendParticles(ParticleTypes.SMOKE,
                    target.getX(), target.getY() + target.getBbHeight() + 0.2, target.getZ(), 6, 0.2, 0.1, 0.2, 0.01);
            player.displayClientMessage(Component.translatable("message.heartbound.propose_not_ready",
                    target.getName(), RelationshipStage.PROPOSAL_MIN_AFFINITY), true);
            return false;
        }
        if (!data.pair(mobId, playerId)) {
            return false;
        }
        if (!player.getAbilities().instabuild) {
            ring.shrink(1);
        }
        level.sendParticles(ParticleTypes.HEART,
                target.getX(), target.getY() + target.getBbHeight() + 0.2, target.getZ(), 25, 0.5, 0.4, 0.5, 0.05);
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1.0F, 1.2F);
        MemoryRecorder.record(player, mobId, target.blockPosition().asLong(), EventType.PROPOSAL, 0);
        player.displayClientMessage(Component.translatable("message.heartbound.propose_accepted", target.getName()), false);
        return true;
    }

    /** Ends the player's relationship with their partner (works even if the partner is gone). */
    public static boolean breakUp(ServerPlayer player) {
        if (player.getServer() == null) {
            return false;
        }
        RelationshipData data = RelationshipData.get(player.getServer());
        UUID mobId = data.unpairPlayer(player.getUUID());
        if (mobId == null) {
            player.displayClientMessage(Component.translatable("message.heartbound.no_partner"), false);
            return false;
        }
        data.clearHome(mobId);
        data.stopFollowing(mobId);
        MemoryRecorder.record(player, mobId, player.blockPosition().asLong(), EventType.BREAKUP, 0);
        int current = data.get(mobId, player.getUUID());
        data.set(mobId, player.getUUID(), Math.min(current, cfg().breakupAffinity));
        player.displayClientMessage(Component.translatable("message.heartbound.breakup"), false);
        return true;
    }

    /** Makes the partner wait at the player's current position. */
    public static boolean setHome(ServerPlayer player, Entity target) {
        if (!RomanceableMobs.isEligible(target) || player.getServer() == null) {
            return false;
        }
        RelationshipData data = RelationshipData.get(player.getServer());
        if (!data.isPartner(target.getUUID(), player.getUUID())) {
            player.displayClientMessage(Component.translatable("message.heartbound.home_need_partner"), true);
            return false;
        }
        data.setHome(target.getUUID(), new Home(
                player.level().dimension().location().toString(), player.blockPosition().asLong()));
        player.serverLevel().sendParticles(ParticleTypes.HEART,
                target.getX(), target.getY() + target.getBbHeight() + 0.2, target.getZ(), 5, 0.3, 0.2, 0.3, 0.02);
        player.displayClientMessage(Component.translatable("message.heartbound.home_set", target.getName()), true);
        return true;
    }

    /** Starts or stops the mob following the player. Needs the Friends stage. */
    public static boolean toggleFollow(ServerPlayer player, Entity target) {
        if (!RomanceableMobs.isEligible(target) || player.getServer() == null) {
            return false;
        }
        RelationshipData data = RelationshipData.get(player.getServer());
        UUID mobId = target.getUUID();
        UUID playerId = player.getUUID();

        if (playerId.equals(data.getFollowTarget(mobId))) {
            data.stopFollowing(mobId);
            player.displayClientMessage(Component.translatable("message.heartbound.follow_off", target.getName()), true);
            return true;
        }
        if (data.get(mobId, playerId) < RelationshipStage.FRIENDS.threshold()) {
            player.displayClientMessage(Component.translatable("message.heartbound.follow_need_friends", target.getName()), true);
            return false;
        }
        if (data.countFollowing(playerId) >= cfg().maxFollowers) {
            player.displayClientMessage(Component.translatable("message.heartbound.follow_too_many", cfg().maxFollowers), true);
            return false;
        }
        data.setFollowing(mobId, playerId);
        player.displayClientMessage(Component.translatable("message.heartbound.follow_on", target.getName()), true);
        return true;
    }

    private static void applyGain(ServerLevel level, ServerPlayer player, Entity target, int gain, int hearts) {
        RelationshipData data = RelationshipData.get(level.getServer());
        int before = data.get(target.getUUID(), player.getUUID());
        int after = data.add(target.getUUID(), player.getUUID(), gain);
        MemoryRecorder.onAffinityChanged(player, target, before, after);
        RelationshipStage stageBefore = RelationshipStage.forAffinity(before);
        RelationshipStage stageAfter = RelationshipStage.forAffinity(after);
        boolean stageUp = stageAfter.ordinal() > stageBefore.ordinal();

        level.sendParticles(ParticleTypes.HEART,
                target.getX(), target.getY() + target.getBbHeight() + 0.2, target.getZ(),
                stageUp ? hearts * 2 : hearts, 0.3, 0.2, 0.3, 0.02);
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                stageUp ? SoundEvents.PLAYER_LEVELUP : SoundEvents.VILLAGER_YES,
                SoundSource.NEUTRAL, 1.0F, 1.0F);

        Component stageName = Component.translatable("stage.heartbound." + stageAfter.name().toLowerCase(Locale.ROOT));
        player.displayClientMessage(Component.translatable("message.heartbound.affinity",
                target.getName(), "+" + (after - before), after, RelationshipStage.MAX_AFFINITY, stageName), true);
    }
}
