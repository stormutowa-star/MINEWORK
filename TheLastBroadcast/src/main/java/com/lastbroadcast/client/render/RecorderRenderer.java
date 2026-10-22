package com.lastbroadcast.client.render;

import com.lastbroadcast.block.DecorBlock;
import com.lastbroadcast.blockentity.RecorderBlockEntity;
import com.lastbroadcast.client.screen.RecorderScreen;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/** Spinning tape reels on the deck's faceplate (when a reel is threaded). */
public class RecorderRenderer implements BlockEntityRenderer<RecorderBlockEntity> {
    public RecorderRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(RecorderBlockEntity deck, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (deck.getTape().isEmpty() || deck.getLevel() == null) return;
        Direction facing = deck.getBlockState().hasProperty(DecorBlock.FACING) ? deck.getBlockState().getValue(DecorBlock.FACING) : Direction.NORTH;
        double time = deck.getLevel().getGameTime() + partialTick;
        float speed = deck.isRecording() ? 1f : RecorderScreen.reelSpeedFor(deck.getBlockPos());
        float angle = (float) (time * 0.25 * speed);
        pose.pushPose();
        // canvas in model pixels, origin at top-left (x=15, y=15) of the faceplate at z=4
        ScreenPose.apply(pose, facing, 15f, 15f, 3.9f, 1f);
        Canvas c = new Canvas(pose, buffers, light, 0.01f, 14, 14);
        c.imageRotated("reel_full", 3.6f, 4.2f, 6.2f, 6.2f, angle, 0xFFFFFFFF);
        c.imageRotated("reel_empty", 10.4f, 4.2f, 6.2f, 6.2f, angle * 1.3f, 0xFFFFFFFF);
        if (deck.isRecording()) {
            c.layer();
            boolean blink = ((int) (time / 6)) % 2 == 0;
            c.fill(12.2f, 9.6f, 13.2f, 10.6f, blink ? 0xFFFF3020 : 0xFF501010);
        }
        pose.popPose();
    }
}
