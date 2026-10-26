package com.firerideur.arcanequest.magic;

import com.firerideur.arcanequest.registry.ModEntities;
import com.firerideur.arcanequest.registry.ModItems;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Magic bolt shot by staffs and by the Corrupted Archmage. Flies straight, no gravity. */
public class SpellProjectile extends ThrowableItemProjectile {
    public enum Element { FIRE, FROST, ARCANE }

    private static final EntityDataAccessor<Integer> ELEMENT =
            SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.INT);
    private static final int MAX_LIFETIME = 60;

    private float damage = 4.0F;

    public SpellProjectile(EntityType<? extends SpellProjectile> type, Level level) {
        super(type, level);
    }

    public SpellProjectile(Level level, LivingEntity owner, Element element, float damage) {
        super(ModEntities.SPELL_PROJECTILE.get(), owner, level);
        this.damage = damage;
        setElement(element);
    }

    /** Spawns a bolt from the caster's eyes, flying along {@code direction}. */
    public static SpellProjectile shoot(LivingEntity caster, Element element, float damage, Vec3 direction, float speed) {
        SpellProjectile bolt = new SpellProjectile(caster.level(), caster, element, damage);
        bolt.setPos(caster.getX(), caster.getEyeY() - 0.1, caster.getZ());
        bolt.shoot(direction.x, direction.y, direction.z, speed, 0.0F);
        caster.level().addFreshEntity(bolt);
        return bolt;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ELEMENT, Element.FIRE.ordinal());
    }

    public Element getElement() {
        int id = entityData.get(ELEMENT);
        Element[] values = Element.values();
        return id >= 0 && id < values.length ? values[id] : Element.FIRE;
    }

    private void setElement(Element element) {
        entityData.set(ELEMENT, element.ordinal());
        setItem(new ItemStack(itemFor(element)));
    }

    private static Item itemFor(Element element) {
        return switch (element) {
            case FIRE -> Items.FIRE_CHARGE;
            case FROST -> Items.SNOWBALL;
            case ARCANE -> ModItems.ARCANITE.get();
        };
    }

    @Override
    protected Item getDefaultItem() {
        return Items.FIRE_CHARGE;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            for (int i = 0; i < 2; i++) {
                level().addParticle(trailParticle(), getX(), getY(), getZ(),
                        random.nextGaussian() * 0.02, random.nextGaussian() * 0.02, random.nextGaussian() * 0.02);
            }
        } else if (tickCount > MAX_LIFETIME) {
            discard();
        }
    }

    private ParticleOptions trailParticle() {
        return switch (getElement()) {
            case FIRE -> ParticleTypes.FLAME;
            case FROST -> ParticleTypes.SNOWFLAKE;
            case ARCANE -> ParticleTypes.WITCH;
        };
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        Entity owner = getOwner();
        if (target.hurt(damageSources().indirectMagic(this, owner), damage) && target instanceof LivingEntity living) {
            switch (getElement()) {
                case FIRE -> living.igniteForSeconds(4);
                case FROST -> living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), owner);
                case ARCANE -> living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0), owner);
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(trailParticle(), getX(), getY(), getZ(), 12, 0.2, 0.2, 0.2, 0.05);
            serverLevel.playSound(null, getX(), getY(), getZ(),
                    getElement() == Element.FROST ? SoundEvents.GLASS_BREAK : SoundEvents.FIRECHARGE_USE,
                    SoundSource.NEUTRAL, 0.6F, 1.2F);
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("damage", damage);
        tag.putInt("element", getElement().ordinal());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("damage");
        entityData.set(ELEMENT, tag.getInt("element"));
    }
}
