package com.heartbound;

import com.heartbound.client.ClientHooks;
import com.heartbound.client.KeyHandler;
import com.heartbound.client.RelationshipScreen;
import com.heartbound.menu.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Constants.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HeartboundNeoForgeClient {

    private HeartboundNeoForgeClient() {
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.RELATIONSHIP, RelationshipScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(KeyHandler.OPEN_KEY);
        event.register(KeyHandler.GESTURE_KEY);
        ClientHooks.sender = payload -> PacketDistributor.sendToServer(payload);
    }
}
