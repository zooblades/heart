package com.heartbound;

import com.heartbound.behavior.StageBehaviors;
import com.heartbound.command.HeartboundCommands;
import com.heartbound.interaction.InteractionHandler;
import com.heartbound.item.ModItems;
import com.heartbound.menu.ModMenus;
import com.heartbound.menu.RelationshipMenu;
import com.heartbound.client.RadialClient;
import com.heartbound.gesture.GestureHandler;
import com.heartbound.network.GesturePayload;
import com.heartbound.network.OpenRadialPayload;
import com.heartbound.network.OpenRelationshipPayload;
import com.heartbound.network.RadialInfoPayload;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(Constants.MOD_ID)
public class HeartboundNeoForge {

    public HeartboundNeoForge(IEventBus modEventBus) {
        Constants.LOG.info("Hello NeoForge world!");
        HeartboundCommon.init();

        ModMenus.init(IMenuTypeExtension.create((containerId, inventory, buffer) -> new RelationshipMenu(containerId, inventory)));

        modEventBus.addListener(HeartboundNeoForge::onRegister);
        modEventBus.addListener(HeartboundNeoForge::onCreativeTab);
        modEventBus.addListener(HeartboundNeoForge::onRegisterPayloads);

        NeoForge.EVENT_BUS.addListener(HeartboundNeoForge::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(HeartboundNeoForge::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(HeartboundNeoForge::onServerTick);
    }

    private static void onRegister(RegisterEvent event) {
        event.register(Registries.ITEM, helper ->
                ModItems.all().forEach((name, item) -> helper.register(ModItems.id(name), item)));
        event.register(Registries.MENU, helper ->
                helper.register(ModMenus.id(ModMenus.RELATIONSHIP_NAME), ModMenus.RELATIONSHIP));
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(OpenRadialPayload.TYPE, OpenRadialPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        GestureHandler.requestRadial(serverPlayer, payload.entityId());
                    }
                }));
        registrar.playToServer(GesturePayload.TYPE, GesturePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        GestureHandler.perform(serverPlayer, payload.entityId(), payload.gesture());
                    }
                }));
        registrar.playToClient(RadialInfoPayload.TYPE, RadialInfoPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> RadialClient.open(payload)));
        registrar.playToServer(OpenRelationshipPayload.TYPE, OpenRelationshipPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        InteractionHandler.openFromKey(serverPlayer, payload.entityId());
                    }
                }));
    }

    private static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            ModItems.all().values().forEach(item -> event.accept(item));
        }
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        HeartboundCommands.register(event.getDispatcher());
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        StageBehaviors.tick(event.getServer());
    }

    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        InteractionResult result = InteractionHandler.onUseEntity(
                event.getEntity(), event.getLevel(), event.getHand(), event.getTarget());
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }
}
