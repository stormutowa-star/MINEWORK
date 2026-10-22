package com.lastbroadcast.network;

import com.lastbroadcast.story.StoryDirector;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record ComparePacket(String a, String b) {
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(a, 128);
        buf.writeUtf(b, 128);
    }

    public static ComparePacket decode(RegistryFriendlyByteBuf buf) {
        return new ComparePacket(buf.readUtf(128), buf.readUtf(128));
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.setPacketHandled(true);
        if (ctx.getSender() != null) StoryDirector.compare(ctx.getSender(), a, b);
    }
}
