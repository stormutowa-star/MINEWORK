package com.lastbroadcast.registry;

import com.lastbroadcast.LastBroadcast;
import com.lastbroadcast.block.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, LastBroadcast.MODID);
    /** Blocks that get a BlockItem and appear in the creative tab, in display order. */
    public static final List<RegistryObject<? extends Block>> WITH_ITEMS = new ArrayList<>();

    // ---- shapes (pixels, north-facing model) ----
    public static final ModelShape S_TV = ModelShape.of(new double[]{1, 0, 3, 15, 15, 14});
    public static final ModelShape S_ANALYZER = ModelShape.of(new double[]{1, 0, 2, 15, 11, 14});
    public static final ModelShape S_RECORDER = ModelShape.of(new double[]{1, 0, 4, 15, 15, 12});
    public static final ModelShape S_WALL_BOARD = ModelShape.of(new double[]{0, 1, 14, 16, 15, 16});
    public static final ModelShape S_CABINET = ModelShape.of(new double[]{1, 0, 1, 15, 16, 15});
    public static final ModelShape S_SHELF = ModelShape.of(new double[]{0, 0, 2, 16, 16, 16});
    public static final ModelShape S_NOTICE = ModelShape.of(new double[]{1, 2, 15, 15, 14, 16});
    public static final ModelShape S_CONSOLE = ModelShape.of(new double[]{0, 0, 2, 16, 8, 16}, new double[]{0, 8, 7, 16, 14, 16});
    public static final ModelShape S_DESK = ModelShape.of(new double[]{0, 0, 1, 16, 11, 16});
    public static final ModelShape S_PHONE = ModelShape.of(new double[]{4, 0, 4, 12, 6, 12});
    public static final ModelShape S_CLOCK = ModelShape.of(new double[]{3, 3, 15, 13, 13, 16});
    public static final ModelShape S_CEILING = ModelShape.of(new double[]{2, 14, 2, 14, 16, 14});
    public static final ModelShape S_POLE = ModelShape.of(new double[]{4, 0, 4, 12, 16, 12});
    public static final ModelShape S_LAMP = ModelShape.of(new double[]{5, 0, 5, 11, 16, 11});
    public static final ModelShape S_SIGN = ModelShape.of(new double[]{1, 5, 14, 15, 11, 16});
    public static final ModelShape S_BEACON = ModelShape.of(new double[]{5, 0, 5, 11, 7, 11});
    public static final ModelShape S_COIL = ModelShape.of(new double[]{3, 0, 3, 13, 16, 13});
    public static final ModelShape S_CHAIR = ModelShape.of(new double[]{2, 0, 2, 14, 8, 14}, new double[]{2, 8, 12, 14, 16, 14});
    public static final ModelShape S_CAMERA = ModelShape.of(new double[]{3, 0, 2.5, 13, 16, 14});
    public static final ModelShape S_FULL = ModelShape.full();

    // ---- interactive equipment ----
    public static final RegistryObject<Block> TELEVISION = reg("television", () -> new TelevisionBlock(machine(MapColor.COLOR_BROWN, SoundType.WOOD), S_TV));
    public static final RegistryObject<Block> SIGNAL_ANALYZER = reg("signal_analyzer", () -> new AnalyzerBlock(machine(MapColor.COLOR_GRAY, SoundType.METAL).lightLevel(s -> 3), S_ANALYZER));
    public static final RegistryObject<Block> TAPE_RECORDER = reg("tape_recorder", () -> new RecorderBlock(machine(MapColor.COLOR_GRAY, SoundType.METAL), S_RECORDER));
    public static final RegistryObject<Block> CASE_BOARD = reg("case_board", () -> new CaseBoardBlock(machine(MapColor.COLOR_BROWN, SoundType.WOOD), S_WALL_BOARD));
    public static final RegistryObject<Block> FILE_CABINET = reg("file_cabinet", () -> new DocumentHolderBlock(machine(MapColor.COLOR_GREEN, SoundType.METAL), S_CABINET,
            "Carbon copies, coffee rings and old rate cards. Nothing useful.", ModSounds.LOCKER));
    public static final RegistryObject<Block> TAPE_SHELF = reg("tape_shelf", () -> new DocumentHolderBlock(machine(MapColor.COLOR_BROWN, SoundType.WOOD), S_SHELF,
            "Boxes of recorded programmes, all labelled and dated. Nothing out of place.", ModSounds.TAPE_INSERT));
    public static final RegistryObject<Block> NOTICE_BOARD = reg("notice_board", () -> new DocumentHolderBlock(machine(MapColor.COLOR_BROWN, SoundType.WOOD), S_NOTICE,
            "Shift rosters and a bowling league flyer.", ModSounds.PAPER));
    public static final RegistryObject<Block> ENGINEERING_LOCKER = reg("engineering_locker", () -> new LockerBlock(machine(MapColor.COLOR_LIGHT_GRAY, SoundType.METAL), S_CABINET));
    public static final RegistryObject<Block> SKYHOOK_CONSOLE = reg("skyhook_console", () -> new ConsoleBlock(machine(MapColor.COLOR_LIGHT_GRAY, SoundType.METAL).lightLevel(s -> 5), S_CONSOLE));
    public static final RegistryObject<Block> TELEPHONE = reg("telephone", () -> new TelephoneBlock(machine(MapColor.COLOR_BLACK, SoundType.STONE), S_PHONE));
    public static final RegistryObject<Block> WALL_CLOCK = reg("wall_clock", () -> new ClockBlock(machine(MapColor.COLOR_LIGHT_GRAY, SoundType.METAL), S_CLOCK));

    // ---- lighting ----
    public static final RegistryObject<Block> CEILING_LIGHT = reg("ceiling_light", () -> new LampBlock(lamp(15), S_CEILING));
    public static final RegistryObject<Block> STUDIO_LIGHT = reg("studio_light", () -> new FacingLampBlock(lamp(15), S_POLE));
    public static final RegistryObject<Block> GOOGIE_LAMP = reg("googie_lamp", () -> new LampBlock(lamp(13), S_LAMP));
    public static final RegistryObject<Block> ON_AIR_SIGN = reg("on_air_sign", () -> new FacingLampBlock(lamp(7), S_SIGN));
    public static final RegistryObject<Block> TOWER_BEACON = reg("tower_beacon", () -> new TowerLampBlock(lamp(12), S_BEACON));

    // ---- machinery & furniture ----
    public static final RegistryObject<Block> TUBE_BANK = reg("tube_bank", () -> new DecorBlock(machine(MapColor.COLOR_GRAY, SoundType.METAL).lightLevel(s -> 6), S_CABINET));
    public static final RegistryObject<Block> TRANSMITTER_COIL = reg("transmitter_coil", () -> new DecorBlock(machine(MapColor.COLOR_ORANGE, SoundType.COPPER), S_COIL));
    public static final RegistryObject<Block> STUDIO_CAMERA = reg("studio_camera", () -> new DecorBlock(machine(MapColor.COLOR_GRAY, SoundType.METAL), S_CAMERA));
    public static final RegistryObject<Block> CONTROL_PANEL = reg("control_panel", () -> new DecorBlock(machine(MapColor.COLOR_GRAY, SoundType.METAL).lightLevel(s -> 2), S_FULL));
    public static final RegistryObject<Block> CONTROL_DESK = reg("control_desk", () -> new DecorBlock(machine(MapColor.COLOR_GRAY, SoundType.METAL).lightLevel(s -> 2), S_DESK));
    public static final RegistryObject<Block> LOUNGE_CHAIR = reg("lounge_chair", () -> new DecorBlock(machine(MapColor.COLOR_ORANGE, SoundType.WOOL), S_CHAIR));

    // ---- architecture ----
    public static final RegistryObject<Block> TERRAZZO = reg("terrazzo", () -> new Block(solid(MapColor.QUARTZ, SoundType.STONE)));
    public static final RegistryObject<Block> STUDIO_TILE = reg("studio_tile", () -> new Block(solid(MapColor.COLOR_LIGHT_GRAY, SoundType.WOOL)));
    public static final RegistryObject<Block> ATOMIC_WALLPAPER = reg("atomic_wallpaper", () -> new Block(solid(MapColor.COLOR_CYAN, SoundType.WOOD)));
    public static final RegistryObject<Block> CHROME_TRIM = reg("chrome_trim", () -> new Block(solid(MapColor.METAL, SoundType.METAL)));
    public static final RegistryObject<Block> WALNUT_PANEL = reg("walnut_panel", () -> new Block(solid(MapColor.COLOR_BROWN, SoundType.WOOD)));
    public static final RegistryObject<Block> GOOGIE_FACADE = reg("googie_facade", () -> new Block(solid(MapColor.TERRACOTTA_WHITE, SoundType.STONE)));

    private ModBlocks() {}

    private static <B extends Block> RegistryObject<B> reg(String name, Supplier<B> sup) {
        RegistryObject<B> ro = BLOCKS.register(name, sup);
        WITH_ITEMS.add(ro);
        return ro;
    }

    private static Properties machine(MapColor color, SoundType sound) {
        return Properties.of().mapColor(color).strength(2.0f, 6.0f).sound(sound).noOcclusion();
    }

    private static Properties lamp(int light) {
        return Properties.of().mapColor(MapColor.QUARTZ).strength(1.0f).sound(SoundType.GLASS).noOcclusion()
                .lightLevel(s -> s.hasProperty(BlockStateProperties.LIT) && s.getValue(BlockStateProperties.LIT) ? light : 0);
    }

    private static Properties solid(MapColor color, SoundType sound) {
        return Properties.of().mapColor(color).strength(1.8f, 6.0f).sound(sound);
    }
}
