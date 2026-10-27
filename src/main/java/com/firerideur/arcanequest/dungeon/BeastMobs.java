package com.firerideur.arcanequest.dungeon;

import com.firerideur.arcanequest.magic.SpellProjectile;
import com.firerideur.arcanequest.skill.SkillRuntime;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Spiders, giant insects, beastmen, ghouls and demons, found behind the gates. */
public final class BeastMobs {
    private BeastMobs() {}

    private static AttributeSupplier.Builder attributes(double health, double damage, double speed, double armor) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    /** True when {@code attacker} stands in front of {@code mob} (shields and shells only protect the front). */
    static boolean fromFront(LivingEntity mob, DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker == null) {
            return false;
        }
        Vec3 toAttacker = attacker.position().subtract(mob.position()).multiply(1, 0, 1).normalize();
        Vec3 facing = Vec3.directionFromRotation(0, mob.getYRot());
        return toAttacker.dot(facing) > 0.3;
    }

    // ================================================================= spiders and insects

    /** Climbs walls, and pins its prey with webs. */
    public static class GiantSpider extends DungeonMob {
        private int webCooldown = 60;

        public GiantSpider(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return attributes(40, 7, 0.32, 2);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            goalSelector.addGoal(1, new LeapAtTargetGoal(this, 0.4F));
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        }

        @Override
        protected PathNavigation createNavigation(Level level) {
            return new WallClimberNavigation(this, level);
        }

        @Override
        public boolean onClimbable() {
            return horizontalCollision;
        }

        @Override
        protected void customServerAiStep() {
            super.customServerAiStep();
            if (webCooldown > 0) {
                webCooldown--;
                return;
            }
            LivingEntity target = getTarget();
            if (target == null || !(level() instanceof ServerLevel level) || !hasLineOfSight(target)) {
                return;
            }
            float distance = distanceTo(target);
            if (distance < 4.0F || distance > 14.0F) {
                return;
            }
            webCooldown = 120;
            BlockPos feet = target.blockPosition();
            ItemParticleOption web = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.COBWEB));
            Vec3 from = getEyePosition();
            Vec3 to = target.position().add(0, 0.5, 0);
            for (int i = 0; i <= 12; i++) {
                Vec3 p = from.lerp(to, i / 12.0);
                level.sendParticles(web, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
            playSound(SoundEvents.SPIDER_AMBIENT, 1.5F, 0.6F);
            if (level.getBlockState(feet).isAir()) {
                level.setBlockAndUpdate(feet, Blocks.COBWEB.defaultBlockState());
                // The web dissolves after a while
                SkillRuntime.later(100, () -> {
                    if (level.getBlockState(feet).is(Blocks.COBWEB)) {
                        level.removeBlock(feet, false);
                    }
                });
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
        public void makeStuckInBlock(net.minecraft.world.level.block.state.BlockState state, Vec3 speed) {
            // Spiders walk through webs
            if (!state.is(Blocks.COBWEB)) {
                super.makeStuckInBlock(state, speed);
            }
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return SoundEvents.SPIDER_AMBIENT;
        }

        @Override
        protected SoundEvent getDeathSound() {
            return SoundEvents.SPIDER_DEATH;
        }

        @Override
        public float getVoicePitch() {
            return 0.6F;
        }
    }

    /** Strikes twice with its scythes. */
    public static class Mantis extends DungeonMob {
        public Mantis(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return attributes(45, 8, 0.36, 3);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit && target instanceof LivingEntity living) {
                // Second scythe, a moment later
                SkillRuntime.later(5, () -> {
                    if (isAlive() && living.isAlive() && distanceTo(living) < 4.0F) {
                        living.invulnerableTime = 0;
                        living.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.6F);
                        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.6F);
                    }
                });
            }
            return hit;
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return SoundEvents.SILVERFISH_AMBIENT;
        }
    }

    /** Its shell stops most hits from the front: attack it from behind. Its horn throws you in the air. */
    public static class Beetle extends DungeonMob {
        public Beetle(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return attributes(50, 9, 0.24, 14).add(Attributes.KNOCKBACK_RESISTANCE, 0.9);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        }

        @Override
        public boolean hurt(DamageSource source, float amount) {
            if (fromFront(this, source)) {
                playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 0.6F);
                return super.hurt(source, amount * 0.35F);
            }
            return super.hurt(source, amount);
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit) {
                target.push(0, 0.7, 0);
                target.hurtMarked = true;
            }
            return hit;
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return SoundEvents.SILVERFISH_STEP;
        }
    }

    /** Flies, and its sting poisons. */
    public static class GiantWasp extends DungeonMob {
        public GiantWasp(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            moveControl = new FlyingMoveControl(this, 20, true);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return attributes(20, 5, 0.3, 0).add(Attributes.FLYING_SPEED, 0.6);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.4, true));
        }

        @Override
        protected PathNavigation createNavigation(Level level) {
            FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
            navigation.setCanOpenDoors(false);
            navigation.setCanFloat(true);
            navigation.setCanPassDoors(true);
            return navigation;
        }

        @Override
        public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
            return false;
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit && target instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1), this);
            }
            return hit;
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return SoundEvents.BEE_LOOP_AGGRESSIVE;
        }

        @Override
        public float getVoicePitch() {
            return 0.6F;
        }
    }

    // ================================================================= beastmen

    /** Wolf-man: goes into a frenzy when badly hurt. */
    public static class Werewolf extends DungeonMob {
        private boolean frenzy;

        public Werewolf(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return attributes(60, 10, 0.34, 3);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            goalSelector.addGoal(1, new LeapAtTargetGoal(this, 0.45F));
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
        }

        @Override
        protected void customServerAiStep() {
            super.customServerAiStep();
            if (!frenzy && getHealth() < getMaxHealth() / 2) {
                frenzy = true;
                addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, -1, 1));
                addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, -1, 1));
                playSound(SoundEvents.WOLF_HOWL, 2.0F, 0.7F);
                if (level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getY(1.1), getZ(), 6, 0.4, 0.3, 0.4, 0);
                }
            }
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
        public float getVoicePitch() {
            return 0.6F;
        }
    }

    /** Rat-man: comes in packs with poisoned daggers. */
    public static class Ratman extends DungeonMob {
        public Ratman(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return attributes(22, 5, 0.34, 1);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit && target instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), this);
            }
            return hit;
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return SoundEvents.SILVERFISH_AMBIENT;
        }

        @Override
        public float getVoicePitch() {
            return 1.4F;
        }
    }

    /** Lizard-man: spear and shield, blocks hits from the front. */
    public static class Lizardman extends DungeonMob {
        public Lizardman(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return attributes(45, 8, 0.3, 6).add(Attributes.ENTITY_INTERACTION_RANGE, 1.0);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        }

        @Override
        public boolean hurt(DamageSource source, float amount) {
            if (fromFront(this, source) && random.nextFloat() < 0.5F) {
                playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 1.0F);
                return super.hurt(source, amount * 0.4F);
            }
            return super.hurt(source, amount);
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return SoundEvents.CAT_HISS;
        }

        @Override
        public float getVoicePitch() {
            return 0.5F;
        }
    }

    // ================================================================= ghouls

    /** Undead eater of flesh: its bite starves you and heals it. */
    public static class Ghoul extends DungeonMob {
        public Ghoul(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return attributes(35, 7, 0.36, 2);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.35, true));
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit && target instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 2), this);
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), this);
                heal((float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.4F);
            }
            return hit;
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return SoundEvents.ZOMBIE_AMBIENT;
        }

        @Override
        protected SoundEvent getHurtSound(DamageSource source) {
            return SoundEvents.ZOMBIE_HURT;
        }

        @Override
        public float getVoicePitch() {
            return 1.3F;
        }
    }

    // ================================================================= demons

    /** Little demon that throws fireballs. */
    public static class Imp extends DungeonMob implements RangedAttackMob {
        public Imp(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return attributes(18, 4, 0.34, 0);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            goalSelector.addGoal(2, new RangedAttackGoal(this, 1.1, 35, 12.0F));
        }

        @Override
        public void performRangedAttack(LivingEntity target, float power) {
            Vec3 aim = target.getEyePosition().subtract(getEyePosition()).normalize();
            SpellProjectile.shoot(this, SpellProjectile.Element.FIRE, (float) getAttributeValue(Attributes.ATTACK_DAMAGE), aim, 1.3F);
            playSound(SoundEvents.BLAZE_SHOOT, 1.0F, 1.6F);
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return SoundEvents.BLAZE_AMBIENT;
        }

        @Override
        public float getVoicePitch() {
            return 1.6F;
        }
    }

    /** Charms her prey: pulls it to her, and drains its life with each hit. */
    public static class Succubus extends DungeonMob {
        private int charmCooldown = 80;

        public Succubus(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return attributes(50, 8, 0.32, 3);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        }

        @Override
        protected void customServerAiStep() {
            super.customServerAiStep();
            if (--charmCooldown > 0 || !(getTarget() instanceof Player player) || !hasLineOfSight(player)
                    || distanceTo(player) > 12.0F || distanceTo(player) < 3.0F) {
                return;
            }
            charmCooldown = 140;
            Vec3 pull = position().subtract(player.position()).normalize().scale(1.4);
            player.push(pull.x, 0.35, pull.z);
            player.hurtMarked = true;
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0), this);
            player.displayClientMessage(Component.translatable("message.arcanequest.charmed").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            if (level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY(1.2), player.getZ(), 6, 0.4, 0.3, 0.4, 0);
            }
            playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 2.0F, 0.6F);
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit) {
                heal((float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5F);
            }
            return hit;
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
        }

        @Override
        public float getVoicePitch() {
            return 0.7F;
        }
    }

    /** A demon lord: burning aura, fireball volleys and a flaming blade. */
    public static class GreaterDemon extends DungeonMob {
        public GreaterDemon(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return attributes(150, 16, 0.27, 10).add(Attributes.KNOCKBACK_RESISTANCE, 0.9).add(Attributes.ARMOR_TOUGHNESS, 6);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        }

        @Override
        protected void customServerAiStep() {
            super.customServerAiStep();
            if (!(level() instanceof ServerLevel level)) {
                return;
            }
            if (tickCount % 20 == 0) {
                // Burning aura
                for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(3.5),
                        p -> !p.isCreative() && !p.isSpectator())) {
                    player.igniteForSeconds(3);
                }
                for (int i = 0; i < 16; i++) {
                    double a = i * Math.PI / 8;
                    level.sendParticles(ParticleTypes.FLAME, getX() + Math.cos(a) * 3, getY() + 0.2, getZ() + Math.sin(a) * 3, 1, 0, 0.05, 0, 0);
                }
            }
            LivingEntity target = getTarget();
            if (target != null && tickCount % 80 == 0 && hasLineOfSight(target) && distanceTo(target) > 4.0F) {
                Vec3 aim = target.getEyePosition().subtract(getEyePosition()).normalize();
                for (int i = -1; i <= 1; i++) {
                    SpellProjectile.shoot(this, SpellProjectile.Element.FIRE, (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.6F,
                            aim.yRot(i * 0.2F), 1.4F);
                }
                playSound(SoundEvents.GHAST_SHOOT, 2.0F, 0.6F);
            }
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit) {
                target.igniteForSeconds(5);
            }
            return hit;
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide() && random.nextInt(2) == 0) {
                level().addParticle(ParticleTypes.FLAME, getRandomX(0.8), getRandomY(), getRandomZ(0.8), 0, 0.03, 0);
            }
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return SoundEvents.WITHER_AMBIENT;
        }

        @Override
        public float getVoicePitch() {
            return 0.5F;
        }
    }
}
