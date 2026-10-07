package com.heartbound;

import com.heartbound.behavior.StageBehaviors;
import com.heartbound.command.HeartboundCommands;
import com.heartbound.interaction.InteractionHandler;
import com.heartbound.item.ModItems;
import com.heartbound.menu.ModMenus;
import com.heartbound.menu.RelationshipMenu;
import com.heartbound.gesture.GestureHandler;
import com.heartbound.network.GesturePayload;
import com.heartbound.network.OpenRadialPayload;
import com.heartbound.network.OpenRelationshipPayload;
import com.heartbound.network.RadialInfoPayload;
import com.heartbound.network.TalkPayload;
import com.heartbound.talk.TalkHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

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

        PayloadTypeRegistry.playC2S().register(OpenRadialPayload.TYPE, OpenRadialPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(GesturePayload.TYPE, GesturePayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(RadialInfoPayload.TYPE, RadialInfoPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(OpenRadialPayload.TYPE, (payload, context) ->
                context.server().execute(() -> GestureHandler.requestRadial(context.player(), payload.entityId())));
        ServerPlayNetworking.registerGlobalReceiver(GesturePayload.TYPE, (payload, context) ->
                context.server().execute(() -> GestureHandler.perform(context.player(), payload.entityId(), payload.gesture())));

        PayloadTypeRegistry.playC2S().register(TalkPayload.TYPE, TalkPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TalkPayload.TYPE, (payload, context) ->
                context.server().execute(() -> TalkHandler.perform(context.player(), payload.entityId(), payload.topic())));

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ModItems.id("main"),
                FabricItemGroup.builder()
                        .title(Component.translatable("itemGroup.heartbound"))
                        .icon(() -> new ItemStack(ModItems.HEART_CHARM))
                        .displayItems((parameters, output) -> ModItems.fillTab(output))
                        .build());

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> HeartboundCommands.register(dispatcher));

        ServerTickEvents.END_SERVER_TICK.register(StageBehaviors::tick);

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
                InteractionHandler.onUseEntity(player, world, hand, entity));
    }
}
