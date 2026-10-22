package com.lastbroadcast;

import com.lastbroadcast.command.BroadcastCommand;
import com.lastbroadcast.content.ContentLibrary;
import com.lastbroadcast.entity.StaffNpc;
import com.lastbroadcast.network.Net;
import com.lastbroadcast.registry.*;
import com.lastbroadcast.story.StoryDirector;
import com.lastbroadcast.world.StationEvents;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(LastBroadcast.MODID)
public class LastBroadcast {
    public static final String MODID = "lastbroadcast";
    public static final Logger LOG = LogUtils.getLogger();

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public LastBroadcast(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.TYPES.register(modBus);
        ModEntities.TYPES.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModTabs.TABS.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::entityAttributes);

        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> BroadcastCommand.register(e.getDispatcher()));
        StationEvents.register(MinecraftForge.EVENT_BUS);
        StoryDirector.register(MinecraftForge.EVENT_BUS);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.lastbroadcast.client.ClientInit.init(modBus);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        Net.init();
        ContentLibrary.load();
    }

    private void entityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.STAFF.get(), StaffNpc.createAttributes().build());
    }
}
