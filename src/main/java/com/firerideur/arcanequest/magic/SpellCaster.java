package com.firerideur.arcanequest.magic;

import com.firerideur.arcanequest.network.ModNetwork;
import com.firerideur.arcanequest.stats.PlayerStats;
import com.firerideur.arcanequest.stats.StatsCapability;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Server side spell logic. */
public final class SpellCaster {
    private static final double LIGHTNING_RANGE = 32.0;
    private static final double LIGHTNING_RADIUS = 2.5;

    private SpellCaster() {}

    /**
     * Tries to cast a spell, spending mana.
     *
     * @return true if the spell was cast
     */
    public static boolean tryCast(ServerPlayer player, Spell spell) {
        PlayerStats stats = StatsCapability.get(player).orElse(null);
        if (stats == null) {
            return false;
        }
        boolean free = player.getAbilities().instabuild;
        float cost = spell.manaCost() * stats.getPlayerClass().bonuses().manaCost();
        if (!free && stats.getMana() < cost) {
            player.displayClientMessage(Component.translatable("message.arcanequest.no_mana").withStyle(ChatFormatting.BLUE), true);
            return false;
        }
        if (!free) {
            stats.setMana(stats.getMana() - cost);
            ModNetwork.sync(player);
        }
        float power = spell.basePower() * stats.spellPower();
        cast(player, spell, spell == Spell.HEAL ? power * stats.getPlayerClass().bonuses().heals() : power);
        return true;
    }

    public static void cast(LivingEntity caster, Spell spell, float power) {
        Vec3 look = caster.getLookAngle();
        switch (spell) {
            case FIRE_BOLT -> SpellProjectile.shoot(caster, SpellProjectile.Element.FIRE, power, look, 1.8F);
            case FROST_BOLT -> SpellProjectile.shoot(caster, SpellProjectile.Element.FROST, power, look, 1.8F);
            case ARCANE_MISSILE -> SpellProjectile.shoot(caster, SpellProjectile.Element.ARCANE, power, look, 2.2F);
            case ARCANE_BARRAGE -> {
                for (int i = -2; i <= 2; i++) {
                    SpellProjectile.shoot(caster, SpellProjectile.Element.ARCANE, power, look.yRot(i * 0.15F), 2.0F);
                }
            }
            case LIGHTNING -> lightning(caster, power);
            case HEAL -> heal(caster, power);
        }
        caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                spell == Spell.HEAL ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    private static void lightning(LivingEntity caster, float damage) {
        Vec3 eye = caster.getEyePosition();
        Vec3 end = eye.add(caster.getLookAngle().scale(LIGHTNING_RANGE));
        HitResult blockHit = caster.pick(LIGHTNING_RANGE, 1.0F, false);
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(caster, eye, end,
                caster.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0),
                entity -> entity instanceof LivingEntity && entity != caster && !entity.isSpectator(),
                eye.distanceToSqr(end));
        Vec3 target = entityHit != null ? entityHit.getLocation() : end;
        strike(caster, target, damage);
    }

    /** Visual lightning at {@code target}, damaging every living entity near it except the caster. */
    public static void strike(LivingEntity caster, Vec3 target, float damage) {
        if (!(caster.level() instanceof ServerLevel level)) {
            return;
        }
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(target.x, target.y, target.z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        AABB area = new AABB(target, target).inflate(LIGHTNING_RADIUS);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != caster && e.isAlive())) {
            if (victim.hurt(caster.damageSources().indirectMagic(caster, caster), damage)) {
                victim.igniteForSeconds(3);
            }
        }
    }

    private static void heal(LivingEntity caster, float amount) {
        caster.heal(amount);
        if (caster.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HEART, caster.getX(), caster.getY() + 1.0, caster.getZ(),
                    6, 0.4, 0.4, 0.4, 0.0);
        }
    }

    /** Direction from {@code from}'s eyes to the middle of {@code to}. */
    public static Vec3 aim(Entity from, Entity to) {
        return to.getBoundingBox().getCenter().subtract(from.getEyePosition()).normalize();
    }
}
