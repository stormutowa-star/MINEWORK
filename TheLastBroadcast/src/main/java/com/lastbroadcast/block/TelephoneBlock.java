package com.lastbroadcast.block;

import com.lastbroadcast.network.Net;
import com.lastbroadcast.network.PlayFragmentPacket;
import com.lastbroadcast.registry.ModSounds;
import com.lastbroadcast.story.StoryDirector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Master-control telephone. Rings when the echo leaks into the exchange line. */
public class TelephoneBlock extends DecorBlock {
    public static final BooleanProperty RINGING = BooleanProperty.create("ringing");

    public TelephoneBlock(Properties props, ModelShape shape) {
        super(props, shape);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(RINGING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, RINGING);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.CONSUME;
        level.playSound(null, pos, ModSounds.PHONE_PICKUP.get(), SoundSource.BLOCKS, 0.9f, 1f);
        if (state.getValue(RINGING)) {
            level.setBlock(pos, state.setValue(RINGING, false), 3);
            StoryDirector.stopRinging(pos);
            Net.toPlayer(sp, new PlayFragmentPacket(StoryDirector.nextFragment()));
        } else {
            Net.toPlayer(sp, new PlayFragmentPacket("dial_tone"));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(RINGING) && random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, random.nextDouble(), 0, 0);
        }
    }
}
