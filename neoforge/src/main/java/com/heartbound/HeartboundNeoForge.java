package com.heartbound;

import com.heartbound.command.HeartboundCommands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(Constants.MOD_ID)
public class HeartboundNeoForge {

    public HeartboundNeoForge(IEventBus modEventBus) {
        Constants.LOG.info("Hello NeoForge world!");
        HeartboundCommon.init();

        NeoForge.EVENT_BUS.addListener(HeartboundNeoForge::onRegisterCommands);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        HeartboundCommands.register(event.getDispatcher());
    }
}
