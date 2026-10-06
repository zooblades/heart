package com.heartbound;

import com.heartbound.client.RelationshipScreen;
import com.heartbound.menu.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Constants.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HeartboundNeoForgeClient {

    private HeartboundNeoForgeClient() {
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.RELATIONSHIP, RelationshipScreen::new);
    }
}
