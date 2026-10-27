package com.firerideur.arcanequest.client.dungeon;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.dungeon.GateEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.joml.Matrix4f;

/** A glowing vortex always facing the camera, in the color of the gate's rank, with two layers turning opposite ways. */
public class GateRenderer extends EntityRenderer<GateEntity> {
    private static final ResourceLocation TEXTURE = ArcaneQuest.id("textures/entity/gate.png");
    private static final float WIDTH = 3.6F;
    private static final float HEIGHT = 4.6F;

    public GateRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GateEntity gate, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        int color = gate.isExit() ? 0x80E0FF : GateEntity.color(gate.getStars());
        int r = FastColor.ARGB32.red(color), g = FastColor.ARGB32.green(color), b = FastColor.ARGB32.blue(color);
        float time = gate.tickCount + partialTick;
        poseStack.pushPose();
        poseStack.translate(0, HEIGHT / 2, 0);
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        VertexConsumer glow = buffers.getBuffer(RenderType.eyes(TEXTURE));
        // Pulsing size, two layers spinning in opposite directions
        float pulse = 1.0F + 0.04F * (float) Math.sin(time * 0.15F);
        for (int layer = 0; layer < 2; layer++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotationDegrees((layer == 0 ? 1 : -1.6F) * time * 2.0F));
            float s = pulse * (layer == 0 ? 1.0F : 0.75F);
            quad(poseStack.last().pose(), glow, WIDTH * s, HEIGHT * s / 1.3F, r, g, b, layer == 0 ? 255 : 200);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(gate, yaw, partialTick, poseStack, buffers, light);
    }

    private static void quad(Matrix4f pose, VertexConsumer consumer, float w, float h, int r, int g, int b, int a) {
        float x = w / 2, y = h / 2;
        vertex(pose, consumer, -x, -y, 0, 1, r, g, b, a);
        vertex(pose, consumer, x, -y, 1, 1, r, g, b, a);
        vertex(pose, consumer, x, y, 1, 0, r, g, b, a);
        vertex(pose, consumer, -x, y, 0, 0, r, g, b, a);
    }

    private static void vertex(Matrix4f pose, VertexConsumer consumer, float x, float y, float u, float v, int r, int g, int b, int a) {
        consumer.addVertex(pose, x, y, 0).setColor(r, g, b, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0).setNormal(0, 0, 1);
    }

    @Override
    public ResourceLocation getTextureLocation(GateEntity entity) {
        return TEXTURE;
    }
}
