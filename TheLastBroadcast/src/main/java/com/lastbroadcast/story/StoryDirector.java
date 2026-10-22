package com.lastbroadcast.story;

import com.lastbroadcast.LastBroadcast;
import com.lastbroadcast.block.FacingLampBlock;
import com.lastbroadcast.block.LampBlock;
import com.lastbroadcast.block.TelephoneBlock;
import com.lastbroadcast.blockentity.TelevisionBlockEntity;
import com.lastbroadcast.broadcast.Channels;
import com.lastbroadcast.content.Broadcast;
import com.lastbroadcast.content.ContentLibrary;
import com.lastbroadcast.content.DeductionDef;
import com.lastbroadcast.item.DocumentItem;
import com.lastbroadcast.item.TapeItem;
import com.lastbroadcast.network.Net;
import com.lastbroadcast.network.NoticePacket;
import com.lastbroadcast.registry.ModItems;
import com.lastbroadcast.registry.ModSounds;
import com.lastbroadcast.world.StationBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.*;

/**
 * Server-side story logic: milestone side effects, evidence, deductions, the ending, and the
 * "echo leak" events (monitor surges, flickering lights, the master-control telephone, stalled
 * clocks). Leak events only start once the player has proved the overdrive exists (D2) and stop
 * for good when the case is solved. They never harm the player.
 */
public final class StoryDirector {
    private static final String KIT_TAG = "lastbroadcast_kit";
    public static final List<String> PHANTOM_ORDER = List.of("p1_today_thursday", "p2_lucky_dial", "p3_report_thursday", "p4_last_broadcast");

    private static final Map<UUID, Long> nextLeak = new HashMap<>();
    private static final Map<UUID, Integer> lastLeak = new HashMap<>();
    private static final Map<BlockPos, Long> ringingUntil = new HashMap<>();
    private static int fragmentCursor = 0;

    private StoryDirector() {}

    public static void register(IEventBus bus) {
        bus.addListener(StoryDirector::onLogin);
        bus.addListener(StoryDirector::onServerTick);
    }

    // ------------------------------------------------------------------ sync

    public static void sync(MinecraftServer server) {
        CaseData data = CaseData.get(server);
        data.publishStation();
        data.setDirty();
        Net.syncAll(server, data.state);
    }

    public static boolean has(MinecraftServer server, String flag) {
        return CaseData.get(server).state.has(flag);
    }

    // ------------------------------------------------------------------ flags

    public static void setFlag(MinecraftServer server, String flag) {
        CaseData data = CaseData.get(server);
        if (!data.state.flags.add(flag)) return;
        LastBroadcast.LOG.info("Story milestone reached: {}", flag);
        long now = server.overworld().getGameTime();
        switch (flag) {
            case Flags.SIGNAL_LOCKED, Flags.D1, Flags.D3 -> rebuildPhantomPlan(data, now, true);
            case Flags.D2 -> {
                for (ServerPlayer p : server.getPlayerList().getPlayers()) nextLeak.put(p.getUUID(), now + 900);
            }
            default -> {}
        }
        sync(server);
    }

    /** 7-B plays the newest unlocked echo first, then rotates through all unlocked echoes. */
    private static void rebuildPhantomPlan(CaseData data, long now, boolean newestFirst) {
        List<String> unlocked = new ArrayList<>();
        CaseState s = data.state;
        if (s.has(Flags.SIGNAL_LOCKED)) {
            unlocked.add("p1_today_thursday");
            unlocked.add("p2_lucky_dial");
        }
        if (s.has(Flags.D1)) unlocked.add("p3_report_thursday");
        if (s.has(Flags.D3)) unlocked.add("p4_last_broadcast");
        List<String> loop = new ArrayList<>();
        for (String id : unlocked) {
            loop.add(id);
            loop.add("static_gap");
        }
        List<String> intro = new ArrayList<>();
        if (newestFirst && !unlocked.isEmpty()) {
            intro.add("static_gap");
            intro.add(unlocked.get(unlocked.size() - 1));
            intro.add("static_gap");
        }
        s.ch7b.set(now, intro, loop);
    }

    // ------------------------------------------------------------------ evidence

    public static void logEvidence(MinecraftServer server, String evidenceId) {
        CaseData data = CaseData.get(server);
        if (!data.state.evidence.add(evidenceId)) return;
        if (evidenceId.equals("frame:console")) data.state.flags.add(Flags.FRAME_FOUND);
        if (evidenceId.equals("doc:transmitter_log")) data.state.flags.add(Flags.LOCKER_OPEN);
        sync(server);
        Net.toAll(server, new NoticePacket(NoticePacket.EVIDENCE, ContentLibrary.evidenceTitle(evidenceId)));
    }

    public static void compare(ServerPlayer player, String a, String b) {
        MinecraftServer server = player.server;
        CaseData data = CaseData.get(server);
        if (a.equals(b) || !data.state.evidence.contains(a) || !data.state.evidence.contains(b)) {
            Net.toPlayer(player, new NoticePacket(NoticePacket.NO_MATCH, "Select two different pieces of logged evidence."));
            return;
        }
        for (DeductionDef d : ContentLibrary.DEDUCTIONS.values()) {
            if (!d.matches(a, b)) continue;
            boolean ready = true;
            for (String r : d.requires) ready &= data.state.has(r);
            if (!ready) continue;
            if (data.state.deductions.contains(d.id)) {
                Net.toPlayer(player, new NoticePacket(NoticePacket.NO_MATCH, "Already in the case file: " + d.title));
                return;
            }
            data.state.deductions.add(d.id);
            Net.toAll(server, new NoticePacket(NoticePacket.DEDUCTION, d.title));
            setFlag(server, d.id);
            sync(server);
            return;
        }
        Net.toPlayer(player, new NoticePacket(NoticePacket.NO_MATCH, "These two agree with each other. Nothing here contradicts."));
    }

    // ------------------------------------------------------------------ items

    /** Gives an item described by a content token: doc:id, tape:blank, reel:memo. */
    public static void give(ServerPlayer player, String token, int count) {
        ItemStack stack = ItemStack.EMPTY;
        if (token.startsWith("doc:")) {
            stack = DocumentItem.create(token.substring(4));
            logEvidence(player.server, token);
        } else if (token.equals("tape:blank")) {
            stack = new ItemStack(ModItems.MAGNETIC_TAPE.get(), count);
        } else if (token.equals("reel:memo")) {
            stack = TapeItem.recorded("memo_penrose", "Unlabeled reel - \"A.P. - DO NOT ERASE\"");
            logEvidence(player.server, "tape:memo_penrose");
        }
        if (!stack.isEmpty()) {
            if (!player.getInventory().add(stack)) player.drop(stack, false);
            player.level().playSound(null, player.blockPosition(), ModSounds.PAPER.get(), SoundSource.PLAYERS, 0.6f, 1f);
        }
    }

    public static boolean playerHasDocument(Player player, String docId) {
        for (ItemStack s : player.getInventory().items) {
            if (s.is(ModItems.DOCUMENT.get()) && docId.equals(DocumentItem.docId(s))) return true;
        }
        for (ItemStack s : player.getInventory().offhand) {
            if (s.is(ModItems.DOCUMENT.get()) && docId.equals(DocumentItem.docId(s))) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ login

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.server;
        CaseData data = CaseData.get(server);
        if (!data.stationBuilt) {
            try {
                StationBuilder.buildNearSpawn(server.overworld());
            } catch (Exception e) {
                LastBroadcast.LOG.error("Failed to build the WKVR station", e);
            }
        }
        CompoundTag persisted = player.getPersistentData();
        if (!persisted.getBoolean(KIT_TAG) && data.stationBuilt) {
            persisted.putBoolean(KIT_TAG, true);
            BlockPos e = data.entrance;
            player.teleportTo(server.overworld(), e.getX() + 0.5, e.getY(), e.getZ() + 0.5, 180f, 0f);
            player.setRespawnPosition(server.overworld().dimension(), e, 180f, true, false);
            give(player, "doc:assignment", 1);
            if (!player.getInventory().add(new ItemStack(ModItems.CASE_JOURNAL.get()))) player.drop(new ItemStack(ModItems.CASE_JOURNAL.get()), false);
            if (!player.getInventory().add(new ItemStack(ModItems.FIELD_METER.get()))) player.drop(new ItemStack(ModItems.FIELD_METER.get()), false);
            give(player, "tape:blank", 4);
            player.sendSystemMessage(Component.literal("Halloway Falls, Ohio - Tuesday, October 23, 1962 - 11:40 PM").withStyle(s -> s.withColor(0xE8C77A).withItalic(true)));
            player.sendSystemMessage(Component.literal("Your assignment letter is in your coat pocket. Report to Master Control.").withStyle(s -> s.withColor(0xBFBFBF)));
        }
        data.publishStation();
        Net.toPlayer(player, new com.lastbroadcast.network.CaseSyncPacket(data.state));
    }

    // ------------------------------------------------------------------ ending

    public static void solveCase(ServerLevel level) {
        MinecraftServer server = level.getServer();
        CaseData data = CaseData.get(server);
        if (data.state.has(Flags.CASE_SOLVED)) return;
        long now = server.overworld().getGameTime();
        data.state.flags.add(Flags.CASE_SOLVED);
        data.state.ch7b.set(now, List.of("static_gap"), List.of("dead_carrier"));
        data.state.ch7.set(now, List.of("signoff"), Channels.CH7_LOOP);
        data.state.clockStallUntil = 0;
        data.endingStartedAt = now;
        data.endingScreenSent = false;
        ringingUntil.clear();
        for (TelevisionBlockEntity tv : TelevisionBlockEntity.serverSets()) {
            if (tv.getLevel() == level && data.inStation(tv.getBlockPos())) tv.tuneForEnding();
        }
        LastBroadcast.LOG.info("Case solved; ending sequence started");
        sync(server);
    }

    // ------------------------------------------------------------------ ticking

    private static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        long now = level.getGameTime();
        if (now % 10 != 0) return;
        CaseData data = CaseData.get(server);
        if (!data.stationBuilt) return;

        tickTelephones(level, now);

        if (data.state.has(Flags.CASE_SOLVED)) {
            Broadcast signoff = ContentLibrary.broadcast("signoff");
            long showAt = data.endingStartedAt + (signoff == null ? 600 : signoff.durationTicks()) + 40;
            if (!data.endingScreenSent && now >= showAt) {
                data.endingScreenSent = true;
                data.setDirty();
                Net.toAll(server, new com.lastbroadcast.network.OpenEndingPacket());
            }
            return;
        }
        if (!data.state.has(Flags.D2)) return;

        RandomSource rnd = level.random;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.level() != level || !data.nearStation(p.getX(), p.getY(), p.getZ(), 2)) continue;
            long due = nextLeak.computeIfAbsent(p.getUUID(), k -> now + 1200);
            if (now < due) continue;
            boolean late = data.state.has(Flags.D3);
            nextLeak.put(p.getUUID(), now + (late ? 1500 + rnd.nextInt(1500) : 2600 + rnd.nextInt(2400)));
            int kind;
            int prev = lastLeak.getOrDefault(p.getUUID(), -1);
            do {
                kind = rnd.nextInt(5);
            } while (kind == prev);
            lastLeak.put(p.getUUID(), kind);
            runLeak(level, data, p, kind, now);
        }
    }

    private static void runLeak(ServerLevel level, CaseData data, ServerPlayer player, int kind, long now) {
        switch (kind) {
            case 0 -> {
                TelevisionBlockEntity best = null;
                double bestD = 14 * 14;
                for (TelevisionBlockEntity tv : TelevisionBlockEntity.serverSets()) {
                    if (tv.getLevel() != level) continue;
                    double d = tv.getBlockPos().distToCenterSqr(player.position());
                    if (d < bestD) {
                        bestD = d;
                        best = tv;
                    }
                }
                if (best != null) best.surge(now + 70);
                else flickerLights(level, player);
            }
            case 1 -> flickerLights(level, player);
            case 2 -> {
                if (!ringPhone(level, data.phonePos, now)) flickerLights(level, player);
            }
            case 3 -> {
                data.state.clockStallUntil = now + 1400;
                sync(level.getServer());
            }
            default -> {
                Vec3 dir = Vec3.atCenterOf(data.consolePos).subtract(player.position());
                Vec3 at = dir.lengthSqr() < 1e-3 ? player.position() : player.position().add(dir.normalize().scale(7));
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                        net.minecraft.core.Holder.direct(ModSounds.ANOMALY_WHISPER.get()), SoundSource.AMBIENT,
                        at.x, at.y, at.z, 0.9f, 0.9f + level.random.nextFloat() * 0.2f, level.random.nextLong()));
            }
        }
    }

    public static void flickerLights(ServerLevel level, ServerPlayer player) {
        BlockPos c = player.blockPosition();
        RandomSource rnd = level.random;
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-10, -3, -10), c.offset(10, 6, 10))) {
            BlockState s = level.getBlockState(p);
            if ((s.getBlock() instanceof LampBlock || s.getBlock() instanceof FacingLampBlock)
                    && s.hasProperty(LampBlock.LIT) && s.getValue(LampBlock.LIT)) {
                level.setBlock(p, s.setValue(LampBlock.LIT, false), 3);
                // Lamps relight themselves through a scheduled block tick, which persists in the save.
                level.scheduleTick(p.immutable(), s.getBlock(), 6 + rnd.nextInt(18));
                if (++n > 40) break;
            }
        }
        if (n > 0) level.playSound(null, c, ModSounds.ELECTRIC_BUZZ.get(), SoundSource.BLOCKS, 0.7f, 0.8f + rnd.nextFloat() * 0.3f);
    }

    // ------------------------------------------------------------------ telephone

    public static boolean ringPhone(ServerLevel level, BlockPos pos, long now) {
        if (!level.isLoaded(pos)) return false;
        BlockState s = level.getBlockState(pos);
        if (!(s.getBlock() instanceof TelephoneBlock)) return false;
        level.setBlock(pos, s.setValue(TelephoneBlock.RINGING, true), 3);
        ringingUntil.put(pos.immutable(), now + 400);
        return true;
    }

    private static void tickTelephones(ServerLevel level, long now) {
        Iterator<Map.Entry<BlockPos, Long>> it = ringingUntil.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            BlockPos pos = e.getKey();
            if (!level.isLoaded(pos)) continue;
            BlockState s = level.getBlockState(pos);
            if (!(s.getBlock() instanceof TelephoneBlock) || !s.getValue(TelephoneBlock.RINGING)) {
                it.remove();
                continue;
            }
            if (now >= e.getValue()) {
                level.setBlock(pos, s.setValue(TelephoneBlock.RINGING, false), 3);
                it.remove();
            } else if (now % 60 == 0) {
                level.playSound(null, pos, ModSounds.PHONE_RING.get(), SoundSource.BLOCKS, 1.2f, 1f);
            }
        }
    }

    /** True if the server is currently driving this phone's ring. */
    public static boolean isRinging(BlockPos pos) {
        return ringingUntil.containsKey(pos);
    }

    public static void stopRinging(BlockPos pos) {
        ringingUntil.remove(pos);
    }

    public static String nextFragment() {
        String[] order = {"call_1", "call_2", "call_3"};
        String f = order[fragmentCursor % order.length];
        fragmentCursor++;
        return f;
    }
}
