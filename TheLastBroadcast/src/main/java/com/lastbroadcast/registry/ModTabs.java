package com.lastbroadcast.registry;

import com.lastbroadcast.LastBroadcast;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LastBroadcast.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.lastbroadcast"))
            .icon(() -> new ItemStack(ModBlocks.TELEVISION.get()))
            .displayItems((params, out) -> {
                for (var b : ModBlocks.WITH_ITEMS) out.accept(b.get());
                out.accept(ModItems.MAGNETIC_TAPE.get());
                out.accept(ModItems.CASE_JOURNAL.get());
                out.accept(ModItems.FIELD_METER.get());
            }).build());

    private ModTabs() {}
}
