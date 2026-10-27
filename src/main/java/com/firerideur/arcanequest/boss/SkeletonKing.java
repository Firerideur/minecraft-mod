package com.firerideur.arcanequest.boss;

import com.firerideur.arcanequest.stats.PlayerClass;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Boss 1: a giant crowned skeleton in a royal cape.
 * <ul>
 *   <li>Whirlwind: raises his sword, then spins towards you, cutting everything around him.</li>
 *   <li>Bone eruption: plunges his sword into the ground; jaws of bone burst out along a line of soul fire.</li>
 *   <li>Arrow volley at range, and raises skeletons.</li>
 * </ul>
 * Enrages at half health: faster, stronger, three eruption lines, red eyes.
 */
public class SkeletonKing extends WitherSkeleton implements ArcaneBoss {
    public static final int WHIRLWIND = 1, ERUPTION = 2, VOLLEY = 3, SUMMON = 4;
    public static final int WHIRLWIND_WINDUP = 30, WHIRLWIND_LENGTH = 80;
    public static final int ERUPTION_SLAM = 14, ERUPTION_LENGTH = 30;
    public static final int VOLLEY_SHOT = 15, VOLLEY_LENGTH = 25;
    public static final int SUMMON_CAST = 22, SUMMON_LENGTH = 35;

    private static final EntityDataAccessor<Byte> DATA_ATTACK = SynchedEntityData.defineId(SkeletonKing.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_ENRAGED = SynchedEntityData.defineId(SkeletonKing.class, EntityDataSerializers.BOOLEAN);

    private static final int MAX_MINIONS = 6;
    private static final double WHIRLWIND_RADIUS = 3.5;
    private static final float WHIRLWIND_DAMAGE = 6.0F;
    private static final int ERUPTION_FANGS = 14;

    private final BossCore core = new BossCore(this, BossEvent.BossBarColor.WHITE, DATA_ATTACK, DATA_ENRAGED);
    private Vec3 eruptionDirection = Vec3.ZERO;

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
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
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
            core.startAttack(SUMMON, SUMMON_LENGTH, 400);
        }
        LivingEntity target = getTarget();
        if (core.isAttacking()) {
            if (target != null) {
                getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
            performAttack(core.getAttack(), core.getAttackTick(), target);
            return;
        }
        if (target == null || !target.isAlive()) {
            return;
        }
        float distance = distanceTo(target);
        boolean sees = hasLineOfSight(target);
        if (core.ready(SUMMON) && countMinions() < MAX_MINIONS / 2) {
            core.startAttack(SUMMON, SUMMON_LENGTH, 400);
        } else if (distance < 5.0F && core.ready(WHIRLWIND)) {
            core.startAttack(WHIRLWIND, WHIRLWIND_LENGTH, 220);
        } else if (distance < 20.0F && sees && core.ready(ERUPTION)) {
            core.startAttack(ERUPTION, ERUPTION_LENGTH, 150);
        } else if (distance > 5.0F && sees && core.ready(VOLLEY)) {
            core.startAttack(VOLLEY, VOLLEY_LENGTH, 100);
        }
    }

    private void performAttack(int attack, int tick, @Nullable LivingEntity target) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (attack != WHIRLWIND || tick < WHIRLWIND_WINDUP) {
            getNavigation().stop();
        }
        switch (attack) {
            case WHIRLWIND -> whirlwind(level, tick, target);
            case ERUPTION -> eruption(level, tick, target);
            case VOLLEY -> {
                if (target != null && (tick == VOLLEY_SHOT || core.isPhaseTwo() && tick == VOLLEY_SHOT + 7)) {
                    arrowVolley(target);
                }
            }
            case SUMMON -> {
                if (tick == 1) {
                    playSound(SoundEvents.WITHER_SKELETON_AMBIENT, 2.5F, 0.4F);
                }
                level.sendParticles(ParticleTypes.SOUL, getX(), getY() + 0.2, getZ(), 2, 1.5, 0.1, 1.5, 0.02);
                if (tick == SUMMON_CAST) {
                    summonMinions(core.isPhaseTwo() ? 4 : 3);
                }
            }
            default -> {
            }
        }
    }

    private void whirlwind(ServerLevel level, int tick, @Nullable LivingEntity target) {
        if (tick < WHIRLWIND_WINDUP) {
            // Wind-up: soul fire gathers around the raised sword
            double angle = tick * 0.6;
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX() + Math.cos(angle) * 1.6, getY() + 1.0 + tick * 0.08,
                    getZ() + Math.sin(angle) * 1.6, 2, 0.05, 0.05, 0.05, 0.0);
            if (tick == 1) {
                playSound(SoundEvents.WITHER_SKELETON_AMBIENT, 2.0F, 0.5F);
            }
            return;
        }
        if (target != null) {
            Vec3 dir = core.directionTo(target.position()).scale(0.13);
            setDeltaMovement(dir.x, getDeltaMovement().y, dir.z);
        }
        if (tick % 5 == 0) {
            for (int i = 0; i < 4; i++) {
                double a = tick * 0.9 + i * Math.PI / 2;
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() + Math.cos(a) * 2.2, getY() + 1.2,
                        getZ() + Math.sin(a) * 2.2, 1, 0, 0, 0, 0);
            }
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5F, 0.6F);
            for (Player player : core.playersWithin(position(), WHIRLWIND_RADIUS)) {
                core.hit(player, WHIRLWIND_DAMAGE, core.directionTo(player.position()).scale(0.6).add(0, 0.25, 0));
            }
        }
    }

    private void eruption(ServerLevel level, int tick, @Nullable LivingEntity target) {
        if (tick == 1) {
            eruptionDirection = target != null ? core.directionTo(target.position()) : getLookAngle().multiply(1, 0, 1).normalize();
            playSound(SoundEvents.SKELETON_AMBIENT, 2.0F, 0.4F);
        }
        if (tick < ERUPTION_SLAM) {
            // Telegraph: a line of soul fire shows where the bones will burst out
            for (Vec3 dir : eruptionLines()) {
                for (int i = 1; i <= ERUPTION_FANGS; i += 2) {
                    Vec3 p = position().add(dir.scale(1.25 * i));
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y + 0.15, p.z, 1, 0.1, 0.0, 0.1, 0.0);
                }
            }
        }
        if (tick == ERUPTION_SLAM) {
            playSound(SoundEvents.EVOKER_FANGS_ATTACK, 2.0F, 0.6F);
            level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.2, getZ(), 1, 0, 0, 0, 0);
            for (Vec3 dir : eruptionLines()) {
                // Same angle convention as the evoker (radians from atan2)
                float yaw = (float) Mth.atan2(dir.z, dir.x);
                for (int i = 1; i <= ERUPTION_FANGS; i++) {
                    Vec3 ground = BossCore.groundAt(level, position().add(dir.scale(1.25 * i)));
                    if (ground != null) {
                        level.addFreshEntity(new EvokerFangs(level, ground.x, ground.y, ground.z, yaw, i, this));
                    }
                }
            }
        }
    }

    private Vec3[] eruptionLines() {
        if (!core.isPhaseTwo()) {
            return new Vec3[]{eruptionDirection};
        }
        return new Vec3[]{eruptionDirection, eruptionDirection.yRot(0.45F), eruptionDirection.yRot(-0.45F)};
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

    private int countMinions() {
        return level().getEntitiesOfClass(Skeleton.class, getBoundingBox().inflate(24.0)).size();
    }

    private void summonMinions(int count) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        int nearby = countMinions();
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
    public boolean doHurtTarget(Entity target) {
        // No plain sword hits in the middle of a special attack
        return !core.isAttacking() && super.doHurtTarget(target);
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
    public int getBossId() {
        return PlayerClass.BossKills.SKELETON_KING;
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
