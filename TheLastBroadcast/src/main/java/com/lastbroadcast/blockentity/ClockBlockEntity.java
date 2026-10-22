package com.lastbroadcast.blockentity;

import com.lastbroadcast.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Stateless: exists so the wall clock's hands can be drawn by a block entity renderer. */
public class ClockBlockEntity extends BlockEntity {
    public ClockBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CLOCK.get(), pos, state);
    }
}
