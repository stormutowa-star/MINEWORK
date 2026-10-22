package com.lastbroadcast.network;

import com.lastbroadcast.registry.ModBlocks;
import com.lastbroadcast.registry.ModSounds;
import com.lastbroadcast.story.Flags;
import com.lastbroadcast.story.StoryDirector;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Analyzer "PATCH TO MONITOR BUS": server re-checks the tuning against the ghost carrier. */
public record AnalyzerLockPacket(BlockPos pos, int freqCenti, int phase, int hold) {
    public static final int TARGET_FREQ = 17525, TARGET_PHASE = 180, TARGET_HOLD = 37;

    public static boolean locked(int freqCenti, int phase, int hold) {
        int dp = Math.abs(((phase - TARGET_PHASE) % 360 + 540) % 360 - 180);
        return Math.abs(freqCenti - TARGET_FREQ) <= 10 && dp <= 10 && Math.abs(hold - TARGET_HOLD) <= 2;
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(freqCenti);
        buf.writeVarInt(phase);
        buf.writeVarInt(hold);
    }

    public static AnalyzerLockPacket decode(RegistryFriendlyByteBuf buf) {
        return new AnalyzerLockPacket(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ctx.setPacketHandled(true);
        ServerPlayer player = ctx.getSender();
        if (!PacketUtil.canReach(player, pos) || !player.level().getBlockState(pos).is(ModBlocks.SIGNAL_ANALYZER.get())) return;
        if (!locked(freqCenti, phase, hold)) {
            Net.toPlayer(player, new NoticePacket(NoticePacket.INFO, com.lastbroadcast.Tx.key("lastbroadcast.msg.no_stable")));
            return;
        }
        player.level().playSound(null, pos, ModSounds.RELAY.get(), SoundSource.BLOCKS, 1f, 1f);
        if (!StoryDirector.has(player.server, Flags.SIGNAL_LOCKED)) {
            StoryDirector.setFlag(player.server, Flags.SIGNAL_LOCKED);
            Net.toAll(player.server, new NoticePacket(NoticePacket.DEDUCTION, com.lastbroadcast.Tx.key("lastbroadcast.msg.ghost_isolated")));
        } else {
            Net.toPlayer(player, new NoticePacket(NoticePacket.INFO, com.lastbroadcast.Tx.key("lastbroadcast.msg.already_patched")));
        }
    }
}
