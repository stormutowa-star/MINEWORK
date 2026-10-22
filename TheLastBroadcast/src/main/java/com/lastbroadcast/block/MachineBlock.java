package com.lastbroadcast.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiFunction;

/** Facing block with a block entity and optional server ticker. */
public class MachineBlock<T extends BlockEntity> extends DecorBlock implements EntityBlock {
    private final java.util.function.Supplier<BlockEntityType<T>> type;
    private final BiFunction<BlockPos, BlockState, T> factory;
    private final BlockEntityTicker<T> serverTicker;

    public MachineBlock(Properties props, ModelShape shape, java.util.function.Supplier<BlockEntityType<T>> type,
                        BiFunction<BlockPos, BlockState, T> factory, BlockEntityTicker<T> serverTicker) {
        super(props, shape);
        this.type = type;
        this.factory = factory;
        this.serverTicker = serverTicker;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return factory.apply(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends BlockEntity> BlockEntityTicker<E> getTicker(Level level, BlockState state, BlockEntityType<E> t) {
        if (level.isClientSide || serverTicker == null || t != type.get()) return null;
        return (BlockEntityTicker<E>) serverTicker;
    }

    @SuppressWarnings("unchecked")
    protected T be(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be != null && be.getType() == type.get() ? (T) be : null;
    }
}
