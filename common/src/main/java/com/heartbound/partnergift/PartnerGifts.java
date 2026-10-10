package com.heartbound.partnergift;

import com.heartbound.behavior.FreezeManager;
import com.heartbound.behavior.RestManager;
import com.heartbound.behavior.StageBehaviors;
import com.heartbound.config.ModGameRules;
import com.heartbound.date.DateManager;
import com.heartbound.partnergift.PartnerGiftRules.Gift;
import com.heartbound.partnergift.PartnerGiftRules.Occasion;
import com.heartbound.relationship.EventType;
import com.heartbound.relationship.MemoryRecorder;
import com.heartbound.relationship.RelationshipData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The partner gives presents by itself: from time to time, when the player is near and not busy, it hands
 * over a small present (flowers, food, something that fits its kind), food if the player is hungry, care
 * (a moment of regeneration) if the player is hurt, and a flower after a good date. Rarer presents come
 * with a strong relationship. Transient: the pause between presents is not saved.
 */
public final class PartnerGifts {

    private static final double GIVE_DISTANCE_SQR = 8.0D * 8.0D;
    private static final Map<UUID, Long> NEXT_GIFT = new HashMap<>();

    private PartnerGifts() {
    }

    /** Called about once a second. */
    public static void tick(MinecraftServer server, RelationshipData data) {
        int seconds = ModGameRules.partnerGiftSeconds(server);
        if (seconds <= 0) {
            return;
        }
        for (Map.Entry<UUID, UUID> entry : data.partnersSnapshot().entrySet()) {
            UUID mobId = entry.getKey();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getValue());
            if (player == null) {
                continue;
            }
            ServerLevel level = player.serverLevel();
            if (!(level.getEntity(mobId) instanceof Mob mob) || !mob.isAlive()) {
                continue;
            }
            long now = level.getGameTime();
            Long next = NEXT_GIFT.get(mobId);
            if (next == null) {
                NEXT_GIFT.put(mobId, now + 3000L + level.random.nextInt(3000));
                continue;
            }
            if (now < next || !canGive(player, mob)) {
                continue;
            }
            Occasion occasion = player.getHealth() <= 8.0F ? Occasion.HURT
                    : player.getFoodData().getFoodLevel() <= 6 ? Occasion.HUNGRY : Occasion.RANDOM;
            give(level, player, mob, occasion, data);
        }
    }

    private static boolean canGive(ServerPlayer player, Mob mob) {
        UUID id = mob.getUUID();
        return !player.isSpectator() && !player.isSleeping() && player.containerMenu == player.inventoryMenu
                && player.distanceToSqr(mob) < GIVE_DISTANCE_SQR
                && !mob.isPassenger() && !mob.isSleeping()
                && !FreezeManager.isFrozen(id) && !RestManager.isResting(id) && !DateManager.isOnDate(id)
                && !StageBehaviors.isHoldingHands(id);
    }

    /** Gives a present (or care) now and sets the pause until the next one. */
    public static void give(ServerLevel level, ServerPlayer player, Mob mob, Occasion occasion, RelationshipData data) {
        UUID mobId = mob.getUUID();
        int seconds = Math.max(60, ModGameRules.partnerGiftSeconds(level.getServer()));
        NEXT_GIFT.put(mobId, level.getGameTime() + seconds * 20L + level.random.nextInt(Math.max(1, seconds * 10)));

        mob.getLookControl().setLookAt(player, 30.0F, 30.0F);
        int variant = level.random.nextInt(PartnerGiftRules.LINE_VARIANTS);
        if (occasion == Occasion.HURT) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
        } else {
            String path = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getPath();
            Gift gift = PartnerGiftRules.choose(path, occasion, data.get(mobId, player.getUUID()),
                    level.random.nextInt(100), level.random.nextInt(100));
            Item item = gift == null ? Items.AIR : BuiltInRegistries.ITEM.get(ResourceLocation.parse(gift.itemId()));
            if (item == Items.AIR) {
                return;
            }
            ItemStack stack = new ItemStack(item, gift.count());
            ItemEntity drop = new ItemEntity(level, mob.getX(), mob.getEyeY() - 0.3D, mob.getZ(), stack);
            Vec3 toPlayer = player.getEyePosition().subtract(drop.position()).normalize().scale(0.3D);
            drop.setDeltaMovement(toPlayer.x, toPlayer.y + 0.1D, toPlayer.z);
            drop.setPickUpDelay(10);
            level.addFreshEntity(drop);
            player.sendSystemMessage(Component.translatable(PartnerGiftRules.GIVE_MESSAGE_KEY, mob.getName(),
                    stack.getHoverName()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            MemoryRecorder.record(player, mobId, mob.blockPosition().asLong(), EventType.FIRST_PARTNER_GIFT, 0);
        }
        int before = data.get(mobId, player.getUUID());
        int after = data.add(mobId, player.getUUID(), 2);
        MemoryRecorder.onAffinityChanged(player, mob, before, after);
        level.sendParticles(ParticleTypes.HEART, mob.getX(), mob.getY() + mob.getBbHeight() + 0.2D, mob.getZ(),
                4, 0.3D, 0.2D, 0.3D, 0.02D);
        player.sendSystemMessage(Component.translatable("dialogue.heartbound.format",
                mob.getName().copy().withStyle(ChatFormatting.YELLOW),
                Component.translatable(PartnerGiftRules.lineKey(occasion, variant))));
    }
}
