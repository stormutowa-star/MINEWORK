package com.lastbroadcast.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record OpenEndingPacket() {
    public void encode(RegistryFriendlyByteBuf buf) {
    }

    public static OpenEndingPacket decode(RegistryFriendlyByteBuf buf) {
        return new OpenEndingPacket();
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.setPacketHandled(true);
        com.lastbroadcast.client.ClientPacketHandler.onEnding();
    }
}
