package com.firerideur.arcanequest.client;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.client.boss.BossGlowLayer;
import com.firerideur.arcanequest.shadow.ShadowSoldier;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Shadow soldiers: a smoky humanoid silhouette with glowing blue eyes. */
public class ShadowSoldierRenderer extends HumanoidMobRenderer<ShadowSoldier, HumanoidModel<ShadowSoldier>> {
    private static final ResourceLocation TEXTURE = ArcaneQuest.id("textures/entity/shadow_soldier.png");
    private static final ResourceLocation GLOW = ArcaneQuest.id("textures/entity/shadow_soldier_glow.png");

    public ShadowSoldierRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
        addLayer(new BossGlowLayer<>(this, GLOW, shadow -> 0xFFFFFFFF));
    }

    @Override
    public ResourceLocation getTextureLocation(ShadowSoldier entity) {
        return TEXTURE;
    }
}
