package com.lastbroadcast.broadcast;

import com.lastbroadcast.content.Broadcast;
import com.lastbroadcast.content.ContentLibrary;
import com.lastbroadcast.story.CaseState;
import com.lastbroadcast.story.Flags;

import java.util.List;

/**
 * Channel line-up and programme schedule. Every set tuned to the same channel shows the same
 * programme at the same moment, derived from the world's game time — so playback stays in sync
 * between players and survives save/reload with no extra state.
 */
public final class Channels {
    public static final String[] DIAL = {"2", "4", "7", "7-B", "9", "11", "13"};
    public static final int CH_7 = 2;
    public static final int CH_7B = 3;
    public static final int CH_4 = 1;

    /** Default Channel 7 rotation (Tuesday 23 October 1962). */
    public static final List<String> CH7_LOOP = List.of("ident_wkvr", "today_tuesday", "futura_ad_62", "report_tuesday", "testcard_wkvr");
    public static final List<String> CH4_LOOP = List.of("testcard_ktrx");

    private Channels() {}

    public static String label(int channel) {
        return channel >= 0 && channel < DIAL.length ? DIAL[channel] : "?";
    }

    public static boolean selectable(CaseState state, int channel) {
        if (channel < 0 || channel >= DIAL.length) return false;
        return channel != CH_7B || state.has(Flags.SIGNAL_LOCKED);
    }

    public static int next(CaseState state, int channel, int dir) {
        int c = channel;
        for (int i = 0; i < DIAL.length; i++) {
            c = Math.floorMod(c + dir, DIAL.length);
            if (selectable(state, c)) return c;
        }
        return channel;
    }

    /** What is on the air: programme and seconds into it. Null programme = no carrier (static). */
    public record OnAir(Broadcast programme, double time, long startTick) {
        public static final OnAir STATIC = new OnAir(null, 0, 0);
    }

    public static OnAir onAir(CaseState state, int channel, double gameTime) {
        return switch (channel) {
            case CH_7 -> plan(state.ch7, CH7_LOOP, gameTime);
            case CH_7B -> state.has(Flags.SIGNAL_LOCKED) ? plan(state.ch7b, List.of(), gameTime) : OnAir.STATIC;
            case CH_4 -> plan(null, CH4_LOOP, gameTime);
            default -> OnAir.STATIC;
        };
    }

    private static OnAir plan(CaseState.ChannelPlan plan, List<String> defaultLoop, double gameTime) {
        List<String> intro = plan == null ? List.of() : plan.intro;
        List<String> loop = plan == null || plan.loop.isEmpty() ? defaultLoop : plan.loop;
        long epoch = plan == null ? 0 : plan.epoch;
        double t = gameTime - epoch;
        if (t < 0) t = 0;
        long cursor = epoch;
        for (String id : intro) {
            Broadcast b = ContentLibrary.broadcast(id);
            if (b == null) continue;
            int d = b.durationTicks();
            if (t < d) return new OnAir(b, t / 20.0, cursor);
            t -= d;
            cursor += d;
        }
        long total = 0;
        for (String id : loop) {
            Broadcast b = ContentLibrary.broadcast(id);
            if (b != null) total += b.durationTicks();
        }
        if (total <= 0) return OnAir.STATIC;
        long cycles = (long) Math.floor(t / total);
        t -= cycles * (double) total;
        cursor += cycles * total;
        for (String id : loop) {
            Broadcast b = ContentLibrary.broadcast(id);
            if (b == null) continue;
            int d = b.durationTicks();
            if (t < d) return new OnAir(b, t / 20.0, cursor);
            t -= d;
            cursor += d;
        }
        return OnAir.STATIC;
    }
}
