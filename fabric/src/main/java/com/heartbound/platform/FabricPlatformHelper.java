package com.heartbound.platform;

import com.heartbound.platform.services.IPlatformHelper;
import net.fabricmc.loader.api.FabricLoader;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public java.nio.file.Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public net.minecraft.world.level.GameRules.Key<net.minecraft.world.level.GameRules.IntegerValue> registerIntGameRule(
            String name, int defaultValue) {
        return net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry.register(name,
                net.minecraft.world.level.GameRules.Category.MOBS,
                net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory.createIntRule(defaultValue));
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }
}
