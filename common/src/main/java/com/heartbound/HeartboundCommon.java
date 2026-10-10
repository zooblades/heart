package com.heartbound;

import com.heartbound.config.HeartboundConfig;
import com.heartbound.config.ModGameRules;
import com.heartbound.platform.Services;

/**
 * Shared entry point. Both loader modules call {@link #init()} from their own entry points.
 * Keep as much logic as possible in the common module.
 */
public final class HeartboundCommon {

    private HeartboundCommon() {
    }

    public static void init() {
        HeartboundConfig.load(Services.PLATFORM.getConfigDir());
        ModGameRules.register();
        Constants.LOG.info("{} common init on {} ({} environment)",
                Constants.MOD_NAME,
                Services.PLATFORM.getPlatformName(),
                Services.PLATFORM.getEnvironmentName());
    }
}
