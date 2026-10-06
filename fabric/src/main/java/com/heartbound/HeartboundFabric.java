package com.heartbound;

import net.fabricmc.api.ModInitializer;

public class HeartboundFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Constants.LOG.info("Hello Fabric world!");
        HeartboundCommon.init();
    }
}
