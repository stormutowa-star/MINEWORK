package com.lastbroadcast.client.render;

import com.lastbroadcast.LastBroadcast;
import com.lastbroadcast.entity.StaffNpc;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class StaffRenderer extends HumanoidMobRenderer<StaffNpc, StaffModel> {
    private static final Map<String, ResourceLocation> SKINS = new HashMap<>();

    public StaffRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new StaffModel(ctx.bakeLayer(StaffModel.LAYER)), 0.45f);
    }

    @Override
    public ResourceLocation getTextureLocation(StaffNpc npc) {
        return SKINS.computeIfAbsent(npc.getCharacter(), c -> LastBroadcast.id("textures/entity/staff/" + c + ".png"));
    }

    @Override
    public void render(StaffNpc npc, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        // Seated characters stand on the seat; drop the body so the hips rest on the cushion.
        if (npc.isSitting()) pose.translate(0, -0.62, 0);
        super.render(npc, yaw, partialTick, pose, buffers, light);
        pose.popPose();
    }
}
