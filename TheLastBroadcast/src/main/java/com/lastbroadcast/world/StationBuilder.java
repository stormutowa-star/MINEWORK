package com.lastbroadcast.world;

import com.lastbroadcast.LastBroadcast;
import com.lastbroadcast.block.DecorBlock;
import com.lastbroadcast.blockentity.DocumentHolderBlockEntity;
import com.lastbroadcast.blockentity.TelevisionBlockEntity;
import com.lastbroadcast.broadcast.Channels;
import com.lastbroadcast.entity.StaffNpc;
import com.lastbroadcast.registry.ModBlocks;
import com.lastbroadcast.story.CaseData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the WKVR-TV station. Local coordinates: x east, z south, y=0 is the floor layer.
 * Footprint x 0..35, z 0..27; entrance on the south side; the Skyhook tower stands north of
 * the transmitter hall. All placements are explicit, so the result is identical in every world.
 */
public final class StationBuilder {
    private final ServerLevel level;
    private final BlockPos origin;
    private final List<BlockPos> reshape = new ArrayList<>();

    private StationBuilder(ServerLevel level, BlockPos origin) {
        this.level = level;
        this.origin = origin;
    }

    /** Builds the station so its entrance faces the world spawn point. */
    public static void buildNearSpawn(ServerLevel level) {
        BlockPos spawn = level.getSharedSpawnPos();
        int ox = spawn.getX() - 5;
        int oz = spawn.getZ() - 31;
        int y = groundLevel(level, ox, oz);
        build(level, new BlockPos(ox, y, oz));
    }

    /** Median surface height over the footprint (water counts as surface), clamped to the build range. */
    private static int groundLevel(ServerLevel level, int ox, int oz) {
        List<Integer> hs = new ArrayList<>();
        for (int x = -4; x <= 40; x += 4) {
            for (int z = -8; z <= 33; z += 4) {
                hs.add(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ox + x, oz + z) - 1);
            }
        }
        hs.sort(Integer::compare);
        int median = hs.get(hs.size() / 2);
        return Math.max(level.getMinBuildHeight() + 2, Math.min(median, level.getMaxBuildHeight() - 48));
    }

    public static void build(ServerLevel level, BlockPos origin) {
        long t0 = System.currentTimeMillis();
        StationBuilder b = new StationBuilder(level, origin);
        b.prepareTerrain();
        b.sealFluids();
        b.buildShell();
        b.buildLobby();
        b.buildOffice();
        b.buildArchive();
        b.buildCorridor();
        b.buildControlRoom();
        b.buildTransmitterHall();
        b.buildStudio();
        b.buildTower();
        b.buildExterior();
        b.finishShapes();
        b.spawnStaff();

        CaseData data = CaseData.get(level.getServer());
        data.stationBuilt = true;
        data.origin = origin;
        data.entrance = b.at(5, 1, 31);
        data.consolePos = b.at(30, 1, 4);
        data.phonePos = b.at(15, 2, 3);
        data.towerTop = b.at(30, 30, -5);
        BlockPos min = b.at(-8, -1, -10), max = b.at(42, 34, 35);
        data.bounds = new AABB(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
        data.setDirty();
        level.setDefaultSpawnPos(data.entrance, 180f);
        LastBroadcast.LOG.info("WKVR station built at {} in {} ms", origin, System.currentTimeMillis() - t0);
    }

    // ------------------------------------------------------------------ helpers

    BlockPos at(int x, int y, int z) {
        return origin.offset(x, y, z);
    }

    private void set(int x, int y, int z, BlockState s) {
        BlockPos p = at(x, y, z);
        level.setBlock(p, s, 2 | 16);
        Block blk = s.getBlock();
        if (blk instanceof IronBarsBlock || blk instanceof FenceBlock || blk instanceof WallBlock) reshape.add(p);
    }

    private void set(int x, int y, int z, Block b) {
        set(x, y, z, b.defaultBlockState());
    }

    private void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++)
                    set(x, y, z, s);
    }

    private void fill(int x0, int y0, int z0, int x1, int y1, int z1, Block b) {
        fill(x0, y0, z0, x1, y1, z1, b.defaultBlockState());
    }

    private void facing(int x, int y, int z, Block b, Direction d) {
        BlockState s = b.defaultBlockState();
        if (s.hasProperty(DecorBlock.FACING)) s = s.setValue(DecorBlock.FACING, d);
        set(x, y, z, s);
    }

    private void door(int x, int z, Block b, Direction facing, DoorHingeSide hinge) {
        BlockState s = b.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HINGE, hinge).setValue(DoorBlock.OPEN, false);
        set(x, 1, z, s.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(x, 2, z, s.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    private void pane(int x, int y, int z) {
        BlockPos p = at(x, y, z);
        level.setBlock(p, Blocks.GLASS_PANE.defaultBlockState(), 2 | 16);
        reshape.add(p);
    }

    private void tv(int x, int y, int z, Direction d, boolean on, int channel) {
        facing(x, y, z, ModBlocks.TELEVISION.get(), d);
        if (level.getBlockEntity(at(x, y, z)) instanceof TelevisionBlockEntity tv) tv.setState(on, channel);
    }

    private void holder(int x, int y, int z, Block b, Direction d, String... tokens) {
        facing(x, y, z, b, d);
        if (level.getBlockEntity(at(x, y, z)) instanceof DocumentHolderBlockEntity h) h.setContents(List.of(tokens));
    }

    private void ceilingLights(int[][] xz) {
        for (int[] p : xz) set(p[0], 6, p[1], ModBlocks.CEILING_LIGHT.get());
    }

    // ------------------------------------------------------------------ terrain & shell

    private void prepareTerrain() {
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = -10; x <= 44; x++) {
            for (int z = -12; z <= 37; z++) {
                // foundation down to natural ground (bounded), so nothing floats
                for (int y = -1; y >= -48 && origin.getY() + y > level.getMinBuildHeight(); y--) {
                    BlockState cur = level.getBlockState(at(x, y, z));
                    if (cur.isSolidRender(level, at(x, y, z)) && !cur.is(Blocks.WATER)) break;
                    set(x, y, z, dirt);
                }
                set(x, 0, z, Blocks.GRASS_BLOCK.defaultBlockState());
                for (int y = 1; y <= 36; y++) {
                    if (!level.getBlockState(at(x, y, z)).isAir()) set(x, y, z, air);
                }
            }
        }
    }

    /** Seals water or lava around the cleared site with a low earth embankment so nothing floods it. */
    private void sealFluids() {
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        for (int x = -14; x <= 48; x++) {
            for (int z = -16; z <= 41; z++) {
                boolean inside = x >= -10 && x <= 44 && z >= -12 && z <= 37;
                if (inside) continue;
                for (int y = -3; y <= 4; y++) {
                    if (!level.getFluidState(at(x, y, z)).isEmpty()) set(x, y, z, dirt);
                }
            }
        }
    }

    private void buildShell() {
        Block facade = ModBlocks.GOOGIE_FACADE.get();
        Block trim = ModBlocks.CHROME_TRIM.get();
        // floors per room
        fill(0, 0, 0, 35, 0, 27, Blocks.SMOOTH_STONE);
        fill(1, 0, 19, 10, 0, 26, ModBlocks.TERRAZZO.get());
        fill(1, 0, 10, 10, 0, 17, Blocks.DARK_OAK_PLANKS);
        fill(12, 0, 1, 13, 0, 26, ModBlocks.TERRAZZO.get());
        fill(15, 0, 1, 24, 0, 12, Blocks.GRAY_CONCRETE);
        fill(26, 0, 1, 34, 0, 12, Blocks.POLISHED_ANDESITE);
        fill(15, 0, 14, 34, 0, 26, Blocks.POLISHED_DEEPSLATE);
        // perimeter walls with chrome crown band
        for (int y = 1; y <= 6; y++) {
            Block b = y == 6 ? trim : facade;
            fill(0, y, 0, 35, y, 0, b);
            fill(0, y, 27, 35, y, 27, b);
            fill(0, y, 0, 0, y, 27, b);
            fill(35, y, 0, 35, y, 27, b);
        }
        // ceiling / roof with a Googie overhang
        fill(0, 7, 0, 35, 7, 27, Blocks.SMOOTH_QUARTZ);
        fill(-1, 7, -1, 36, 7, -1, trim);
        fill(-1, 7, 28, 36, 7, 28, trim);
        fill(-1, 7, -1, -1, 7, 28, trim);
        fill(36, 7, -1, 36, 7, 28, trim);
        fill(15, 7, 14, 34, 7, 26, Blocks.BLACK_CONCRETE);
        // interior partitions
        fill(1, 1, 18, 10, 5, 18, ModBlocks.ATOMIC_WALLPAPER.get());
        fill(11, 1, 19, 11, 5, 26, ModBlocks.ATOMIC_WALLPAPER.get());
        fill(1, 1, 9, 10, 5, 9, ModBlocks.WALNUT_PANEL.get());
        fill(11, 1, 10, 11, 5, 17, ModBlocks.WALNUT_PANEL.get());
        fill(11, 1, 18, 11, 5, 18, ModBlocks.WALNUT_PANEL.get());
        fill(11, 1, 1, 11, 5, 9, Blocks.WHITE_CONCRETE);
        fill(14, 1, 1, 14, 5, 12, ModBlocks.WALNUT_PANEL.get());
        fill(25, 1, 1, 25, 5, 12, ModBlocks.WALNUT_PANEL.get());
        fill(14, 1, 13, 35, 5, 13, ModBlocks.STUDIO_TILE.get());
        fill(14, 1, 14, 14, 5, 26, ModBlocks.STUDIO_TILE.get());
        // ceiling trim line at y=6 inside partitions
        fill(1, 6, 18, 10, 6, 18, trim);
        fill(11, 6, 1, 11, 6, 26, trim);
        fill(1, 6, 9, 10, 6, 9, trim);
        fill(14, 6, 1, 14, 6, 26, trim);
        fill(25, 6, 1, 25, 6, 12, trim);
        fill(15, 6, 13, 34, 6, 13, trim);
        // doors
        door(5, 27, Blocks.BIRCH_DOOR, Direction.SOUTH, DoorHingeSide.LEFT);
        door(6, 27, Blocks.BIRCH_DOOR, Direction.SOUTH, DoorHingeSide.RIGHT);
        door(11, 22, Blocks.BIRCH_DOOR, Direction.EAST, DoorHingeSide.LEFT);
        door(11, 14, Blocks.DARK_OAK_DOOR, Direction.EAST, DoorHingeSide.LEFT);
        door(11, 4, Blocks.SPRUCE_DOOR, Direction.EAST, DoorHingeSide.LEFT);
        door(14, 6, Blocks.DARK_OAK_DOOR, Direction.WEST, DoorHingeSide.RIGHT);
        door(14, 20, Blocks.DARK_OAK_DOOR, Direction.WEST, DoorHingeSide.RIGHT);
        door(25, 9, Blocks.DARK_OAK_DOOR, Direction.WEST, DoorHingeSide.RIGHT);
        door(30, 0, Blocks.SPRUCE_DOOR, Direction.NORTH, DoorHingeSide.LEFT);
        // windows
        for (int x : new int[]{2, 3, 4, 7, 8, 9}) for (int y = 2; y <= 4; y++) pane(x, y, 27);
        for (int z = 12; z <= 15; z++) for (int y = 2; y <= 3; y++) pane(0, y, z);
        for (int x = 17; x <= 22; x++) for (int y = 2; y <= 3; y++) pane(x, y, 13);
    }

    // ------------------------------------------------------------------ rooms

    private void buildLobby() {
        Block walnut = ModBlocks.WALNUT_PANEL.get();
        fill(3, 1, 21, 7, 1, 21, walnut);
        set(3, 2, 21, ModBlocks.CHROME_TRIM.get());
        set(7, 2, 21, ModBlocks.CHROME_TRIM.get());
        set(4, 2, 21, ModBlocks.TELEPHONE.get().defaultBlockState().setValue(DecorBlock.FACING, Direction.SOUTH));
        facing(9, 1, 24, ModBlocks.LOUNGE_CHAIR.get(), Direction.WEST);
        facing(9, 1, 26, ModBlocks.LOUNGE_CHAIR.get(), Direction.WEST);
        tv(7, 1, 25, Direction.EAST, true, Channels.CH_7);
        set(1, 1, 26, ModBlocks.GOOGIE_LAMP.get());
        set(10, 1, 19, ModBlocks.GOOGIE_LAMP.get());
        set(1, 1, 19, Blocks.POTTED_FERN);
        set(10, 1, 26, Blocks.POTTED_FERN);
        facing(5, 3, 19, ModBlocks.WALL_CLOCK.get(), Direction.SOUTH);
        holder(8, 2, 19, ModBlocks.NOTICE_BOARD.get(), Direction.SOUTH, "doc:program_guide");
        ceilingLights(new int[][]{{3, 23}, {8, 23}});
    }

    private void buildOffice() {
        Block walnut = ModBlocks.WALNUT_PANEL.get();
        fill(4, 1, 13, 6, 1, 13, walnut);
        set(4, 2, 13, Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        facing(5, 1, 12, ModBlocks.LOUNGE_CHAIR.get(), Direction.SOUTH);
        facing(4, 1, 15, ModBlocks.LOUNGE_CHAIR.get(), Direction.NORTH);
        facing(6, 1, 15, ModBlocks.LOUNGE_CHAIR.get(), Direction.NORTH);
        holder(1, 1, 16, ModBlocks.FILE_CABINET.get(), Direction.EAST, "doc:staff_roster", "doc:futura_letter");
        tv(9, 1, 10, Direction.SOUTH, false, Channels.CH_7);
        set(1, 1, 10, ModBlocks.GOOGIE_LAMP.get());
        set(10, 1, 17, Blocks.POTTED_FERN);
        facing(10, 3, 12, ModBlocks.WALL_CLOCK.get(), Direction.WEST);
        ceilingLights(new int[][]{{3, 12}, {8, 15}});
    }

    private void buildArchive() {
        for (int x = 1; x <= 10; x++) {
            holder(x, 1, 1, ModBlocks.TAPE_SHELF.get(), Direction.SOUTH, "tape:blank");
            holder(x, 2, 1, ModBlocks.TAPE_SHELF.get(), Direction.SOUTH);
        }
        for (int z = 3; z <= 7; z++) {
            holder(1, 1, z, ModBlocks.TAPE_SHELF.get(), Direction.EAST);
            holder(1, 2, z, ModBlocks.TAPE_SHELF.get(), Direction.EAST);
        }
        holder(10, 1, 7, ModBlocks.FILE_CABINET.get(), Direction.WEST, "doc:skyhook_brochure");
        holder(10, 1, 8, ModBlocks.FILE_CABINET.get(), Direction.WEST);
        fill(5, 1, 5, 6, 1, 5, ModBlocks.WALNUT_PANEL.get());
        ceilingLights(new int[][]{{4, 4}, {8, 6}});
    }

    private void buildCorridor() {
        facing(13, 4, 20, ModBlocks.ON_AIR_SIGN.get(), Direction.WEST);
        ceilingLights(new int[][]{{12, 4}, {13, 10}, {12, 16}, {13, 23}});
    }

    private void buildControlRoom() {
        Block panel = ModBlocks.CONTROL_PANEL.get();
        for (int x = 16; x <= 23; x++) facing(x, 1, 1, panel, Direction.SOUTH);
        tv(17, 2, 1, Direction.SOUTH, true, Channels.CH_7);
        tv(19, 2, 1, Direction.SOUTH, true, Channels.CH_4);
        tv(21, 2, 1, Direction.SOUTH, true, Channels.CH_7);
        facing(19, 4, 1, ModBlocks.WALL_CLOCK.get(), Direction.SOUTH);
        for (int x = 17; x <= 21; x++) facing(x, 1, 3, ModBlocks.CONTROL_DESK.get(), Direction.SOUTH);
        facing(19, 1, 4, ModBlocks.LOUNGE_CHAIR.get(), Direction.NORTH);
        // bench: analyzer and deck
        for (int z = 3; z <= 6; z++) facing(24, 1, z, panel, Direction.WEST);
        facing(24, 2, 4, ModBlocks.SIGNAL_ANALYZER.get(), Direction.WEST);
        facing(24, 2, 6, ModBlocks.TAPE_RECORDER.get(), Direction.WEST);
        facing(24, 2, 3, ModBlocks.TUBE_BANK.get(), Direction.WEST);
        // telephone on a walnut side table
        set(15, 1, 3, ModBlocks.WALNUT_PANEL.get());
        facing(15, 2, 3, ModBlocks.TELEPHONE.get(), Direction.EAST);
        facing(15, 2, 9, ModBlocks.CASE_BOARD.get(), Direction.EAST);
        holder(15, 1, 11, ModBlocks.FILE_CABINET.get(), Direction.EAST, "doc:program_log");
        holder(15, 1, 12, ModBlocks.FILE_CABINET.get(), Direction.EAST);
        facing(18, 1, 8, ModBlocks.LOUNGE_CHAIR.get(), Direction.EAST);
        ceilingLights(new int[][]{{17, 5}, {22, 5}, {17, 10}, {22, 10}});
    }

    private void buildTransmitterHall() {
        Block panel = ModBlocks.CONTROL_PANEL.get();
        for (int x : new int[]{27, 28, 29, 31, 32, 33}) {
            facing(x, 1, 1, panel, Direction.SOUTH);
            facing(x, 2, 1, ModBlocks.TUBE_BANK.get(), Direction.SOUTH);
        }
        for (int z = 3; z <= 10; z++) {
            facing(34, 1, z, ModBlocks.TUBE_BANK.get(), Direction.WEST);
            facing(34, 2, z, ModBlocks.TUBE_BANK.get(), Direction.WEST);
        }
        facing(30, 1, 4, ModBlocks.SKYHOOK_CONSOLE.get(), Direction.SOUTH);
        set(32, 1, 7, ModBlocks.TRANSMITTER_COIL.get());
        set(32, 1, 9, ModBlocks.TRANSMITTER_COIL.get());
        set(28, 1, 7, ModBlocks.TRANSMITTER_COIL.get());
        facing(27, 1, 12, ModBlocks.ENGINEERING_LOCKER.get(), Direction.NORTH);
        holder(29, 2, 12, ModBlocks.NOTICE_BOARD.get(), Direction.NORTH, "doc:procedure");
        facing(26, 3, 5, ModBlocks.WALL_CLOCK.get(), Direction.EAST);
        ceilingLights(new int[][]{{28, 4}, {32, 4}, {28, 10}, {32, 10}});
    }

    private void buildStudio() {
        Block wall = ModBlocks.ATOMIC_WALLPAPER.get();
        fill(34, 1, 17, 34, 5, 24, wall);
        fill(33, 1, 17, 33, 1, 24, Blocks.WHITE_CONCRETE);
        fill(33, 5, 17, 33, 5, 24, ModBlocks.CHROME_TRIM.get());
        set(33, 3, 18, Blocks.CYAN_TERRACOTTA);
        set(33, 4, 20, Blocks.ORANGE_TERRACOTTA);
        set(33, 3, 23, Blocks.CYAN_TERRACOTTA);
        facing(31, 1, 19, ModBlocks.LOUNGE_CHAIR.get(), Direction.WEST);
        facing(31, 1, 22, ModBlocks.LOUNGE_CHAIR.get(), Direction.WEST);
        set(31, 1, 20, Blocks.SPRUCE_SLAB.defaultBlockState());
        set(31, 1, 21, Blocks.SPRUCE_SLAB.defaultBlockState());
        set(32, 1, 24, ModBlocks.GOOGIE_LAMP.get());
        facing(24, 1, 17, ModBlocks.STUDIO_CAMERA.get(), Direction.EAST);
        facing(22, 1, 21, ModBlocks.STUDIO_CAMERA.get(), Direction.EAST);
        facing(24, 1, 24, ModBlocks.STUDIO_CAMERA.get(), Direction.EAST);
        facing(27, 1, 15, ModBlocks.STUDIO_LIGHT.get(), Direction.EAST);
        facing(27, 1, 26, ModBlocks.STUDIO_LIGHT.get(), Direction.EAST);
        facing(29, 1, 15, ModBlocks.STUDIO_LIGHT.get(), Direction.SOUTH);
        tv(29, 1, 26, Direction.NORTH, true, Channels.CH_7);
        facing(20, 4, 14, ModBlocks.ON_AIR_SIGN.get(), Direction.SOUTH);
        facing(26, 4, 14, ModBlocks.WALL_CLOCK.get(), Direction.SOUTH);
        ceilingLights(new int[][]{{18, 17}, {18, 24}, {26, 20}});
    }

    private void buildTower() {
        fill(26, 0, -9, 34, 0, -1, Blocks.SMOOTH_STONE);
        set(30, 1, -5, ModBlocks.TRANSMITTER_COIL.get());
        for (int y = 1; y <= 24; y++) {
            int inset = Math.min(2, (y - 1) / 8);
            int x0 = 28 + inset, x1 = 32 - inset, z0 = -7 + inset, z1 = -3 - inset;
            BlockState bars = Blocks.IRON_BARS.defaultBlockState();
            set(x0, y, z0, bars);
            set(x1, y, z0, bars);
            set(x0, y, z1, bars);
            set(x1, y, z1, bars);
            if (y % 6 == 0 || y == 1) {
                for (int x = x0; x <= x1; x++) {
                    set(x, y, z0, bars);
                    set(x, y, z1, bars);
                }
                for (int z = z0; z <= z1; z++) {
                    set(x0, y, z, bars);
                    set(x1, y, z, bars);
                }
            }
        }
        // inner mast above the coil, then the Skyhook antenna and its beacon
        for (int y = 2; y <= 24; y++) set(30, y, -5, Blocks.IRON_BARS.defaultBlockState());
        set(30, 25, -5, ModBlocks.CHROME_TRIM.get());
        for (int y = 26; y <= 29; y++) set(30, y, -5, Blocks.LIGHTNING_ROD.defaultBlockState());
        set(30, 30, -5, ModBlocks.TOWER_BEACON.get());
        set(29, 13, -5, ModBlocks.TOWER_BEACON.get());
        set(31, 13, -5, ModBlocks.TOWER_BEACON.get());
        // feedline from the hall
        set(30, 4, -1, Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Z));
        for (int z = -2; z >= -4; z--) set(30, 4, z, Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Z));
    }

    private void buildExterior() {
        // parking lot and walkway
        fill(-4, 0, 28, 39, 0, 35, Blocks.GRAY_CONCRETE);
        for (int x = 14; x <= 34; x += 4) fill(x, 0, 29, x, 0, 33, Blocks.WHITE_CONCRETE);
        fill(3, 0, 28, 8, 0, 35, ModBlocks.TERRAZZO.get());
        // entrance canopy
        for (int y = 1; y <= 4; y++) {
            set(2, y, 31, ModBlocks.CHROME_TRIM.get());
            set(9, y, 31, ModBlocks.CHROME_TRIM.get());
        }
        fill(1, 5, 28, 10, 5, 32, Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState());
        fill(1, 5, 32, 10, 5, 32, ModBlocks.CHROME_TRIM.get());
        set(5, 4, 28, ModBlocks.CEILING_LIGHT.get());
        set(6, 4, 28, ModBlocks.CEILING_LIGHT.get());
        // lot lamps
        set(-2, 1, 33, ModBlocks.GOOGIE_LAMP.get());
        set(13, 1, 33, ModBlocks.GOOGIE_LAMP.get());
        set(37, 1, 33, ModBlocks.GOOGIE_LAMP.get());
        // pylon sign with a block-letter "7"
        for (int y = 1; y <= 12; y++) set(-3, y, 28, ModBlocks.CHROME_TRIM.get());
        fill(-7, 6, 29, -1, 12, 29, Blocks.RED_CONCRETE);
        BlockState w = Blocks.WHITE_CONCRETE.defaultBlockState();
        String[] seven = {"#####", "#####", "...##", "...##", "..##.", "..##.", ".##.."};
        for (int row = 0; row < seven.length; row++) {
            for (int col = 0; col < 5; col++) {
                if (seven[row].charAt(col) == '#') set(-6 + col, 12 - row, 29, w);
            }
        }
        fill(-7, 13, 29, -1, 13, 29, ModBlocks.CHROME_TRIM.get());
        set(-3, 14, 29, ModBlocks.TOWER_BEACON.get());
        set(-4, 14, 29, Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.WEST));
        set(-2, 14, 29, Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.EAST));
        set(-3, 15, 29, Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.UP));
        // planters along the front
        for (int x : new int[]{11, 12, 20, 28}) {
            set(x, 1, 28, Blocks.POTTED_FERN);
        }
    }

    private void finishShapes() {
        for (BlockPos p : reshape) {
            BlockState s = level.getBlockState(p);
            BlockState updated = Block.updateFromNeighbourShapes(s, level, p);
            if (updated != s) level.setBlock(p, updated, 2 | 16);
        }
        reshape.clear();
    }

    private void spawnStaff() {
        // Seated: Rosa at master control (facing the monitor wall), Brandt behind his desk.
        StaffNpc rosa = StaffNpc.create(level, "rosa", at(19, 1, 4), 180f, true, List.of(), List.of());
        StaffNpc brandt = StaffNpc.create(level, "brandt", at(5, 1, 12), 0f, true, List.of(), List.of());
        StaffNpc penrose = StaffNpc.create(level, "penrose", at(30, 1, 5), 180f, false,
                List.of(at(30, 1, 5), at(33, 1, 6), at(28, 1, 11), at(31, 1, 3)), List.of(180f, -90f, 0f, 180f));
        StaffNpc dot = StaffNpc.create(level, "dot", at(30, 1, 21), 90f, false,
                List.of(at(30, 1, 21), at(29, 1, 24), at(23, 1, 20), at(27, 1, 18)), List.of(90f, 0f, -90f, 90f));
        for (StaffNpc n : List.of(rosa, brandt, penrose, dot)) level.addFreshEntity(n);
    }
}
