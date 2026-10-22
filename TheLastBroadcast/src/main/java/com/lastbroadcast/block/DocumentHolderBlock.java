package com.lastbroadcast.block;

import com.lastbroadcast.blockentity.DocumentHolderBlockEntity;
import com.lastbroadcast.content.ContentLibrary;
import com.lastbroadcast.registry.ModBlockEntities;
import com.lastbroadcast.registry.ModSounds;
import com.lastbroadcast.story.StoryDirector;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Supplier;

/** Filing cabinet or notice board. Hands out a copy of each document it holds (never runs dry). */
public class DocumentHolderBlock extends MachineBlock<DocumentHolderBlockEntity> {
    private final String emptyText;
    private final Supplier<SoundEvent> sound;

    public DocumentHolderBlock(Properties props, ModelShape shape, String emptyText, Supplier<SoundEvent> sound) {
        super(props, shape, ModBlockEntities.DOCUMENT_HOLDER, DocumentHolderBlockEntity::new, null);
        this.emptyText = emptyText;
        this.sound = sound;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        DocumentHolderBlockEntity holder = be(level, pos);
        if (holder == null || !(player instanceof ServerPlayer sp)) return InteractionResult.CONSUME;
        level.playSound(null, pos, sound.get(), SoundSource.BLOCKS, 0.8f, 0.9f + level.random.nextFloat() * 0.2f);
        boolean gaveAny = false;
        StringBuilder already = new StringBuilder();
        for (String token : holder.getContents()) {
            if (token.startsWith("doc:")) {
                String id = token.substring(4);
                if (StoryDirector.playerHasDocument(player, id)) {
                    if (already.length() > 0) already.append(", ");
                    already.append(ContentLibrary.evidenceTitle(token));
                    continue;
                }
                StoryDirector.give(sp, token, 1);
                gaveAny = true;
            } else if (token.equals("tape:blank")) {
                StoryDirector.give(sp, token, 2);
                player.displayClientMessage(Component.translatable("lastbroadcast.msg.take_tape"), true);
                gaveAny = true;
            }
        }
        if (!gaveAny) {
            if (already.length() > 0) player.displayClientMessage(Component.translatable("lastbroadcast.msg.already_carry", already.toString()), true);
            else player.displayClientMessage(Component.translatable(emptyText), true);
        }
        return InteractionResult.CONSUME;
    }
}
