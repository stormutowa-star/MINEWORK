package com.lastbroadcast.network;

import com.lastbroadcast.LastBroadcast;
import com.lastbroadcast.story.CaseState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class Net {
    private static final int VERSION = 1;
    public static SimpleChannel CHANNEL;

    private Net() {}

    public static void init() {
        if (CHANNEL != null) return;
        CHANNEL = ChannelBuilder.named(LastBroadcast.id("main")).networkProtocolVersion(VERSION).simpleChannel();
        // client -> server
        c2s(TvControlPacket.class, TvControlPacket::encode, TvControlPacket::decode, TvControlPacket::handle);
        c2s(AnalyzerLockPacket.class, AnalyzerLockPacket::encode, AnalyzerLockPacket::decode, AnalyzerLockPacket::handle);
        c2s(RecorderActionPacket.class, RecorderActionPacket::encode, RecorderActionPacket::decode, RecorderActionPacket::handle);
        c2s(ComparePacket.class, ComparePacket::encode, ComparePacket::decode, ComparePacket::handle);
        c2s(DialogueActionPacket.class, DialogueActionPacket::encode, DialogueActionPacket::decode, DialogueActionPacket::handle);
        c2s(KeypadPacket.class, KeypadPacket::encode, KeypadPacket::decode, KeypadPacket::handle);
        c2s(ConsoleActionPacket.class, ConsoleActionPacket::encode, ConsoleActionPacket::decode, ConsoleActionPacket::handle);
        c2s(FrameExaminedPacket.class, FrameExaminedPacket::encode, FrameExaminedPacket::decode, FrameExaminedPacket::handle);
        // server -> client
        s2c(CaseSyncPacket.class, CaseSyncPacket::encode, CaseSyncPacket::decode, CaseSyncPacket::handle);
        s2c(NoticePacket.class, NoticePacket::encode, NoticePacket::decode, NoticePacket::handle);
        s2c(PlayFragmentPacket.class, PlayFragmentPacket::encode, PlayFragmentPacket::decode, PlayFragmentPacket::handle);
        s2c(OpenEndingPacket.class, OpenEndingPacket::encode, OpenEndingPacket::decode, OpenEndingPacket::handle);
        s2c(OpenDialoguePacket.class, OpenDialoguePacket::encode, OpenDialoguePacket::decode, OpenDialoguePacket::handle);
        CHANNEL.build();
    }

    private static <M> void c2s(Class<M> type, BiConsumer<M, RegistryFriendlyByteBuf> enc, Function<RegistryFriendlyByteBuf, M> dec,
                                BiConsumer<M, CustomPayloadEvent.Context> handler) {
        CHANNEL.messageBuilder(type, NetworkDirection.PLAY_TO_SERVER).encoder(enc).decoder(dec).consumerMainThread(handler).add();
    }

    private static <M> void s2c(Class<M> type, BiConsumer<M, RegistryFriendlyByteBuf> enc, Function<RegistryFriendlyByteBuf, M> dec,
                                BiConsumer<M, CustomPayloadEvent.Context> handler) {
        CHANNEL.messageBuilder(type, NetworkDirection.PLAY_TO_CLIENT).encoder(enc).decoder(dec).consumerMainThread(handler).add();
    }

    public static void toServer(Object msg) {
        CHANNEL.send(msg, PacketDistributor.SERVER.noArg());
    }

    public static void toPlayer(ServerPlayer player, Object msg) {
        CHANNEL.send(msg, PacketDistributor.PLAYER.with(player));
    }

    public static void toAll(MinecraftServer server, Object msg) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) toPlayer(p, msg);
    }

    public static void syncAll(MinecraftServer server, CaseState state) {
        toAll(server, new CaseSyncPacket(state));
    }
}
