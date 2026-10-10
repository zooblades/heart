package com.heartbound.platform;

import com.heartbound.platform.services.IPlatformHelper;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public java.nio.file.Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public net.minecraft.world.level.GameRules.Key<net.minecraft.world.level.GameRules.IntegerValue> registerIntGameRule(
            String name, int defaultValue) {
        return net.minecraft.world.level.GameRules.register(name, net.minecraft.world.level.GameRules.Category.MOBS,
                net.minecraft.world.level.GameRules.IntegerValue.create(defaultValue));
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }
}
