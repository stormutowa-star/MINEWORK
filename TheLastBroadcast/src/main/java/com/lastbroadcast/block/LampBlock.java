package com.lastbroadcast.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Non-directional light fixture. LIT is toggled by the story director for flicker events. */
public class LampBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private final ModelShape shape;

    public LampBlock(Properties props, ModelShape shape) {
        super(props);
        this.shape = shape;
        registerDefaultState(stateDefinition.any().setValue(LIT, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shape.get(Direction.NORTH);
    }

    /** Scheduled by flicker events: always restores the light. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.hasProperty(LIT) && !state.getValue(LIT)) level.setBlock(pos, state.setValue(LIT, true), 3);
    }
}
