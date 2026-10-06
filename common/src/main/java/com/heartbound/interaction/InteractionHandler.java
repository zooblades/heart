package com.heartbound.interaction;

import com.heartbound.item.GiftItem;
import com.heartbound.relationship.CooldownTracker;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Locale;

/**
 * Handles right-clicking a mob: giving a gift (holding a gift item) and petting (sneaking with an empty hand).
 * Each loader calls {@link #onUseEntity} from its own "player uses entity" event.
 * Returns PASS when the interaction is not ours, so vanilla behaviour is untouched.
 */
public final class InteractionHandler {

    public static final int GIFT_COOLDOWN_TICKS = 20;
    public static final int PET_COOLDOWN_TICKS = 100;
    public static final int PET_GAIN = 3;

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
        if (stack.getItem() instanceof GiftItem gift) {
            return handleGift(player, level, target, stack, gift);
        }
        if (stack.isEmpty() && player.isShiftKeyDown()) {
            return handlePet(player, level, target);
        }
        return InteractionResult.PASS;
    }

    private static InteractionResult handleGift(Player player, Level level, Entity target, ItemStack stack, GiftItem gift) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        if (!GIFT_COOLDOWNS.tryUse(player.getUUID(), target.getUUID(), level.getGameTime(), GIFT_COOLDOWN_TICKS)) {
            return InteractionResult.SUCCESS;
        }
        String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).getPath();
        int gain = GiftPreferences.gain(mobId, gift.kind());
        applyGain(serverLevel, serverPlayer, target, gain, 6 + gain / 10);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult handlePet(Player player, Level level, Entity target) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        if (!PET_COOLDOWNS.tryUse(player.getUUID(), target.getUUID(), level.getGameTime(), PET_COOLDOWN_TICKS)) {
            return InteractionResult.SUCCESS;
        }
        applyGain(serverLevel, serverPlayer, target, PET_GAIN, 3);
        return InteractionResult.SUCCESS;
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
