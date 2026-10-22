package com.lastbroadcast.client.screen;

import com.lastbroadcast.content.ContentLibrary;
import com.lastbroadcast.content.DeductionDef;
import com.lastbroadcast.network.ComparePacket;
import com.lastbroadcast.network.Net;
import com.lastbroadcast.registry.ModSounds;
import com.lastbroadcast.story.CaseState;
import com.lastbroadcast.story.Leads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * FCC case file. Evidence on the left (tick two items to compare them), details on the right,
 * deductions and the current lead in their own tab. Comparisons are judged by the server.
 */
public class JournalScreen extends RetroScreen {
    private static int tab = 0;
    private static String focused;
    private static final List<String> selected = new ArrayList<>();
    private int scroll = 0;
    private static final int ROWS = 12;

    public JournalScreen() {
        super(Component.literal("Case File"), 340, 220);
    }

    @Override
    protected void layout() {
        Control t0 = new Control(14, 6, 70, 16);
        t0.painter = (g, c, h, d) -> tabLabel(g, c, "EVIDENCE", tab == 0, h);
        t0.onClick = () -> tab = 0;
        t0.clickSound = ModSounds.PAPER.get();
        controls.add(t0);
        Control t1 = new Control(88, 6, 78, 16);
        t1.painter = (g, c, h, d) -> tabLabel(g, c, "DEDUCTIONS", tab == 1, h);
        t1.onClick = () -> tab = 1;
        t1.clickSound = ModSounds.PAPER.get();
        controls.add(t1);

        Control compare = button(14, 192, 120, 20, "COMPARE SELECTED", () -> {
            if (selected.size() == 2) Net.toServer(new ComparePacket(selected.get(0), selected.get(1)));
        });
        compare.tooltip = "Look for a contradiction between the two ticked items";
        compare.clickSound = ModSounds.PAPER.get();
        Control read = button(232, 192, 94, 20, "READ / REVIEW", () -> {
            if (focused != null && focused.startsWith("doc:")) {
                Minecraft.getInstance().setScreen(new DocumentScreen(focused.substring(4), this));
            }
        });
        read.tooltip = "Read the full document";
    }

    private void tabLabel(GuiGraphics g, Control c, String text, boolean active, boolean hover) {
        int col = active ? 0xFFE9DBB0 : hover ? 0xFFD6C38F : 0xFFBFA86E;
        g.fill(c.x, c.y, c.x + c.w, c.y + c.h + (active ? 2 : 0), col);
        g.fill(c.x, c.y, c.x + c.w, c.y + 1, 0xFFFFF4D0);
        labelCentered(g, text, c.x + c.w / 2, c.y + 5, INK);
    }

    private List<String> evidence() {
        return new ArrayList<>(CaseState.CLIENT.evidence);
    }

    @Override
    protected boolean panelClicked(int mx, int my, int button) {
        if (tab != 0) return false;
        List<String> ev = evidence();
        for (int i = 0; i < ROWS && i + scroll < ev.size(); i++) {
            int y = 44 + i * 12;
            String id = ev.get(i + scroll);
            if (my >= y && my < y + 12 && mx >= 14 && mx < 170) {
                if (mx < 26) {
                    if (selected.contains(id)) selected.remove(id);
                    else {
                        if (selected.size() >= 2) selected.remove(0);
                        selected.add(id);
                    }
                    playUi(ModSounds.KNOB.get(), 1.3f);
                } else {
                    focused = id;
                    playUi(ModSounds.PAPER.get(), 1.1f);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean panelScrolled(int mx, int my, double sy) {
        int max = Math.max(0, evidence().size() - ROWS);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(sy)));
        return true;
    }

    @Override
    protected void drawPanel(GuiGraphics g, int mx, int my, float partial) {
        // manila folder
        g.fill(4, 18, panelW - 4, panelH - 2, 0xFF6B5A3A);
        bevel(g, 6, 20, panelW - 12, panelH - 26, 0xFFE9DBB0, 0xFFFFF4D0, 0xFFB09A68);
        label(g, "FCC COMPLAINT 62-1147 - WKVR-TV", 178, 8, 0xFFE9DBB0);
        CaseState cs = CaseState.CLIENT;
        // lead
        g.fill(12, 24, panelW - 12, 40, 0xFFF6EED5);
        label(g, "LEAD:", 16, 28, RED);
        String lead = Leads.current(cs);
        String cut = font().plainSubstrByWidth(lead, panelW - 70);
        label(g, cut, 48, 28, INK);
        if (!cut.equals(lead)) tooltipIf(mx, my, 12, 24, panelW - 24, 16, lead);

        if (tab == 0) drawEvidence(g, mx, my);
        else drawDeductions(g);
        controls.get(2).visible = tab == 0;
        controls.get(2).enabled = selected.size() == 2;
        controls.get(3).visible = tab == 0 && focused != null && focused.startsWith("doc:");
    }

    private void tooltipIf(int mx, int my, int x, int y, int w, int h, String text) {
        if (mx >= x && my >= y && mx < x + w && my < y + h) tooltipLater = text;
    }

    private void drawEvidence(GuiGraphics g, int mx, int my) {
        List<String> ev = evidence();
        selected.removeIf(id -> !ev.contains(id));
        recess(g, 12, 42, 160, ROWS * 12 + 4, 0xFFF6EED5);
        if (ev.isEmpty()) label(g, "No evidence logged yet.", 18, 48, 0xFF8A7A5A);
        for (int i = 0; i < ROWS && i + scroll < ev.size(); i++) {
            String id = ev.get(i + scroll);
            int y = 44 + i * 12;
            boolean hover = mx >= 14 && mx < 170 && my >= y && my < y + 12;
            if (id.equals(focused)) g.fill(13, y, 171, y + 12, 0x40C08040);
            else if (hover) g.fill(13, y, 171, y + 12, 0x20C08040);
            g.fill(15, y + 2, 23, y + 10, 0xFF3A3020);
            g.fill(16, y + 3, 22, y + 9, 0xFFFFFBEA);
            if (selected.contains(id)) {
                g.fill(17, y + 4, 21, y + 8, RED);
            }
            String kind = ContentLibrary.evidenceInfo(id).kind;
            int kc = switch (kind) {
                case "tape" -> 0xFF3070A0;
                case "document" -> 0xFF806030;
                default -> 0xFFA03050;
            };
            g.fill(26, y + 3, 30, y + 9, kc);
            String title = font().plainSubstrByWidth(ContentLibrary.evidenceTitle(id), 136);
            label(g, title, 33, y + 2, INK);
        }
        if (ev.size() > ROWS) smallLabel(g, "scroll for more (" + ev.size() + ")", 92, 44 + ROWS * 12 + 6, 0xFF6B5A3A);
        // detail
        recess(g, 178, 42, 150, 146, 0xFFFFFBEA);
        if (focused != null && ev.contains(focused)) {
            var info = ContentLibrary.evidenceInfo(focused);
            int h = wrapped(g, info.title, 182, 46, 142, RED, 3);
            g.fill(182, 48 + h, 324, 49 + h, 0xFFB09A68);
            wrapped(g, info.note, 182, 52 + h, 142, INK, 13 - h / 10);
        } else {
            wrapped(g, "Click an item to review it. Tick the boxes of two items and press COMPARE to test them against each other.", 182, 46, 142, 0xFF8A7A5A, 8);
        }
        smallLabel(g, "selected " + selected.size() + "/2", 154, 198, INK);
    }

    private void drawDeductions(GuiGraphics g) {
        recess(g, 12, 42, panelW - 24, 168, 0xFFFFFBEA);
        int y = 46;
        CaseState cs = CaseState.CLIENT;
        int total = 0, found = 0;
        for (DeductionDef d : ContentLibrary.DEDUCTIONS.values()) {
            if (!d.optional) total++;
            if (cs.deductions.contains(d.id)) {
                if (!d.optional) found++;
                if (y > 190) continue;
                label(g, (d.optional ? "* " : "> ") + d.title, 18, y, RED);
                y += 10;
                y += wrapped(g, d.text, 26, y, panelW - 50, INK, 4) + 4;
            }
        }
        if (cs.deductions.isEmpty()) wrapped(g, "No contradictions established yet. A contradiction needs two pieces of evidence that cannot both be true.", 18, 48, panelW - 40, 0xFF8A7A5A, 4);
        smallLabel(g, "Established " + found + " of " + total + " key contradictions", panelW / 2, 204, INK);
    }
}
