package com.lastbroadcast.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record OpenDialoguePacket(int entityId, String character) {
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeUtf(character, 32);
    }

    public static OpenDialoguePacket decode(RegistryFriendlyByteBuf buf) {
        return new OpenDialoguePacket(buf.readVarInt(), buf.readUtf(32));
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.setPacketHandled(true);
        com.lastbroadcast.client.ClientPacketHandler.onOpenDialogue(entityId, character);
    }
}
