package com.lastbroadcast.client;

import com.lastbroadcast.LastBroadcast;
import com.lastbroadcast.client.hud.Hud;
import com.lastbroadcast.client.render.*;
import com.lastbroadcast.client.sound.BroadcastAudio;
import com.lastbroadcast.item.TapeItem;
import com.lastbroadcast.registry.ModBlockEntities;
import com.lastbroadcast.registry.ModEntities;
import com.lastbroadcast.registry.ModItems;
import com.lastbroadcast.story.CaseState;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-only wiring. Called from the mod constructor on the physical client. */
public final class ClientInit {
    private ClientInit() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientInit::clientSetup);
        modBus.addListener(ClientInit::registerRenderers);
        modBus.addListener(ClientInit::registerLayers);
        modBus.addListener(ClientInit::reloadListeners);
        modBus.addListener((AddGuiOverlayLayersEvent e) -> e.getLayeredDraw().add(LastBroadcast.id("case_hud"),
                (g, delta) -> Hud.render(g, delta.getGameTimeDeltaPartialTick(false))));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent.Post e) -> {
            BroadcastAudio.tick();
            DevAutotest.tick();
        });
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            BroadcastAudio.reset();
            Hud.reset();
            ClientDialogueState.clear();
            CaseState.CLIENT = new CaseState();
        });
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(ModItems.MAGNETIC_TAPE.get(), LastBroadcast.id("recorded"),
                (stack, level, entity, seed) -> TapeItem.isBlank(stack) ? 0f : 1f));
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.TELEVISION.get(), TelevisionRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.RECORDER.get(), RecorderRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.CLOCK.get(), ClockRenderer::new);
        event.registerEntityRenderer(ModEntities.STAFF.get(), StaffRenderer::new);
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(StaffModel.LAYER, StaffModel::createLayer);
    }

    private static void reloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) rm -> Atlas.invalidate());
        // story text follows the game language (runs at start-up and whenever the language changes)
        event.registerReloadListener((ResourceManagerReloadListener) rm ->
                com.lastbroadcast.content.ContentLibrary.reloadFor(net.minecraft.client.Minecraft.getInstance().getLanguageManager().getSelected()));
    }

}
