package com.firerideur.arcanequest.dungeon;

import com.firerideur.arcanequest.magic.SpellProjectile;
import com.firerideur.arcanequest.mobs.MobLevels;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Monsters from fantasy worlds, found behind the gates alongside the {@link DungeonMobs}. */
public final class FantasyMobs {
    private FantasyMobs() {}

    private static AttributeSupplier.Builder attributes(double health, double damage, double speed, double armor) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    /** Cave troll: regenerates quickly, unless it was burned recently. */
    public static class Troll extends DungeonMob {
        private int burnedUntil;

        public Troll(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return attributes(70, 11, 0.25, 3).add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level() instanceof ServerLevel level) {
                if (isOnFire()) {
                    burnedUntil = tickCount + 100;
                }
                if (tickCount % 20 == 0 && tickCount > burnedUntil && getHealth() < getMaxHealth() && isAlive()) {
                    heal(getMaxHealth() * 0.02F);
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY(1.0), getZ(), 2, 0.4, 0.4, 0.4, 0);
                }
            }
        }

        @Override
        public boolean hurt(DamageSource source, float amount) {
            if (source.is(DamageTypeTags.IS_FIRE)) {
                burnedUntil = tickCount + 100;
            }
            return super.hurt(source, amount);
        }

        @Override
        public float getVoicePitch() {
            return 0.5F;
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return SoundEvents.RAVAGER_AMBIENT;
        }
    }

    /** Labyrinth guardian: lowers its horns and charges from afar. */
    public static class Minotaur extends DungeonMob {
        private int chargeTicks;
        private int chargeCooldown = 60;
        private boolean chargeHit;
        private Vec3 chargeDirection = Vec3.ZERO;

        public Minotaur(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return attributes(80, 12, 0.3, 5).add(Attributes.KNOCKBACK_RESISTANCE, 0.7);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        }

        public boolean isCharging() {
            return chargeTicks > 0;
        }

        @Override
        protected void customServerAiStep() {
            super.customServerAiStep();
            LivingEntity target = getTarget();
            if (chargeCooldown > 0) {
                chargeCooldown--;
            }
            if (chargeTicks > 0) {
                chargeTicks--;
                getNavigation().stop();
                setDeltaMovement(chargeDirection.x * 0.6, getDeltaMovement().y, chargeDirection.z * 0.6);
                float yaw = (float) (Mth.atan2(chargeDirection.z, chargeDirection.x) * Mth.RAD_TO_DEG) - 90.0F;
                setYRot(yaw);
                yBodyRot = yaw;
                yHeadRot = yaw;
                if (level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.1, getZ(), 2, 0.3, 0.0, 0.3, 0.01);
                }
                if (!chargeHit && target != null && distanceTo(target) < 2.2F) {
                    chargeHit = true;
                    target.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5F);
                    target.push(chargeDirection.x * 2.0, 0.6, chargeDirection.z * 2.0);
                    target.hurtMarked = true;
                    playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.5F, 0.7F);
                    chargeTicks = 0;
                }
                if (horizontalCollision) {
                    chargeTicks = 0;
                }
                return;
            }
            if (target != null && chargeCooldown <= 0 && onGround() && hasLineOfSight(target)) {
                float distance = distanceTo(target);
                if (distance > 5.0F && distance < 16.0F) {
                    Vec3 d = target.position().subtract(position()).multiply(1, 0, 1);
                    chargeDirection = d.normalize();
                    chargeTicks = 22;
                    chargeHit = false;
                    chargeCooldown = 120;
                    playSound(SoundEvents.RAVAGER_ROAR, 1.5F, 1.0F);
                }
            }
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return SoundEvents.COW_AMBIENT;
        }

        @Override
        public float getVoicePitch() {
            return 0.5F;
        }
    }

    /** A stone statue... until you come close. Almost invulnerable while asleep. */
    public static class Gargoyle extends DungeonMob {
        public Gargoyle(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            setDormant(true);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return attributes(50, 9, 0.32, 12);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
        }

        @Override
        protected double wakeRadius() {
            return 6.0;
        }

        @Override
        protected float dormantDamageFactor() {
            return 0.2F;
        }

        @Override
        protected void onWake() {
            if (level() instanceof ServerLevel level) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                        getX(), getY(1.0), getZ(), 40, 0.4, 0.8, 0.4, 0.1);
            }
            playSound(SoundEvents.STONE_BREAK, 1.5F, 0.6F);
            playSound(SoundEvents.PHANTOM_AMBIENT, 1.5F, 0.6F);
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return isDormant() ? null : SoundEvents.PHANTOM_AMBIENT;
        }
    }

    /** Undead sorcerer: soul bolts, and raises skeletons to fight for it. */
    public static class Lich extends DungeonMob implements RangedAttackMob {
        public Lich(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return attributes(60, 8, 0.27, 4);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 40, 14.0F));
        }

        @Override
        public void performRangedAttack(LivingEntity target, float power) {
            Vec3 aim = target.getEyePosition().subtract(getEyePosition()).normalize();
            SpellProjectile.shoot(this, SpellProjectile.Element.ARCANE, (float) getAttributeValue(Attributes.ATTACK_DAMAGE), aim, 1.5F);
            playSound(SoundEvents.EVOKER_CAST_SPELL, 1.0F, 0.6F);
        }

        @Override
        protected void customServerAiStep() {
            super.customServerAiStep();
            if (getTarget() != null && tickCount % 300 == 0 && level() instanceof ServerLevel level
                    && level.getEntitiesOfClass(Skeleton.class, getBoundingBox().inflate(16)).size() < 4) {
                for (int i = 0; i < 2; i++) {
                    Skeleton skeleton = EntityType.SKELETON.create(level);
                    if (skeleton == null) {
                        continue;
                    }
                    double a = random.nextDouble() * Math.PI * 2;
                    skeleton.moveTo(getX() + Math.cos(a) * 2, getY(), getZ() + Math.sin(a) * 2, getYRot(), 0);
                    skeleton.finalizeSpawn(level, level.getCurrentDifficultyAt(skeleton.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                    MobLevels.setLevel(skeleton, Math.max(1, MobLevels.getLevel(this) - 5), false);
                    skeleton.setTarget(getTarget());
                    level.addFreshEntity(skeleton);
                    level.sendParticles(ParticleTypes.SOUL, skeleton.getX(), skeleton.getY() + 0.5, skeleton.getZ(), 15, 0.3, 0.5, 0.3, 0.03);
                }
                playSound(SoundEvents.WITHER_SKELETON_AMBIENT, 1.5F, 0.5F);
            }
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide() && random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.WITCH, getRandomX(0.5), getRandomY(), getRandomZ(0.5), 0, 0.02, 0);
            }
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return SoundEvents.WITHER_SKELETON_AMBIENT;
        }
    }

    /** Serpent warrior of the sunken temples: throws poison flasks. */
    public static class Naga extends DungeonMob implements RangedAttackMob {
        public Naga(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return attributes(55, 7, 0.3, 4);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 50, 12.0F));
        }

        @Override
        public void performRangedAttack(LivingEntity target, float power) {
            ThrownPotion potion = new ThrownPotion(level(), this);
            potion.setItem(PotionContents.createItemStack(Items.SPLASH_POTION, Potions.STRONG_POISON));
            double dx = target.getX() - getX();
            double dy = target.getEyeY() - 1.1 - getY(0.6);
            double dz = target.getZ() - getZ();
            potion.setXRot(potion.getXRot() + 20.0F);
            potion.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.2, dz, 0.75F, 6.0F);
            level().addFreshEntity(potion);
            playSound(SoundEvents.LLAMA_SPIT, 1.0F, 0.6F);
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return SoundEvents.CAT_HISS;
        }

        @Override
        public float getVoicePitch() {
            return 0.6F;
        }
    }

    /** It looks like a treasure chest. It is not. */
    public static class Mimic extends DungeonMob {
        public Mimic(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            setDormant(true);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return attributes(40, 9, 0.3, 4);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
        }

        @Override
        protected double wakeRadius() {
            return 2.5;
        }

        @Override
        protected void onWake() {
            playSound(SoundEvents.CHEST_OPEN, 1.5F, 0.5F);
            playSound(SoundEvents.RAVAGER_ROAR, 0.8F, 1.6F);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            // Moves by hopping
            if (!level().isClientSide() && !isDormant() && onGround() && getNavigation().isInProgress() && tickCount % 8 == 0) {
                jumpFromGround();
            }
        }

        @Override
        protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
            super.dropCustomDeathLoot(level, source, recentlyHit);
            int stars = Mth.clamp(MobLevels.getLevel(this) / 12 + 1, 1, Gates.MAX_STARS);
            Rewards.roomLoot(stars, random).forEach(this::spawnAtLocation);
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return isDormant() ? null : SoundEvents.CHEST_CLOSE;
        }
    }

    /** Walking tree: slow and very tough, its roots hold you in place. */
    public static class Treant extends DungeonMob {
        public Treant(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return attributes(90, 10, 0.2, 6).add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit && target instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3), this);
                if (level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.COMPOSTER, living.getX(), living.getY() + 0.2, living.getZ(), 12, 0.4, 0.1, 0.4, 0);
                }
            }
            return hit;
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return SoundEvents.WOOD_BREAK;
        }

        @Override
        public float getVoicePitch() {
            return 0.4F;
        }
    }

    /** Giant lizard: its bite poisons, and meeting its gaze petrifies you. Look away! */
    public static class Basilisk extends DungeonMob {
        private static final int GAZE_INTERVAL = 100;

        public Basilisk(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return attributes(55, 8, 0.28, 5);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        }

        @Override
        protected void customServerAiStep() {
            super.customServerAiStep();
            if (tickCount % GAZE_INTERVAL != 0 || !(getTarget() instanceof ServerPlayer player) || !hasLineOfSight(player)
                    || distanceTo(player) > 14.0F) {
                return;
            }
            Vec3 toBasilisk = getEyePosition().subtract(player.getEyePosition()).normalize();
            if (player.getViewVector(1.0F).dot(toBasilisk) > 0.8) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 5), this);
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 2), this);
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1), this);
                player.displayClientMessage(Component.translatable("message.arcanequest.petrified").withStyle(ChatFormatting.GRAY), true);
                if (level() instanceof ServerLevel level) {
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                            player.getX(), player.getY(1.0), player.getZ(), 25, 0.3, 0.6, 0.3, 0.05);
                }
                playSound(SoundEvents.BASALT_PLACE, 1.5F, 0.5F);
            }
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit && target instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1), this);
            }
            return hit;
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return SoundEvents.HOGLIN_AMBIENT;
        }

        @Override
        public float getVoicePitch() {
            return 0.7F;
        }
    }
}
