package com.heartbound.item;

import com.heartbound.relationship.GiftKind;
import net.minecraft.world.item.Item;

/** A gift that can be given to a mob by right-clicking it. The logic lives in InteractionHandler. */
public class GiftItem extends Item {

    private final GiftKind kind;

    public GiftItem(Properties properties, GiftKind kind) {
        super(properties);
        this.kind = kind;
    }

    public GiftKind kind() {
        return kind;
    }
}
