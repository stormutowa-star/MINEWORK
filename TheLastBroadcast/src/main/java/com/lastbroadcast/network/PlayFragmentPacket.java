package com.lastbroadcast.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Plays a short audio fragment (telephone line) with subtitles on the client. */
public record PlayFragmentPacket(String fragment) {
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(fragment, 64);
    }

    public static PlayFragmentPacket decode(RegistryFriendlyByteBuf buf) {
        return new PlayFragmentPacket(buf.readUtf(64));
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.setPacketHandled(true);
        com.lastbroadcast.client.ClientPacketHandler.onFragment(fragment);
    }
}
