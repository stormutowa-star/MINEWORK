package com.lastbroadcast.client.screen;

import com.lastbroadcast.client.render.Atlas;
import com.lastbroadcast.client.render.Canvas;
import com.lastbroadcast.registry.ModSounds;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Base for all mod screens: a fixed-size panel laid out in "panel units", uniformly scaled down
 * if the window is too small, with lightweight hand-drawn controls in a mid-century instrument
 * style (bakelite, chrome bezels, engraved labels, pilot lamps). Never pauses the game, so
 * broadcasts keep running while a screen is open.
 */
public abstract class RetroScreen extends Screen {
    public static final int BAKELITE = 0xFF3A2A20, BAKELITE_HI = 0xFF5A4232, BAKELITE_LO = 0xFF22180F;
    public static final int CREAM = 0xFFEFE6CF, CREAM_DK = 0xFFD9CDAE, INK = 0xFF2A2420;
    public static final int CHROME_HI = 0xFFE9EDF1, CHROME = 0xFFB8BEC6, CHROME_LO = 0xFF6E747C;
    public static final int AMBER = 0xFFFFB347, PHOSPHOR = 0xFF7DFF9A, RED = 0xFFD6452F, CYAN = 0xFF54C6C6;

    /** Translates a language key (with optional format arguments) into the player's language. */
    protected static String tr(String key, Object... args) {
        return net.minecraft.client.resources.language.I18n.get(key, args);
    }

    protected final int panelW, panelH;
    protected float scale = 1f;
    protected int left, top;
    protected final List<Control> controls = new ArrayList<>();
    private Control pressed;

    protected RetroScreen(Component title, int panelW, int panelH) {
        super(title);
        this.panelW = panelW;
        this.panelH = panelH;
    }

    @Override
    protected void init() {
        scale = Math.min(1f, Math.min((width - 8f) / panelW, (height - 8f) / panelH));
        left = Math.round((width / scale - panelW) / 2f);
        top = Math.round((height / scale - panelH) / 2f);
        controls.clear();
        layout();
    }

    /** Create controls; coordinates are panel-relative. */
    protected abstract void layout();

    /** Draw panel contents (panel-relative coordinates, pose already scaled and translated). */
    protected abstract void drawPanel(GuiGraphics g, int mx, int my, float partial);

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, 0xA0080604);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g, mouseX, mouseY, partial);
        int mx = toPanelX(mouseX), my = toPanelY(mouseY);
        g.pose().pushPose();
        g.pose().scale(scale, scale, 1f);
        g.pose().translate(left, top, 0);
        drawPanel(g, mx, my, partial);
        for (Control c : controls) if (c.visible) c.render(g, mx, my, c == pressed);
        drawOverlay(g, mx, my, partial);
        if (tooltipLater != null) {
            g.pose().translate(0, 0, 300);
            g.renderTooltip(font(), Component.literal(tooltipLater), mx, my);
            tooltipLater = null;
        }
        g.pose().popPose();
    }

    protected void drawOverlay(GuiGraphics g, int mx, int my, float partial) {
    }

    protected int toPanelX(double mouseX) {
        return (int) Math.floor(mouseX / scale) - left;
    }

    protected int toPanelY(double mouseY) {
        return (int) Math.floor(mouseY / scale) - top;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = toPanelX(mouseX), my = toPanelY(mouseY);
        for (int i = controls.size() - 1; i >= 0; i--) {
            Control c = controls.get(i);
            if (c.visible && c.enabled && c.contains(mx, my)) {
                pressed = c;
                c.click(mx, my, button);
                return true;
            }
        }
        return panelClicked(mx, my, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    protected boolean panelClicked(int mx, int my, int button) {
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        pressed = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (pressed != null && pressed.onDrag != null) {
            pressed.onDrag.drag(dx / scale, dy / scale);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double sx, double sy) {
        int mx = toPanelX(mouseX), my = toPanelY(mouseY);
        for (Control c : controls) {
            if (c.visible && c.enabled && c.onScroll != null && c.contains(mx, my)) {
                c.onScroll.scroll(sy > 0 ? 1 : -1);
                return true;
            }
        }
        return panelScrolled(mx, my, sy) || super.mouseScrolled(mouseX, mouseY, sx, sy);
    }

    protected boolean panelScrolled(int mx, int my, double sy) {
        return false;
    }

    // ------------------------------------------------------------------ controls

    public interface Drag {
        void drag(double dx, double dy);
    }

    public interface Scroll {
        void scroll(int dir);
    }

    public interface Painter {
        void paint(GuiGraphics g, Control c, boolean hover, boolean down);
    }

    /** A rectangular interactive element. */
    public class Control {
        public int x, y, w, h;
        public boolean visible = true, enabled = true;
        public Runnable onClick;
        public Drag onDrag;
        public Scroll onScroll;
        public Painter painter;
        public String tooltip;
        public SoundEvent clickSound;

        public Control(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        boolean contains(int mx, int my) {
            return mx >= x && my >= y && mx < x + w && my < y + h;
        }

        void click(int mx, int my, int button) {
            SoundEvent s = clickSound != null ? clickSound : ModSounds.KNOB.get();
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, 1f, 0.6f));
            if (onClick != null) onClick.run();
        }

        void render(GuiGraphics g, int mx, int my, boolean down) {
            boolean hover = contains(mx, my) && enabled;
            if (painter != null) painter.paint(g, this, hover, down && hover);
            if (hover && tooltip != null) tooltipLater = tooltip;
        }
    }

    protected String tooltipLater;

    protected Control button(int x, int y, int w, int h, String label, Runnable action) {
        Control c = new Control(x, y, w, h);
        c.onClick = action;
        c.painter = (g, ctl, hover, down) -> drawButton(g, ctl.x, ctl.y, ctl.w, ctl.h, label, hover, down, ctl.enabled);
        controls.add(c);
        return c;
    }

    // ------------------------------------------------------------------ drawing helpers

    protected Font font() {
        return Minecraft.getInstance().font;
    }

    public static void bevel(GuiGraphics g, int x, int y, int w, int h, int fill, int hi, int lo) {
        g.fill(x, y, x + w, y + h, fill);
        g.fill(x, y, x + w, y + 1, hi);
        g.fill(x, y, x + 1, y + h, hi);
        g.fill(x, y + h - 1, x + w, y + h, lo);
        g.fill(x + w - 1, y, x + w, y + h, lo);
    }

    /** Bakelite cabinet panel with a chrome rim. */
    protected void cabinet(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 3, y - 3, x + w + 3, y + h + 3, 0xFF15100C);
        bevel(g, x - 2, y - 2, w + 4, h + 4, CHROME, CHROME_HI, CHROME_LO);
        bevel(g, x, y, w, h, BAKELITE, BAKELITE_HI, BAKELITE_LO);
        // subtle walnut grain
        for (int i = 0; i < h; i += 3) {
            int a = 0x0E + ((i * 7) % 5) * 3;
            g.fill(x + 1, y + i, x + w - 1, y + i + 1, (a << 24) | 0x000000);
        }
    }

    /** Cream engraved plate. */
    protected void plate(GuiGraphics g, int x, int y, int w, int h) {
        bevel(g, x, y, w, h, CREAM, 0xFFFFFFFF, CREAM_DK);
    }

    protected void recess(GuiGraphics g, int x, int y, int w, int h, int fill) {
        bevel(g, x, y, w, h, fill, 0xFF000000, 0xFF8A8070);
    }

    protected void drawButton(GuiGraphics g, int x, int y, int w, int h, String label, boolean hover, boolean down, boolean enabled) {
        int face = !enabled ? 0xFF8C8478 : down ? 0xFFCFC3A5 : hover ? 0xFFFFF6DE : CREAM;
        g.fill(x, y + (down ? 1 : 2), x + w, y + h, 0xFF1A120C);
        int oy = down ? 1 : 0;
        bevel(g, x, y + oy, w, h - 2, face, 0xFFFFFFFF, 0xFF9A8E74);
        int tw = font().width(label);
        g.drawString(font(), label, x + (w - tw) / 2, y + oy + (h - 2 - 8) / 2 + 1, enabled ? INK : 0xFF5A5248, false);
    }

    protected void lamp(GuiGraphics g, int x, int y, int color, boolean on) {
        g.fill(x - 1, y - 1, x + 7, y + 7, 0xFF111111);
        int c = on ? color : darken(color, 0.25f);
        g.fill(x, y, x + 6, y + 6, c);
        if (on) g.fill(x + 1, y + 1, x + 3, y + 3, 0xC0FFFFFF);
    }

    protected void label(GuiGraphics g, String s, int x, int y, int color) {
        g.drawString(font(), s, x, y, color, false);
    }

    protected void labelCentered(GuiGraphics g, String s, int cx, int y, int color) {
        g.drawString(font(), s, cx - font().width(s) / 2, y, color, false);
    }

    protected void smallLabel(GuiGraphics g, String s, int cx, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(cx, y, 0);
        g.pose().scale(0.5f, 0.5f, 1f);
        g.drawString(font(), s, -font().width(s) / 2, 0, color, false);
        g.pose().popPose();
    }

    /** Word-wrapped text; returns the height used. */
    protected int wrapped(GuiGraphics g, String text, int x, int y, int w, int color, int maxLines) {
        List<FormattedCharSequence> lines = font().split(Component.literal(text), w);
        int n = 0;
        for (FormattedCharSequence l : lines) {
            if (n >= maxLines) break;
            g.drawString(font(), l, x, y + n * 10, color, false);
            n++;
        }
        return n * 10;
    }

    /** Rotary knob from the atlas, rotated to an angle in degrees (0 = up). */
    protected void knob(GuiGraphics g, int cx, int cy, int size, float degrees) {
        Canvas c = guiCanvas(g);
        c.imageRotated("knob", cx, cy, size, size, (float) Math.toRadians(degrees), 0xFFFFFFFF);
        g.flush();
    }

    /** Needle meter: arc face with a needle at value 0..1. */
    protected void meter(GuiGraphics g, int x, int y, int w, int h, float value, String caption) {
        Canvas c = guiCanvas(g);
        c.image("meter_face", x, y, x + w, y + h, 0xFFFFFFFF);
        c.layer();
        float a = (float) Math.toRadians(-50 + 100 * Math.max(0, Math.min(1, value)));
        float px = x + w / 2f, py = y + h * 0.92f;
        float len = h * 0.78f;
        c.line(px, py, px + (float) Math.sin(a) * len, py - (float) Math.cos(a) * len, 0.9f, 0xFF201010);
        c.layer();
        c.fill(px - 1.5f, py - 1.5f, px + 1.5f, py + 1.5f, 0xFF302820);
        g.flush();
        if (caption != null) smallLabel(g, caption, x + w / 2, y + h - 6, INK);
    }

    protected Canvas guiCanvas(GuiGraphics g) {
        return new Canvas(g.pose(), g.bufferSource(), 0xF000F0, 0.5f, 100000, 100000);
    }

    public static int darken(int argb, float k) {
        int r = (int) (((argb >> 16) & 255) * k), gg = (int) (((argb >> 8) & 255) * k), b = (int) ((argb & 255) * k);
        return (argb & 0xFF000000) | (r << 16) | (gg << 8) | b;
    }

    protected void playUi(SoundEvent s, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, pitch, 0.7f));
    }

    protected static boolean hasAtlas() {
        return Atlas.has("knob");
    }

    protected void resetShader() {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }
}
