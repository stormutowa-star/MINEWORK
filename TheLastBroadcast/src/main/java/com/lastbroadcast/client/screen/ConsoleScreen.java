package com.lastbroadcast.client.screen;

import com.lastbroadcast.blockentity.ConsoleBlockEntity;
import com.lastbroadcast.network.ConsoleActionPacket;
import com.lastbroadcast.network.Net;
import com.lastbroadcast.registry.ModSounds;
import com.lastbroadcast.story.CaseState;
import com.lastbroadcast.story.Flags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Skyhook overdrive control console: coolant valves, final drive, plate breaker, antenna ground. */
public class ConsoleScreen extends RetroScreen {
    private final BlockPos pos;

    public ConsoleScreen(BlockPos pos) {
        super(Component.translatable("lastbroadcast.ui.console.title"), 330, 206);
        this.pos = pos;
    }

    private ConsoleBlockEntity console() {
        var level = Minecraft.getInstance().level;
        return level != null && level.getBlockEntity(pos) instanceof ConsoleBlockEntity c ? c : null;
    }

    private long now() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0 : level.getGameTime();
    }

    @Override
    public void tick() {
        var p = Minecraft.getInstance().player;
        if (p == null || console() == null || p.distanceToSqr(pos.getCenter()) > 64) onClose();
    }

    private void send(int action, int index) {
        Net.toServer(new ConsoleActionPacket(pos, action, index));
    }

    @Override
    protected void layout() {
        for (int i = 0; i < 6; i++) {
            final int idx = i;
            Control v = new Control(16 + i * 34, 44, 26, 40);
            v.painter = (g, c, hover, down) -> {
                ConsoleBlockEntity con = console();
                boolean open = con != null && (con.getValves() & (1 << idx)) != 0;
                recess(g, c.x, c.y, c.w, c.h, 0xFF1A1612);
                // valve wheel handle drawn as a lever: up = open
                int ly = open ? c.y + 4 : c.y + c.h - 16;
                bevel(g, c.x + 6, ly, c.w - 12, 12, hover ? 0xFFE04030 : 0xFFC03020, 0xFFFF8070, 0xFF601010);
                lamp(g, c.x + 10, c.y + c.h + 4, 0xFF40E060, open);
                smallLabel(g, tr("lastbroadcast.ui.console.valve", idx + 1), c.x + c.w / 2, c.y + c.h + 13, CREAM);
            };
            v.onClick = () -> send(ConsoleBlockEntity.A_VALVE, idx);
            v.clickSound = ModSounds.VALVE.get();
            controls.add(v);
        }
        Control down = button(222, 112, 26, 20, "-", () -> send(ConsoleBlockEntity.A_DRIVE_DOWN, 0));
        down.tooltip = tr("lastbroadcast.ui.console.drive_down");
        Control up = button(290, 112, 26, 20, "+", () -> send(ConsoleBlockEntity.A_DRIVE_UP, 0));
        up.tooltip = tr("lastbroadcast.ui.console.drive_up");

        Control plate = new Control(30, 150, 70, 36);
        plate.painter = (g, c, hover, d) -> breaker(g, c, tr("lastbroadcast.ui.console.plate"), console() != null && console().isPlateClosed(), hover, tr("lastbroadcast.ui.console.closed"), tr("lastbroadcast.ui.console.open"));
        plate.onClick = () -> send(ConsoleBlockEntity.A_PLATE, 0);
        plate.clickSound = ModSounds.BREAKER.get();
        controls.add(plate);
        Control ground = new Control(120, 150, 70, 36);
        ground.painter = (g, c, hover, d) -> breaker(g, c, tr("lastbroadcast.ui.console.ground"), console() != null && console().isGrounded(), hover, tr("lastbroadcast.ui.console.grounded"), tr("lastbroadcast.ui.console.live"));
        ground.onClick = () -> send(ConsoleBlockEntity.A_GROUND, 0);
        ground.clickSound = ModSounds.BREAKER.get();
        controls.add(ground);
    }

    private void breaker(GuiGraphics g, Control c, String name, boolean on, boolean hover, String onText, String offText) {
        recess(g, c.x, c.y, c.w, c.h - 10, 0xFF1A1612);
        int ky = on ? c.y + 3 : c.y + c.h - 24;
        bevel(g, c.x + c.w / 2 - 8, ky, 16, 11, hover ? CHROME_HI : CHROME, 0xFFFFFFFF, CHROME_LO);
        smallLabel(g, on ? onText : offText, c.x + c.w / 2, c.y + 12, on ? AMBER : CREAM_DK);
        smallLabel(g, name, c.x + c.w / 2, c.y + c.h - 6, CREAM);
    }

    @Override
    protected void drawPanel(GuiGraphics g, int mx, int my, float partial) {
        bevel(g, 0, 0, panelW, panelH, 0xFF6E7A70, 0xFFA8B4AA, 0xFF3A443C);
        for (int y = 3; y < panelH; y += 6) g.fill(2, y, panelW - 2, y + 1, 0x0C000000);
        plate(g, 10, 8, 196, 16);
        labelCentered(g, tr("lastbroadcast.ui.console.header"), 108, 12, INK);
        ConsoleBlockEntity con = console();
        if (con == null) return;
        CaseState cs = CaseState.CLIENT;
        boolean keyed = cs.has(Flags.PENROSE_CONFESSED);
        boolean fault = now() < con.getFaultUntil();
        boolean done = con.isShutDown() || cs.has(Flags.CASE_SOLVED);
        // status lamps
        lamp(g, 222, 12, 0xFFFF3020, !done && (now() / 8) % 2 == 0);
        smallLabel(g, tr("lastbroadcast.ui.console.overdrive"), 244, 14, CREAM);
        lamp(g, 272, 12, 0xFFFFB020, fault && (now() / 3) % 2 == 0);
        smallLabel(g, tr("lastbroadcast.ui.console.fault"), 292, 14, CREAM);
        lamp(g, 222, 26, 0xFF40E060, keyed);
        smallLabel(g, tr("lastbroadcast.ui.console.key"), 238, 28, CREAM);
        lamp(g, 272, 26, 0xFF40E060, done);
        smallLabel(g, tr("lastbroadcast.ui.console.safe"), 290, 28, CREAM);
        // gauges
        float flow = con.coolantFlowing() ? 0.78f + (float) Math.sin(now() * 0.3) * 0.03f : 0.03f;
        meter(g, 222, 40, 96, 30, flow, tr("lastbroadcast.ui.console.flow"));
        float temp = done ? 0.08f : con.coolantFlowing() ? 0.35f + con.getDrive() * 0.02f : 0.62f + con.getDrive() * 0.03f;
        if (fault) temp = 1f;
        meter(g, 222, 74, 96, 30, temp, tr("lastbroadcast.ui.console.temp"));
        // drive
        recess(g, 250, 112, 38, 20, 0xFF0C0A08);
        labelCentered(g, String.valueOf(con.getDrive() * 10) + "%", 269, 118, AMBER);
        smallLabel(g, tr("lastbroadcast.ui.console.final_drive"), 269, 136, CREAM);
        // locked / done overlays
        if (!keyed) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 80);
            g.fill(208, 150, 324, 196, 0xD0201810);
            wrapped(g, tr("lastbroadcast.ui.console.locked"), 214, 156, 106, CREAM, 4);
            g.pose().popPose();
        } else if (done) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 80);
            g.fill(208, 150, 324, 196, 0xD0102018);
            wrapped(g, tr("lastbroadcast.ui.console.done"), 214, 156, 106, 0xFF90FFB0, 4);
            g.pose().popPose();
        } else {
            wrapped(g, fault ? tr("lastbroadcast.ui.console.tripped") : tr("lastbroadcast.ui.console.in_service"), 214, 156, 106, fault ? AMBER : CREAM_DK, 4);
        }
    }
}
