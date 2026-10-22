package com.lastbroadcast.client.screen;

import com.lastbroadcast.content.ContentLibrary;
import com.lastbroadcast.content.DeductionDef;
import com.lastbroadcast.story.CaseState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** The investigator's closing report, typed out line by line. */
public class EndingScreen extends RetroScreen {
    private static final String[] REPORT = {
            "FEDERAL COMMUNICATIONS COMMISSION",
            "Field Engineering & Monitoring Bureau - Detroit District",
            "",
            "COMPLAINT 62-1147  /  WKVR-TV, HALLOWAY FALLS, OHIO",
            "DISPOSITION: RESOLVED ON SITE, 24 OCT 1962, 00:40",
            "",
            "Licensee was operating the Skyhook array at approx. four times authorized visual power on Tuesday nights since 18 September. Interference reports coincide with every unauthorized run.",
            "",
            "Overdrive equipment was shut down by this engineer and the chief engineer and is now disconnected. The posted emergency shutdown procedure was found to be out of date and has been withdrawn. The demonstration scheduled for 25 October is cancelled.",
            "",
            "Nature of the received interference: see attached annex, which this engineer does not expect anyone to believe.",
            "",
            "Channel 7-B: no carrier.",
    };
    private long opened = System.currentTimeMillis();

    public EndingScreen() {
        super(Component.literal("Report"), 300, 226);
    }

    @Override
    protected void layout() {
        button(100, 200, 100, 20, "FILE REPORT", this::onClose);
    }

    @Override
    protected void drawPanel(GuiGraphics g, int mx, int my, float partial) {
        g.fill(2, 2, panelW, panelH - 28, 0x60000000);
        g.fill(0, 0, panelW - 2, panelH - 30, 0xFFFBF7EA);
        int chars = (int) ((System.currentTimeMillis() - opened) / 9);
        int y = 8;
        for (String line : REPORT) {
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
            label(g, "Contradictions established: " + found + "/" + total + (optional > 0 ? "  (+" + optional + " additional)" : ""), 10, y, 0xFF7A2020);
            label(g, "Evidence items logged: " + cs.evidence.size(), 10, y + 10, 0xFF7A2020);
            g.pose().pushPose();
            g.pose().translate(214, 150, 0);
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-10));
            g.fill(-4, -4, 66, 16, 0x50B02020);
            g.drawString(font(), "CASE CLOSED", 2, 2, 0xD0B02020, false);
            g.pose().popPose();
        }
    }
}
