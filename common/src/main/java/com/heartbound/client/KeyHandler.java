package com.heartbound.client;

import com.heartbound.network.OpenRelationshipPayload;
import com.heartbound.relationship.RomanceableMobs;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.entity.Entity;

/**
 * The "open relationship window" key (default F, rebindable in Controls).
 *
 * F is also vanilla's "swap with offhand". To keep both working, the key is read directly, and when
 * the two bindings share a key: looking at a mob opens the window, otherwise the offhand swap runs.
 */
public final class KeyHandler {

    public static final KeyMapping OPEN_KEY = new KeyMapping(
            "key.heartbound.open", InputConstants.Type.KEYSYM, InputConstants.KEY_F, "key.categories.heartbound");

    private static boolean wasDown;

    private KeyHandler() {
    }

    /** Call at the start of every client tick. */
    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            wasDown = false;
            return;
        }

        boolean pressed = false;
        while (OPEN_KEY.consumeClick()) {
            pressed = true;
        }
        InputConstants.Key key = OPEN_KEY.getKey();
        if (key.getType() == InputConstants.Type.KEYSYM) {
            boolean down = InputConstants.isKeyDown(mc.getWindow().getWindow(), key.getValue());
            if (down && !wasDown) {
                pressed = true;
            }
            wasDown = down;
        }
        if (!pressed || mc.screen != null) {
            return;
        }

        boolean sharesKeyWithSwap = OPEN_KEY.same(mc.options.keySwapOffhand);
        Entity target = mc.crosshairPickEntity;
        if (target != null && RomanceableMobs.isEligible(target)) {
            if (sharesKeyWithSwap) {
                drainSwapClicks(mc);
            }
            ClientHooks.send(new OpenRelationshipPayload(target.getId()));
        } else if (sharesKeyWithSwap) {
            drainSwapClicks(mc);
            swapOffhand(mc);
        }
    }

    private static void drainSwapClicks(Minecraft mc) {
        while (mc.options.keySwapOffhand.consumeClick()) {
            // swallow, so vanilla does not swap a second time
        }
    }

    private static void swapOffhand(Minecraft mc) {
        if (mc.player != null && !mc.player.isSpectator() && mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
        }
    }
}
