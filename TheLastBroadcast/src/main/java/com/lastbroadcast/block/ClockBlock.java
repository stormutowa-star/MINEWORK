package com.lastbroadcast.block;

import com.lastbroadcast.blockentity.ClockBlockEntity;
import com.lastbroadcast.registry.ModBlockEntities;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/** Wall clock; hands drawn by ClockRenderer (follows the day, or stalls at 9:14). */
public class ClockBlock extends MachineBlock<ClockBlockEntity> {
    public ClockBlock(Properties props, ModelShape shape) {
        super(props, shape, ModBlockEntities.CLOCK, ClockBlockEntity::new, null);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
