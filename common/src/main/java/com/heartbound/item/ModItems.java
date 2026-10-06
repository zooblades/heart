package com.heartbound.item;

import com.heartbound.Constants;
import com.heartbound.relationship.GiftKind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * All items of the mod. They are created here (common) and registered by each loader module.
 */
public final class ModItems {

    public static final GiftItem BOUQUET = new GiftItem(new Item.Properties().stacksTo(16), GiftKind.BOUQUET);
    public static final GiftItem HEART_CHARM = new GiftItem(new Item.Properties().stacksTo(16), GiftKind.HEART_CHARM);

    public static final RingItem PROMISE_RING = new RingItem(new Item.Properties().stacksTo(1));

    private static final Map<String, Item> ALL = new LinkedHashMap<>();

    static {
        ALL.put("bouquet", BOUQUET);
        ALL.put("heart_charm", HEART_CHARM);
        ALL.put("promise_ring", PROMISE_RING);
    }

    private ModItems() {
    }

    /** Registry name (path) to item, in creative tab order. */
    public static Map<String, Item> all() {
        return Collections.unmodifiableMap(ALL);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
    }
}
