package com.heartbound;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class HeartboundNeoForge {

    public HeartboundNeoForge(IEventBus eventBus) {
        Constants.LOG.info("Hello NeoForge world!");
        HeartboundCommon.init();
    }
}
