package com.lastbroadcast.client.screen;

import com.lastbroadcast.blockentity.TelevisionBlockEntity;
import com.lastbroadcast.broadcast.Channels;
import com.lastbroadcast.client.render.BroadcastRenderer;
import com.lastbroadcast.client.render.Canvas;
import com.lastbroadcast.content.Broadcast;
import com.lastbroadcast.content.ContentLibrary;
import com.lastbroadcast.network.Net;
import com.lastbroadcast.network.TvControlPacket;
import com.lastbroadcast.registry.ModSounds;
import com.lastbroadcast.story.CaseState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Close-up of a receiver: the picture, subtitles, a click-stop channel dial and power switch. */
public class TelevisionScreen extends RetroScreen {
    private static final int SX = 12, SY = 12, SW = 224, SH = 168;
    private final BlockPos pos;

    public TelevisionScreen(BlockPos pos) {
        super(Component.literal("Television"), 320, 222);
        this.pos = pos;
    }

    private TelevisionBlockEntity tv() {
        var level = Minecraft.getInstance().level;
        return level != null && level.getBlockEntity(pos) instanceof TelevisionBlockEntity t ? t : null;
    }

    @Override
    public void tick() {
        TelevisionBlockEntity tv = tv();
        Minecraft mc = Minecraft.getInstance();
        if (tv == null || mc.player == null || mc.player.distanceToSqr(pos.getCenter()) > 64) onClose();
    }

    @Override
    protected void layout() {
        Control dial = new Control(252, 40, 56, 56);
        dial.painter = (g, c, hover, down) -> {
            TelevisionBlockEntity tv = tv();
            int ch = tv == null ? 2 : tv.getChannel();
            knob(g, c.x + 28, c.y + 28, 44, -150 + ch * 50f);
        };
        dial.onClick = () -> change(1);
        dial.onScroll = this::change;
        dial.clickSound = ModSounds.CHANNEL_CLICK.get();
        dial.tooltip = "Channel selector - click or scroll";
        controls.add(dial);

        Control power = new Control(262, 150, 36, 18);
        power.painter = (g, c, hover, down) -> {
            TelevisionBlockEntity tv = tv();
            boolean on = tv != null && tv.isPowered();
            recess(g, c.x, c.y, c.w, c.h, 0xFF1A1410);
            int kx = on ? c.x + c.w - 16 : c.x + 2;
            bevel(g, kx, c.y + 2, 14, c.h - 4, hover ? CHROME_HI : CHROME, 0xFFFFFFFF, CHROME_LO);
        };
        power.onClick = () -> {
            TelevisionBlockEntity tv = tv();
            if (tv != null) Net.toServer(new TvControlPacket(pos, !tv.isPowered(), tv.getChannel()));
        };
        power.clickSound = ModSounds.BREAKER.get();
        power.tooltip = "Power";
        controls.add(power);
    }

    private void change(int dir) {
        TelevisionBlockEntity tv = tv();
        if (tv == null) return;
        int next = Channels.next(CaseState.CLIENT, tv.getChannel(), dir);
        Net.toServer(new TvControlPacket(pos, true, next));
    }

    @Override
    protected void drawPanel(GuiGraphics g, int mx, int my, float partial) {
        cabinet(g, 0, 0, panelW, panelH);
        // tube bezel
        g.fill(SX - 6, SY - 6, SX + SW + 6, SY + SH + 6, 0xFF0E0B09);
        bevel(g, SX - 4, SY - 4, SW + 8, SH + 8, 0xFF2B2420, CHROME_HI, CHROME_LO);
        TelevisionBlockEntity tv = tv();
        var level = Minecraft.getInstance().level;
        if (tv != null && level != null) {
            double time = level.getGameTime() + partial;
            g.pose().pushPose();
            g.pose().translate(SX, SY, 0);
            g.pose().scale(SW / BroadcastRenderer.W, SH / BroadcastRenderer.H, 1f);
            Canvas c = new Canvas(g.pose(), g.bufferSource(), 0xF000F0, 0.05f, BroadcastRenderer.W, BroadcastRenderer.H);
            Broadcast shown = null;
            double t = 0;
            boolean surging = tv.isSurging(level.getGameTime());
            if (!tv.isPowered() && !surging) {
                c.fill(0, 0, BroadcastRenderer.W, BroadcastRenderer.H, 0xFF101412);
                c.layer();
                c.image("screen_mask", 0, 0, BroadcastRenderer.W, BroadcastRenderer.H, 0xFFFFFFFF);
            } else if (surging) {
                shown = ContentLibrary.broadcast("surge_frame");
                t = (time % 60) / 20.0;
                BroadcastRenderer.draw(c, shown, t, BroadcastRenderer.Signal.clean());
            } else {
                Channels.OnAir onAir = Channels.onAir(CaseState.CLIENT, tv.getChannel(), time);
                shown = onAir.programme();
                t = onAir.time();
                if (shown == null) BroadcastRenderer.drawStatic(c, time / 20.0, 0.9f);
                else BroadcastRenderer.draw(c, shown, t, BroadcastRenderer.Signal.clean());
            }
            g.flush();
            g.pose().popPose();
            // subtitles
            String sub = tv.isPowered() || surging ? BroadcastRenderer.subtitle(shown, t, false) : null;
            g.pose().pushPose();
            g.pose().translate(0, 0, 50);
            recess(g, SX, SY + SH + 10, SW, 28, 0xFF14100C);
            if (sub != null) wrapped(g, sub, SX + 5, SY + SH + 14, SW - 10, 0xFFF2E6C0, 2);
            else if (tv.isPowered() && shown == null) label(g, "[ snow - no carrier on this channel ]", SX + 5, SY + SH + 14, 0xFF8A8070);
            g.pose().popPose();
        }
        // control column
        plate(g, 248, 14, 64, 20);
        labelCentered(g, "CHANNEL", 280, 20, INK);
        String ch = tv == null ? "-" : Channels.label(tv.getChannel());
        recess(g, 256, 104, 48, 20, 0xFF0C0A08);
        labelCentered(g, ch, 280, 110, AMBER);
        // dial index marks
        for (int i = 0; i < Channels.DIAL.length; i++) {
            if (!Channels.selectable(CaseState.CLIENT, i)) continue;
            double a = Math.toRadians(-150 + i * 50f);
            int px = 280 + (int) Math.round(Math.sin(a) * 30), py = 68 - (int) Math.round(Math.cos(a) * 30);
            g.fill(px - 1, py - 1, px + 1, py + 1, CREAM_DK);
        }
        plate(g, 248, 130, 64, 14);
        smallLabel(g, "OFF        ON", 280, 134, INK);
        // speaker grille
        for (int i = 0; i < 6; i++) g.fill(254, 178 + i * 5, 306, 180 + i * 5, 0xFF1A120C);
    }
}
