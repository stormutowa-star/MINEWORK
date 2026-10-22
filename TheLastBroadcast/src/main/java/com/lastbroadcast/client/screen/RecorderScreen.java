package com.lastbroadcast.client.screen;

import com.lastbroadcast.blockentity.RecorderBlockEntity;
import com.lastbroadcast.client.render.BroadcastRenderer;
import com.lastbroadcast.client.render.Canvas;
import com.lastbroadcast.client.sound.BroadcastAudio;
import com.lastbroadcast.client.sound.LoopSound;
import com.lastbroadcast.content.Broadcast;
import com.lastbroadcast.content.ContentLibrary;
import com.lastbroadcast.item.TapeItem;
import com.lastbroadcast.network.FrameExaminedPacket;
import com.lastbroadcast.network.Net;
import com.lastbroadcast.network.RecorderActionPacket;
import com.lastbroadcast.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

import java.util.HashSet;
import java.util.Set;

/**
 * Tape deck transport. Plays a recorded programme on the deck's monitor at normal, half or
 * fast speed, forwards or backwards, with single-frame stepping while paused. Reverse playback
 * makes backwards-recorded speech intelligible; frame stepping reveals single-frame inserts.
 */
public class RecorderScreen extends RetroScreen {
    private static BlockPos activePos;
    private static float activeRate;
    private static final Set<String> examined = new HashSet<>();

    private final BlockPos pos;
    private double t = 0;
    private float rate = 0;
    private boolean half = false;
    private long lastNanos = System.nanoTime();
    private double lastAudioT = 0;
    private double nextBlip = 0;
    private LoopSound hiss;

    public RecorderScreen(BlockPos pos) {
        super(Component.literal("Tape Deck"), 336, 224);
        this.pos = pos;
        activePos = pos;
    }

    public static float reelSpeedFor(BlockPos pos) {
        return pos.equals(activePos) ? activeRate : 0f;
    }

    private RecorderBlockEntity deck() {
        var level = Minecraft.getInstance().level;
        return level != null && level.getBlockEntity(pos) instanceof RecorderBlockEntity d ? d : null;
    }

    private Broadcast tape() {
        RecorderBlockEntity d = deck();
        if (d == null || d.getTape().isEmpty()) return null;
        return ContentLibrary.broadcast(TapeItem.broadcastId(d.getTape()));
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        RecorderBlockEntity d = deck();
        if (d == null || d.getTape().isEmpty() || mc.player == null || mc.player.distanceToSqr(pos.getCenter()) > 64) onClose();
    }

    @Override
    public void removed() {
        super.removed();
        activePos = null;
        activeRate = 0;
        if (hiss != null) hiss.stopNow();
    }

    @Override
    protected void layout() {
        int y = 192, x = 12;
        transport(x, y, "|<", "Back one frame", () -> step(-1));
        transport(x + 26, y, "<<", "Rewind", () -> setRate(-4));
        transport(x + 52, y, "<", "Play in reverse", () -> setRate(-1));
        transport(x + 78, y, "||", "Pause", () -> setRate(0));
        transport(x + 104, y, ">", "Play", () -> setRate(1));
        transport(x + 130, y, ">>", "Fast forward", () -> setRate(4));
        transport(x + 156, y, ">|", "Forward one frame", () -> step(1));
        Control halfBtn = new Control(x + 186, y, 28, 22);
        halfBtn.painter = (g, c, hover, down) -> drawButton(g, c.x, c.y, c.w, c.h, half ? "1/2*" : "1/2", hover, down, true);
        halfBtn.onClick = () -> {
            half = !half;
            if (Math.abs(rate) == 1f || Math.abs(rate) == 0.5f) setRate(Math.signum(rate));
        };
        halfBtn.tooltip = "Half speed";
        controls.add(halfBtn);
        Control stop = button(x + 218, y, 34, 22, "STOP", () -> {
            setRate(0);
            t = 0;
        });
        stop.tooltip = "Stop and rewind to the head";
        Control rec = button(x + 256, y, 28, 22, "REC", () -> Net.toServer(new RecorderActionPacket(pos, RecorderActionPacket.RECORD)));
        rec.tooltip = "Record the programme on the nearest receiver";
        rec.clickSound = ModSounds.TAPE_REC.get();
        Control ej = button(x + 288, y, 32, 22, "EJECT", () -> {
            Net.toServer(new RecorderActionPacket(pos, RecorderActionPacket.EJECT));
            onClose();
        });
        ej.clickSound = ModSounds.TAPE_EJECT.get();
    }

    /** Development hook for automated screenshots: jump to a time and pause. */
    public void devSeek(double time) {
        t = time;
        rate = 0;
        lastAudioT = time;
    }

    private void transport(int x, int y, String label, String tip, Runnable r) {
        Control c = button(x, y, 24, 22, label, r);
        c.tooltip = tip;
    }

    private void setRate(float r) {
        Broadcast b = tape();
        if (b == null) return;
        float effective = Math.abs(r) == 1f && half ? r * 0.5f : r;
        if (effective != 0 && rate == 0) playUi(ModSounds.RELAY.get(), 1.2f);
        if (Math.abs(effective) >= 4 && Math.abs(rate) < 4) playUi(ModSounds.TAPE_REWIND.get(), 1f);
        rate = effective;
        lastAudioT = t;
    }

    private void step(int dir) {
        if (tape() == null) return;
        rate = 0;
        t = Mth.clamp(t + dir * 0.05, 0, tape().duration);
        lastAudioT = t;
        playUi(ModSounds.KNOB.get(), 1.5f);
    }

    @Override
    protected void drawPanel(GuiGraphics g, int mx, int my, float partial) {
        long now = System.nanoTime();
        double dt = Math.min(0.1, (now - lastNanos) / 1e9);
        lastNanos = now;
        Broadcast b = tape();
        RecorderBlockEntity deck = deck();
        if (b != null && rate != 0) {
            t += dt * rate;
            if (t <= 0 || t >= b.duration) {
                t = Mth.clamp(t, 0, b.duration);
                rate = 0;
                playUi(ModSounds.TAPE_STOP.get(), 1f);
            }
        }
        activeRate = rate;
        updateAudio(b);
        checkFrame(b);

        cabinet(g, 0, 0, panelW, panelH);
        // monitor
        int sx = 12, sy = 12, sw = 176, sh = 132;
        g.fill(sx - 4, sy - 4, sx + sw + 4, sy + sh + 4, 0xFF0E0B09);
        bevel(g, sx - 3, sy - 3, sw + 6, sh + 6, 0xFF2B2420, CHROME_HI, CHROME_LO);
        g.pose().pushPose();
        g.pose().translate(sx, sy, 0);
        g.pose().scale(sw / BroadcastRenderer.W, sh / BroadcastRenderer.H, 1f);
        Canvas c = new Canvas(g.pose(), g.bufferSource(), 0xF000F0, 0.05f, BroadcastRenderer.W, BroadcastRenderer.H);
        if (deck != null && deck.isRecording()) {
            BroadcastRenderer.drawStatic(c, now / 1e9, 0.4f);
            c.text("RECORDING FROM MONITOR BUS", 80, 54, 0xFFFF6050, 1f, true, true);
        } else if (b == null) {
            c.fill(0, 0, BroadcastRenderer.W, BroadcastRenderer.H, 0xFF0A0D0B);
            c.layer();
            c.text("BLANK REEL", 80, 50, 0xFF80A090, 1f, true, false);
            c.text("press REC to record", 80, 62, 0xFF607068, 1f, true, false);
            c.layer();
            c.image("screen_mask", 0, 0, BroadcastRenderer.W, BroadcastRenderer.H, 0xFFFFFFFF);
        } else {
            BroadcastRenderer.Signal sig = BroadcastRenderer.Signal.clean();
            if (Math.abs(rate) >= 4) {
                sig.tear = 0.8f;
                sig.snow = 0.25f;
            }
            BroadcastRenderer.draw(c, b, t, sig);
        }
        g.flush();
        g.pose().popPose();

        // subtitles / caption
        g.pose().pushPose();
        g.pose().translate(0, 0, 50);
        recess(g, sx, sy + sh + 8, sw, 30, 0xFF14100C);
        if (b != null) {
            String sub = rate == 0 && !inFlash(b) ? null : BroadcastRenderer.subtitle(b, t, rate < 0);
            if (Math.abs(rate) >= 4) sub = rate > 0 ? "[ fast forward - chipmunk chatter ]" : "[ rewinding ]";
            if (rate == 0 && inFlash(b)) sub = b.flash.caption;
            if (sub != null) wrapped(g, sub, sx + 4, sy + sh + 12, sw - 8, 0xFFF2E6C0, 2);
            else if (rate == 0) label(g, "[ paused - frame " + (int) Math.round(t * 20) + " ]", sx + 4, sy + sh + 12, 0xFF8A8070);
        }
        g.pose().popPose();

        // deck faceplate
        int fx = 198, fy = 10;
        plate(g, fx, fy, 128, 14);
        labelCentered(g, "VALLEY  VR-600", fx + 64, fy + 3, INK);
        recess(g, fx, fy + 18, 128, 74, 0xFF2A2622);
        boolean hasTape = deck != null && !deck.getTape().isEmpty();
        float spin = deck != null && deck.isRecording() ? 1f : rate;
        float angle = (float) ((now / 1e9) * spin * 4.0);
        Canvas rc = guiCanvas(g);
        if (hasTape) {
            float frac = b == null ? 0.5f : (float) (t / Math.max(1, b.duration));
            rc.imageRotated("reel_full", fx + 33, fy + 55, 46 - frac * 10, 46 - frac * 10, angle, 0xFFFFFFFF);
            rc.imageRotated("reel_empty", fx + 95, fy + 55, 36 + frac * 10, 36 + frac * 10, angle * 1.2f, 0xFFFFFFFF);
            rc.layer();
            rc.line(fx + 33, fy + 78, fx + 95, fy + 78, 1.2f, 0xFF3A2410);
        }
        g.flush();
        // counter & status
        recess(g, fx, fy + 96, 128, 36, 0xFF0C0A08);
        String status;
        if (deck != null && deck.isRecording()) {
            long left = deck.getRecordEnd() - (Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime());
            int pct = (int) Mth.clamp(100 - left * 100 / RecorderBlockEntity.RECORD_TICKS, 0, 100);
            status = "REC  loop buffer " + pct + "%";
        } else if (b == null) {
            status = "STOPPED";
        } else if (rate == 0) {
            status = "PAUSE";
        } else {
            status = (rate < 0 ? "REV " : "PLAY ") + (Math.abs(rate) == 0.5f ? "1/2" : Math.abs(rate) >= 4 ? "x4" : "x1");
        }
        label(g, status, fx + 5, fy + 100, deck != null && deck.isRecording() ? 0xFFFF6050 : AMBER);
        if (b != null) {
            label(g, String.format("%05.1f / %05.1f", t, b.duration), fx + 5, fy + 114, AMBER);
        }
        // label strip
        plate(g, fx, fy + 136, 128, 22);
        String lab = hasTape ? (b == null ? "Blank reel" : TapeItem.label(deck.getTape())) : "-";
        String shown = font().plainSubstrByWidth(lab, 120);
        label(g, shown, fx + 4, fy + 143, INK);
        lamp(g, fx + 116, fy + 100, 0xFFFF4030, deck != null && deck.isRecording());
    }

    private boolean inFlash(Broadcast b) {
        return b.flash != null && t >= b.flash.t && t < b.flash.t + b.flash.d;
    }

    private void checkFrame(Broadcast b) {
        if (b == null || rate != 0 || !inFlash(b) || b.flash.evidence == null) return;
        if (examined.add(b.id)) {
            Net.toServer(new FrameExaminedPacket(b.id));
            playUi(ModSounds.EVIDENCE.get(), 1f);
        }
    }

    private void updateAudio(Broadcast b) {
        boolean moving = b != null && rate != 0;
        if (moving && (hiss == null || hiss.isStopped())) {
            hiss = new LoopSound(ModSounds.TAPE_RUN.get(), SoundSource.BLOCKS, pos.getCenter(), 0.35f, 1f);
            Minecraft.getInstance().getSoundManager().play(hiss);
        }
        if (hiss != null) {
            if (!moving) hiss.stopNow();
            else hiss.setPitch(Mth.clamp(Math.abs(rate), 0.5f, 2f));
        }
        if (!moving) {
            lastAudioT = t;
            return;
        }
        if (Math.abs(rate) >= 4) {
            lastAudioT = t;
            return;
        }
        double from = Math.min(lastAudioT, t), to = Math.max(lastAudioT, t);
        if (rate > 0) {
            for (Broadcast.SoundCue cue : b.sounds) {
                if (cue.t > from && cue.t <= to) BroadcastAudio.playAt(cue.id, pos.getCenter(), cue.vol, cue.pitch * Math.abs(rate));
            }
        }
        Broadcast.Line line = b.lineAt(t);
        if (line != null && line.who != null) {
            double wall = System.nanoTime() / 1e9;
            if (wall >= nextBlip) {
                boolean garble = (rate < 0) != line.reversedSpeech;
                String voice = garble ? "voice_reverse" : ContentLibrary.speaker(line.who).voice;
                float p = ContentLibrary.speaker(line.who).pitch * (Math.abs(rate) < 1 ? 0.75f : 1f);
                BroadcastAudio.playAt(voice, pos.getCenter(), 0.55f, p * (0.9f + (float) Math.random() * 0.2f));
                nextBlip = wall + 0.1 + Math.random() * 0.09 / Math.max(0.5, Math.abs(rate));
            }
        }
        lastAudioT = t;
    }
}
