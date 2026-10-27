package com.firerideur.arcanequest.registry;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.boss.AdamantiteGolem;
import com.firerideur.arcanequest.boss.CorruptedArchmage;
import com.firerideur.arcanequest.boss.SkeletonKing;
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

    private ModEntities() {}

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(SKELETON_KING.get(), SkeletonKing.createAttributes().build());
        event.put(CORRUPTED_ARCHMAGE.get(), CorruptedArchmage.createAttributes().build());
        event.put(ADAMANTITE_GOLEM.get(), AdamantiteGolem.createAttributes().build());
        event.put(SHADOW_SOLDIER.get(), ShadowSoldier.createAttributes().build());
    }
}
