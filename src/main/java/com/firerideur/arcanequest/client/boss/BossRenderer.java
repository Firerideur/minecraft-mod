package com.firerideur.arcanequest.client.boss;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.boss.ArcaneBoss;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

import java.util.function.ToIntFunction;

/** Renders a boss with its own model, texture and glowing parts (eyes, core, runes). */
public class BossRenderer<T extends Mob & ArcaneBoss, M extends EntityModel<T>> extends MobRenderer<T, M> {
    private final ResourceLocation texture;
    private final float scale;

    /**
     * @param name      texture name under textures/entity/boss/ (with a {@code _glow} variant)
     * @param glowColor ARGB tint of the glowing parts, per frame
     */
    public BossRenderer(EntityRendererProvider.Context context, M model, float shadow, String name, float scale,
                        ToIntFunction<T> glowColor) {
        super(context, model, shadow);
        this.texture = ArcaneQuest.id("textures/entity/boss/" + name + ".png");
        this.scale = scale;
        addLayer(new BossGlowLayer<>(this, ArcaneQuest.id("textures/entity/boss/" + name + "_glow.png"), glowColor));
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
