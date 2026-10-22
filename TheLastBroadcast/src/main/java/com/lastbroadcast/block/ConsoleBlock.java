package com.lastbroadcast.block;

import com.lastbroadcast.ClientBridge;
import com.lastbroadcast.blockentity.ConsoleBlockEntity;
import com.lastbroadcast.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Skyhook overdrive control console. */
public class ConsoleBlock extends MachineBlock<ConsoleBlockEntity> {
    public ConsoleBlock(Properties props, ModelShape shape) {
        super(props, shape, ModBlockEntities.CONSOLE, ConsoleBlockEntity::new, ConsoleBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) ClientBridge.openConsole(pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
