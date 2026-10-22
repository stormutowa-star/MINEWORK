package com.lastbroadcast.network;

import com.lastbroadcast.story.CaseState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record CaseSyncPacket(CaseState state) {
    public CaseSyncPacket {
        state = state.copy();
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        state.write(buf);
    }

    public static CaseSyncPacket decode(RegistryFriendlyByteBuf buf) {
        return new CaseSyncPacket(CaseState.read(buf));
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.setPacketHandled(true);
        com.lastbroadcast.client.ClientPacketHandler.onCaseSync(state);
    }
}
