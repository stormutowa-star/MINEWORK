package com.lastbroadcast.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Directional light fixture (studio lamps, ON AIR sign). */
public class FacingLampBlock extends DecorBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public FacingLampBlock(Properties props, ModelShape shape) {
        super(props, shape);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    /** Scheduled by flicker events: always restores the light. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.hasProperty(LIT) && !state.getValue(LIT)) level.setBlock(pos, state.setValue(LIT, true), 3);
    }
}
