package com.heartbound.gesture;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** A very simple mood check: a mob that was hurt recently or is badly wounded is upset. */
public final class MobMood {

    public static final int RECENT_HURT_TICKS = 2400;

    private MobMood() {
    }

    public static boolean isUpset(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        boolean hurtRecently = living.getLastHurtByMob() != null
                && living.tickCount - living.getLastHurtByMobTimestamp() < RECENT_HURT_TICKS;
        return hurtRecently || living.getHealth() < living.getMaxHealth() * 0.5F;
    }
}
