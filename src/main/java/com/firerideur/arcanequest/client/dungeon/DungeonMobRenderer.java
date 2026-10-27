package com.firerideur.arcanequest.client.dungeon;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.client.boss.BossGlowLayer;
import com.firerideur.arcanequest.dungeon.DungeonMob;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renders a dungeon monster with its generated model, texture and glowing eyes. */
public class DungeonMobRenderer<T extends DungeonMob, M extends EntityModel<T>> extends MobRenderer<T, M> {
    private final ResourceLocation texture;
    private final float scale;

    public DungeonMobRenderer(EntityRendererProvider.Context context, M model, String name, float shadow, float scale) {
        super(context, model, shadow * scale);
        this.texture = ArcaneQuest.id("textures/entity/dungeon/" + name + ".png");
        this.scale = scale;
        addLayer(new BossGlowLayer<>(this, ArcaneQuest.id("textures/entity/dungeon/" + name + "_glow.png"), // Sleeping statues and mimics don't glow: they must look harmless
                mob -> mob.isDormant() ? 0 : 0xFFFFFFFF));
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }

    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(scale, scale, scale);
    }
}
