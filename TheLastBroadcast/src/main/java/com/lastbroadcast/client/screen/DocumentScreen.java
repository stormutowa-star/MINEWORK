package com.lastbroadcast.client.screen;

import com.lastbroadcast.content.ContentLibrary;
import com.lastbroadcast.content.DocumentDef;
import com.lastbroadcast.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** A sheet of typed paper. Pages flip with the arrows (or mouse wheel). */
public class DocumentScreen extends RetroScreen {
    private final DocumentDef doc;
    private final Screen parent;
    private int page = 0;

    public DocumentScreen(String docId, Screen parent) {
        super(Component.literal("Document"), 240, 226);
        this.doc = ContentLibrary.DOCUMENTS.get(docId);
        this.parent = parent;
    }

    @Override
    protected void layout() {
        Control prev = button(8, 200, 30, 20, "<", () -> flip(-1));
        prev.clickSound = ModSounds.PAPER.get();
        Control next = button(202, 200, 30, 20, ">", () -> flip(1));
        next.clickSound = ModSounds.PAPER.get();
    }

    private void flip(int d) {
        if (doc == null) return;
        page = Math.max(0, Math.min(doc.pages.size() - 1, page + d));
    }

    @Override
    protected boolean panelScrolled(int mx, int my, double sy) {
        flip(sy > 0 ? -1 : 1);
        return true;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    protected void drawPanel(GuiGraphics g, int mx, int my, float partial) {
        if (doc == null) return;
        int paper = switch (doc.kind) {
            case "log" -> 0xFFF1EBD8;
            case "notice" -> 0xFFF7F2E2;
            case "clipping" -> 0xFFE8DFC4;
            case "memo" -> 0xFFF4F0DC;
            default -> 0xFFFBF7EA;
        };
        g.fill(2, 2, panelW, panelH - 26 + 2, 0x60000000);
        g.fill(0, 0, panelW - 2, panelH - 26, paper);
        if ("log".equals(doc.kind)) {
            for (int y = 34; y < panelH - 30; y += 10) g.fill(6, y + 9, panelW - 8, y + 10, 0x302060A0);
            g.fill(22, 0, 23, panelH - 26, 0x40C04040);
        }
        g.fill(0, 0, panelW - 2, 2, 0x20000000);
        labelCentered(g, doc.title.toUpperCase(), panelW / 2, 8, INK);
        if (doc.header != null && !doc.header.isEmpty()) {
            g.pose().pushPose();
            g.pose().translate(panelW / 2f, 19, 0);
            g.pose().scale(0.75f, 0.75f, 1);
            g.drawString(font(), doc.header, -font().width(doc.header) / 2, 0, 0xFF5A5040, false);
            g.pose().popPose();
        }
        g.fill(12, 28, panelW - 14, 29, 0x60403020);
        String text = doc.pages.isEmpty() ? "" : doc.pages.get(Math.min(page, doc.pages.size() - 1));
        int y = 34;
        for (String para : text.split("\n")) {
            List<FormattedCharSequence> lines = font().split(Component.literal(para), panelW - 34);
            if (lines.isEmpty()) y += 5;
            for (FormattedCharSequence l : lines) {
                if (y > panelH - 40) break;
                g.drawString(font(), l, 14, y, 0xFF2A2420, false);
                y += 10;
            }
        }
        if (doc.stamp != null && page == doc.pages.size() - 1) {
            g.pose().pushPose();
            g.pose().translate(panelW - 70, panelH - 52, 0);
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-12));
            int w = font().width(doc.stamp) + 8;
            g.fill(-2, -2, w + 2, 14, 0x50B02020);
            g.fill(0, 0, w, 12, 0x30FFFFFF);
            g.drawString(font(), doc.stamp, 4, 2, 0xC0B02020, false);
            g.pose().popPose();
        }
        labelCentered(g, "page " + (page + 1) + " of " + Math.max(1, doc.pages.size()), panelW / 2, panelH - 18, 0xFFE9DBB0);
        controls.get(0).visible = page > 0;
        controls.get(1).visible = page < doc.pages.size() - 1;
    }
}
