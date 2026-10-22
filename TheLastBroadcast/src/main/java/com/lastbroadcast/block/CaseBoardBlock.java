package com.lastbroadcast.block;

import com.lastbroadcast.ClientBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Cork evidence board: opens the case file (same as the journal item). */
public class CaseBoardBlock extends DecorBlock {
    public CaseBoardBlock(Properties props, ModelShape shape) {
        super(props, shape);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) ClientBridge.openJournal();
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
