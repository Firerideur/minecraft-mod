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
    public static final ModelLayerLocation TROLL = layer("troll");
    public static final ModelLayerLocation MINOTAUR = layer("minotaur");
    public static final ModelLayerLocation GARGOYLE = layer("gargoyle");
    public static final ModelLayerLocation LICH = layer("lich");
    public static final ModelLayerLocation NAGA = layer("naga");
    public static final ModelLayerLocation MIMIC = layer("mimic");
    public static final ModelLayerLocation TREANT = layer("treant");
    public static final ModelLayerLocation BASILISK = layer("basilisk");
    public static final ModelLayerLocation GIANT_SPIDER = layer("giant_spider");
    public static final ModelLayerLocation MANTIS = layer("mantis");
    public static final ModelLayerLocation BEETLE = layer("beetle");
    public static final ModelLayerLocation GIANT_WASP = layer("giant_wasp");
    public static final ModelLayerLocation WEREWOLF = layer("werewolf");
    public static final ModelLayerLocation RATMAN = layer("ratman");
    public static final ModelLayerLocation LIZARDMAN = layer("lizardman");
    public static final ModelLayerLocation GHOUL = layer("ghoul");
    public static final ModelLayerLocation IMP = layer("imp");
    public static final ModelLayerLocation SUCCUBUS = layer("succubus");
    public static final ModelLayerLocation GREATER_DEMON = layer("greater_demon");

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
        event.registerLayerDefinition(TROLL, MobGeometry::troll);
        event.registerLayerDefinition(MINOTAUR, MobGeometry::minotaur);
        event.registerLayerDefinition(GARGOYLE, MobGeometry::gargoyle);
        event.registerLayerDefinition(LICH, MobGeometry::lich);
        event.registerLayerDefinition(NAGA, MobGeometry::naga);
        event.registerLayerDefinition(MIMIC, MobGeometry::mimic);
        event.registerLayerDefinition(TREANT, MobGeometry::treant);
        event.registerLayerDefinition(BASILISK, MobGeometry::basilisk);
        event.registerLayerDefinition(GIANT_SPIDER, MobGeometry::giantSpider);
        event.registerLayerDefinition(MANTIS, MobGeometry::mantis);
        event.registerLayerDefinition(BEETLE, MobGeometry::beetle);
        event.registerLayerDefinition(GIANT_WASP, MobGeometry::giantWasp);
        event.registerLayerDefinition(WEREWOLF, MobGeometry::werewolf);
        event.registerLayerDefinition(RATMAN, MobGeometry::ratman);
        event.registerLayerDefinition(LIZARDMAN, MobGeometry::lizardman);
        event.registerLayerDefinition(GHOUL, MobGeometry::ghoul);
        event.registerLayerDefinition(IMP, MobGeometry::imp);
        event.registerLayerDefinition(SUCCUBUS, MobGeometry::succubus);
        event.registerLayerDefinition(GREATER_DEMON, MobGeometry::greaterDemon);
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
        event.registerEntityRenderer(ModEntities.TROLL.get(), c -> new DungeonMobRenderer<>(c,
                new BipedMobModel<>(c.bakeLayer(TROLL), Style.MELEE), "troll", 0.7F, 1.5F));
        event.registerEntityRenderer(ModEntities.MINOTAUR.get(), c -> new DungeonMobRenderer<>(c,
                new BipedMobModel<>(c.bakeLayer(MINOTAUR), Style.MELEE), "minotaur", 0.6F, 1.2F));
        event.registerEntityRenderer(ModEntities.GARGOYLE.get(), c -> new DungeonMobRenderer<>(c,
                new BipedMobModel<>(c.bakeLayer(GARGOYLE), Style.MELEE), "gargoyle", 0.5F, 1.05F));
        event.registerEntityRenderer(ModEntities.LICH.get(), c -> new DungeonMobRenderer<>(c,
                new BipedMobModel<>(c.bakeLayer(LICH), Style.CASTER), "lich", 0.5F, 1.1F));
        event.registerEntityRenderer(ModEntities.NAGA.get(), c -> new DungeonMobRenderer<>(c,
                new BipedMobModel<>(c.bakeLayer(NAGA), Style.CASTER), "naga", 0.6F, 1.0F));
        event.registerEntityRenderer(ModEntities.MIMIC.get(), c -> new DungeonMobRenderer<>(c,
                new MimicModel<>(c.bakeLayer(MIMIC)), "mimic", 0.6F, 1.0F));
        event.registerEntityRenderer(ModEntities.TREANT.get(), c -> new DungeonMobRenderer<>(c,
                new BipedMobModel<>(c.bakeLayer(TREANT), Style.MELEE), "treant", 0.8F, 1.4F));
        event.registerEntityRenderer(ModEntities.BASILISK.get(), c -> new DungeonMobRenderer<>(c,
                new QuadMobModel<>(c.bakeLayer(BASILISK)), "basilisk", 0.8F, 1.0F));
        event.registerEntityRenderer(ModEntities.GIANT_SPIDER.get(), c -> new DungeonMobRenderer<>(c,
                new QuadMobModel<>(c.bakeLayer(GIANT_SPIDER), true), "giant_spider", 0.9F, 1.0F));
        event.registerEntityRenderer(ModEntities.MANTIS.get(), c -> new DungeonMobRenderer<>(c,
                new QuadMobModel<>(c.bakeLayer(MANTIS)), "mantis", 0.6F, 1.0F));
        event.registerEntityRenderer(ModEntities.BEETLE.get(), c -> new DungeonMobRenderer<>(c,
                new QuadMobModel<>(c.bakeLayer(BEETLE)), "beetle", 0.8F, 1.0F));
        event.registerEntityRenderer(ModEntities.GIANT_WASP.get(), c -> new DungeonMobRenderer<>(c,
                new QuadMobModel<>(c.bakeLayer(GIANT_WASP)), "giant_wasp", 0.5F, 1.0F));
        event.registerEntityRenderer(ModEntities.WEREWOLF.get(), c -> new DungeonMobRenderer<>(c,
                new BipedMobModel<>(c.bakeLayer(WEREWOLF), Style.MELEE), "werewolf", 0.6F, 1.1F));
        event.registerEntityRenderer(ModEntities.RATMAN.get(), c -> new DungeonMobRenderer<>(c,
                new BipedMobModel<>(c.bakeLayer(RATMAN), Style.MELEE), "ratman", 0.4F, 1.0F));
        event.registerEntityRenderer(ModEntities.LIZARDMAN.get(), c -> new DungeonMobRenderer<>(c,
                new BipedMobModel<>(c.bakeLayer(LIZARDMAN), Style.MELEE), "lizardman", 0.5F, 1.0F));
        event.registerEntityRenderer(ModEntities.GHOUL.get(), c -> new DungeonMobRenderer<>(c,
                new BipedMobModel<>(c.bakeLayer(GHOUL), Style.MELEE), "ghoul", 0.5F, 1.05F));
        event.registerEntityRenderer(ModEntities.IMP.get(), c -> new DungeonMobRenderer<>(c,
                new BipedMobModel<>(c.bakeLayer(IMP), Style.CASTER), "imp", 0.3F, 0.85F));
        event.registerEntityRenderer(ModEntities.SUCCUBUS.get(), c -> new DungeonMobRenderer<>(c,
                new BipedMobModel<>(c.bakeLayer(SUCCUBUS), Style.MELEE), "succubus", 0.5F, 1.0F));
        event.registerEntityRenderer(ModEntities.GREATER_DEMON.get(), c -> new DungeonMobRenderer<>(c,
                new BipedMobModel<>(c.bakeLayer(GREATER_DEMON), Style.MELEE), "greater_demon", 1.2F, 1.7F));
    }
}
