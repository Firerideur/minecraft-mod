package com.firerideur.arcanequest.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Boss 3: a hunched colossus of living adamantite with a molten core and crystals growing from its back.
 * <ul>
 *   <li>Ground slam: raises both fists, then a shockwave rolls out along the ground (jump over it).</li>
 *   <li>Charge: lowers its head and rushes in a straight line; hitting a wall stuns it (it takes more damage).</li>
 * </ul>
 * Enrages at half health: resistance, speed, charges more often.
 */
public class AdamantiteGolem extends IronGolem implements Enemy, ArcaneBoss {
    public static final int SLAM = 1, CHARGE = 2, STUNNED = 3;
    public static final int SLAM_IMPACT = 22, SLAM_LENGTH = 45;
    public static final int CHARGE_WINDUP = 20, CHARGE_LENGTH = 50;
    public static final int STUN_LENGTH = 50;

    private static final EntityDataAccessor<Byte> DATA_ATTACK = SynchedEntityData.defineId(AdamantiteGolem.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_ENRAGED = SynchedEntityData.defineId(AdamantiteGolem.class, EntityDataSerializers.BOOLEAN);

    private static final double WAVE_SPEED = 0.65;
    private static final int WAVE_TICKS = 12;
    private static final float SLAM_DAMAGE = 12.0F;
    private static final double CHARGE_SPEED = 0.55;
    private static final float CHARGE_DAMAGE = 14.0F;
    private static final float STUNNED_DAMAGE_MULTIPLIER = 1.5F;

    private final BossCore core = new BossCore(this, BossEvent.BossBarColor.RED, DATA_ATTACK, DATA_ENRAGED);
    private final Set<UUID> alreadyHit = new HashSet<>();
    private Vec3 chargeDirection = Vec3.ZERO;

    public AdamantiteGolem(EntityType<? extends IronGolem> type, Level level) {
        super(type, level);
        xpReward = 100;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return IronGolem.createAttributes()
                .add(Attributes.MAX_HEALTH, 400.0)
                .add(Attributes.ARMOR, 20.0)
                .add(Attributes.ARMOR_TOUGHNESS, 8.0)
                .add(Attributes.ATTACK_DAMAGE, 18.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.SCALE, 1.6);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ATTACK, (byte) 0);
        builder.define(DATA_ENRAGED, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (core != null) {
            core.onSyncedDataUpdated(key);
        }
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 0.9, 32.0F));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (core.tick()) {
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, -1, 0));
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, -1, 0));
            core.stopAttack();
            core.startAttack(SLAM, SLAM_LENGTH, 140);
        }
        LivingEntity target = getTarget();
        if (core.isAttacking()) {
            getNavigation().stop();
            performAttack(core.getAttack(), core.getAttackTick(), target);
            return;
        }
        if (target == null || !target.isAlive()) {
            return;
        }
        float distance = distanceTo(target);
        if (distance < 7.0F && core.ready(SLAM)) {
            core.startAttack(SLAM, SLAM_LENGTH, 140);
        } else if (distance >= 6.0F && distance < 24.0F && hasLineOfSight(target) && core.ready(CHARGE)) {
            core.startAttack(CHARGE, CHARGE_LENGTH, 200);
        }
    }

    private void performAttack(int attack, int tick, @Nullable LivingEntity target) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        switch (attack) {
            case SLAM -> slam(level, tick, target);
            case CHARGE -> charge(level, tick, target);
            case STUNNED -> {
                if (tick % 6 == 0) {
                    level.sendParticles(ParticleTypes.CRIT, getX(), getY() + getBbHeight() + 0.3, getZ(), 6, 0.6, 0.1, 0.6, 0.1);
                }
            }
            default -> {
            }
        }
    }

    private void slam(ServerLevel level, int tick, @Nullable LivingEntity target) {
        if (tick == 1) {
            alreadyHit.clear();
            playSound(SoundEvents.RAVAGER_ROAR, 2.0F, 0.6F);
        }
        if (tick < SLAM_IMPACT) {
            if (target != null) {
                getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
            return;
        }
        if (tick == SLAM_IMPACT) {
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0F, 0.6F);
        }
        int waveTick = tick - SLAM_IMPACT;
        if (waveTick > WAVE_TICKS) {
            return;
        }
        // Shockwave: a ring of debris rolling out; it only hits players standing on the ground
        double radius = 1.5 + waveTick * WAVE_SPEED;
        BlockState ground = level.getBlockState(BlockPos.containing(getX(), getY() - 0.5, getZ()));
        BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, ground.isAir() ? Blocks.STONE.defaultBlockState() : ground);
        int points = (int) (radius * 8);
        for (int i = 0; i < points; i++) {
            double a = i * Math.PI * 2 / points;
            level.sendParticles(debris, getX() + Math.cos(a) * radius, getY() + 0.2, getZ() + Math.sin(a) * radius, 3, 0.1, 0.2, 0.1, 0.1);
        }
        float damage = core.isPhaseTwo() ? SLAM_DAMAGE * 1.3F : SLAM_DAMAGE;
        for (Player player : core.playersWithin(position(), radius + 0.5)) {
            double distance = player.position().subtract(position()).horizontalDistance();
            if (player.onGround() && distance > radius - WAVE_SPEED - 0.5 && alreadyHit.add(player.getUUID())) {
                core.hit(player, damage, core.directionTo(player.position()).scale(0.6).add(0, 0.8, 0));
            }
        }
    }

    private void charge(ServerLevel level, int tick, @Nullable LivingEntity target) {
        if (tick == 1) {
            alreadyHit.clear();
            chargeDirection = target != null ? core.directionTo(target.position()) : getLookAngle().multiply(1, 0, 1).normalize();
            playSound(SoundEvents.RAVAGER_ROAR, 2.0F, 0.8F);
        }
        float yaw = (float) (Mth.atan2(chargeDirection.z, chargeDirection.x) * Mth.RAD_TO_DEG) - 90.0F;
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
        if (tick < CHARGE_WINDUP) {
            // Pawing the ground before the rush
            if (tick % 4 == 0) {
                level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.1, getZ(), 4, 0.6, 0.0, 0.6, 0.02);
                playSound(SoundEvents.IRON_GOLEM_STEP, 1.5F, 0.6F);
            }
            return;
        }
        setDeltaMovement(chargeDirection.x * CHARGE_SPEED, getDeltaMovement().y, chargeDirection.z * CHARGE_SPEED);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 0.2, getZ(), 1, 0.4, 0.1, 0.4, 0.0);
        for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(0.6),
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative())) {
            if (alreadyHit.add(player.getUUID())) {
                core.hit(player, CHARGE_DAMAGE, chargeDirection.scale(1.6).add(0, 0.5, 0));
                playSound(SoundEvents.IRON_GOLEM_ATTACK, 2.0F, 0.6F);
            }
        }
        if (horizontalCollision && tick > CHARGE_WINDUP + 2) {
            // Crashed into a wall: stunned for a moment
            level.sendParticles(ParticleTypes.EXPLOSION, getX() + chargeDirection.x, getY() + 1.5, getZ() + chargeDirection.z, 3, 0.5, 0.5, 0.5, 0);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 2.0F, 0.5F);
            setDeltaMovement(chargeDirection.scale(-0.4).add(0, 0.2, 0));
            core.stopAttack();
            core.startAttack(STUNNED, STUN_LENGTH, 0);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, core.getAttack() == STUNNED ? amount * STUNNED_DAMAGE_MULTIPLIER : amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return !core.isAttacking() && super.doHurtTarget(target);
    }

    @Override
    public void checkDespawn() {
        if (level().getDifficulty() == Difficulty.PEACEFUL) {
            discard();
        } else {
            noActionTime = 0;
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public int getBossLevel() {
        return 60;
    }

    @Override
    public long getXpReward() {
        return 9000;
    }

    @Override
    public BossCore getCore() {
        return core;
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
