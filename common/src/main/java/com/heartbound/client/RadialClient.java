package com.heartbound.client;

import com.heartbound.network.RadialInfoPayload;
import net.minecraft.client.Minecraft;

/** Client-side entry for packets that need to open screens. */
public final class RadialClient {

    private RadialClient() {
    }

    public static void open(RadialInfoPayload info) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null && mc.screen == null) {
            mc.setScreen(new RadialMenuScreen(info.entityId(), info.affinity(), info.partner(), info.holding()));
        }
    }
}
