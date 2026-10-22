package com.lastbroadcast.block;

import com.lastbroadcast.ClientBridge;
import com.lastbroadcast.blockentity.LockerBlockEntity;
import com.lastbroadcast.registry.ModBlockEntities;
import com.lastbroadcast.registry.ModSounds;
import com.lastbroadcast.story.StoryDirector;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Penrose's engineering locker. Closed: combination wheels. Open: the Skyhook transmitter log. */
public class LockerBlock extends MachineBlock<LockerBlockEntity> {
    public LockerBlock(Properties props, ModelShape shape) {
        super(props, shape, ModBlockEntities.LOCKER, LockerBlockEntity::new, null);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        LockerBlockEntity locker = be(level, pos);
        if (locker == null) return InteractionResult.PASS;
        if (!locker.isOpen()) {
            if (level.isClientSide) ClientBridge.openKeypad(pos);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            level.playSound(null, pos, ModSounds.LOCKER.get(), SoundSource.BLOCKS, 0.8f, 1.1f);
            if (StoryDirector.playerHasDocument(player, "transmitter_log")) {
                player.displayClientMessage(Component.literal("Spare tubes, a slide rule, a thermos. You already took the log."), true);
            } else {
                StoryDirector.give(sp, "doc:transmitter_log", 1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Server: validate a combination attempt. */
    public static void tryCode(ServerPlayer player, BlockPos pos, String code) {
        Level level = player.level();
        if (!level.isLoaded(pos) || player.distanceToSqr(pos.getCenter()) > 64) return;
        if (!(level.getBlockEntity(pos) instanceof LockerBlockEntity locker) || locker.isOpen()) return;
        if (LockerBlockEntity.COMBINATION.equals(code)) {
            locker.setOpen(true);
            level.playSound(null, pos, ModSounds.LOCKER.get(), SoundSource.BLOCKS, 1f, 1f);
            StoryDirector.give(player, "doc:transmitter_log", 1);
        } else {
            level.playSound(null, pos, ModSounds.DENIED.get(), SoundSource.BLOCKS, 0.8f, 1f);
            player.displayClientMessage(Component.literal("The handle won't budge."), true);
        }
    }
}
