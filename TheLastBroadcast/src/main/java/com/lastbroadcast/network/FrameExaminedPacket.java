package com.lastbroadcast.network;

import com.lastbroadcast.content.Broadcast;
import com.lastbroadcast.content.ContentLibrary;
import com.lastbroadcast.story.CaseData;
import com.lastbroadcast.story.StoryDirector;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Sent when the player freezes playback on a single-frame insert. */
public record FrameExaminedPacket(String broadcastId) {
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(broadcastId, 128);
    }

    public static FrameExaminedPacket decode(RegistryFriendlyByteBuf buf) {
        return new FrameExaminedPacket(buf.readUtf(128));
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.setPacketHandled(true);
        if (ctx.getSender() == null) return;
        Broadcast b = ContentLibrary.broadcast(broadcastId);
        if (b == null || b.flash == null || b.flash.evidence == null) return;
        // The frame only exists on a tape the investigation has actually recorded.
        if (!CaseData.get(ctx.getSender().server).state.evidence.contains("tape:" + b.id)) return;
        StoryDirector.logEvidence(ctx.getSender().server, b.flash.evidence);
    }
}
