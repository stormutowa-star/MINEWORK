package com.lastbroadcast.network;

import com.lastbroadcast.entity.StaffNpc;
import com.lastbroadcast.story.DialogueLogic;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** choice >= 0: pick a choice in node; choice == ENTER: node shown; choice == CLOSE: conversation ended. */
public record DialogueActionPacket(int entityId, String node, int choice) {
    public static final int ENTER = -1, CLOSE = -2;

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeUtf(node, 64);
        buf.writeVarInt(choice);
    }

    public static DialogueActionPacket decode(RegistryFriendlyByteBuf buf) {
        return new DialogueActionPacket(buf.readVarInt(), buf.readUtf(64), buf.readVarInt());
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.setPacketHandled(true);
        ServerPlayer player = ctx.getSender();
        if (player == null || !(player.level().getEntity(entityId) instanceof StaffNpc npc)) return;
        if (choice == CLOSE) {
            npc.endConversation(player);
            return;
        }
        if (npc.distanceToSqr(player) > 10 * 10) return;
        DialogueLogic.apply(player, npc.getCharacter(), node, choice);
    }
}
