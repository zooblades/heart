package com.heartbound;

import com.heartbound.client.KeyHandler;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Client game-bus events (the mod-bus ones live in HeartboundNeoForgeClient). */
@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public final class HeartboundNeoForgeClientEvents {

    private HeartboundNeoForgeClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        KeyHandler.tick(Minecraft.getInstance());
    }
}
