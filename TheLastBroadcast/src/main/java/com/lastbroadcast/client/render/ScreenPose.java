package com.lastbroadcast.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;

/** Places a canvas on a block face: model pixels of the NORTH-facing model, rotated to facing. */
public final class ScreenPose {
    private ScreenPose() {}

    /**
     * After this call, canvas unit (0,0) is the top-left of a rectangle whose top-left corner (as
     * seen by a viewer facing the block's front) is at model pixel (leftX, topY, z), and
     * {@code unitsPerPixel} canvas units span one model pixel.
     */
    public static void apply(PoseStack pose, Direction facing, float leftX, float topY, float z, float canvasUnitsPerPixel) {
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180f - facing.toYRot()));
        pose.translate(-0.5, -0.5, -0.5);
        pose.translate(leftX / 16f, topY / 16f, z / 16f);
        float s = 1f / 16f / canvasUnitsPerPixel;
        pose.scale(-s, -s, -s);
    }
}
