package com.lastbroadcast.client.render;

import com.lastbroadcast.block.DecorBlock;
import com.lastbroadcast.blockentity.TelevisionBlockEntity;
import com.lastbroadcast.broadcast.Channels;
import com.lastbroadcast.content.ContentLibrary;
import com.lastbroadcast.story.CaseState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Draws the live picture on a console television's tube face. */
public class TelevisionRenderer implements BlockEntityRenderer<TelevisionBlockEntity> {
    // Tube face in model pixels (north-facing model): x 4..14, y 5.5..13, glass front at z 2.5
    private static final float LEFT_X = 14f, TOP_Y = 13f, Z = 2.45f, WIDTH_PX = 10f;
    private static final int FULLBRIGHT = 0xF000F0;

    public TelevisionRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(TelevisionBlockEntity tv, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Level level = tv.getLevel();
        if (level == null) return;
        long gt = level.getGameTime();
        double time = gt + partialTick;
        boolean surging = tv.isSurging(gt);
        double sinceChange = time - tv.clientPowerChange;
        boolean powered = tv.isPowered() || surging;
        if (!powered && sinceChange > 8) return;
        Direction facing = tv.getBlockState().hasProperty(DecorBlock.FACING) ? tv.getBlockState().getValue(DecorBlock.FACING) : Direction.NORTH;

        pose.pushPose();
        ScreenPose.apply(pose, facing, LEFT_X, TOP_Y, Z, BroadcastRenderer.W / WIDTH_PX);
        Canvas c = new Canvas(pose, buffers, FULLBRIGHT, 0.12f, BroadcastRenderer.W, BroadcastRenderer.H);

        if (!powered) {
            // switch-off: picture collapses to a bright line, then a dot
            float k = (float) Math.min(1, sinceChange / 8.0);
            float h = Math.max(0.8f, BroadcastRenderer.H * (1 - k) * 0.12f);
            float w = BroadcastRenderer.W * (k < 0.6f ? 1f : (1f - (k - 0.6f) / 0.4f) + 0.02f);
            c.fill(80 - w / 2, 60 - h / 2, 80 + w / 2, 60 + h / 2, ((int) ((1 - k * 0.6f) * 255) << 24) | 0xE8F4FF);
            pose.popPose();
            return;
        }

        BroadcastRenderer.Signal sig = BroadcastRenderer.Signal.clean();
        if (surging) {
            BroadcastRenderer.draw(c, ContentLibrary.broadcast("surge_frame"), (time % 60) / 20.0, sig);
        } else {
            Channels.OnAir onAir = Channels.onAir(CaseState.CLIENT, tv.getChannel(), time);
            if (onAir.programme() == null) BroadcastRenderer.drawStatic(c, time / 20.0, 0.9f);
            else BroadcastRenderer.draw(c, onAir.programme(), onAir.time(), sig);
        }
        // CRT warm-up: picture grows out of a horizontal line
        if (tv.isPowered() && sinceChange >= 0 && sinceChange < 14) {
            float k = (float) (sinceChange / 14.0);
            float open = BroadcastRenderer.H * k * k / 2f;
            c.layer();
            c.fill(0, 0, BroadcastRenderer.W, 60 - open, 0xFF050505);
            c.fill(0, 60 + open, BroadcastRenderer.W, BroadcastRenderer.H, 0xFF050505);
            c.fill(0, 59.5f - open, BroadcastRenderer.W, 60.5f + open, ((int) ((1 - k) * 200) << 24) | 0xFFFFFF);
        }
        pose.popPose();
    }

    @Override
    public int getViewDistance() {
        return 48;
    }
}
