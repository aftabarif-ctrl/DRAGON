package com.dragonlords.client;

import com.dragonlords.DragonLords;
import com.dragonlords.entity.DragonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Fully procedural dragon rig (256x256 texture). All motion is computed in setupAnim from smoothed entity state. */
public class DragonModel extends EntityModel<DragonEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(DragonLords.MOD_ID, "dragon"), "main");

    private final ModelPart root, body, neck1, neck2, head, jaw, tail1, tail2, tail3;
    private final ModelPart lWing, lTip, rWing, rTip, flLeg, frLeg, blLeg, brLeg, saddle;

    public DragonModel(ModelPart root) {
        this.root = root;
        body = root.getChild("body");
        neck1 = body.getChild("neck1");
        neck2 = neck1.getChild("neck2");
        head = neck2.getChild("head");
        jaw = head.getChild("jaw");
        tail1 = body.getChild("tail1");
        tail2 = tail1.getChild("tail2");
        tail3 = tail2.getChild("tail3");
        lWing = body.getChild("lWing"); lTip = lWing.getChild("lTip");
        rWing = body.getChild("rWing"); rTip = rWing.getChild("rTip");
        flLeg = body.getChild("flLeg"); frLeg = body.getChild("frLeg");
        blLeg = body.getChild("blLeg"); brLeg = body.getChild("brLeg");
        saddle = body.getChild("saddle");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();

        PartDefinition body = r.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-7, -6, -14, 14, 12, 28)
                .texOffs(120, 64).addBox(-1, -8, -9, 2, 2, 3)
                .texOffs(120, 64).addBox(-1, -8, -1, 2, 2, 3)
                .texOffs(120, 64).addBox(-1, -8, 7, 2, 2, 3),
                PartPose.offset(0, 8, 0));

        body.addOrReplaceChild("saddle", CubeListBuilder.create().texOffs(100, 120).addBox(-5, -7.5f, -6, 10, 2, 12), PartPose.ZERO);

        PartDefinition n1 = body.addOrReplaceChild("neck1", CubeListBuilder.create()
                .texOffs(0, 44).addBox(-4, -4, -10, 8, 8, 10)
                .texOffs(120, 64).addBox(-1, -6, -8, 2, 2, 3), PartPose.offset(0, -1, -13));
        PartDefinition n2 = n1.addOrReplaceChild("neck2", CubeListBuilder.create()
                .texOffs(40, 44).addBox(-3, -3, -9, 6, 6, 9)
                .texOffs(120, 64).addBox(-1, -5, -7, 2, 2, 3), PartPose.offset(0, 0, -10));
        PartDefinition hd = n2.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(80, 44).addBox(-4, -3, -8, 8, 6, 8)
                .texOffs(120, 44).addBox(-3, -1, -14, 6, 3, 6), PartPose.offset(0, 0, -9));
        hd.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(150, 44).addBox(-3, 0, -11, 6, 2, 11), PartPose.offset(0, 2.5f, -3));
        hd.addOrReplaceChild("hornL", CubeListBuilder.create().texOffs(190, 44).addBox(-1, -1, 0, 2, 2, 6), PartPose.offsetAndRotation(3, -2.5f, -1, 0.6f, 0.25f, 0));
        hd.addOrReplaceChild("hornR", CubeListBuilder.create().mirror().texOffs(190, 44).addBox(-1, -1, 0, 2, 2, 6), PartPose.offsetAndRotation(-3, -2.5f, -1, 0.6f, -0.25f, 0));

        PartDefinition t1 = body.addOrReplaceChild("tail1", CubeListBuilder.create()
                .texOffs(0, 64).addBox(-4, -4, 0, 8, 8, 12)
                .texOffs(120, 64).addBox(-1, -6, 3, 2, 2, 3), PartPose.offset(0, -1, 13));
        PartDefinition t2 = t1.addOrReplaceChild("tail2", CubeListBuilder.create()
                .texOffs(44, 64).addBox(-3, -3, 0, 6, 6, 12)
                .texOffs(120, 64).addBox(-1, -5, 3, 2, 2, 3), PartPose.offset(0, 0, 12));
        t2.addOrReplaceChild("tail3", CubeListBuilder.create().texOffs(84, 64).addBox(-2, -2, 0, 4, 4, 12), PartPose.offset(0, 0, 12));

        PartDefinition lw = body.addOrReplaceChild("lWing", CubeListBuilder.create()
                .texOffs(0, 88).addBox(0, -1.5f, -1.5f, 18, 3, 3)
                .texOffs(0, 96).addBox(0, -0.5f, 1.5f, 18, 1, 18), PartPose.offset(6, -5, -8));
        lw.addOrReplaceChild("lTip", CubeListBuilder.create()
                .texOffs(80, 88).addBox(0, -1, -1, 22, 2, 2)
                .texOffs(80, 96).addBox(0, -0.5f, 1, 22, 1, 20), PartPose.offset(18, 0, 0));
        PartDefinition rw = body.addOrReplaceChild("rWing", CubeListBuilder.create().mirror()
                .texOffs(0, 88).addBox(-18, -1.5f, -1.5f, 18, 3, 3)
                .texOffs(0, 96).addBox(-18, -0.5f, 1.5f, 18, 1, 18), PartPose.offset(-6, -5, -8));
        rw.addOrReplaceChild("rTip", CubeListBuilder.create().mirror()
                .texOffs(80, 88).addBox(-22, -1, -1, 22, 2, 2)
                .texOffs(80, 96).addBox(-22, -0.5f, 1, 22, 1, 20), PartPose.offset(-18, 0, 0));

        body.addOrReplaceChild("flLeg", legs(0, 120, -2, 12, 4), PartPose.offset(6, 4, -9));
        body.addOrReplaceChild("frLeg", legs(0, 120, -2, 12, 4), PartPose.offset(-6, 4, -9));
        body.addOrReplaceChild("blLeg", legs(0, 140, -2.5f, 13, 5), PartPose.offset(6, 3, 10));
        body.addOrReplaceChild("brLeg", legs(0, 140, -2.5f, 13, 5), PartPose.offset(-6, 3, 10));

        return LayerDefinition.create(mesh, 256, 256);
    }

    private static CubeListBuilder legs(int u, int v, float off, int len, int w) {
        return CubeListBuilder.create().texOffs(u, v).addBox(off, 0, off, w, len, w)
                .texOffs(36, v).addBox(-2.5f, len - 2, -5, 5, 2, 7);
    }

    @Override
    public void setupAnim(DragonEntity e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        float pt = age - e.tickCount;
        float phase = e.flapPhaseAt(pt), amp = e.flapAmpAt(pt), fly = e.flyAnimAt(pt);
        float bank = e.bankAt(pt), jawV = e.jawAt(pt), fold = 1f - fly;

        // asymmetric wave: sharp upstroke peak, long powerful downstroke
        float w = Mth.cos(phase) + 0.25f * Mth.cos(2f * phase);
        float lag = Mth.cos(phase - 1.1f) + 0.2f * Mth.cos(2f * (phase - 1.1f));

        saddle.visible = e.isSaddled();
        body.y = 8f + fly * 1.4f * amp * w;
        body.xRot = fly * 0.04f * amp * w;

        // ---- wings
        float lift = 0.22f + amp * 0.62f * w;
        float bankRad = bank * 0.005f;
        float tipLift = 0.10f + amp * 0.55f * lag;
        float scoop = fly * (-0.10f + 0.25f * amp * Mth.sin(phase));
        float sweep = fly * 0.15f * amp * Mth.sin(phase + 0.6f);

        lWing.zRot = -fly * (lift + bankRad) + fold * 0.25f;
        lWing.yRot = fold * -1.3f + sweep;
        lWing.xRot = scoop;
        lTip.zRot = -fly * (tipLift + bankRad * 0.5f);
        lTip.yRot = fold * -2.9f;
        lTip.xRot = scoop * 0.6f;

        rWing.zRot = fly * (lift - bankRad) - fold * 0.25f;
        rWing.yRot = fold * 1.3f - sweep;
        rWing.xRot = scoop;
        rTip.zRot = fly * (tipLift - bankRad * 0.5f);
        rTip.yRot = fold * 2.9f;
        rTip.xRot = scoop * 0.6f;

        // ---- neck & head
        float pitch = headPitch * Mth.DEG_TO_RAD * 0.4f;
        float yaw = netHeadYaw * Mth.DEG_TO_RAD;
        float idle = Mth.sin(age * 0.08f) * 0.03f;
        neck1.xRot = -0.6f * fold - 0.08f * fly + pitch * 0.3f + idle;
        neck2.xRot = 0.35f * fold + 0.04f * fly * w * amp + pitch * 0.3f + idle;
        head.xRot = 0.25f * fold + pitch * 0.4f - jawV * 0.25f + 0.05f * fly * amp * lag;
        neck1.yRot = yaw * 0.25f; neck2.yRot = yaw * 0.3f; head.yRot = yaw * 0.45f;
        jaw.xRot = jawV + 0.04f + 0.04f * Mth.sin(age * 0.1f);

        // ---- tail (trails and whips; leans against the turn)
        float t = fly > 0.5f ? phase * 0.5f : age * 0.07f + limbSwing * 0.3f;
        float sw = 0.10f + 0.12f * fly * amp + 0.2f * limbSwingAmount;
        tail1.yRot = Mth.sin(t) * sw - bank * 0.006f;
        tail2.yRot = Mth.sin(t - 0.9f) * sw * 1.2f - bank * 0.008f;
        tail3.yRot = Mth.sin(t - 1.8f) * sw * 1.5f - bank * 0.010f;
        tail1.xRot = 0.05f + fly * 0.06f * w * amp;
        tail2.xRot = fly * 0.07f * Mth.cos(phase - 0.9f) * amp;
        tail3.xRot = fly * 0.09f * Mth.cos(phase - 1.8f) * amp;

        // ---- legs: walk cycle on ground, tucked in flight
        float sp = limbSwing * 0.55f, am = limbSwingAmount;
        flLeg.xRot = Mth.cos(sp) * am * fold + 1.1f * fly;
        frLeg.xRot = Mth.cos(sp + Mth.PI) * am * fold + 1.1f * fly;
        blLeg.xRot = Mth.cos(sp + Mth.PI) * am * fold + 0.9f * fly;
        brLeg.xRot = Mth.cos(sp) * am * fold + 0.9f * fly;
    }

    @Override
    public void renderToBuffer(PoseStack ps, VertexConsumer buf, int light, int overlay, int color) {
        root.render(ps, buf, light, overlay, color);
    }
}
