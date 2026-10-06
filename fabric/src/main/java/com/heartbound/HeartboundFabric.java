package com.heartbound;

import com.heartbound.command.HeartboundCommands;
import com.heartbound.interaction.InteractionHandler;
import com.heartbound.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;

public class HeartboundFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Constants.LOG.info("Hello Fabric world!");
        HeartboundCommon.init();

        ModItems.all().forEach((name, item) -> Registry.register(BuiltInRegistries.ITEM, ModItems.id(name), item));

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries ->
                ModItems.all().values().forEach(item -> entries.accept(item)));

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> HeartboundCommands.register(dispatcher));

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
                InteractionHandler.onUseEntity(player, world, hand, entity));
    }
}
