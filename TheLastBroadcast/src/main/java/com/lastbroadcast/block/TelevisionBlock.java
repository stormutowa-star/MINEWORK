package com.lastbroadcast.block;

import com.lastbroadcast.ClientBridge;
import com.lastbroadcast.blockentity.TelevisionBlockEntity;
import com.lastbroadcast.registry.ModBlockEntities;
import com.lastbroadcast.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Console television receiver. Use: open the tuning dial. Sneak-use: power toggle. */
public class TelevisionBlock extends MachineBlock<TelevisionBlockEntity> {
    public TelevisionBlock(Properties props, ModelShape shape) {
        super(props, shape, ModBlockEntities.TELEVISION, TelevisionBlockEntity::new, null);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        TelevisionBlockEntity tv = be(level, pos);
        if (tv == null) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                tv.setState(!tv.isPowered(), tv.getChannel());
                level.playSound(null, pos, tv.isPowered() ? ModSounds.TV_ON.get() : ModSounds.TV_OFF.get(), SoundSource.BLOCKS, 0.8f, 1f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) ClientBridge.openTelevision(pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
