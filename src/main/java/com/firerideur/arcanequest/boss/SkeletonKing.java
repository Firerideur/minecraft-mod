package com.firerideur.arcanequest.boss;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Boss 1: a giant crowned wither skeleton.
 * Shoots arrow volleys at range and raises skeleton minions; enrages at half health.
 */
public class SkeletonKing extends WitherSkeleton implements ArcaneBoss {
    private static final int MAX_MINIONS = 6;

    private final BossCore core = new BossCore(this, BossEvent.BossBarColor.WHITE);

    public SkeletonKing(EntityType<? extends WitherSkeleton> type, Level level) {
        super(type, level);
        xpReward = 100;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractSkeleton.createAttributes()
                .add(Attributes.MAX_HEALTH, 300.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.SCALE, 1.75);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, 0.0F);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (core.tick()) {
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, -1, 0));
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, -1, 0));
            summonMinions(4);
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        if (core.every(120) && distanceTo(target) > 4.0F && hasLineOfSight(target)) {
            arrowVolley(target);
        }
        if (core.every(300)) {
            summonMinions(3);
        }
    }

    private void arrowVolley(LivingEntity target) {
        Vec3 aim = target.getEyePosition().subtract(getEyePosition()).normalize();
        for (int i = -2; i <= 2; i++) {
            Arrow arrow = new Arrow(level(), this, new ItemStack(Items.ARROW), null);
            arrow.setPos(getX(), getEyeY() - 0.2, getZ());
            Vec3 dir = aim.yRot(i * 0.12F);
            arrow.shoot(dir.x, dir.y + 0.05, dir.z, 2.0F, 2.0F);
            arrow.setBaseDamage(4.0);
            arrow.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
            level().addFreshEntity(arrow);
        }
        playSound(SoundEvents.SKELETON_SHOOT, 1.5F, 0.6F);
    }

    private void summonMinions(int count) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        int nearby = level.getEntitiesOfClass(Skeleton.class, getBoundingBox().inflate(24.0)).size();
        for (int i = 0; i < count && nearby < MAX_MINIONS; i++, nearby++) {
            Skeleton minion = EntityType.SKELETON.create(level);
            if (minion == null) {
                continue;
            }
            double angle = random.nextDouble() * Math.PI * 2;
            minion.moveTo(getX() + Math.cos(angle) * 3, getY(), getZ() + Math.sin(angle) * 3, getYRot(), 0);
            minion.finalizeSpawn(level, level.getCurrentDifficultyAt(minion.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            minion.setTarget(getTarget());
            level.addFreshEntity(minion);
            level.sendParticles(ParticleTypes.SOUL, minion.getX(), minion.getY() + 0.5, minion.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
        }
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.WITHER_SKELETON_AMBIENT, SoundSource.HOSTILE, 2.0F, 0.5F);
    }

    @Override
    public float getVoicePitch() {
        return 0.6F;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public int getBossLevel() {
        return 30;
    }

    @Override
    public long getXpReward() {
        return 2500;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        core.startSeenByPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        core.stopSeenByPlayer(player);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        core.onNameChanged();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        core.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        core.load(tag);
    }
}
