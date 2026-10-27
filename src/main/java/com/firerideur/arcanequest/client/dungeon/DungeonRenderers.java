package com.firerideur.arcanequest.client.dungeon;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.client.dungeon.BipedMobModel.Style;
import com.firerideur.arcanequest.registry.ModEntities;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Registers the models and renderers of the gates and the dungeon monsters. */
public final class DungeonRenderers {
    public static final ModelLayerLocation GOBLIN = layer("goblin");
    public static final ModelLayerLocation GOBLIN_SHAMAN = layer("goblin_shaman");
    public static final ModelLayerLocation ICE_ELF = layer("ice_elf");
    public static final ModelLayerLocation HIGH_ORC = layer("high_orc");
    public static final ModelLayerLocation DEMON_KNIGHT = layer("demon_knight");
    public static final ModelLayerLocation STEELFANG_WOLF = layer("steelfang_wolf");
    public static final ModelLayerLocation ANT_SOLDIER = layer("ant_soldier");

    private DungeonRenderers() {}

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ArcaneQuest.id(name), "main");
    }

    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GOBLIN, MobGeometry::goblin);
        event.registerLayerDefinition(GOBLIN_SHAMAN, MobGeometry::goblinShaman);
        event.registerLayerDefinition(ICE_ELF, MobGeometry::iceElf);
        event.registerLayerDefinition(HIGH_ORC, MobGeometry::highOrc);
        event.registerLayerDefinition(DEMON_KNIGHT, MobGeometry::demonKnight);
        event.registerLayerDefinition(STEELFANG_WOLF, MobGeometry::steelfangWolf);
        event.registerLayerDefinition(ANT_SOLDIER, MobGeometry::antSoldier);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GATE.get(), GateRenderer::new);
        event.registerEntityRenderer(ModEntities.GOBLIN.get(), c -> new DungeonMobRenderer<>(c,
                new BipedMobModel<>(c.bakeLayer(GOBLIN), Style.MELEE), "goblin", 0.4F, 1.0F));
        event.registerEntityRenderer(ModEntities.GOBLIN_SHAMAN.get(), c -> new DungeonMobRenderer<>(c,
                new BipedMobModel<>(c.bakeLayer(GOBLIN_SHAMAN), Style.CASTER), "goblin_shaman", 0.4F, 1.0F));
        event.registerEntityRenderer(ModEntities.ICE_ELF.get(), c -> new DungeonMobRenderer<>(c,
                new BipedMobModel<>(c.bakeLayer(ICE_ELF), Style.BOW), "ice_elf", 0.5F, 1.0F));
        event.registerEntityRenderer(ModEntities.HIGH_ORC.get(), c -> new DungeonMobRenderer<>(c,
                new BipedMobModel<>(c.bakeLayer(HIGH_ORC), Style.MELEE), "high_orc", 0.7F, 1.25F));
        event.registerEntityRenderer(ModEntities.DEMON_KNIGHT.get(), c -> new DungeonMobRenderer<>(c,
                new BipedMobModel<>(c.bakeLayer(DEMON_KNIGHT), Style.MELEE), "demon_knight", 0.6F, 1.1F));
        event.registerEntityRenderer(ModEntities.STEELFANG_WOLF.get(), c -> new DungeonMobRenderer<>(c,
                new QuadMobModel<>(c.bakeLayer(STEELFANG_WOLF)), "steelfang_wolf", 0.6F, 1.0F));
        event.registerEntityRenderer(ModEntities.ANT_SOLDIER.get(), c -> new DungeonMobRenderer<>(c,
                new QuadMobModel<>(c.bakeLayer(ANT_SOLDIER)), "ant_soldier", 0.7F, 1.0F));
    }
}
