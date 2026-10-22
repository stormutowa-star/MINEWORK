package com.lastbroadcast.client.render;

import com.lastbroadcast.content.Broadcast;
import com.lastbroadcast.content.ContentLibrary;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * Draws one frame of a broadcast onto a {@link Canvas} of 160x120 units: camera framing and
 * moves, presenters with lip-flap and blinks, captions, transitions, and analogue faults
 * (snow, vertical roll, line tearing, ghosting, hum bars, flicker), finished with scanlines and
 * a CRT vignette. Pure function of (programme, time) so every screen showing the same channel
 * draws the same picture.
 */
public final class BroadcastRenderer {
    public static final float W = 160f, H = 120f;

    private BroadcastRenderer() {}

    /** Extra degradation applied by the receiver (bad tuning, surges). */
    public static final class Signal {
        public float snow = 0f;
        public float roll = 0f;
        public float ghost = 0f;
        public float tear = 0f;
        public float brightness = 1f;
        public boolean scanlines = true;

        public static Signal clean() {
            return new Signal();
        }
    }

    /** Snow-only frame for an empty channel. */
    public static void drawStatic(Canvas c, double t, float brightness) {
        c.fill(0, 0, W, H, 0xFF101010);
        c.layer();
        noise(c, t, 1f, brightness);
        c.layer();
        // faint hum bar drifting through the snow
        float barY = (float) ((t * 22) % (H + 30)) - 30;
        c.fill(0, barY, W, barY + 18, 0x30000000);
        c.layer();
        finish(c, true);
    }

    public static void draw(Canvas c, Broadcast b, double t, Signal sig) {
        if (b == null) {
            drawStatic(c, t, 1f);
            return;
        }
        if (b.audioOnly) {
            c.fill(0, 0, W, H, 0xFF000000);
            c.layer();
            c.text(net.minecraft.client.resources.language.I18n.get("lastbroadcast.ui.audio_reel"), W / 2, H / 2 - 4, 0xFF60FF80, 1f, true, false);
            finish(c, sig.scanlines);
            return;
        }
        t = Mth.clamp(t, 0, b.duration);
        Broadcast.Shot shot = b.shotAt(t);
        if (shot == null) {
            drawStatic(c, t, 1f);
            return;
        }
        Broadcast.Fx fx = shot.fx;
        Broadcast.Fx base = b.fx;
        float snow = Broadcast.Fx.v(fx == null ? null : fx.stat, base.stat, 0f) + sig.snow;
        float roll = Broadcast.Fx.v(fx == null ? null : fx.roll, base.roll, 0f) + sig.roll;
        float tear = Broadcast.Fx.v(fx == null ? null : fx.tear, base.tear, 0f) + sig.tear;
        float ghost = Broadcast.Fx.v(fx == null ? null : fx.ghost, base.ghost, 0f) + sig.ghost;
        float flicker = Broadcast.Fx.v(fx == null ? null : fx.flicker, base.flicker, 0f);
        float bars = Broadcast.Fx.v(fx == null ? null : fx.bars, base.bars, 0f);
        float vignette = Broadcast.Fx.v(fx == null ? null : fx.vignette, base.vignette, 0.6f);
        float shake = Broadcast.Fx.v(fx == null ? null : fx.shake, base.shake, 0f);

        double local = t - shot.t;
        int idx = b.shots.indexOf(shot);
        double shotEnd = idx + 1 < b.shots.size() ? b.shots.get(idx + 1).t : b.duration;
        double shotLen = Math.max(0.01, shotEnd - shot.t);

        // transition into this shot
        float transK = shot.transDur > 0 && local < shot.transDur ? (float) (local / shot.transDur) : 1f;
        if (transK < 1f && "static".equals(shot.trans)) snow = Math.max(snow, 1f - transK);
        if (transK < 1f && "roll".equals(shot.trans)) roll = Math.max(roll, (1f - transK) * 3f);

        // brightness / flicker
        float bright = sig.brightness;
        if (flicker > 0) bright *= 1f - flicker * (float) (0.5 + 0.5 * Math.sin(t * 47.0) * Math.sin(t * 13.0 + 1.3));
        float[] tint = parseTint(shot.tint);

        // vertical roll: draw the frame twice, offset, with a black blanking bar between
        float rollOff = roll > 0 ? (float) ((t * roll * 40.0) % H) : 0f;
        c.fill(0, 0, W, H, 0xFF000000);
        c.layer();
        for (int pass = 0; pass < (rollOff > 0 ? 2 : 1); pass++) {
            float dy = pass == 0 ? rollOff : rollOff - H - 6;
            c.clip(0, Math.max(0, dy), W, Math.min(H, dy + H));
            c.tr = bright * tint[0];
            c.tg = bright * tint[1];
            c.tb = bright * tint[2];
            if (transK < 1f && "dissolve".equals(shot.trans)) {
                Broadcast.Shot prev = b.previousShot(shot);
                if (prev != null) {
                    drawShot(c, b, prev, shot.t - prev.t, shot.t - prev.t + 0.0001, t, dy, tear, ghost, shake, 1f);
                }
                drawShot(c, b, shot, local, shotLen, t, dy, tear, ghost, shake, transK);
            } else {
                drawShot(c, b, shot, local, shotLen, t, dy, tear, ghost, shake, 1f);
            }
            c.resetTint();
            if (rollOff > 0) c.fill(0, dy + H, W, dy + H + 6, 0xFF000000);
        }
        c.clip(0, 0, W, H);

        // single-frame insert (found by stepping frame by frame on tape)
        if (b.flash != null && t >= b.flash.t && t < b.flash.t + b.flash.d) {
            c.layer();
            c.image(bgName(b, b.flash.bg), 0, 0, W, H, 0xFFFFFFFF);
        }

        c.layer();
        if (bars > 0) {
            float barY = (float) ((t * 18) % (H + 40)) - 40;
            c.fill(0, barY, W, barY + 26, ((int) (bars * 120) << 24));
            c.layer();
        }
        if (snow > 0.01f) {
            noise(c, t, Math.min(1f, snow), 1f);
            c.layer();
        }
        if (vignette > 0) {
            c.image("vignette", 0, 0, W, H, ((int) (Mth.clamp(vignette, 0, 1) * 255) << 24) | 0xFFFFFF);
            c.layer();
        }
        finish(c, sig.scanlines);
    }

    private static void drawShot(Canvas c, Broadcast b, Broadcast.Shot shot, double local, double shotLen, double t,
                                 float dy, float tear, float ghost, float shake, float alpha) {
        float k = (float) Mth.clamp(local / shotLen, 0, 1);
        float k2 = k * k * (3 - 2 * k); // ease in/out camera moves
        float[] crop = shot.crop != null ? shot.crop : new float[]{0, 0, W, H};
        float[] end = shot.cropEnd != null ? shot.cropEnd : crop;
        float cx = Mth.lerp(k2, crop[0], end[0]);
        float cy = Mth.lerp(k2, crop[1], end[1]);
        float cw = Mth.lerp(k2, crop[2], end[2]);
        float ch = Mth.lerp(k2, crop[3], end[3]);
        if (shake > 0) {
            cx += (float) (Math.sin(t * 31) * shake * 2 + Math.sin(t * 7.3) * shake * 3);
            cy += (float) (Math.cos(t * 27) * shake * 2);
        }
        float sx = W / cw, sy = H / ch;
        int a = (int) (alpha * 255) << 24;

        // background
        String bg = bgName(b, shot.bg);
        Atlas.Region r = Atlas.get(bg);
        float rx = r.w() / W, ry = r.h() / H;
        drawImageTorn(c, bg, cx * rx, cy * ry, cw * rx, ch * ry, 0, dy, W, dy + H, a | 0xFFFFFF, tear, t);
        if (ghost > 0) {
            c.layer();
            drawImageTorn(c, bg, cx * rx, cy * ry, cw * rx, ch * ry, 5, dy, W + 5, dy + H, ((int) (ghost * alpha * 140) << 24) | 0xFFFFFF, tear, t);
        }
        c.layer();

        // presenters
        for (Broadcast.Actor act : shot.actors) {
            String sprite = spriteName(b, act.sprite);
            if (!Atlas.has(sprite + "_0") && !Atlas.has(sprite)) continue;
            String frame = actorFrame(b, act, t, sprite);
            Atlas.Region ar = Atlas.get(frame);
            float h = act.h;
            float w = h * ar.w() / ar.h();
            float ax = act.x, ay = act.y;
            if (act.moveTo != null) {
                ax = Mth.lerp(k2, act.x, act.moveTo[0]);
                ay = Mth.lerp(k2, act.y, act.moveTo[1]);
            }
            // actor anchor is bottom centre, in scene units
            float x0 = (ax - w / 2 - cx) * sx, x1 = (ax + w / 2 - cx) * sx;
            float y0 = (ay - h - cy) * sy + dy, y1 = (ay - cy) * sy + dy;
            if (act.flip) {
                float tmp = x0;
                x0 = x1;
                x1 = tmp;
            }
            drawActor(c, frame, x0, y0, x1, y1, a | 0xFFFFFF, tear, t);
            if (ghost > 0) drawActor(c, frame, x0 + 5, y0, x1 + 5, y1, ((int) (ghost * alpha * 120) << 24) | 0xFFFFFF, 0, t);
            c.layer();
        }

        // titles and captions
        for (Broadcast.TextEl te : shot.texts) {
            if (local < te.t0 || local > te.t1) continue;
            if (te.blink && ((int) (t * 2.5)) % 2 == 1) continue;
            float tx = te.x, ty = te.y, ts = te.scale;
            if (te.scene) {
                tx = (te.x - cx) * sx;
                ty = (te.y - cy) * sy;
                ts = te.scale * sx;
            }
            ty += dy;
            int col = parseColor(te.color, 0xFFFFFFFF);
            col = (((int) (((col >>> 24) & 255) * alpha)) << 24) | (col & 0xFFFFFF);
            if (te.box != null) {
                float tw = net.minecraft.client.Minecraft.getInstance().font.width(te.s) * ts;
                float left = te.center ? tx - tw / 2 : tx;
                int box = parseColor(te.box, 0x80000000);
                c.fill(left - 3, ty - 2, left + tw + 3, ty + 8 * ts + 2, box);
            }
            c.text(te.s, tx, ty, col, ts, te.center, te.shadow);
        }
    }

    private static void drawActor(Canvas c, String frame, float x0, float y0, float x1, float y1, int argb, float tear, double t) {
        if (tear <= 0.01f) {
            boolean flipped = x1 < x0;
            Atlas.Region r = Atlas.get(frame);
            if (flipped) c.quad(x1, y0, x0, y1, r.u1(), r.v0(), r.u0(), r.v1(), argb);
            else c.quad(x0, y0, x1, y1, r.u0(), r.v0(), r.u1(), r.v1(), argb);
            return;
        }
        Atlas.Region r = Atlas.get(frame);
        float left = Math.min(x0, x1), right = Math.max(x0, x1);
        boolean flipped = x1 < x0;
        int strips = 10;
        for (int i = 0; i < strips; i++) {
            float fy0 = y0 + (y1 - y0) * i / strips, fy1 = y0 + (y1 - y0) * (i + 1) / strips;
            float off = tearOffset(tear, t, (fy0 + fy1) / 2);
            float v0 = r.v0() + (r.v1() - r.v0()) * i / strips, v1 = r.v0() + (r.v1() - r.v0()) * (i + 1) / strips;
            if (flipped) c.quad(left + off, fy0, right + off, fy1, r.u1(), v0, r.u0(), v1, argb);
            else c.quad(left + off, fy0, right + off, fy1, r.u0(), v0, r.u1(), v1, argb);
        }
    }

    /** Draws a source rect (in region pixels) of an atlas image into a destination rect, optionally torn into strips. */
    private static void drawImageTorn(Canvas c, String region, float sx, float sy, float sw, float sh,
                                      float x0, float y0, float x1, float y1, int argb, float tear, double t) {
        if (tear <= 0.01f) {
            c.imagePart(region, sx, sy, sw, sh, x0, y0, x1, y1, argb);
            return;
        }
        int strips = 24;
        for (int i = 0; i < strips; i++) {
            float fy0 = y0 + (y1 - y0) * i / strips, fy1 = y0 + (y1 - y0) * (i + 1) / strips;
            float off = tearOffset(tear, t, (fy0 + fy1) / 2);
            c.imagePart(region, sx, sy + sh * i / strips, sw, sh / strips, x0 + off, fy0, x1 + off, fy1, argb);
        }
    }

    private static float tearOffset(float tear, double t, float y) {
        double band = Math.sin(y * 0.21 + t * 9.0) * Math.sin(y * 0.05 - t * 3.1);
        double burst = Math.max(0, Math.sin(t * 2.3 + y * 0.01)) * 0.7 + 0.3;
        return (float) (tear * 14 * band * burst);
    }

    private static String actorFrame(Broadcast b, Broadcast.Actor act, double t, String sprite) {
        if (!Atlas.has(sprite + "_0")) return sprite;
        boolean talking = false;
        if (act.talk != null) {
            Broadcast.Line l = b.lineAt(t);
            talking = l != null && act.talk.equals(l.who) && t < l.t + l.d - 0.15;
        }
        int seed = act.sprite.hashCode();
        if (talking) {
            double s = Math.sin(t * 23.0 + seed) + Math.sin(t * 11.0 + seed * 0.3) * 0.6;
            if (s > 0.15) return sprite + "_1";
        }
        double blinkPhase = (t + (seed & 7) * 0.37) % 3.7;
        if (blinkPhase < 0.13 && Atlas.has(sprite + "_2")) return sprite + "_2";
        return sprite + "_0";
    }

    private static String bgName(Broadcast b, String bg) {
        if (bg == null) return "black";
        if (b.mono && Atlas.has(bg + "_mono")) return bg + "_mono";
        return bg;
    }

    private static String spriteName(Broadcast b, String sprite) {
        if (b.mono && Atlas.has(sprite + "_mono_0")) return sprite + "_mono";
        return sprite;
    }

    /** Animated snow from four pre-baked noise tiles with random offsets. */
    public static void noise(Canvas c, double t, float amount, float brightness) {
        int frame = (int) (t * 24);
        int tile = Math.floorMod(frame, 4);
        Atlas.Region r = Atlas.get("noise_" + tile);
        float maxU = Math.max(0, r.w() - W), maxV = Math.max(0, r.h() - H);
        float ou = (float) Math.floorMod(frame * 37, (int) Math.max(1, maxU));
        float ov = (float) Math.floorMod(frame * 53, (int) Math.max(1, maxV));
        int a = (int) (Mth.clamp(amount, 0, 1) * 255);
        int g = (int) (Mth.clamp(brightness, 0, 1) * 255);
        c.imagePart("noise_" + tile, ou, ov, W, H, 0, 0, W, H, (a << 24) | (g << 16) | (g << 8) | g);
    }

    private static void finish(Canvas c, boolean scanlines) {
        if (scanlines) {
            c.image("scanlines", 0, 0, W, H, 0x60FFFFFF);
            c.layer();
        }
        c.image("screen_mask", 0, 0, W, H, 0xFFFFFFFF);
        c.layer();
    }

    private static float[] parseTint(String hex) {
        if (hex == null) return new float[]{1, 1, 1};
        int c = parseColor(hex, 0xFFFFFFFF);
        return new float[]{((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f};
    }

    public static int parseColor(String hex, int def) {
        if (hex == null) return def;
        try {
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            long v = Long.parseLong(h, 16);
            if (h.length() <= 6) v |= 0xFF000000L;
            return (int) v;
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** Line spoken at time t (or null), with reverse-playback text substitution. */
    public static String subtitle(Broadcast b, double t, boolean reverse) {
        if (b == null) return null;
        Broadcast.Line l = b.lineAt(t);
        if (l == null) return null;
        String who = l.who == null ? "" : ContentLibrary.speaker(l.who).name;
        String text;
        if (l.reversedSpeech) {
            // recorded backwards: garbled forwards, intelligible in reverse
            text = reverse ? l.text : new StringBuilder(l.text).reverse().toString().toLowerCase();
        } else if (reverse) {
            text = l.rev != null ? l.rev : new StringBuilder(l.text).reverse().toString().toLowerCase();
        } else {
            text = l.text;
        }
        return who.isEmpty() ? text : who + ": " + text;
    }

    public static List<Broadcast.Line> lines(Broadcast b) {
        return b == null ? List.of() : b.lines;
    }
}
