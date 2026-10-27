package com.firerideur.arcanequest.client.boss;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.boss.AdamantiteGolem;
import com.firerideur.arcanequest.boss.CorruptedArchmage;
import com.firerideur.arcanequest.boss.SkeletonKing;
import com.firerideur.arcanequest.registry.ModEntities;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Registers the boss models and renderers. */
public final class BossRenderers {
    public static final ModelLayerLocation SKELETON_KING = layer("skeleton_king");
    public static final ModelLayerLocation CORRUPTED_ARCHMAGE = layer("corrupted_archmage");
    public static final ModelLayerLocation ADAMANTITE_GOLEM = layer("adamantite_golem");

    private BossRenderers() {}

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ArcaneQuest.id(name), "main");
    }

    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SKELETON_KING, BossGeometry::skeletonKing);
        event.registerLayerDefinition(CORRUPTED_ARCHMAGE, BossGeometry::corruptedArchmage);
        event.registerLayerDefinition(ADAMANTITE_GOLEM, BossGeometry::adamantiteGolem);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SKELETON_KING.get(), context -> {
            BossRenderer<SkeletonKing, SkeletonKingModel> renderer = new BossRenderer<>(context,
                    new SkeletonKingModel(context.bakeLayer(SKELETON_KING)), 0.7F, "skeleton_king", 1.2F,
                    // Soul-blue eyes, red when enraged
                    king -> king.getCore().isEnraged() ? 0xFFFF5040 : 0xFFFFFFFF);
            renderer.addLayer(new ItemInHandLayer<>(renderer, context.getItemInHandRenderer()));
            return renderer;
        });
        event.registerEntityRenderer(ModEntities.CORRUPTED_ARCHMAGE.get(), context -> new BossRenderer<CorruptedArchmage, ArchmageModel>(
                context, new ArchmageModel(context.bakeLayer(CORRUPTED_ARCHMAGE)), 0.6F, "corrupted_archmage", 1.0F,
                mage -> mage.getCore().isEnraged() ? pulse(mage.tickCount, 0.3F) : 0xFFFFFFFF));
        event.registerEntityRenderer(ModEntities.ADAMANTITE_GOLEM.get(), context -> new BossRenderer<AdamantiteGolem, GolemModel>(
                context, new GolemModel(context.bakeLayer(ADAMANTITE_GOLEM)), 1.0F, "adamantite_golem", 1.2F,
                // The molten core beats; faster when enraged
                golem -> pulse(golem.tickCount, golem.getCore().isEnraged() ? 0.35F : 0.12F)));
    }

    private static int pulse(int tick, float speed) {
        int v = (int) (190 + 65 * Mth.sin(tick * speed));
        return FastColor.ARGB32.color(255, v, v, v);
    }
}
