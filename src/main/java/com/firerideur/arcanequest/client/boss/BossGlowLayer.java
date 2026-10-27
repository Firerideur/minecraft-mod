package com.firerideur.arcanequest.client.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.ToIntFunction;

/** Full-bright additive pass over the boss model, like spider eyes: glowing eyes, cores and runes. */
public class BossGlowLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final int FULL_BRIGHT = 0xF000F0;

    private final RenderType renderType;
    private final ToIntFunction<T> color;

    public BossGlowLayer(RenderLayerParent<T, M> parent, ResourceLocation texture, ToIntFunction<T> color) {
        super(parent);
        this.renderType = RenderType.eyes(texture);
        this.color = color;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, T entity, float limbSwing,
                       float limbSwingAmount, float partialTick, float age, float headYaw, float headPitch) {
        getParentModel().renderToBuffer(poseStack, buffers.getBuffer(renderType), FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                color.applyAsInt(entity));
    }
}
