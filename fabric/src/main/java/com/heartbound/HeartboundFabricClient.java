package com.heartbound;

import com.heartbound.client.RelationshipScreen;
import com.heartbound.menu.ModMenus;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class HeartboundFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MenuScreens.register(ModMenus.RELATIONSHIP, RelationshipScreen::new);
    }
}
