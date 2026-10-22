package com.lastbroadcast.client.hud;

import com.lastbroadcast.blockentity.TelevisionBlockEntity;
import com.lastbroadcast.broadcast.Channels;
import com.lastbroadcast.client.render.BroadcastRenderer;
import com.lastbroadcast.client.render.Canvas;
import com.lastbroadcast.client.sound.BroadcastAudio;
import com.lastbroadcast.content.Broadcast;
import com.lastbroadcast.content.ContentLibrary;
import com.lastbroadcast.item.FieldMeterItem;
import com.lastbroadcast.network.NoticePacket;
import com.lastbroadcast.registry.ModItems;
import com.lastbroadcast.story.CaseState;
import com.lastbroadcast.story.Flags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** In-game overlays: case notices, broadcast subtitles, telephone fragments, field meter. */
public final class Hud {
    private record Notice(int kind, String text) {}

    private static final Deque<Notice> notices = new ArrayDeque<>();
    private static Notice current;
    private static long currentSince;

    private static List<Broadcast.Line> fragment;
    private static long fragmentStart;
    private static double fragmentNextBlip;
    private static float meterNeedle = 0f;

    private Hud() {}

    public static void notice(int kind, String text) {
        if (notices.size() < 8) notices.add(new Notice(kind, text));
    }

    public static void playFragment(String id, List<Broadcast.Line> lines) {
        if ("dial_tone".equals(id) || lines == null) {
            BroadcastAudio.playUi("dial_tone", 0.6f, 1f);
            fragment = List.of(line(0, 2.5, null, net.minecraft.client.resources.language.I18n.get("lastbroadcast.hud.dial_tone")));
        } else {
            BroadcastAudio.playUi("static_burst", 0.7f, 0.8f);
            fragment = lines;
        }
        fragmentStart = System.currentTimeMillis();
    }

    private static Broadcast.Line line(double t, double d, String who, String text) {
        Broadcast.Line l = new Broadcast.Line();
        l.t = t;
        l.d = d;
        l.who = who;
        l.text = text;
        return l;
    }

    public static void reset() {
        notices.clear();
        current = null;
        fragment = null;
    }

    public static void render(GuiGraphics g, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        int w = g.guiWidth(), h = g.guiHeight();
        Font font = mc.font;
        renderMeter(g, mc, w, h, partial);
        if (mc.screen == null) renderSubtitles(g, mc, font, w, h);
        renderNotice(g, font, w);
    }

    // ------------------------------------------------------------------ notices

    private static void renderNotice(GuiGraphics g, Font font, int w) {
        long now = System.currentTimeMillis();
        if (current == null || now - currentSince > 4800) {
            current = notices.poll();
            currentSince = now;
            if (current == null) return;
        }
        float age = (now - currentSince) / 1000f;
        float alpha = Math.min(1f, Math.min(age * 4f, (4.8f - age) * 2f));
        if (alpha <= 0.02f) return;
        String head = switch (current.kind) {
            case NoticePacket.EVIDENCE -> net.minecraft.client.resources.language.I18n.get("lastbroadcast.hud.notice.evidence");
            case NoticePacket.DEDUCTION -> net.minecraft.client.resources.language.I18n.get("lastbroadcast.hud.notice.finding");
            case NoticePacket.NO_MATCH -> net.minecraft.client.resources.language.I18n.get("lastbroadcast.hud.notice.case");
            case NoticePacket.WARNING -> net.minecraft.client.resources.language.I18n.get("lastbroadcast.hud.notice.warning");
            default -> net.minecraft.client.resources.language.I18n.get("lastbroadcast.hud.notice.note");
        };
        int headColor = switch (current.kind) {
            case NoticePacket.DEDUCTION -> 0xD6452F;
            case NoticePacket.WARNING -> 0xFFB347;
            default -> 0x6A5A3A;
        };
        List<FormattedCharSequence> lines = font.split(Component.literal(com.lastbroadcast.client.ClientText.resolve(current.text)), 220);
        int bw = 236, bh = 18 + Math.min(4, lines.size()) * 10;
        int x = w / 2 - bw / 2, y = 6;
        int a = (int) (alpha * 230) << 24;
        g.fill(x - 1, y - 1, x + bw + 1, y + bh + 1, a | 0x2A1E14);
        g.fill(x, y, x + bw, y + bh, a | 0xEFE6CF);
        int ta = Math.max(4, (int) (alpha * 255)) << 24;
        g.drawString(font, head, x + 8, y + 4, ta | headColor, false);
        int yy = y + 15;
        for (int i = 0; i < lines.size() && i < 4; i++) {
            g.drawString(font, lines.get(i), x + 8, yy, ta | 0x2A2420, false);
            yy += 10;
        }
    }

    // ------------------------------------------------------------------ subtitles

    private static void renderSubtitles(GuiGraphics g, Minecraft mc, Font font, int w, int h) {
        String text = null;
        String tag = null;
        if (fragment != null) {
            double t = (System.currentTimeMillis() - fragmentStart) / 1000.0;
            double end = 0;
            for (Broadcast.Line l : fragment) end = Math.max(end, l.t + l.d);
            if (t > end + 0.5) {
                fragment = null;
            } else {
                for (Broadcast.Line l : fragment) {
                    if (t >= l.t && t < l.t + l.d) {
                        text = (l.who == null ? "" : ContentLibrary.speaker(l.who).name + ": ") + l.text;
                        if (l.who != null && t * 1000 >= fragmentNextBlip && t < l.t + l.d - 0.3) {
                            ContentLibrary.Speaker sp = ContentLibrary.speaker(l.who);
                            BroadcastAudio.playUi(sp.voice, 0.45f, sp.pitch * (0.85f + (float) Math.random() * 0.2f));
                            fragmentNextBlip = t * 1000 + 110 + Math.random() * 80;
                        }
                    }
                }
                tag = net.minecraft.client.resources.language.I18n.get("lastbroadcast.hud.telephone");
            }
        }
        if (text == null) {
            TelevisionBlockEntity tv = watchedSet(mc);
            if (tv != null) {
                long gt = mc.level.getGameTime();
                if (!tv.isSurging(gt)) {
                    Channels.OnAir onAir = Channels.onAir(CaseState.CLIENT, tv.getChannel(), gt);
                    text = BroadcastRenderer.subtitle(onAir.programme(), onAir.time(), false);
                    tag = net.minecraft.client.resources.language.I18n.get("lastbroadcast.hud.channel", Channels.label(tv.getChannel()));
                }
            }
        }
        if (text == null) return;
        List<FormattedCharSequence> lines = font.split(Component.literal(text), Math.min(320, w - 40));
        int lh = 10, bh = lines.size() * lh + 6;
        int y = h - 72 - bh;
        int maxW = 0;
        for (FormattedCharSequence l : lines) maxW = Math.max(maxW, font.width(l));
        g.fill(w / 2 - maxW / 2 - 6, y - 3, w / 2 + maxW / 2 + 6, y + bh - 3, 0xA0000000);
        if (tag != null) g.drawString(font, tag, w / 2 - maxW / 2 - 4, y - 12, 0xFFB0A070, true);
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(font, lines.get(i), w / 2 - font.width(lines.get(i)) / 2, y + i * lh, 0xFFF2E6C0, false);
        }
    }

    private static TelevisionBlockEntity watchedSet(Minecraft mc) {
        Player p = mc.player;
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getViewVector(1f);
        TelevisionBlockEntity best = null;
        double bestScore = 0;
        for (TelevisionBlockEntity tv : TelevisionBlockEntity.clientSets()) {
            if (tv.isRemoved() || tv.getLevel() != mc.level || !tv.isPowered()) continue;
            Vec3 c = tv.getBlockPos().getCenter();
            Vec3 d = c.subtract(eye);
            double dist = d.length();
            if (dist > 7) continue;
            double facing = d.normalize().dot(look);
            if (dist > 2.5 && facing < 0.75) continue;
            double score = facing + (7 - dist) * 0.2;
            if (score > bestScore) {
                bestScore = score;
                best = tv;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ field meter

    private static void renderMeter(GuiGraphics g, Minecraft mc, int w, int h, float partial) {
        Player p = mc.player;
        if (!p.getMainHandItem().is(ModItems.FIELD_METER.get()) && !p.getOffhandItem().is(ModItems.FIELD_METER.get())) return;
        CaseState cs = CaseState.CLIENT;
        boolean overdrive = cs.stationBuilt && !cs.has(Flags.CASE_SOLVED);
        float target = FieldMeterItem.strength(p.getPosition(partial), BlockPos.of(cs.consolePos), overdrive);
        target += (float) (Math.sin(mc.level.getGameTime() * 0.7) * 0.015 + (Math.random() - 0.5) * 0.02 * target);
        meterNeedle += (target - meterNeedle) * 0.15f;
        int mw = 84, mh = 50;
        int x = w - mw - 8, y = h - mh - 40;
        g.fill(x - 3, y - 3, x + mw + 3, y + mh + 13, 0xE02A1E14);
        Canvas c = new Canvas(g.pose(), g.bufferSource(), 0xF000F0, 0.5f, 100000, 100000);
        c.image("meter_face", x, y, x + mw, y + mh, 0xFFFFFFFF);
        c.layer();
        float a = (float) Math.toRadians(-50 + 100 * Math.max(0, Math.min(1, meterNeedle)));
        float px = x + mw / 2f, py = y + mh * 0.92f, len = mh * 0.78f;
        c.line(px, py, px + (float) Math.sin(a) * len, py - (float) Math.cos(a) * len, 1f, 0xFF201010);
        g.flush();
        String s = String.format("%d dBuV  175.25 Mc", Math.round(40 + meterNeedle * 80));
        g.drawString(mc.font, s, x + mw / 2 - mc.font.width(s) / 2, y + mh + 2, 0xFF7DFF9A, false);
    }
}
