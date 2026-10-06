package com.heartbound.relationship;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

/**
 * Which mobs can take part in relationships. First version: adult villagers, wolves, cats and foxes.
 * Babies are never eligible. This will become data driven later.
 */
public final class RomanceableMobs {

    private static final Set<EntityType<?>> SUPPORTED = Set.<EntityType<?>>of(
            EntityType.VILLAGER,
            EntityType.WOLF,
            EntityType.CAT,
            EntityType.FOX
    );

    private RomanceableMobs() {
    }

    public static boolean isSupportedType(Entity entity) {
        return SUPPORTED.contains(entity.getType());
    }

    public static boolean isAdult(Entity entity) {
        return entity instanceof LivingEntity living && !living.isBaby();
    }

    public static boolean isEligible(Entity entity) {
        return isSupportedType(entity) && isAdult(entity);
    }
}
