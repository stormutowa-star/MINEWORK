package com.lastbroadcast.network;

import com.lastbroadcast.blockentity.RecorderBlockEntity;
import com.lastbroadcast.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record RecorderActionPacket(BlockPos pos, int action) {
    public static final int RECORD = 0, EJECT = 1;

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(action);
    }

    public static RecorderActionPacket decode(RegistryFriendlyByteBuf buf) {
        return new RecorderActionPacket(buf.readBlockPos(), buf.readVarInt());
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.setPacketHandled(true);
        ServerPlayer player = ctx.getSender();
        if (!PacketUtil.canReach(player, pos)) return;
        if (!(player.level().getBlockEntity(pos) instanceof RecorderBlockEntity deck)) return;
        if (action == RECORD) {
            deck.startRecording(player);
        } else if (action == EJECT && !deck.getTape().isEmpty() && !deck.isRecording()) {
            ItemStack out = deck.eject();
            if (!player.getInventory().add(out)) player.drop(out, false);
            player.level().playSound(null, pos, ModSounds.TAPE_EJECT.get(), SoundSource.BLOCKS, 0.9f, 1f);
        }
    }
}
