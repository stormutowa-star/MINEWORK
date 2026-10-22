package com.lastbroadcast.network;

import com.lastbroadcast.block.LockerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record KeypadPacket(BlockPos pos, String code) {
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(code, 8);
    }

    public static KeypadPacket decode(RegistryFriendlyByteBuf buf) {
        return new KeypadPacket(buf.readBlockPos(), buf.readUtf(8));
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.setPacketHandled(true);
        if (PacketUtil.canReach(ctx.getSender(), pos)) LockerBlock.tryCode(ctx.getSender(), pos, code);
    }
}
