package com.heartbound;

import com.heartbound.command.HeartboundCommands;
import com.heartbound.interaction.InteractionHandler;
import com.heartbound.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(Constants.MOD_ID)
public class HeartboundNeoForge {

    public HeartboundNeoForge(IEventBus modEventBus) {
        Constants.LOG.info("Hello NeoForge world!");
        HeartboundCommon.init();

        modEventBus.addListener(HeartboundNeoForge::onRegister);
        modEventBus.addListener(HeartboundNeoForge::onCreativeTab);

        NeoForge.EVENT_BUS.addListener(HeartboundNeoForge::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(HeartboundNeoForge::onEntityInteract);
    }

    private static void onRegister(RegisterEvent event) {
        event.register(Registries.ITEM, helper ->
                ModItems.all().forEach((name, item) -> helper.register(ModItems.id(name), item)));
    }

    private static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            ModItems.all().values().forEach(item -> event.accept(item));
        }
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        HeartboundCommands.register(event.getDispatcher());
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
