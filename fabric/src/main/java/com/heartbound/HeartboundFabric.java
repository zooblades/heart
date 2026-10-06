package com.heartbound;

import com.heartbound.behavior.StageBehaviors;
import com.heartbound.command.HeartboundCommands;
import com.heartbound.interaction.InteractionHandler;
import com.heartbound.item.ModItems;
import com.heartbound.menu.ModMenus;
import com.heartbound.menu.RelationshipMenu;
import com.heartbound.network.OpenRelationshipPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;

public class HeartboundFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Constants.LOG.info("Hello Fabric world!");
        HeartboundCommon.init();

        ModItems.all().forEach((name, item) -> Registry.register(BuiltInRegistries.ITEM, ModItems.id(name), item));

        ModMenus.init(new MenuType<>(RelationshipMenu::new, FeatureFlags.DEFAULT_FLAGS));
        Registry.register(BuiltInRegistries.MENU, ModMenus.id(ModMenus.RELATIONSHIP_NAME), ModMenus.RELATIONSHIP);

        PayloadTypeRegistry.playC2S().register(OpenRelationshipPayload.TYPE, OpenRelationshipPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(OpenRelationshipPayload.TYPE, (payload, context) ->
                context.server().execute(() -> InteractionHandler.openFromKey(context.player(), payload.entityId())));

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries ->
                ModItems.all().values().forEach(item -> entries.accept(item)));

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> HeartboundCommands.register(dispatcher));

        ServerTickEvents.END_SERVER_TICK.register(StageBehaviors::tick);

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
                InteractionHandler.onUseEntity(player, world, hand, entity));
    }
}
