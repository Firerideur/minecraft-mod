package com.firerideur.arcanequest.registry;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.boss.AdamantiteGolem;
import com.firerideur.arcanequest.boss.CorruptedArchmage;
import com.firerideur.arcanequest.boss.SkeletonKing;
import com.firerideur.arcanequest.dungeon.BeastMobs;
import com.firerideur.arcanequest.dungeon.DungeonMobs;
import com.firerideur.arcanequest.dungeon.FantasyMobs;
import com.firerideur.arcanequest.dungeon.GateEntity;
import com.firerideur.arcanequest.magic.SpellProjectile;
import com.firerideur.arcanequest.shadow.ShadowSoldier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ArcaneQuest.MODID);

    public static final RegistryObject<EntityType<SpellProjectile>> SPELL_PROJECTILE = ENTITIES.register("spell_projectile",
            () -> EntityType.Builder.<SpellProjectile>of(SpellProjectile::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F).clientTrackingRange(4).updateInterval(10).build("spell_projectile"));

    // Sizes are the unscaled vanilla ones: bosses get bigger through the SCALE attribute.
    public static final RegistryObject<EntityType<SkeletonKing>> SKELETON_KING = ENTITIES.register("skeleton_king",
            () -> EntityType.Builder.of(SkeletonKing::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.4F).eyeHeight(2.1F).fireImmune().clientTrackingRange(10).build("skeleton_king"));

    public static final RegistryObject<EntityType<CorruptedArchmage>> CORRUPTED_ARCHMAGE = ENTITIES.register("corrupted_archmage",
            () -> EntityType.Builder.of(CorruptedArchmage::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).passengerAttachments(2.0F).clientTrackingRange(10).build("corrupted_archmage"));

    public static final RegistryObject<EntityType<AdamantiteGolem>> ADAMANTITE_GOLEM = ENTITIES.register("adamantite_golem",
            () -> EntityType.Builder.of(AdamantiteGolem::new, MobCategory.MONSTER)
                    .sized(1.4F, 2.7F).fireImmune().clientTrackingRange(10).build("adamantite_golem"));

    public static final RegistryObject<EntityType<ShadowSoldier>> SHADOW_SOLDIER = ENTITIES.register("shadow_soldier",
            () -> EntityType.Builder.of(ShadowSoldier::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F).clientTrackingRange(8).build("shadow_soldier"));

    // Dungeon monsters (behind the gates)
    public static final RegistryObject<EntityType<DungeonMobs.Goblin>> GOBLIN = ENTITIES.register("goblin",
            () -> EntityType.Builder.of(DungeonMobs.Goblin::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.2F).clientTrackingRange(8).build("goblin"));

    public static final RegistryObject<EntityType<DungeonMobs.GoblinShaman>> GOBLIN_SHAMAN = ENTITIES.register("goblin_shaman",
            () -> EntityType.Builder.of(DungeonMobs.GoblinShaman::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.3F).clientTrackingRange(8).build("goblin_shaman"));

    public static final RegistryObject<EntityType<DungeonMobs.SteelfangWolf>> STEELFANG_WOLF = ENTITIES.register("steelfang_wolf",
            () -> EntityType.Builder.of(DungeonMobs.SteelfangWolf::new, MobCategory.MONSTER)
                    .sized(1.2F, 1.4F).clientTrackingRange(8).build("steelfang_wolf"));

    public static final RegistryObject<EntityType<DungeonMobs.IceElf>> ICE_ELF = ENTITIES.register("ice_elf",
            () -> EntityType.Builder.of(DungeonMobs.IceElf::new, MobCategory.MONSTER)
                    .sized(0.6F, 2.0F).clientTrackingRange(8).build("ice_elf"));

    public static final RegistryObject<EntityType<DungeonMobs.AntSoldier>> ANT_SOLDIER = ENTITIES.register("ant_soldier",
            () -> EntityType.Builder.of(DungeonMobs.AntSoldier::new, MobCategory.MONSTER)
                    .sized(1.1F, 1.0F).clientTrackingRange(8).build("ant_soldier"));

    public static final RegistryObject<EntityType<DungeonMobs.HighOrc>> HIGH_ORC = ENTITIES.register("high_orc",
            () -> EntityType.Builder.of(DungeonMobs.HighOrc::new, MobCategory.MONSTER)
                    .sized(1.0F, 2.3F).clientTrackingRange(8).build("high_orc"));

    public static final RegistryObject<EntityType<DungeonMobs.DemonKnight>> DEMON_KNIGHT = ENTITIES.register("demon_knight",
            () -> EntityType.Builder.of(DungeonMobs.DemonKnight::new, MobCategory.MONSTER)
                    .sized(0.8F, 2.2F).fireImmune().clientTrackingRange(8).build("demon_knight"));

    // Fantasy monsters (behind the gates)
    public static final RegistryObject<EntityType<FantasyMobs.Troll>> TROLL = ENTITIES.register("troll",
            () -> EntityType.Builder.of(FantasyMobs.Troll::new, MobCategory.MONSTER)
                    .sized(1.2F, 2.6F).clientTrackingRange(8).build("troll"));

    public static final RegistryObject<EntityType<FantasyMobs.Minotaur>> MINOTAUR = ENTITIES.register("minotaur",
            () -> EntityType.Builder.of(FantasyMobs.Minotaur::new, MobCategory.MONSTER)
                    .sized(0.9F, 2.3F).clientTrackingRange(8).build("minotaur"));

    public static final RegistryObject<EntityType<FantasyMobs.Gargoyle>> GARGOYLE = ENTITIES.register("gargoyle",
            () -> EntityType.Builder.of(FantasyMobs.Gargoyle::new, MobCategory.MONSTER)
                    .sized(0.8F, 1.8F).clientTrackingRange(8).build("gargoyle"));

    public static final RegistryObject<EntityType<FantasyMobs.Lich>> LICH = ENTITIES.register("lich",
            () -> EntityType.Builder.of(FantasyMobs.Lich::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.2F).clientTrackingRange(8).build("lich"));

    public static final RegistryObject<EntityType<FantasyMobs.Naga>> NAGA = ENTITIES.register("naga",
            () -> EntityType.Builder.of(FantasyMobs.Naga::new, MobCategory.MONSTER)
                    .sized(0.9F, 2.2F).clientTrackingRange(8).build("naga"));

    public static final RegistryObject<EntityType<FantasyMobs.Mimic>> MIMIC = ENTITIES.register("mimic",
            () -> EntityType.Builder.of(FantasyMobs.Mimic::new, MobCategory.MONSTER)
                    .sized(0.95F, 0.95F).clientTrackingRange(8).build("mimic"));

    public static final RegistryObject<EntityType<FantasyMobs.Treant>> TREANT = ENTITIES.register("treant",
            () -> EntityType.Builder.of(FantasyMobs.Treant::new, MobCategory.MONSTER)
                    .sized(1.2F, 2.9F).clientTrackingRange(8).build("treant"));

    public static final RegistryObject<EntityType<FantasyMobs.Basilisk>> BASILISK = ENTITIES.register("basilisk",
            () -> EntityType.Builder.of(FantasyMobs.Basilisk::new, MobCategory.MONSTER)
                    .sized(1.4F, 1.0F).clientTrackingRange(8).build("basilisk"));

    // Spiders, insects, beastmen, ghouls and demons (behind the gates)
    public static final RegistryObject<EntityType<BeastMobs.GiantSpider>> GIANT_SPIDER = ENTITIES.register("giant_spider",
            () -> EntityType.Builder.of(BeastMobs.GiantSpider::new, MobCategory.MONSTER)
                    .sized(1.6F, 0.9F).clientTrackingRange(8).build("giant_spider"));

    public static final RegistryObject<EntityType<BeastMobs.Mantis>> MANTIS = ENTITIES.register("mantis",
            () -> EntityType.Builder.of(BeastMobs.Mantis::new, MobCategory.MONSTER)
                    .sized(0.9F, 1.6F).clientTrackingRange(8).build("mantis"));

    public static final RegistryObject<EntityType<BeastMobs.Beetle>> BEETLE = ENTITIES.register("beetle",
            () -> EntityType.Builder.of(BeastMobs.Beetle::new, MobCategory.MONSTER)
                    .sized(1.3F, 1.0F).clientTrackingRange(8).build("beetle"));

    public static final RegistryObject<EntityType<BeastMobs.GiantWasp>> GIANT_WASP = ENTITIES.register("giant_wasp",
            () -> EntityType.Builder.of(BeastMobs.GiantWasp::new, MobCategory.MONSTER)
                    .sized(0.9F, 0.8F).clientTrackingRange(8).build("giant_wasp"));

    public static final RegistryObject<EntityType<BeastMobs.Werewolf>> WEREWOLF = ENTITIES.register("werewolf",
            () -> EntityType.Builder.of(BeastMobs.Werewolf::new, MobCategory.MONSTER)
                    .sized(0.8F, 2.3F).clientTrackingRange(8).build("werewolf"));

    public static final RegistryObject<EntityType<BeastMobs.Ratman>> RATMAN = ENTITIES.register("ratman",
            () -> EntityType.Builder.of(BeastMobs.Ratman::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.3F).clientTrackingRange(8).build("ratman"));

    public static final RegistryObject<EntityType<BeastMobs.Lizardman>> LIZARDMAN = ENTITIES.register("lizardman",
            () -> EntityType.Builder.of(BeastMobs.Lizardman::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.0F).clientTrackingRange(8).build("lizardman"));

    public static final RegistryObject<EntityType<BeastMobs.Ghoul>> GHOUL = ENTITIES.register("ghoul",
            () -> EntityType.Builder.of(BeastMobs.Ghoul::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.1F).clientTrackingRange(8).build("ghoul"));

    public static final RegistryObject<EntityType<BeastMobs.Imp>> IMP = ENTITIES.register("imp",
            () -> EntityType.Builder.of(BeastMobs.Imp::new, MobCategory.MONSTER)
                    .sized(0.5F, 0.9F).fireImmune().clientTrackingRange(8).build("imp"));

    public static final RegistryObject<EntityType<BeastMobs.Succubus>> SUCCUBUS = ENTITIES.register("succubus",
            () -> EntityType.Builder.of(BeastMobs.Succubus::new, MobCategory.MONSTER)
                    .sized(0.6F, 2.0F).fireImmune().clientTrackingRange(8).build("succubus"));

    public static final RegistryObject<EntityType<BeastMobs.GreaterDemon>> GREATER_DEMON = ENTITIES.register("greater_demon",
            () -> EntityType.Builder.of(BeastMobs.GreaterDemon::new, MobCategory.MONSTER)
                    .sized(1.6F, 3.8F).fireImmune().clientTrackingRange(8).build("greater_demon"));

    public static final RegistryObject<EntityType<GateEntity>> GATE = ENTITIES.register("gate",
            () -> EntityType.Builder.<GateEntity>of(GateEntity::new, MobCategory.MISC)
                    .sized(2.0F, 3.2F).clientTrackingRange(10).updateInterval(20).build("gate"));

    private ModEntities() {}

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(SKELETON_KING.get(), SkeletonKing.createAttributes().build());
        event.put(CORRUPTED_ARCHMAGE.get(), CorruptedArchmage.createAttributes().build());
        event.put(ADAMANTITE_GOLEM.get(), AdamantiteGolem.createAttributes().build());
        event.put(SHADOW_SOLDIER.get(), ShadowSoldier.createAttributes().build());
        event.put(GOBLIN.get(), DungeonMobs.Goblin.createAttributes().build());
        event.put(GOBLIN_SHAMAN.get(), DungeonMobs.GoblinShaman.createAttributes().build());
        event.put(STEELFANG_WOLF.get(), DungeonMobs.SteelfangWolf.createAttributes().build());
        event.put(ICE_ELF.get(), DungeonMobs.IceElf.createAttributes().build());
        event.put(ANT_SOLDIER.get(), DungeonMobs.AntSoldier.createAttributes().build());
        event.put(HIGH_ORC.get(), DungeonMobs.HighOrc.createAttributes().build());
        event.put(DEMON_KNIGHT.get(), DungeonMobs.DemonKnight.createAttributes().build());
        event.put(TROLL.get(), FantasyMobs.Troll.createAttributes().build());
        event.put(MINOTAUR.get(), FantasyMobs.Minotaur.createAttributes().build());
        event.put(GARGOYLE.get(), FantasyMobs.Gargoyle.createAttributes().build());
        event.put(LICH.get(), FantasyMobs.Lich.createAttributes().build());
        event.put(NAGA.get(), FantasyMobs.Naga.createAttributes().build());
        event.put(MIMIC.get(), FantasyMobs.Mimic.createAttributes().build());
        event.put(TREANT.get(), FantasyMobs.Treant.createAttributes().build());
        event.put(BASILISK.get(), FantasyMobs.Basilisk.createAttributes().build());
        event.put(GIANT_SPIDER.get(), BeastMobs.GiantSpider.createAttributes().build());
        event.put(MANTIS.get(), BeastMobs.Mantis.createAttributes().build());
        event.put(BEETLE.get(), BeastMobs.Beetle.createAttributes().build());
        event.put(GIANT_WASP.get(), BeastMobs.GiantWasp.createAttributes().build());
        event.put(WEREWOLF.get(), BeastMobs.Werewolf.createAttributes().build());
        event.put(RATMAN.get(), BeastMobs.Ratman.createAttributes().build());
        event.put(LIZARDMAN.get(), BeastMobs.Lizardman.createAttributes().build());
        event.put(GHOUL.get(), BeastMobs.Ghoul.createAttributes().build());
        event.put(IMP.get(), BeastMobs.Imp.createAttributes().build());
        event.put(SUCCUBUS.get(), BeastMobs.Succubus.createAttributes().build());
        event.put(GREATER_DEMON.get(), BeastMobs.GreaterDemon.createAttributes().build());
    }
}
