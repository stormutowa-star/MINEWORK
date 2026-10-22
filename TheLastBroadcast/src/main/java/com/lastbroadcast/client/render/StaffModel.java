package com.lastbroadcast.client.render;

import com.lastbroadcast.LastBroadcast;
import com.lastbroadcast.client.ClientDialogueState;
import com.lastbroadcast.entity.StaffNpc;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Player-proportioned model with a hinged mouth and eyelids for lip-flap and blinking, plus
 * poses for sitting, operating equipment and gesturing in conversation.
 */
public class StaffModel extends PlayerModel<StaffNpc> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(LastBroadcast.id("staff"), "main");
    private final ModelPart mouth;
    private final ModelPart eyelids;

    public StaffModel(ModelPart root) {
        super(root, false);
        this.mouth = head.getChild("mouth");
        this.eyelids = head.getChild("eyelids");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
        PartDefinition head = mesh.getRoot().getChild("head");
        // Mouth interior (dark) sits just in front of the face; shown while speaking.
        head.addOrReplaceChild("mouth", CubeListBuilder.create().texOffs(0, 0).addBox(-2f, -3f, -4.05f, 4, 1, 0), PartPose.ZERO);
        // Eyelids in skin tone cover both eyes during a blink.
        head.addOrReplaceChild("eyelids", CubeListBuilder.create().texOffs(24, 0).addBox(-3f, -5f, -4.06f, 6, 1, 0), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(StaffNpc npc, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch) {
        this.riding = npc.isSitting();
        super.setupAnim(npc, limbSwing, limbSwingAmount, age, headYaw, headPitch);
        boolean inMyDialogue = ClientDialogueState.entityId == npc.getId();
        boolean talking = npc.isTalking() || inMyDialogue;
        float phase = npc.getId() * 1.7f;

        if (npc.isSitting()) {
            // seated: slight forward lean, hands resting toward the desk
            body.xRot = 0.08f;
            rightArm.xRot = -0.55f + Mth.sin(age * 0.05f + phase) * 0.04f;
            leftArm.xRot = -0.5f + Mth.sin(age * 0.045f + phase) * 0.04f;
            rightArm.zRot = 0.08f;
            leftArm.zRot = -0.08f;
        }
        if (npc.isWorking() && !talking) {
            // operating equipment: both hands forward, alternating small adjustments
            rightArm.xRot = -1.05f + Mth.sin(age * 0.35f + phase) * 0.12f;
            leftArm.xRot = -0.95f + Mth.cos(age * 0.31f + phase) * 0.10f;
            rightArm.zRot = 0.12f;
            leftArm.zRot = -0.12f;
            head.xRot = 0.35f;
        }
        if (talking) {
            String pose = inMyDialogue ? ClientDialogueState.pose : "gesture";
            float g = Mth.sin(age * 0.13f + phase);
            switch (pose) {
                case "nervous" -> {
                    rightArm.xRot = -0.35f + Mth.sin(age * 0.5f) * 0.05f;
                    leftArm.xRot = -0.35f + Mth.cos(age * 0.5f) * 0.05f;
                    rightArm.zRot = 0.35f;
                    leftArm.zRot = -0.35f;
                    head.xRot = 0.25f;
                }
                case "point" -> {
                    rightArm.xRot = -1.45f;
                    rightArm.yRot = -0.15f;
                    leftArm.xRot = 0.05f;
                }
                case "idle" -> {
                }
                default -> {
                    rightArm.xRot = -0.55f + g * 0.3f;
                    rightArm.zRot = -0.08f + g * 0.06f;
                    leftArm.xRot = npc.isSitting() ? -0.5f : -0.15f + Mth.sin(age * 0.09f + 1) * 0.1f;
                }
            }
        }
        boolean speaking = inMyDialogue && ClientDialogueState.speaking;
        mouth.visible = speaking && Mth.sin(age * 1.9f + phase) > -0.2f;
        eyelids.visible = ((int) (age + npc.getId() * 17)) % 83 < 3;

        leftSleeve.copyFrom(leftArm);
        rightSleeve.copyFrom(rightArm);
        leftPants.copyFrom(leftLeg);
        rightPants.copyFrom(rightLeg);
        jacket.copyFrom(body);
        hat.copyFrom(head);
    }
}
