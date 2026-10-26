package com.firerideur.arcanequest.boss;

import com.firerideur.arcanequest.magic.SpellCaster;
import com.firerideur.arcanequest.magic.SpellProjectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Boss 2: an evoker gone mad with arcane power.
 * Keeps vanilla fangs and vexes, adds fire bolt volleys, teleports when hit, and calls lightning when enraged.
 */
public class CorruptedArchmage extends Evoker implements ArcaneBoss {
    private static final float BOLT_DAMAGE = 8.0F;
    private static final float LIGHTNING_DAMAGE = 10.0F;
    private static final float TELEPORT_CHANCE = 0.3F;

    private final BossCore core = new BossCore(this, BossEvent.BossBarColor.PURPLE);

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
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (core.tick()) {
            teleportRandomly();
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || !hasLineOfSight(target)) {
            return;
        }
        if (core.every(50)) {
            Vec3 aim = SpellCaster.aim(this, target);
            int bolts = core.isPhaseTwo() ? 3 : 1;
            for (int i = 0; i < bolts; i++) {
                SpellProjectile.shoot(this, SpellProjectile.Element.FIRE, BOLT_DAMAGE, aim.yRot((i - bolts / 2) * 0.2F), 1.6F);
            }
            playSound(SoundEvents.EVOKER_CAST_SPELL, 1.5F, 0.8F);
        }
        if (core.isPhaseTwo() && tickCount % 160 == 0) {
            SpellCaster.strike(this, target.position(), LIGHTNING_DAMAGE);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide() && isAlive() && random.nextFloat() < TELEPORT_CHANCE) {
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
