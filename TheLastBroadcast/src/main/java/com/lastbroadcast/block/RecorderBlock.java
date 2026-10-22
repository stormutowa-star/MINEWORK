package com.lastbroadcast.block;

import com.lastbroadcast.ClientBridge;
import com.lastbroadcast.blockentity.RecorderBlockEntity;
import com.lastbroadcast.item.TapeItem;
import com.lastbroadcast.registry.ModBlockEntities;
import com.lastbroadcast.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Reel-to-reel deck. Use with tape: thread it. Use: transport controls. Sneak-use: eject. */
public class RecorderBlock extends MachineBlock<RecorderBlockEntity> {
    public RecorderBlock(Properties props, ModelShape shape) {
        super(props, shape, ModBlockEntities.RECORDER, RecorderBlockEntity::new, RecorderBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof TapeItem)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        RecorderBlockEntity deck = be(level, pos);
        if (deck == null) return ItemInteractionResult.FAIL;
        if (!deck.getTape().isEmpty()) {
            if (level.isClientSide) player.displayClientMessage(Component.translatable("lastbroadcast.msg.reel_threaded"), true);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            deck.insert(stack);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            level.playSound(null, pos, ModSounds.TAPE_INSERT.get(), SoundSource.BLOCKS, 0.9f, 1f);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        RecorderBlockEntity deck = be(level, pos);
        if (deck == null) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide && !deck.getTape().isEmpty()) {
                ItemStack out = deck.eject();
                if (!player.getInventory().add(out)) player.drop(out, false);
                level.playSound(null, pos, ModSounds.TAPE_EJECT.get(), SoundSource.BLOCKS, 0.9f, 1f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) {
            if (deck.getTape().isEmpty()) player.displayClientMessage(Component.translatable("lastbroadcast.msg.no_reel"), true);
            else ClientBridge.openRecorder(pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            RecorderBlockEntity deck = be(level, pos);
            if (deck != null) deck.dropContents(level);
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
