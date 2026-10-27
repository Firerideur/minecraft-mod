package com.firerideur.arcanequest.dungeon;

import com.firerideur.arcanequest.magic.SpellProjectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** The monsters found behind the gates, from the weakest (goblins, 1 star) to the strongest (demon knights, 7 stars). */
public final class DungeonMobs {
    private DungeonMobs() {}

    private static AttributeSupplier.Builder attributes(double health, double damage, double speed, double armor) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    /** Small, fast and in packs. */
    public static class Goblin extends DungeonMob {
        public Goblin(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return attributes(14, 3, 0.33, 1);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
        }

        @Override
        public float getVoicePitch() {
            return 1.6F;
        }
    }

    /** Keeps its distance and throws cursed bolts. */
    public static class GoblinShaman extends DungeonMob implements RangedAttackMob {
        public GoblinShaman(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return attributes(18, 4, 0.28, 0);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 40, 12.0F));
        }

        @Override
        public void performRangedAttack(LivingEntity target, float power) {
            Vec3 aim = target.getEyePosition().subtract(getEyePosition()).normalize();
            SpellProjectile.shoot(this, SpellProjectile.Element.ARCANE, (float) getAttributeValue(Attributes.ATTACK_DAMAGE), aim, 1.4F);
            playSound(SoundEvents.EVOKER_CAST_SPELL, 1.0F, 1.6F);
        }
    }

    /** A wolf with steel fangs that leaps at its prey. */
    public static class SteelfangWolf extends DungeonMob {
        public SteelfangWolf(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return attributes(24, 5, 0.36, 3);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            goalSelector.addGoal(1, new LeapAtTargetGoal(this, 0.4F));
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return SoundEvents.WOLF_GROWL;
        }

        @Override
        protected SoundEvent getHurtSound(DamageSource source) {
            return SoundEvents.WOLF_HURT;
        }

        @Override
        protected SoundEvent getDeathSound() {
            return SoundEvents.WOLF_DEATH;
        }
    }

    /** Archer of the ice fortress: its arrows slow. */
    public static class IceElf extends DungeonMob implements RangedAttackMob {
        public IceElf(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return attributes(26, 5, 0.3, 2);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 30, 16.0F));
        }

        @Override
        public void performRangedAttack(LivingEntity target, float power) {
            Arrow arrow = new Arrow(level(), this, new ItemStack(Items.ARROW), null);
            arrow.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            double dx = target.getX() - getX();
            double dz = target.getZ() - getZ();
            double dy = target.getY(0.33) - arrow.getY() + Math.sqrt(dx * dx + dz * dz) * 0.2;
            arrow.shoot(dx, dy, dz, 1.8F, 4.0F);
            arrow.setBaseDamage(getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.6);
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            level().addFreshEntity(arrow);
            playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.3F);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide() && random.nextInt(6) == 0) {
                level().addParticle(ParticleTypes.SNOWFLAKE, getRandomX(0.5), getRandomY(), getRandomZ(0.5), 0, 0, 0);
            }
        }
    }

    /** Armored insect: its bite poisons. */
    public static class AntSoldier extends DungeonMob {
        public AntSoldier(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return attributes(34, 7, 0.3, 6);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit && target instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0), this);
            }
            return hit;
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return SoundEvents.SILVERFISH_AMBIENT;
        }
    }

    /** Huge brute: slow, but every hit sends you flying. */
    public static class HighOrc extends DungeonMob {
        public HighOrc(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return attributes(60, 10, 0.26, 4).add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit && target instanceof LivingEntity living) {
                Vec3 push = living.position().subtract(position()).multiply(1, 0, 1).normalize().scale(1.2);
                living.push(push.x, 0.4, push.z);
                living.hurtMarked = true;
            }
            return hit;
        }

        @Override
        public float getVoicePitch() {
            return 0.6F;
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return SoundEvents.PIGLIN_BRUTE_ANGRY;
        }
    }

    /** Knight of the abyss: heavy armor, burning blade. */
    public static class DemonKnight extends DungeonMob {
        public DemonKnight(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return attributes(80, 12, 0.28, 10).add(Attributes.KNOCKBACK_RESISTANCE, 0.5).add(Attributes.ARMOR_TOUGHNESS, 4);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit) {
                target.igniteForSeconds(4);
            }
            return hit;
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide() && random.nextInt(4) == 0) {
                level().addParticle(ParticleTypes.SMALL_FLAME, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0.02, 0);
            } else if (level() instanceof ServerLevel && isOnFire()) {
                clearFire();
            }
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return SoundEvents.WITHER_SKELETON_AMBIENT;
        }

        @Override
        public float getVoicePitch() {
            return 0.5F;
        }
    }
}
