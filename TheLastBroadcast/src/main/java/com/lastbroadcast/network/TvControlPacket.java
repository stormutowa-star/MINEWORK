package com.lastbroadcast.network;

import com.lastbroadcast.blockentity.TelevisionBlockEntity;
import com.lastbroadcast.broadcast.Channels;
import com.lastbroadcast.registry.ModSounds;
import com.lastbroadcast.story.CaseData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record TvControlPacket(BlockPos pos, boolean power, int channel) {
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(power);
        buf.writeVarInt(channel);
    }

    public static TvControlPacket decode(RegistryFriendlyByteBuf buf) {
        return new TvControlPacket(buf.readBlockPos(), buf.readBoolean(), buf.readVarInt());
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.setPacketHandled(true);
        ServerPlayer player = ctx.getSender();
        if (!PacketUtil.canReach(player, pos)) return;
        if (!(player.level().getBlockEntity(pos) instanceof TelevisionBlockEntity tv)) return;
        int ch = Channels.selectable(CaseData.get(player.server).state, channel) ? channel : tv.getChannel();
        boolean wasOn = tv.isPowered();
        int oldCh = tv.getChannel();
        tv.setState(power, ch);
        if (wasOn != power) {
            player.level().playSound(null, pos, power ? ModSounds.TV_ON.get() : ModSounds.TV_OFF.get(), SoundSource.BLOCKS, 0.8f, 1f);
        } else if (oldCh != ch) {
            player.level().playSound(null, pos, ModSounds.CHANNEL_CLICK.get(), SoundSource.BLOCKS, 0.8f, 1f);
        }
    }
}
