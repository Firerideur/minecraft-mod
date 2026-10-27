package com.firerideur.arcanequest.boss;

import com.firerideur.arcanequest.magic.SpellCaster;
import com.firerideur.arcanequest.magic.SpellProjectile;
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
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Boss 2: a hooded mage floating above the ground, surrounded by arcane crystals.
 * <ul>
 *   <li>Meteor: a burning circle marks the ground, then a meteor crashes on it (three in phase 2).</li>
 *   <li>Arcane nova: the crystals gather, then a blast throws back everyone close.</li>
 *   <li>Fire bolts, vanilla fangs and vexes, teleports when hit, lightning when enraged.</li>
 * </ul>
 */
public class CorruptedArchmage extends Evoker implements ArcaneBoss {
    public static final int BOLTS = 1, METEOR = 2, NOVA = 3;
    public static final int BOLTS_CAST = 12, BOLTS_LENGTH = 20;
    public static final int METEOR_FALL = 38, METEOR_IMPACT = 48, METEOR_LENGTH = 55;
    public static final int NOVA_BLAST = 25, NOVA_LENGTH = 35;

    private static final EntityDataAccessor<Byte> DATA_ATTACK = SynchedEntityData.defineId(CorruptedArchmage.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_ENRAGED = SynchedEntityData.defineId(CorruptedArchmage.class, EntityDataSerializers.BOOLEAN);

    private static final float BOLT_DAMAGE = 8.0F;
    private static final float LIGHTNING_DAMAGE = 10.0F;
    private static final float TELEPORT_CHANCE = 0.3F;
    private static final double METEOR_RADIUS = 3.5;
    private static final float METEOR_DAMAGE = 14.0F;
    private static final double NOVA_RADIUS = 6.0;
    private static final float NOVA_DAMAGE = 8.0F;

    private final BossCore core = new BossCore(this, BossEvent.BossBarColor.PURPLE, DATA_ATTACK, DATA_ENRAGED);
    private final List<Vec3> meteorTargets = new ArrayList<>();

    public CorruptedArchmage(EntityType<? extends Evoker> type, Level level) {
        super(type, level);
        xpReward = 100;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Evoker.createAttributes()
                .add(Attributes.MAX_HEALTH, 260.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.SCALE, 1.4);
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
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (core.tick()) {
            teleportRandomly();
        }
        LivingEntity target = getTarget();
        if (core.isAttacking()) {
            getNavigation().stop();
            if (target != null) {
                getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
            performAttack(core.getAttack(), core.getAttackTick(), target);
            return;
        }
        if (target == null || !target.isAlive()) {
            return;
        }
        boolean sees = hasLineOfSight(target);
        if (distanceTo(target) < 5.0F && core.ready(NOVA)) {
            core.startAttack(NOVA, NOVA_LENGTH, 160);
        } else if (sees && core.ready(METEOR)) {
            core.startAttack(METEOR, METEOR_LENGTH, 180);
        } else if (sees && core.ready(BOLTS)) {
            core.startAttack(BOLTS, BOLTS_LENGTH, 45);
        }
        if (core.isPhaseTwo() && sees && tickCount % 160 == 0) {
            SpellCaster.strike(this, target.position(), LIGHTNING_DAMAGE);
        }
    }

    private void performAttack(int attack, int tick, @Nullable LivingEntity target) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        switch (attack) {
            case BOLTS -> {
                level.sendParticles(ParticleTypes.WITCH, getX(), getY() + 2.2, getZ(), 2, 0.4, 0.2, 0.4, 0.0);
                if (tick == BOLTS_CAST && target != null) {
                    Vec3 aim = SpellCaster.aim(this, target);
                    int bolts = core.isPhaseTwo() ? 3 : 1;
                    for (int i = 0; i < bolts; i++) {
                        SpellProjectile.shoot(this, SpellProjectile.Element.FIRE, BOLT_DAMAGE, aim.yRot((i - bolts / 2) * 0.2F), 1.6F);
                    }
                    playSound(SoundEvents.EVOKER_CAST_SPELL, 1.5F, 0.8F);
                }
            }
            case METEOR -> meteor(level, tick, target);
            case NOVA -> nova(level, tick);
            default -> {
            }
        }
    }

    private void meteor(ServerLevel level, int tick, @Nullable LivingEntity target) {
        if (tick == 1) {
            meteorTargets.clear();
            if (target != null) {
                addMeteorTarget(level, target.position());
                if (core.isPhaseTwo()) {
                    for (int i = 0; i < 2; i++) {
                        double a = random.nextDouble() * Math.PI * 2;
                        addMeteorTarget(level, target.position().add(Math.cos(a) * 4.5, 0, Math.sin(a) * 4.5));
                    }
                }
            }
            playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 2.0F, 0.6F);
        }
        for (Vec3 spot : meteorTargets) {
            if (tick < METEOR_IMPACT && tick % 3 == 0) {
                // Warning circle, tightening as the impact gets close
                double radius = METEOR_RADIUS * (1.0 - 0.3 * tick / METEOR_IMPACT);
                for (int i = 0; i < 20; i++) {
                    double a = i * Math.PI * 2 / 20 + tick * 0.05;
                    level.sendParticles(ParticleTypes.FLAME, spot.x + Math.cos(a) * radius, spot.y + 0.1,
                            spot.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
                }
                level.sendParticles(ParticleTypes.SMOKE, spot.x, spot.y + 0.1, spot.z, 3, 0.4, 0, 0.4, 0.01);
            }
            if (tick >= METEOR_FALL && tick < METEOR_IMPACT) {
                double height = 14.0 * (METEOR_IMPACT - tick) / (METEOR_IMPACT - METEOR_FALL);
                level.sendParticles(ParticleTypes.FLAME, spot.x, spot.y + height, spot.z, 12, 0.35, 0.35, 0.35, 0.02);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, spot.x, spot.y + height + 0.8, spot.z, 4, 0.3, 0.5, 0.3, 0.0);
                level.sendParticles(ParticleTypes.LAVA, spot.x, spot.y + height, spot.z, 2, 0.2, 0.2, 0.2, 0.0);
            }
            if (tick == METEOR_IMPACT) {
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, spot.x, spot.y + 0.5, spot.z, 1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.LAVA, spot.x, spot.y + 0.5, spot.z, 20, 1.5, 0.3, 1.5, 0.0);
                level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0F, 0.7F);
                for (Player player : core.playersWithin(spot, METEOR_RADIUS)) {
                    Vec3 away = player.position().subtract(spot).multiply(1, 0, 1);
                    Vec3 push = away.lengthSqr() < 1.0E-4 ? new Vec3(0, 0.6, 0) : away.normalize().scale(0.8).add(0, 0.5, 0);
                    if (core.hit(player, METEOR_DAMAGE, push)) {
                        player.igniteForSeconds(4.0F);
                    }
                }
            }
        }
        if (tick == METEOR_FALL) {
            playSound(SoundEvents.BLAZE_SHOOT, 2.0F, 0.5F);
        }
    }

    private void addMeteorTarget(ServerLevel level, Vec3 pos) {
        Vec3 ground = BossCore.groundAt(level, pos);
        meteorTargets.add(ground != null ? ground : pos);
    }

    private void nova(ServerLevel level, int tick) {
        if (tick == 1) {
            playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 2.0F, 0.5F);
        }
        if (tick < NOVA_BLAST) {
            // Energy flows in from all around
            double radius = NOVA_RADIUS * (1.0 - (double) tick / NOVA_BLAST) + 1.0;
            for (int i = 0; i < 6; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                level.sendParticles(ParticleTypes.WITCH, getX() + Math.cos(a) * radius, getY() + 1.0 + random.nextDouble() * 1.5,
                        getZ() + Math.sin(a) * radius, 1, 0, 0, 0, 0);
            }
        }
        if (tick == NOVA_BLAST) {
            level.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + 1.4, getZ(), 1, 0, 0, 0, 0);
            for (int i = 0; i < 32; i++) {
                double a = i * Math.PI * 2 / 32;
                level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.2, getZ(), 0, Math.cos(a), 0.05, Math.sin(a), 0.6);
            }
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.HOSTILE, 2.5F, 0.6F);
            for (Player player : core.playersWithin(position(), NOVA_RADIUS)) {
                core.hit(player, NOVA_DAMAGE, core.directionTo(player.position()).scale(1.6).add(0, 0.5, 0));
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide() && isAlive() && !core.isAttacking() && random.nextFloat() < TELEPORT_CHANCE) {
            teleportRandomly();
        }
        return hurt;
    }

    private void teleportRandomly() {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        double oldX = getX();
        double oldY = getY();
        double oldZ = getZ();
        for (int attempt = 0; attempt < 16; attempt++) {
            double x = getX() + (random.nextDouble() - 0.5) * 20.0;
            double y = getY() + random.nextInt(9) - 4;
            double z = getZ() + (random.nextDouble() - 0.5) * 20.0;
            if (randomTeleport(x, y, z, true)) {
                level.sendParticles(ParticleTypes.PORTAL, oldX, oldY + 1.0, oldZ, 30, 0.5, 1.0, 0.5, 0.1);
                playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 0.8F);
                return;
            }
        }
    }

    @Override
    public boolean canBeLeader() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public int getBossLevel() {
        return 45;
    }

    @Override
    public long getXpReward() {
        return 5000;
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
