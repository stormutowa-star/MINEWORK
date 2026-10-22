package com.lastbroadcast.registry;

import com.lastbroadcast.LastBroadcast;
import com.lastbroadcast.blockentity.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, LastBroadcast.MODID);

    public static final RegistryObject<BlockEntityType<TelevisionBlockEntity>> TELEVISION = TYPES.register("television",
            () -> BlockEntityType.Builder.of(TelevisionBlockEntity::new, ModBlocks.TELEVISION.get()).build(null));
    public static final RegistryObject<BlockEntityType<RecorderBlockEntity>> RECORDER = TYPES.register("tape_recorder",
            () -> BlockEntityType.Builder.of(RecorderBlockEntity::new, ModBlocks.TAPE_RECORDER.get()).build(null));
    public static final RegistryObject<BlockEntityType<DocumentHolderBlockEntity>> DOCUMENT_HOLDER = TYPES.register("document_holder",
            () -> BlockEntityType.Builder.of(DocumentHolderBlockEntity::new, ModBlocks.FILE_CABINET.get(), ModBlocks.TAPE_SHELF.get(), ModBlocks.NOTICE_BOARD.get()).build(null));
    public static final RegistryObject<BlockEntityType<LockerBlockEntity>> LOCKER = TYPES.register("engineering_locker",
            () -> BlockEntityType.Builder.of(LockerBlockEntity::new, ModBlocks.ENGINEERING_LOCKER.get()).build(null));
    public static final RegistryObject<BlockEntityType<ConsoleBlockEntity>> CONSOLE = TYPES.register("skyhook_console",
            () -> BlockEntityType.Builder.of(ConsoleBlockEntity::new, ModBlocks.SKYHOOK_CONSOLE.get()).build(null));
    public static final RegistryObject<BlockEntityType<ClockBlockEntity>> CLOCK = TYPES.register("wall_clock",
            () -> BlockEntityType.Builder.of(ClockBlockEntity::new, ModBlocks.WALL_CLOCK.get()).build(null));

    private ModBlockEntities() {}
}
