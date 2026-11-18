package com.dragonlords.client;

import com.dragonlords.DragonLords;
import com.dragonlords.DragonType;
import com.dragonlords.entity.DragonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class DragonRenderer extends MobRenderer<DragonEntity, DragonModel> {
    private final ResourceLocation texture;

    public DragonRenderer(EntityRendererProvider.Context ctx, DragonType type) {
        super(ctx, new DragonModel(ctx.bakeLayer(DragonModel.LAYER)), 1.2f * type.scale);
        this.texture = ResourceLocation.fromNamespaceAndPath(DragonLords.MOD_ID, "textures/entity/" + type.entityName() + ".png");
    }

    @Override public ResourceLocation getTextureLocation(DragonEntity e) { return texture; }

    @Override protected void scale(DragonEntity e, PoseStack ps, float pt) {
        float s = e.dragonType.scale;
        ps.scale(s, s, s);
    }

    /** Banking (roll into turns) and body pitch (dives / climbs), pivoting around the body centre. */
    @Override
    protected void setupRotations(DragonEntity e, PoseStack ps, float bob, float yBodyRot, float pt, float scale) {
        super.setupRotations(e, ps, bob, yBodyRot, pt, scale);
        float pivot = e.dragonType.scale * 1.0f;
        ps.translate(0, pivot, 0);
        ps.mulPose(Axis.ZP.rotationDegrees(-e.bankAt(pt)));
        ps.mulPose(Axis.XP.rotationDegrees(e.pitchAt(pt)));
        ps.translate(0, -pivot, 0);
    }
}
