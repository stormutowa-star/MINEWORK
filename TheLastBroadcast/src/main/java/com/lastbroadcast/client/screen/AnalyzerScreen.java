package com.lastbroadcast.client.screen;

import com.lastbroadcast.client.render.BroadcastRenderer;
import com.lastbroadcast.client.render.Canvas;
import com.lastbroadcast.content.ContentLibrary;
import com.lastbroadcast.network.AnalyzerLockPacket;
import com.lastbroadcast.network.Net;
import com.lastbroadcast.registry.ModSounds;
import com.lastbroadcast.story.CaseState;
import com.lastbroadcast.story.Flags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Bench signal analyzer. Three controls interact: FREQUENCY finds the carrier, PHASE nulls the
 * station's own picture (revealing what hides underneath), H-HOLD stops the ghost picture rolling.
 * The scope, needle meter and preview monitor all respond continuously, so the player can feel
 * their way to a lock.
 */
public class AnalyzerScreen extends RetroScreen {
    private static int freq = 17150, phase = 0, hold = 60; // remembered between openings
    private final BlockPos pos;
    private float meterValue = 0f;
    private int lastQuantum = -1;

    public AnalyzerScreen(BlockPos pos) {
        super(Component.translatable("lastbroadcast.ui.analyzer.title"), 330, 214);
        this.pos = pos;
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.distanceToSqr(pos.getCenter()) > 64) onClose();
        float target = strength() * (0.45f + 0.55f * phaseQuality());
        meterValue += (target - meterValue) * 0.35f + (float) (Math.random() - 0.5) * 0.02f * target;
        int q = Math.round(strength() * 6);
        if (q != lastQuantum && q >= 4 && lastQuantum >= 0) playUi(ModSounds.METER_CLICK.get(), 0.8f + q * 0.1f);
        lastQuantum = q;
    }

    private float strength() {
        float f = Math.abs(freq - AnalyzerLockPacket.TARGET_FREQ) / 38f;
        return (float) Math.exp(-f * f);
    }

    private float phaseQuality() {
        int dp = Math.abs(((phase - AnalyzerLockPacket.TARGET_PHASE) % 360 + 540) % 360 - 180);
        return 1f - dp / 180f;
    }

    private float holdError() {
        return Math.abs(hold - AnalyzerLockPacket.TARGET_HOLD);
    }

    private boolean locked() {
        return AnalyzerLockPacket.locked(freq, phase, hold);
    }

    @Override
    protected void layout() {
        knobControl(18, 140, "FREQUENCY", v -> freq = Mth.clamp(freq + v * (Screen.hasShiftDown() ? 1 : 5), 17000, 18000));
        knobControl(98, 140, "PHASE", v -> phase = Math.floorMod(phase + v * 5, 360));
        knobControl(178, 140, "H-HOLD", v -> hold = Mth.clamp(hold + v, 0, 100));
        Control patch = button(250, 184, 50, 20, tr("lastbroadcast.ui.analyzer.patch"), () -> Net.toServer(new AnalyzerLockPacket(pos, freq, phase, hold)));
        patch.tooltip = tr("lastbroadcast.ui.analyzer.patch_tip");
        patch.clickSound = ModSounds.RELAY.get();
    }

    private interface Adjust {
        void by(int v);
    }

    private void knobControl(int x, int y, String name, Adjust adj) {
        Control k = new Control(x, y, 60, 60);
        k.painter = (g, c, hover, down) -> {
            float angle = switch (name) {
                case "FREQUENCY" -> (freq - 17000) / 1000f * 300f - 150f;
                case "PHASE" -> phase - 150f;
                default -> hold / 100f * 300f - 150f;
            };
            knob(g, c.x + 30, c.y + 26, 40, angle);
            smallLabel(g, tr(switch (name) { case "FREQUENCY" -> "lastbroadcast.ui.analyzer.freq"; case "PHASE" -> "lastbroadcast.ui.analyzer.phase"; default -> "lastbroadcast.ui.analyzer.hhold"; }), c.x + 30, c.y + 52, CREAM);
            smallLabel(g, "-          +", c.x + 30, c.y + 4, CREAM_DK);
        };
        k.onScroll = adj::by;
        double[] acc = {0};
        k.onDrag = (dx, dy) -> {
            acc[0] += dx - dy;
            while (acc[0] >= 3) { adj.by(1); acc[0] -= 3; }
            while (acc[0] <= -3) { adj.by(-1); acc[0] += 3; }
        };
        k.onClick = () -> {
            int mx = lastMouseX;
            adj.by(mx < k.x + 30 ? -1 : 1);
        };
        k.tooltip = tr("lastbroadcast.ui.analyzer.knob_tip");
        controls.add(k);
    }

    private int lastMouseX;

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        lastMouseX = toPanelX(mouseX);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void drawPanel(GuiGraphics g, int mx, int my, float partial) {
        cabinet(g, 0, 0, panelW, panelH);
        // header plate
        plate(g, 10, 8, 200, 14);
        labelCentered(g, tr("lastbroadcast.ui.analyzer.header"), 110, 11, INK);
        double time = (Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime()) + partial;

        // ---- scope ----
        int sx = 12, sy = 28, sw = 196, sh = 104;
        g.fill(sx - 3, sy - 3, sx + sw + 3, sy + sh + 3, 0xFF0A0A0A);
        bevel(g, sx - 2, sy - 2, sw + 4, sh + 4, 0xFF06120A, CHROME_LO, CHROME_HI);
        for (int i = 1; i < 8; i++) g.fill(sx + i * sw / 8, sy, sx + i * sw / 8 + 1, sy + sh, 0x2840FF70);
        for (int i = 1; i < 4; i++) g.fill(sx, sy + i * sh / 4, sx + sw, sy + i * sh / 4 + 1, 0x2840FF70);
        Canvas c = guiCanvas(g);
        float s = strength(), pq = phaseQuality();
        float refAmp = (float) Math.exp(-Math.pow(Math.abs(freq - 17525) / 60f, 2)) * (1f - pq) * 0.9f + 0.04f;
        float ghostAmp = s * (0.25f + 0.75f * pq);
        float drift = (float) (time * holdError() * 0.8);
        float phaseShift = (phase / 360f) * 40f;
        trace(c, sx, sy, sw, sh, refAmp, 0, 0, false, 0x9050FF80, time);
        c.layer();
        trace(c, sx, sy, sw, sh, ghostAmp, drift + phaseShift, holdError() * 0.4f, true, 0xE0FFB040, time);
        g.flush();
        smallLabel(g, tr("lastbroadcast.ui.analyzer.ref"), sx + 22, sy + 3, 0xFF60FF80);
        smallLabel(g, tr("lastbroadcast.ui.analyzer.residual"), sx + sw - 20, sy + 3, AMBER);

        // ---- readouts ----
        recess(g, 218, 28, 102, 50, 0xFF0C0A08);
        label(g, String.format("%d.%02d Mc", freq / 100, freq % 100), 224, 33, AMBER);
        label(g, tr("lastbroadcast.ui.analyzer.phase_readout", phase), 224, 47, AMBER);
        label(g, tr("lastbroadcast.ui.analyzer.hhold_readout", hold), 224, 61, AMBER);

        // ---- meter ----
        meter(g, 218, 84, 102, 46, meterValue, tr("lastbroadcast.ui.analyzer.field"));

        // ---- preview monitor ----
        int px = 248, py = 136;
        // drawn as a 4:3 inset; the residual picture fades in as the lock improves
        g.fill(px - 2, py - 2, px + 48 + 2, py + 36 + 2, 0xFF000000);
        g.pose().pushPose();
        g.pose().translate(px, py, 0);
        g.pose().scale(48 / BroadcastRenderer.W, 36 / BroadcastRenderer.H, 1);
        Canvas pc = new Canvas(g.pose(), g.bufferSource(), 0xF000F0, 0.05f, BroadcastRenderer.W, BroadcastRenderer.H);
        BroadcastRenderer.Signal sig = new BroadcastRenderer.Signal();
        float clarity = s * pq * pq;
        sig.snow = Mth.clamp(1.05f - clarity, 0, 1);
        sig.roll = holdError() > 2 ? Math.min(4f, holdError() * 0.12f) : 0f;
        sig.ghost = (1 - pq) * 0.8f;
        sig.tear = holdError() > 2 ? Math.min(1f, holdError() * 0.03f) : 0f;
        BroadcastRenderer.draw(pc, ContentLibrary.broadcast("p1_today_thursday"), (time / 20.0) % 50.0, sig);
        g.flush();
        g.pose().popPose();
        smallLabel(g, tr("lastbroadcast.ui.analyzer.preview"), px + 24, py + 39, CREAM_DK);

        // ---- lamps ----
        boolean patched = CaseState.CLIENT.has(Flags.SIGNAL_LOCKED);
        lamp(g, 302, 140, 0xFF40FF60, locked());
        smallLabel(g, tr("lastbroadcast.ui.analyzer.lock"), 305, 150, CREAM);
        lamp(g, 302, 158, AMBER, patched);
        smallLabel(g, "7-B", 305, 168, CREAM);
    }

    /** One video line: sync tip, back porch, colour burst, a luminance staircase, front porch. */
    private void trace(Canvas c, int x, int y, int w, int h, float amp, float shift, float jitter, boolean inverted, int color, double time) {
        float mid = y + h * 0.55f;
        float prevX = 0, prevY = 0;
        int n = 120;
        for (int i = 0; i <= n; i++) {
            float fx = i / (float) n;
            float ph = ((fx * 2f * 40f + shift) % 40f + 40f) % 40f / 40f; // two lines across the screen
            float v;
            if (ph < 0.08f) v = -0.35f;
            else if (ph < 0.14f) v = 0f;
            else if (ph < 0.2f) v = (float) Math.sin(ph * 900) * 0.12f;
            else if (ph < 0.9f) v = 0.12f + ((int) ((ph - 0.2f) / 0.7f * 6f)) * 0.12f;
            else v = 0f;
            if (inverted) v = -v;
            v += (float) (Math.sin(time * 3.7 + i * 0.9) * 0.03 * jitter);
            v += (float) ((Math.random() - 0.5) * 0.04 * (1.1 - amp));
            float px = x + fx * w;
            float py = mid - v * amp * h * 0.8f;
            if (i > 0) c.line(prevX, prevY, px, py, 0.9f, color);
            prevX = px;
            prevY = py;
        }
    }
}
