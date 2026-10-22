package com.lastbroadcast.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

/**
 * 2D drawing surface in "canvas units" (y down) on top of a PoseStack. Used both in GUIs and on
 * in-world screens. Every quad goes through one atlas render type, in order, with a small z
 * step per layer so later layers always win the depth test. Quads are clipped in software to
 * the clip rectangle (no scissor exists for in-world surfaces).
 */
public final class Canvas {
    public final PoseStack pose;
    public final MultiBufferSource buffers;
    public final int light;
    private final float zStep;
    private float z = 0f;
    private float cx0, cy0, cx1, cy1;
    /** Colour multiplier applied to every quad (picture brightness / tint). */
    public float tr = 1f, tg = 1f, tb = 1f, ta = 1f;

    public Canvas(PoseStack pose, MultiBufferSource buffers, int light, float zStep, float w, float h) {
        this.pose = pose;
        this.buffers = buffers;
        this.light = light;
        this.zStep = zStep;
        clip(0, 0, w, h);
    }

    public void clip(float x0, float y0, float x1, float y1) {
        cx0 = x0;
        cy0 = y0;
        cx1 = x1;
        cy1 = y1;
    }

    public void layer() {
        z += zStep;
    }

    public float z() {
        return z;
    }

    public void resetTint() {
        tr = tg = tb = ta = 1f;
    }

    private VertexConsumer vc() {
        return buffers.getBuffer(RenderType.text(Atlas.TEXTURE));
    }

    public void image(String region, float x0, float y0, float x1, float y1, int argb) {
        Atlas.Region r = Atlas.get(region);
        quad(x0, y0, x1, y1, r.u0(), r.v0(), r.u1(), r.v1(), argb);
    }

    /** Draws a sub-rectangle (in region pixels) of an atlas region. */
    public void imagePart(String region, float sx, float sy, float sw, float sh, float x0, float y0, float x1, float y1, int argb) {
        Atlas.Region r = Atlas.get(region);
        float u0 = (r.x() + sx) / Atlas.SIZE, v0 = (r.y() + sy) / Atlas.SIZE;
        float u1 = (r.x() + sx + sw) / Atlas.SIZE, v1 = (r.y() + sy + sh) / Atlas.SIZE;
        quad(x0, y0, x1, y1, u0, v0, u1, v1, argb);
    }

    public void fill(float x0, float y0, float x1, float y1, int argb) {
        Atlas.Region r = Atlas.get("white");
        float u = (r.x() + 1.5f) / Atlas.SIZE, v = (r.y() + 1.5f) / Atlas.SIZE;
        quad(x0, y0, x1, y1, u, v, u, v, argb);
    }

    public void quad(float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, int argb) {
        if (x1 <= cx0 || x0 >= cx1 || y1 <= cy0 || y0 >= cy1 || x1 <= x0 || y1 <= y0) return;
        float w = x1 - x0, h = y1 - y0;
        if (x0 < cx0) { u0 += (u1 - u0) * (cx0 - x0) / w; x0 = cx0; }
        if (x1 > cx1) { u1 -= (u1 - u0) * (x1 - cx1) / (x1 - x0); x1 = cx1; }
        if (y0 < cy0) { v0 += (v1 - v0) * (cy0 - y0) / h; y0 = cy0; }
        if (y1 > cy1) { v1 -= (v1 - v0) * (y1 - cy1) / (y1 - y0); y1 = cy1; }
        int a = (int) (((argb >>> 24) & 255) * ta);
        int r = (int) (((argb >> 16) & 255) * tr);
        int g = (int) (((argb >> 8) & 255) * tg);
        int b = (int) ((argb & 255) * tb);
        if (a <= 2) return;
        int c = (a << 24) | (r << 16) | (g << 8) | b;
        Matrix4f m = pose.last().pose();
        VertexConsumer v = vc();
        v.addVertex(m, x0, y0, z).setColor(c).setUv(u0, v0).setLight(light);
        v.addVertex(m, x0, y1, z).setColor(c).setUv(u0, v1).setLight(light);
        v.addVertex(m, x1, y1, z).setColor(c).setUv(u1, v1).setLight(light);
        v.addVertex(m, x1, y0, z).setColor(c).setUv(u1, v0).setLight(light);
    }

    /** Rotated textured quad around (cx, cy). */
    public void imageRotated(String region, float cx, float cy, float w, float h, float radians, int argb) {
        Atlas.Region r = Atlas.get(region);
        float cos = (float) Math.cos(radians), sin = (float) Math.sin(radians);
        float hw = w / 2, hh = h / 2;
        float[][] pts = {{-hw, -hh}, {-hw, hh}, {hw, hh}, {hw, -hh}};
        float[][] uv = {{r.u0(), r.v0()}, {r.u0(), r.v1()}, {r.u1(), r.v1()}, {r.u1(), r.v0()}};
        int a = (int) (((argb >>> 24) & 255) * ta);
        int c = (a << 24) | ((int) (((argb >> 16) & 255) * tr) << 16) | ((int) (((argb >> 8) & 255) * tg) << 8) | (int) ((argb & 255) * tb);
        Matrix4f m = pose.last().pose();
        VertexConsumer v = vc();
        for (int i = 0; i < 4; i++) {
            float x = cx + pts[i][0] * cos - pts[i][1] * sin;
            float y = cy + pts[i][0] * sin + pts[i][1] * cos;
            v.addVertex(m, x, y, z).setColor(c).setUv(uv[i][0], uv[i][1]).setLight(light);
        }
    }

    /** A thin line from (x0,y0) to (x1,y1). */
    public void line(float x0, float y0, float x1, float y1, float width, int argb) {
        float dx = x1 - x0, dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-4f) return;
        float nx = -dy / len * width / 2, ny = dx / len * width / 2;
        Atlas.Region r = Atlas.get("white");
        float u = (r.x() + 1.5f) / Atlas.SIZE, vv = (r.y() + 1.5f) / Atlas.SIZE;
        int a = (int) (((argb >>> 24) & 255) * ta);
        int c = (a << 24) | ((int) (((argb >> 16) & 255) * tr) << 16) | ((int) (((argb >> 8) & 255) * tg) << 8) | (int) ((argb & 255) * tb);
        Matrix4f m = pose.last().pose();
        VertexConsumer v = vc();
        v.addVertex(m, x0 + nx, y0 + ny, z).setColor(c).setUv(u, vv).setLight(light);
        v.addVertex(m, x1 + nx, y1 + ny, z).setColor(c).setUv(u, vv).setLight(light);
        v.addVertex(m, x1 - nx, y1 - ny, z).setColor(c).setUv(u, vv).setLight(light);
        v.addVertex(m, x0 - nx, y0 - ny, z).setColor(c).setUv(u, vv).setLight(light);
    }

    /** Text in canvas units. Drawn on its own layer above everything drawn so far. */
    public void text(String s, float x, float y, int argb, float scale, boolean center, boolean shadow) {
        if (s == null || s.isEmpty()) return;
        Font font = Minecraft.getInstance().font;
        float w = font.width(s) * scale;
        float left = center ? x - w / 2f : x;
        if (left < cx0 - 0.5f || left + w > cx1 + 0.5f || y < cy0 - 0.5f || y + 8 * scale > cy1 + 0.5f) return;
        int a = (int) (((argb >>> 24) & 255) * ta);
        if (a < 8) return;
        int c = (a << 24) | ((int) (((argb >> 16) & 255) * tr) << 16) | ((int) (((argb >> 8) & 255) * tg) << 8) | (int) ((argb & 255) * tb);
        layer();
        pose.pushPose();
        pose.translate(left, y, z);
        pose.scale(scale, scale, 1f);
        font.drawInBatch(s, 0, 0, c, shadow, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, light);
        pose.popPose();
        layer();
    }
}
