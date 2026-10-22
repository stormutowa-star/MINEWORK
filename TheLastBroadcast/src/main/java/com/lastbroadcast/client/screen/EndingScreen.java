package com.lastbroadcast.client.screen;

import com.lastbroadcast.content.ContentLibrary;
import com.lastbroadcast.content.DeductionDef;
import com.lastbroadcast.story.CaseState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** The investigator's closing report, typed out line by line. */
public class EndingScreen extends RetroScreen {
    private static final String[] REPORT = {
            "lastbroadcast.ui.ending.r1",
            "lastbroadcast.ui.ending.r2",
            "",
            "lastbroadcast.ui.ending.r3",
            "lastbroadcast.ui.ending.r4",
            "",
            "lastbroadcast.ui.ending.r5",
            "",
            "lastbroadcast.ui.ending.r6",
            "",
            "lastbroadcast.ui.ending.r7",
            "",
            "lastbroadcast.ui.ending.r8"
    };
    private long opened = System.currentTimeMillis();

    public EndingScreen() {
        super(Component.translatable("lastbroadcast.ui.ending.title"), 300, 226);
    }

    @Override
    protected void layout() {
        button(100, 200, 100, 20, tr("lastbroadcast.ui.ending.file"), this::onClose);
    }

    @Override
    protected void drawPanel(GuiGraphics g, int mx, int my, float partial) {
        g.fill(2, 2, panelW, panelH - 28, 0x60000000);
        g.fill(0, 0, panelW - 2, panelH - 30, 0xFFFBF7EA);
        int chars = (int) ((System.currentTimeMillis() - opened) / 9);
        int y = 8;
        for (String key : REPORT) {
            String line = key.isEmpty() ? "" : tr(key);
            if (chars <= 0) break;
            String shown = line.length() > chars ? line.substring(0, chars) : line;
            chars -= line.length();
            if (line.isEmpty()) {
                y += 5;
                continue;
            }
            y += wrapped(g, shown, 10, y, panelW - 24, 0xFF2A2420, 6);
        }
        if (chars > 0) {
            CaseState cs = CaseState.CLIENT;
            int found = 0, total = 0, optional = 0;
            for (DeductionDef d : ContentLibrary.DEDUCTIONS.values()) {
                if (d.optional) {
                    if (cs.deductions.contains(d.id)) optional++;
                } else {
                    total++;
                    if (cs.deductions.contains(d.id)) found++;
                }
            }
            y += 4;
            label(g, tr("lastbroadcast.ui.ending.contradictions", found, total) + (optional > 0 ? tr("lastbroadcast.ui.ending.additional", optional) : ""), 10, y, 0xFF7A2020);
            label(g, tr("lastbroadcast.ui.ending.evidence", cs.evidence.size()), 10, y + 10, 0xFF7A2020);
            g.pose().pushPose();
            g.pose().translate(214, 150, 0);
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-10));
            g.fill(-4, -4, 78, 16, 0x50B02020);
            g.drawString(font(), tr("lastbroadcast.ui.ending.closed"), 2, 2, 0xD0B02020, false);
            g.pose().popPose();
        }
    }
}
