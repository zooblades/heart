package com.heartbound;

import com.heartbound.client.ClientHooks;
import com.heartbound.client.KeyHandler;
import com.heartbound.client.RadialClient;
import com.heartbound.client.RelationshipScreen;
import com.heartbound.menu.ModMenus;
import com.heartbound.network.RadialInfoPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;

public class HeartboundFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MenuScreens.register(ModMenus.RELATIONSHIP, RelationshipScreen::new);

        KeyBindingHelper.registerKeyBinding(KeyHandler.OPEN_KEY);
        KeyBindingHelper.registerKeyBinding(KeyHandler.GESTURE_KEY);
        ClientPlayNetworking.registerGlobalReceiver(RadialInfoPayload.TYPE, (payload, context) ->
                context.client().execute(() -> RadialClient.open(payload)));
        ClientHooks.sender = payload -> ClientPlayNetworking.send(payload);
        ClientTickEvents.START_CLIENT_TICK.register(KeyHandler::tick);
    }
}
