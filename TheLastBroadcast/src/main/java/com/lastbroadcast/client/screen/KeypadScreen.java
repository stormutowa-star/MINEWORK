package com.lastbroadcast.client.screen;

import com.lastbroadcast.network.KeypadPacket;
import com.lastbroadcast.network.Net;
import com.lastbroadcast.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Four-wheel combination lock on Penrose's locker. */
public class KeypadScreen extends RetroScreen {
    private final BlockPos pos;
    private final int[] digits = {0, 0, 0, 0};

    public KeypadScreen(BlockPos pos) {
        super(Component.translatable("lastbroadcast.ui.keypad.title"), 200, 150);
        this.pos = pos;
    }

    @Override
    public void tick() {
        var p = Minecraft.getInstance().player;
        if (p == null || p.distanceToSqr(pos.getCenter()) > 64) onClose();
    }

    @Override
    protected void layout() {
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            int x = 30 + i * 36;
            Control up = button(x, 34, 28, 16, "+", () -> digits[idx] = (digits[idx] + 1) % 10);
            up.clickSound = ModSounds.KNOB.get();
            Control wheel = new Control(x, 52, 28, 34);
            wheel.painter = (g, c, h, d) -> {
                recess(g, c.x, c.y, c.w, c.h, 0xFF2A2018);
                g.fill(c.x + 2, c.y + 2, c.x + c.w - 2, c.y + c.h - 2, 0xFFB08A3A);
                g.fill(c.x + 2, c.y + 2, c.x + c.w - 2, c.y + 8, 0x40000000);
                g.fill(c.x + 2, c.y + c.h - 8, c.x + c.w - 2, c.y + c.h - 2, 0x40000000);
                g.pose().pushPose();
                g.pose().translate(c.x + c.w / 2f, c.y + 10, 0);
                g.pose().scale(2, 2, 1);
                String s = String.valueOf(digits[idx]);
                g.drawString(font(), s, -font().width(s) / 2, 0, 0xFF1A1208, false);
                g.pose().popPose();
            };
            wheel.onScroll = dir -> digits[idx] = Math.floorMod(digits[idx] + dir, 10);
            wheel.onClick = () -> digits[idx] = (digits[idx] + 1) % 10;
            controls.add(wheel);
            Control down = button(x, 88, 28, 16, "-", () -> digits[idx] = Math.floorMod(digits[idx] - 1, 10));
            down.clickSound = ModSounds.KNOB.get();
        }
        Control tryBtn = button(50, 116, 100, 22, tr("lastbroadcast.ui.keypad.try"), () -> {
            StringBuilder sb = new StringBuilder();
            for (int d : digits) sb.append(d);
            Net.toServer(new KeypadPacket(pos, sb.toString()));
            onClose();
        });
        tryBtn.clickSound = ModSounds.LOCKER.get();
    }

    @Override
    protected void drawPanel(GuiGraphics g, int mx, int my, float partial) {
        bevel(g, 0, 0, panelW, panelH, 0xFF8E969E, CHROME_HI, CHROME_LO);
        for (int y = 4; y < panelH - 4; y += 4) g.fill(4, y, panelW - 4, y + 1, 0x10000000);
        plate(g, 40, 8, 120, 18);
        labelCentered(g, tr("lastbroadcast.ui.keypad.plate"), 100, 13, INK);
    }
}
