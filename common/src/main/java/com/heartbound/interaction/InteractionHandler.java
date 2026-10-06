package com.heartbound.interaction;

import com.heartbound.item.GiftItem;
import com.heartbound.menu.RelationshipMenu;
import com.heartbound.relationship.CooldownTracker;
import com.heartbound.relationship.Gender;
import com.heartbound.relationship.GiftPreferences;
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
 * - sneaking: opens the relationship window;
 * - holding a gift: gives the gift directly.
 * Each loader calls {@link #onUseEntity} from its own "player uses entity" event.
 * Returns PASS when the interaction is not ours, so vanilla behaviour is untouched.
 */
public final class InteractionHandler {

    public static final int GIFT_COOLDOWN_TICKS = 20;
    public static final int PET_COOLDOWN_TICKS = 100;
    public static final int PET_GAIN = 3;
    public static final int MAX_FOLLOWERS = 3;

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
        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer serverPlayer) {
                openWindow(serverPlayer, target);
            }
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof GiftItem gift) {
            if (player instanceof ServerPlayer serverPlayer) {
                giveGift(serverPlayer, target, stack, gift);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
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
        if (!GIFT_COOLDOWNS.tryUse(player.getUUID(), target.getUUID(), level.getGameTime(), GIFT_COOLDOWN_TICKS)) {
            return false;
        }
        String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).getPath();
        int gain = GiftPreferences.gain(mobId, Gender.of(target.getUUID()), gift.kind());
        applyGain(level, player, target, gain, 6 + gain / 10);
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
        if (!PET_COOLDOWNS.tryUse(player.getUUID(), target.getUUID(), level.getGameTime(), PET_COOLDOWN_TICKS)) {
            return false;
        }
        applyGain(level, player, target, PET_GAIN, 3);
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
        if (data.countFollowing(playerId) >= MAX_FOLLOWERS) {
            player.displayClientMessage(Component.translatable("message.heartbound.follow_too_many", MAX_FOLLOWERS), true);
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
