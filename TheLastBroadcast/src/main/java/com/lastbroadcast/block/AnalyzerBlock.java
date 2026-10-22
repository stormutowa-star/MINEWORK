package com.lastbroadcast.block;

import com.lastbroadcast.ClientBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Bench signal analyzer: tuning puzzle to isolate the ghost carrier. */
public class AnalyzerBlock extends DecorBlock {
    public AnalyzerBlock(Properties props, ModelShape shape) {
        super(props, shape);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) ClientBridge.openAnalyzer(pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
