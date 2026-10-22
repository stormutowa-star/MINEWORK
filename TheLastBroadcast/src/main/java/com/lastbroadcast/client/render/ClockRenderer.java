package com.lastbroadcast.client.render;

import com.lastbroadcast.block.DecorBlock;
import com.lastbroadcast.blockentity.ClockBlockEntity;
import com.lastbroadcast.story.CaseState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Clock hands: follow the time of day, or stall at 9:14 while the echo leaks. */
public class ClockRenderer implements BlockEntityRenderer<ClockBlockEntity> {
    public ClockRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(ClockBlockEntity clock, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Level level = clock.getLevel();
        if (level == null) return;
        Direction facing = clock.getBlockState().hasProperty(DecorBlock.FACING) ? clock.getBlockState().getValue(DecorBlock.FACING) : Direction.NORTH;
        long gt = level.getGameTime();
        double hours, minutes, seconds;
        boolean stalled = gt < CaseState.CLIENT.clockStallUntil;
        if (stalled) {
            hours = 9 + 14 / 60.0;
            minutes = 14;
            seconds = 0 + ((gt / 7) % 2) * 1.0;
        } else {
            double day = (level.getDayTime() + partialTick) % 24000;
            double h24 = (day / 1000.0 + 6.0) % 24.0;
            hours = h24 % 12.0;
            minutes = (h24 * 60.0) % 60.0;
            seconds = (h24 * 3600.0) % 60.0;
        }
        pose.pushPose();
        // canvas origin at the dial centre (model 8,8), units = model pixels, just in front of the face (z=15)
        ScreenPose.apply(pose, facing, 8f, 8f, 14.85f, 1f);
        Canvas c = new Canvas(pose, buffers, light, 0.02f, 100, 100);
        c.clip(-10, -10, 10, 10);
        hand(c, hours / 12.0, 2.4f, 0.55f, 0xFF1A1A1A);
        c.layer();
        hand(c, minutes / 60.0, 3.6f, 0.4f, 0xFF1A1A1A);
        c.layer();
        hand(c, seconds / 60.0, 3.8f, 0.18f, 0xFFC03020);
        c.layer();
        c.fill(-0.4f, -0.4f, 0.4f, 0.4f, 0xFFB08A3A);
        pose.popPose();
    }

    private static void hand(Canvas c, double turns, float len, float width, int color) {
        double a = turns * Math.PI * 2;
        float x = (float) (Math.sin(a) * len), y = (float) (-Math.cos(a) * len);
        c.line((float) (-Math.sin(a) * 0.6), (float) (Math.cos(a) * 0.6), x, y, width, color);
    }
}
