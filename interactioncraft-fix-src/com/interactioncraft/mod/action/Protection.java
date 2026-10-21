/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.tags.BlockTags
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.LiquidBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.BlockStateProperties
 *  net.minecraft.world.level.block.state.properties.Property
 */
package com.interactioncraft.mod.action;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

final class Protection {
    static final int NATURAL = 0;
    static final int AMBIGUOUS = 1;
    static final int MADE = 2;
    private static final Map<Block, Integer> KINDS = new ConcurrentHashMap<Block, Integer>();
    private static final Set<String> NATURAL_PATHS = Set.of("gravel", "clay", "snow_block", "powder_snow", "soul_sand", "soul_soil", "magma_block", "sugar_cane", "cactus", "pumpkin", "melon", "bamboo", "bamboo_sapling", "sweet_berry_bush", "kelp", "kelp_plant", "lily_pad", "brown_mushroom_block", "red_mushroom_block", "mushroom_stem", "moss_block", "moss_carpet", "mud", "mangrove_roots", "muddy_mangrove_roots", "vine", "glow_lichen", "cave_vines", "cave_vines_plant", "pointed_dripstone", "cobweb", "end_stone", "azalea", "flowering_azalea", "bedrock", "hanging_roots", "spore_blossom", "big_dripleaf", "big_dripleaf_stem", "small_dripleaf", "seagrass", "tall_seagrass", "fern", "large_fern", "tall_grass", "short_grass", "grass", "dead_bush", "ancient_debris", "sculk_vein", "sculk", "sea_pickle", "turtle_egg", "coral", "brain_coral", "bubble_column", "water", "lava");
    private static final Set<String> AMBIGUOUS_PATHS = Set.of("calcite", "smooth_basalt", "sandstone", "red_sandstone", "tuff", "dripstone_block", "basalt", "blackstone");

    private Protection() {
    }

    static int kind(BlockState st) {
        if (st.isAir()) {
            return 0;
        }
        return KINDS.computeIfAbsent(st.getBlock(), b -> Protection.compute(st));
    }

    private static int compute(BlockState st) {
        Block b = st.getBlock();
        if (b instanceof LiquidBlock) {
            return 0;
        }
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(b);
        String path = key.getPath();
        boolean vanilla = "minecraft".equals(key.getNamespace());
        if (path.endsWith("_ore") || path.contains("_ore_")) {
            return 0;
        }
        if (st.is(BlockTags.CROPS) || b instanceof net.minecraft.world.level.block.CropBlock || b instanceof net.minecraft.world.level.block.StemBlock
                || b instanceof net.minecraft.world.level.block.AttachedStemBlock || b instanceof net.minecraft.world.level.block.BushBlock && vanilla
                || path.equals("nether_wart") || path.equals("cocoa") || path.equals("sweet_berry_bush") || path.equals("torchflower_crop") || path.equals("pitcher_crop")
                || path.endsWith("_sapling") || path.endsWith("_mushroom") || path.equals("chorus_flower") || path.equals("chorus_plant") || path.equals("bee_nest")) {
            return 0;
        }
        if (path.startsWith("stripped_") || path.endsWith("_wood") || path.endsWith("_hyphae") || path.endsWith("_planks")) {
            return 2;
        }
        if (st.is(BlockTags.LOGS)) {
            return 1;
        }
        if (st.is(BlockTags.LEAVES) || st.is(BlockTags.DIRT) || st.is(BlockTags.SAND) || st.is(BlockTags.FLOWERS) || st.is(BlockTags.SAPLINGS) || st.is(BlockTags.SNOW) || st.is(BlockTags.ICE) || st.is(BlockTags.NYLIUM) || st.is(BlockTags.WART_BLOCKS)) {
            return 0;
        }
        if (st.is(BlockTags.BASE_STONE_OVERWORLD) || st.is(BlockTags.BASE_STONE_NETHER) || st.is(BlockTags.TERRACOTTA)) {
            return 1;
        }
        if (!vanilla) {
            return 2;
        }
        if (AMBIGUOUS_PATHS.contains(path)) {
            return 1;
        }
        if (NATURAL_PATHS.contains(path) || path.endsWith("_coral") || path.endsWith("_coral_block") || path.endsWith("_coral_fan")) {
            return 0;
        }
        if (st.canBeReplaced()) {
            return 0;
        }
        return 2;
    }

    static boolean made(BlockState st) {
        return !st.isAir() && Protection.kind(st) == 2;
    }

    static boolean nearMade(ServerLevel level, BlockPos p, int r) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; ++dx) {
            for (int dz = -r; dz <= r; ++dz) {
                for (int dy = -r; dy <= r; ++dy) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    m.set(p.getX() + dx, p.getY() + dy, p.getZ() + dz);
                    if (!level.hasChunkAt((BlockPos)m) || !Protection.made(level.getBlockState((BlockPos)m))) continue;
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean naturalLeavesNear(ServerLevel level, BlockPos p) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -3; dx <= 3; ++dx) {
            for (int dz = -3; dz <= 3; ++dz) {
                for (int dy = -1; dy <= 7; ++dy) {
                    BlockState st;
                    m.set(p.getX() + dx, p.getY() + dy, p.getZ() + dz);
                    if (!level.hasChunkAt((BlockPos)m) || !(st = level.getBlockState((BlockPos)m)).is(BlockTags.LEAVES) || !st.hasProperty((Property)BlockStateProperties.PERSISTENT) || ((Boolean)st.getValue((Property)BlockStateProperties.PERSISTENT)).booleanValue()) continue;
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean treeLog(ServerLevel level, BlockPos start, Map<Long, Boolean> cache) {
        boolean tree;
        ArrayDeque<BlockPos> queue = new ArrayDeque<BlockPos>();
        ArrayList<BlockPos> cluster = new ArrayList<BlockPos>();
        HashSet<Long> seen = new HashSet<Long>();
        queue.add(start.immutable());
        seen.add(start.asLong());
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        while (!queue.isEmpty()) {
            BlockPos p = (BlockPos)queue.poll();
            cluster.add(p);
            if (cluster.size() >= 160) break;
            for (int dx = -1; dx <= 1; ++dx) {
                for (int dz = -1; dz <= 1; ++dz) {
                    for (int dy = -1; dy <= 1; ++dy) {
                        BlockState st;
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        m.set(p.getX() + dx, p.getY() + dy, p.getZ() + dz);
                        if (!level.hasChunkAt((BlockPos)m) || seen.contains(m.asLong()) || !(st = level.getBlockState((BlockPos)m)).is(BlockTags.LOGS) || Protection.kind(st) != 1) continue;
                        seen.add(m.asLong());
                        queue.add(m.immutable());
                    }
                }
            }
        }
        boolean leaves = false;
        boolean made = false;
        for (BlockPos p : cluster) {
            if (!leaves && Protection.naturalLeavesNear(level, p)) {
                leaves = true;
            }
            if (made || !Protection.nearMade(level, p, 2)) continue;
            made = true;
            break;
        }
        boolean bl = tree = leaves && !made;
        if (cache != null) {
            for (BlockPos p : cluster) {
                cache.put(p.asLong(), !tree);
            }
        }
        return tree;
    }

    static boolean built(ServerLevel level, BlockPos pos, BlockState st, Map<Long, Boolean> cache) {
        Boolean c;
        if (st.isAir()) {
            return false;
        }
        int k = Protection.kind(st);
        if (k == 0) {
            return false;
        }
        if (k == 2) {
            return true;
        }
        Boolean bl = c = cache == null ? null : cache.get(pos.asLong());
        if (c != null) {
            return c;
        }
        boolean r = st.is(BlockTags.LOGS) ? !Protection.treeLog(level, pos, cache) : Protection.nearMade(level, pos, 3);
        if (cache != null) {
            cache.put(pos.asLong(), r);
        }
        return r;
    }

    static boolean clearable(ServerLevel level, BlockPos pos, BlockState st, Map<Long, Boolean> cache) {
        if (st.isAir() || Protection.isFluid(st)) {
            return true;
        }
        return st.getDestroySpeed((BlockGetter)level, pos) >= 0.0f && !Protection.built(level, pos, st, cache);
    }

    private static boolean isFluid(BlockState st) {
        return st.getBlock() instanceof LiquidBlock;
    }

    static boolean liquidNeighbor(ServerLevel level, BlockPos p) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (Direction d : Direction.values()) {
            m.setWithOffset((Vec3i)p, d);
            if (!level.hasChunkAt((BlockPos)m) || level.getFluidState((BlockPos)m).isEmpty()) continue;
            return true;
        }
        return false;
    }
}

