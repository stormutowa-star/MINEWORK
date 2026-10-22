package com.lastbroadcast.network;

import com.lastbroadcast.blockentity.ConsoleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record ConsoleActionPacket(BlockPos pos, int action, int index) {
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(action);
        buf.writeVarInt(index);
    }

    public static ConsoleActionPacket decode(RegistryFriendlyByteBuf buf) {
        return new ConsoleActionPacket(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt());
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.setPacketHandled(true);
        if (!PacketUtil.canReach(ctx.getSender(), pos)) return;
        if (ctx.getSender().level().getBlockEntity(pos) instanceof ConsoleBlockEntity console) console.action(ctx.getSender(), action, index);
    }
}
