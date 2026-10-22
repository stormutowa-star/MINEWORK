package com.lastbroadcast.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Case-file notifications shown as a typed banner on the HUD. */
public record NoticePacket(int kind, String text) {
    public static final int INFO = 0, EVIDENCE = 1, DEDUCTION = 2, NO_MATCH = 3, WARNING = 4;

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(kind);
        buf.writeUtf(text, 512);
    }

    public static NoticePacket decode(RegistryFriendlyByteBuf buf) {
        return new NoticePacket(buf.readVarInt(), buf.readUtf(512));
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.setPacketHandled(true);
        com.lastbroadcast.client.ClientPacketHandler.onNotice(kind, text);
    }
}
