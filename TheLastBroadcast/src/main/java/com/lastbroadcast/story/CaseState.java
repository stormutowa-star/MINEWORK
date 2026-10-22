package com.lastbroadcast.story;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.*;

/**
 * Shared investigation state. The server owns the authoritative copy inside {@link CaseData};
 * clients receive a copy through CaseSyncPacket ({@link #CLIENT}).
 */
public class CaseState {
    /** Client-side mirror. Never read on a dedicated server. */
    public static CaseState CLIENT = new CaseState();

    public final Set<String> flags = new LinkedHashSet<>();
    public final LinkedHashSet<String> evidence = new LinkedHashSet<>();
    public final LinkedHashSet<String> deductions = new LinkedHashSet<>();
    /** Channel 7 programme schedule: plays intro once from epoch, then loops loop. */
    public final ChannelPlan ch7 = new ChannelPlan();
    /** Phantom channel 7-B schedule. */
    public final ChannelPlan ch7b = new ChannelPlan();
    public long clockStallUntil = 0L;
    public long overdriveSurgeUntil = 0L;
    /** Station bounds (minX, minY, minZ, maxX, maxY, maxZ) and console position, for client ambience/HUD. */
    public int[] station = new int[6];
    public long consolePos = 0L;
    public boolean stationBuilt = false;

    /** Deep copy, so a packet never shares mutable state across threads (integrated server). */
    public CaseState copy() {
        CaseState c = new CaseState();
        c.flags.addAll(flags);
        c.evidence.addAll(evidence);
        c.deductions.addAll(deductions);
        c.ch7.set(ch7.epoch, ch7.intro, ch7.loop);
        c.ch7b.set(ch7b.epoch, ch7b.intro, ch7b.loop);
        c.clockStallUntil = clockStallUntil;
        c.overdriveSurgeUntil = overdriveSurgeUntil;
        c.station = station.clone();
        c.consolePos = consolePos;
        c.stationBuilt = stationBuilt;
        return c;
    }

    public boolean has(String flag) {
        return flags.contains(flag);
    }

    public boolean inStation(double x, double y, double z, double margin) {
        return stationBuilt && x >= station[0] - margin && y >= station[1] - margin && z >= station[2] - margin
                && x <= station[3] + margin && y <= station[4] + margin && z <= station[5] + margin;
    }

    public static class ChannelPlan {
        public long epoch = 0L;
        public final List<String> intro = new ArrayList<>();
        public final List<String> loop = new ArrayList<>();

        void write(FriendlyByteBuf buf) {
            buf.writeLong(epoch);
            writeList(buf, intro);
            writeList(buf, loop);
        }

        void read(FriendlyByteBuf buf) {
            epoch = buf.readLong();
            intro.clear();
            intro.addAll(readList(buf));
            loop.clear();
            loop.addAll(readList(buf));
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putLong("epoch", epoch);
            t.put("intro", toTag(intro));
            t.put("loop", toTag(loop));
            return t;
        }

        void load(CompoundTag t) {
            epoch = t.getLong("epoch");
            intro.clear();
            intro.addAll(fromTag(t.getList("intro", Tag.TAG_STRING)));
            loop.clear();
            loop.addAll(fromTag(t.getList("loop", Tag.TAG_STRING)));
        }

        public void set(long epoch, List<String> intro, List<String> loop) {
            this.epoch = epoch;
            this.intro.clear();
            this.intro.addAll(intro);
            this.loop.clear();
            this.loop.addAll(loop);
        }
    }

    public void write(FriendlyByteBuf buf) {
        writeList(buf, flags);
        writeList(buf, evidence);
        writeList(buf, deductions);
        ch7.write(buf);
        ch7b.write(buf);
        buf.writeLong(clockStallUntil);
        buf.writeLong(overdriveSurgeUntil);
        buf.writeBoolean(stationBuilt);
        for (int i = 0; i < 6; i++) buf.writeInt(station[i]);
        buf.writeLong(consolePos);
    }

    public static CaseState read(FriendlyByteBuf buf) {
        CaseState s = new CaseState();
        s.flags.addAll(readList(buf));
        s.evidence.addAll(readList(buf));
        s.deductions.addAll(readList(buf));
        s.ch7.read(buf);
        s.ch7b.read(buf);
        s.clockStallUntil = buf.readLong();
        s.overdriveSurgeUntil = buf.readLong();
        s.stationBuilt = buf.readBoolean();
        for (int i = 0; i < 6; i++) s.station[i] = buf.readInt();
        s.consolePos = buf.readLong();
        return s;
    }

    public CompoundTag save(CompoundTag tag) {
        tag.put("flags", toTag(flags));
        tag.put("evidence", toTag(evidence));
        tag.put("deductions", toTag(deductions));
        tag.put("ch7", ch7.save());
        tag.put("ch7b", ch7b.save());
        return tag;
    }

    public void load(CompoundTag tag) {
        flags.clear();
        flags.addAll(fromTag(tag.getList("flags", Tag.TAG_STRING)));
        evidence.clear();
        evidence.addAll(fromTag(tag.getList("evidence", Tag.TAG_STRING)));
        deductions.clear();
        deductions.addAll(fromTag(tag.getList("deductions", Tag.TAG_STRING)));
        ch7.load(tag.getCompound("ch7"));
        ch7b.load(tag.getCompound("ch7b"));
    }

    private static void writeList(FriendlyByteBuf buf, Collection<String> list) {
        buf.writeVarInt(list.size());
        for (String s : list) buf.writeUtf(s);
    }

    private static List<String> readList(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<String> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(buf.readUtf());
        return out;
    }

    private static ListTag toTag(Collection<String> list) {
        ListTag t = new ListTag();
        for (String s : list) t.add(StringTag.valueOf(s));
        return t;
    }

    private static List<String> fromTag(ListTag t) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < t.size(); i++) out.add(t.getString(i));
        return out;
    }
}
