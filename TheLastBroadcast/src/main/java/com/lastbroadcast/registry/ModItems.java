package com.lastbroadcast.registry;

import com.lastbroadcast.LastBroadcast;
import com.lastbroadcast.item.CaseJournalItem;
import com.lastbroadcast.item.DocumentItem;
import com.lastbroadcast.item.FieldMeterItem;
import com.lastbroadcast.item.TapeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, LastBroadcast.MODID);

    public static final RegistryObject<Item> MAGNETIC_TAPE = ITEMS.register("magnetic_tape", () -> new TapeItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> DOCUMENT = ITEMS.register("document", () -> new DocumentItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CASE_JOURNAL = ITEMS.register("case_journal", () -> new CaseJournalItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> FIELD_METER = ITEMS.register("field_meter", () -> new FieldMeterItem(new Item.Properties().stacksTo(1)));

    static {
        for (RegistryObject<? extends net.minecraft.world.level.block.Block> b : ModBlocks.WITH_ITEMS) {
            ITEMS.register(b.getId().getPath(), () -> new BlockItem(b.get(), new Item.Properties()));
        }
    }

    private ModItems() {}
}
