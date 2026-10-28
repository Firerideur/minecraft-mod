package com.firerideur.arcanequest.skill;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.boss.ArcaneBoss;
import com.firerideur.arcanequest.magic.Mana;
import com.firerideur.arcanequest.magic.SpellProjectile;
import com.firerideur.arcanequest.shadow.Corpses;
import com.firerideur.arcanequest.shadow.ShadowSoldier;
import com.firerideur.arcanequest.skill.SkillRuntime.Buff;
import com.firerideur.arcanequest.stats.PlayerClass;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** What each {@link Skill} does. Damage and durations are multiplied by the skill power. */
final class SkillActions {
    private static final DustParticleOptions SHADOW_DUST = new DustParticleOptions(new Vector3f(0.25F, 0.2F, 0.6F), 1.5F);
    private static final DustParticleOptions HOLY_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.9F, 0.4F), 1.5F);
    private static final DustParticleOptions BLOOD_DUST = new DustParticleOptions(new Vector3f(0.8F, 0.05F, 0.05F), 1.5F);
    private static final DustParticleOptions ARCANE_DUST = new DustParticleOptions(new Vector3f(0.6F, 0.3F, 1.0F), 1.3F);

    private SkillActions() {}

    // =================================================================== helpers

    /** Monsters, and anything that is fighting the player; never other players, pets or shadows. */
    static boolean isEnemy(Player player, Entity entity) {
        if (!(entity instanceof LivingEntity living) || entity == player || !living.isAlive() || entity instanceof Player
                || entity instanceof ShadowSoldier || entity instanceof ArmorStand
                || entity instanceof TamableAnimal pet && pet.isOwnedBy(player)) {
            return false;
        }
        return entity instanceof Enemy || entity instanceof Mob mob && mob.getTarget() == player;
    }

    static List<LivingEntity> enemiesAround(SkillContext c, Vec3 center, double radius) {
        return c.level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                e -> isEnemy(c.player(), e) && e.position().distanceTo(center) <= radius);
    }

    /** The enemy under the crosshair, up to {@code range} blocks, not through walls. */
    @Nullable
    static LivingEntity targetInFront(SkillContext c, double range) {
        ServerPlayer p = c.player();
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        HitResult block = c.level().clip(new ClipContext(eye, eye.add(look.scale(range)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, p));
        double reach = block.getType() == HitResult.Type.MISS ? range : block.getLocation().distanceTo(eye);
        Vec3 end = eye.add(look.scale(reach));
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p, eye, end, p.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.5),
                e -> isEnemy(p, e), reach * reach);
        if (hit != null) {
            return (LivingEntity) hit.getEntity();
        }
        // Forgiving aim: the closest enemy in a narrow cone
        return enemiesAround(c, eye, Math.min(range, 8)).stream()
                .filter(e -> e.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) > 0.9)
                .min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
    }

    /** Point looked at, up to {@code range} blocks. */
    static Vec3 lookedAt(SkillContext c, double range) {
        ServerPlayer p = c.player();
        Vec3 eye = p.getEyePosition();
        HitResult hit = c.level().clip(new ClipContext(eye, eye.add(p.getLookAngle().scale(range)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getLocation();
    }

    /** Healing of a skill: grows with power and with the class's healing bonus. */
    static float healAmount(SkillContext c, float base) {
        return base * c.power() * c.stats().getPlayerClass().bonuses().heals();
    }

    /** Enemies in front of the player, up to {@code range} blocks, in a cone ({@code minDot} = cosine of the half-angle). */
    static List<LivingEntity> inCone(SkillContext c, double range, double minDot) {
        ServerPlayer p = c.player();
        Vec3 look = flatLook(p);
        return enemiesAround(c, p.position(), range).stream().filter(e -> {
            Vec3 to = e.position().subtract(p.position()).multiply(1, 0, 1);
            return to.lengthSqr() < 2.25 || to.normalize().dot(look) >= minDot;
        }).toList();
    }

    /** Enemies touching the segment {@code from}-{@code to}, {@code width} blocks around it. */
    static List<LivingEntity> onLine(SkillContext c, Vec3 from, Vec3 to, double width) {
        return c.level().getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(width + 1.0),
                e -> isEnemy(c.player(), e) && e.getBoundingBox().inflate(width).clip(from, to).isPresent());
    }

    static Vec3 flatLook(Player p) {
        Vec3 look = p.getLookAngle().multiply(1, 0, 1);
        return look.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0, p.getYRot()) : look.normalize();
    }

    /**
     * Fear (Bloodlust, Dragon's Fear): the monster forgets its target and runs away from the player, weakened.
     * Bosses are only weakened.
     */
    static void frighten(SkillContext c, LivingEntity e, int ticks) {
        ServerPlayer p = c.player();
        effect(e, MobEffects.MOVEMENT_SLOWDOWN, ticks, 1);
        effect(e, MobEffects.WEAKNESS, ticks, 1);
        c.level().sendParticles(ParticleTypes.SMOKE, e.getX(), e.getY(1.1), e.getZ(), 6, 0.2, 0.2, 0.2, 0.01);
        if (!(e instanceof Mob mob) || e instanceof ArcaneBoss) {
            return;
        }
        SkillRuntime.fear(mob, ticks);
        mob.setTarget(null);
        for (int t = 0; t < ticks; t += 10) {
            SkillRuntime.later(t, () -> {
                if (!mob.isAlive()) {
                    return;
                }
                Vec3 away = mob.position().subtract(p.position()).multiply(1, 0, 1);
                away = mob.position().add(away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize().scale(8.0));
                mob.getNavigation().moveTo(away.x, away.y, away.z, 1.3);
            });
        }
    }

    /**
     * Temporary attribute bonus (transformations): a transient modifier with a fixed id, removed after {@code ticks}.
     * Transient modifiers aren't saved, so a logout can't keep them.
     */
    static void temporary(LivingEntity entity, Holder<Attribute> attribute, String id, double amount,
                          AttributeModifier.Operation operation, int ticks) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        ResourceLocation key = ArcaneQuest.id("skill_" + id);
        instance.addOrUpdateTransientModifier(new AttributeModifier(key, amount, operation));
        SkillRuntime.later(ticks, () -> {
            instance.removeModifier(key);
            if (entity.getHealth() > entity.getMaxHealth()) {
                entity.setHealth(entity.getMaxHealth());
            }
        });
    }

    static void lightning(SkillContext c, Vec3 pos) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(c.level());
        if (bolt != null) {
            bolt.moveTo(pos.x, pos.y, pos.z);
            // Only the flash and the thunder: the damage is the skill's
            bolt.setVisualOnly(true);
            c.level().addFreshEntity(bolt);
        }
    }

    static boolean hurt(SkillContext c, LivingEntity target, float amount, boolean magic) {
        ServerPlayer p = c.player();
        DamageSource source = magic ? p.damageSources().indirectMagic(p, p) : p.damageSources().playerAttack(p);
        // Several hits in a row: skip the usual half-second invulnerability
        target.invulnerableTime = 0;
        return target.hurt(source, amount);
    }

    static void push(LivingEntity target, Vec3 from, double strength, double up) {
        Vec3 away = target.position().subtract(from).multiply(1, 0, 1);
        away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize().scale(strength);
        target.push(away.x, up, away.z);
        target.hurtMarked = true;
    }

    static void ring(SkillContext c, Vec3 center, double radius, ParticleOptions particle, int points) {
        for (int i = 0; i < points; i++) {
            double a = i * Math.PI * 2 / points;
            c.level().sendParticles(particle, center.x + Math.cos(a) * radius, center.y + 0.2, center.z + Math.sin(a) * radius,
                    1, 0, 0.05, 0, 0);
        }
    }

    static void line(SkillContext c, Vec3 from, Vec3 to, ParticleOptions particle) {
        int steps = (int) (from.distanceTo(to) * 3) + 1;
        for (int i = 0; i <= steps; i++) {
            Vec3 pos = from.lerp(to, (double) i / steps);
            c.level().sendParticles(particle, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0);
        }
    }

    static void sound(SkillContext c, SoundEvent sound, float volume, float pitch) {
        c.level().playSound(null, c.player().blockPosition(), sound, c.player().getSoundSource(), volume, pitch);
    }

    static void effect(LivingEntity entity, Holder<MobEffect> effect, int ticks, int amplifier) {
        entity.addEffect(new MobEffectInstance(effect, ticks, amplifier));
    }

    /** Duration scaled by power, for buffs. */
    static int ticks(SkillContext c, int base) {
        return Math.round(base * Math.min(2.0F, c.power()));
    }

    // =================================================================== warrior

    static boolean charge(SkillContext c) {
        ServerPlayer p = c.player();
        Vec3 dir = p.getLookAngle().multiply(1, 0, 1).normalize();
        p.setDeltaMovement(dir.scale(2.2).add(0, 0.15, 0));
        p.hurtMarked = true;
        sound(c, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F, 0.7F);
        Set<LivingEntity> hit = new HashSet<>();
        for (int i = 1; i <= 8; i++) {
            SkillRuntime.later(i, () -> {
                c.level().sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.3, p.getZ(), 3, 0.2, 0.1, 0.2, 0.01);
                for (LivingEntity e : enemiesAround(c, p.position(), 1.8)) {
                    if (hit.add(e) && hurt(c, e, 6.0F * c.power(), false)) {
                        push(e, p.position(), 1.0, 0.4);
                    }
                }
            });
        }
        return true;
    }

    static boolean whirlwind(SkillContext c) {
        ServerPlayer p = c.player();
        for (int spin = 0; spin < 3; spin++) {
            SkillRuntime.later(spin * 5, () -> {
                ring(c, p.position().add(0, 0.8, 0), 2.5, ParticleTypes.SWEEP_ATTACK, 8);
                sound(c, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.8F);
                for (LivingEntity e : enemiesAround(c, p.position(), 4.0)) {
                    if (hurt(c, e, 4.0F * c.power(), false)) {
                        push(e, p.position(), 0.4, 0.15);
                    }
                }
            });
        }
        return true;
    }

    static boolean groundSlam(SkillContext c) {
        ServerPlayer p = c.player();
        c.level().sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY(), p.getZ(), 3, 1.5, 0.1, 1.5, 0);
        ring(c, p.position(), 4.5, ParticleTypes.CAMPFIRE_COSY_SMOKE, 24);
        sound(c, SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 1.2F);
        for (LivingEntity e : enemiesAround(c, p.position(), 5.0)) {
            if (hurt(c, e, 7.0F * c.power(), false)) {
                push(e, p.position(), 0.5, 0.8);
            }
        }
        return true;
    }

    static boolean earthquake(SkillContext c) {
        ServerPlayer p = c.player();
        Vec3 center = p.position();
        for (int wave = 0; wave < 3; wave++) {
            double radius = 3.0 + wave * 3.0;
            SkillRuntime.later(wave * 8, () -> {
                ring(c, center, radius, ParticleTypes.EXPLOSION, 12);
                sound(c, SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 0.6F);
                for (LivingEntity e : enemiesAround(c, center, radius)) {
                    if (hurt(c, e, 6.0F * c.power(), false)) {
                        push(e, center, 0.3, 0.6);
                        effect(e, MobEffects.MOVEMENT_SLOWDOWN, 60, 2);
                    }
                }
            });
        }
        return true;
    }

    static boolean warCry(SkillContext c) {
        ServerPlayer p = c.player();
        int ticks = ticks(c, 200);
        effect(p, MobEffects.DAMAGE_BOOST, ticks, 1);
        effect(p, MobEffects.MOVEMENT_SPEED, ticks, 0);
        for (LivingEntity e : enemiesAround(c, p.position(), 10.0)) {
            effect(e, MobEffects.WEAKNESS, ticks, 1);
        }
        ring(c, p.position().add(0, 1, 0), 3.0, ParticleTypes.ANGRY_VILLAGER, 12);
        sound(c, SoundEvents.RAVAGER_ROAR, 1.0F, 1.2F);
        return true;
    }

    /** Sung Jinwoo's Bloodlust: killing intent that terrifies the weak. */
    static boolean bloodlust(SkillContext c) {
        ServerPlayer p = c.player();
        int ticks = ticks(c, 80);
        for (LivingEntity e : enemiesAround(c, p.position(), 10.0)) {
            frighten(c, e, ticks);
        }
        effect(p, MobEffects.DAMAGE_BOOST, ticks, 0);
        ring(c, p.position().add(0, 0.5, 0), 4.0, BLOOD_DUST, 24);
        c.level().sendParticles(BLOOD_DUST, p.getX(), p.getY(1.0), p.getZ(), 30, 0.5, 0.7, 0.5, 0);
        sound(c, SoundEvents.WARDEN_HEARTBEAT, 2.0F, 0.8F);
        sound(c, SoundEvents.RAVAGER_ROAR, 0.6F, 1.4F);
        return true;
    }

    static boolean bloodFrenzy(SkillContext c) {
        SkillRuntime.addBuff(c.player(), Buff.LIFESTEAL, ticks(c, 200), c.power());
        c.level().sendParticles(new DustParticleOptions(new Vector3f(0.8F, 0.05F, 0.05F), 1.5F),
                c.player().getX(), c.player().getY(1.0), c.player().getZ(), 30, 0.5, 0.7, 0.5, 0);
        sound(c, SoundEvents.WARDEN_HEARTBEAT, 2.0F, 1.0F);
        return true;
    }

    static boolean berserk(SkillContext c) {
        ServerPlayer p = c.player();
        int ticks = ticks(c, 200);
        effect(p, MobEffects.DAMAGE_BOOST, ticks, 2);
        effect(p, MobEffects.MOVEMENT_SPEED, ticks, 1);
        effect(p, MobEffects.DIG_SPEED, ticks, 1);
        SkillRuntime.addBuff(p, Buff.BERSERK, ticks, c.power());
        c.level().sendParticles(ParticleTypes.FLAME, p.getX(), p.getY(1.0), p.getZ(), 30, 0.5, 0.8, 0.5, 0.05);
        sound(c, SoundEvents.RAVAGER_ROAR, 1.2F, 0.7F);
        return true;
    }

    static boolean execute(SkillContext c) {
        LivingEntity target = targetInFront(c, 5.0);
        if (target == null) {
            return false;
        }
        boolean weak = target.getHealth() < target.getMaxHealth() * 0.3F;
        hurt(c, target, 10.0F * c.power() * (weak ? 3.0F : 1.0F), false);
        c.level().sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 25, 0.3, 0.5, 0.3, 0.3);
        sound(c, weak ? SoundEvents.PLAYER_ATTACK_CRIT : SoundEvents.PLAYER_ATTACK_STRONG, 1.2F, 0.6F);
        return true;
    }

    static boolean bladeDance(SkillContext c) {
        ServerPlayer p = c.player();
        List<LivingEntity> targets = new ArrayList<>(enemiesAround(c, p.position(), 10.0));
        if (targets.isEmpty()) {
            return false;
        }
        targets.sort(Comparator.comparingDouble(p::distanceToSqr));
        for (int i = 0; i < Math.min(5, targets.size()); i++) {
            LivingEntity e = targets.get(i);
            SkillRuntime.later(i * 4, () -> {
                if (!e.isAlive()) {
                    return;
                }
                Vec3 behind = e.position().subtract(e.getLookAngle().multiply(1, 0, 1).normalize().scale(1.2));
                c.level().sendParticles(ParticleTypes.SWEEP_ATTACK, p.getX(), p.getY(1.0), p.getZ(), 1, 0, 0, 0, 0);
                p.teleportTo(behind.x, e.getY(), behind.z);
                hurt(c, e, 8.0F * c.power(), false);
                c.level().sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY(0.6), e.getZ(), 2, 0.3, 0.2, 0.3, 0);
                sound(c, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.3F);
            });
        }
        return true;
    }

    static boolean taunt(SkillContext c) {
        ServerPlayer p = c.player();
        for (Mob mob : c.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(12.0), m -> isEnemy(p, m))) {
            mob.setTarget(p);
            c.level().sendParticles(ParticleTypes.ANGRY_VILLAGER, mob.getX(), mob.getY(1.1), mob.getZ(), 2, 0.2, 0.2, 0.2, 0);
        }
        effect(p, MobEffects.DAMAGE_RESISTANCE, ticks(c, 120), 1);
        sound(c, SoundEvents.BELL_BLOCK, 1.5F, 0.8F);
        return true;
    }

    static boolean shieldWall(SkillContext c) {
        effect(c.player(), MobEffects.DAMAGE_RESISTANCE, ticks(c, 100), 2);
        effect(c.player(), MobEffects.MOVEMENT_SLOWDOWN, ticks(c, 100), 0);
        ring(c, c.player().position().add(0, 1, 0), 1.2, ParticleTypes.WAX_ON, 16);
        sound(c, SoundEvents.SHIELD_BLOCK, 1.2F, 0.7F);
        return true;
    }

    static boolean ironSkin(SkillContext c) {
        effect(c.player(), MobEffects.ABSORPTION, ticks(c, 300), 3);
        effect(c.player(), MobEffects.DAMAGE_RESISTANCE, ticks(c, 300), 0);
        c.level().sendParticles(ParticleTypes.WAX_OFF, c.player().getX(), c.player().getY(1.0), c.player().getZ(), 20, 0.4, 0.6, 0.4, 0);
        sound(c, SoundEvents.ARMOR_EQUIP_IRON.value(), 1.5F, 0.7F);
        return true;
    }

    static boolean parry(SkillContext c) {
        SkillRuntime.addBuff(c.player(), Buff.PARRY, 60, c.power());
        sound(c, SoundEvents.ARMOR_EQUIP_CHAIN.value(), 1.2F, 1.5F);
        c.level().sendParticles(ParticleTypes.ENCHANTED_HIT, c.player().getX(), c.player().getY(1.0), c.player().getZ(), 12, 0.4, 0.4, 0.4, 0.1);
        return true;
    }

    static boolean flurry(SkillContext c) {
        LivingEntity target = targetInFront(c, 4.5);
        if (target == null) {
            return false;
        }
        for (int i = 0; i < 6; i++) {
            SkillRuntime.later(i * 3, () -> {
                if (target.isAlive()) {
                    hurt(c, target, 2.5F * c.power(), false);
                    c.level().sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 6, 0.2, 0.3, 0.2, 0.2);
                    sound(c, SoundEvents.PLAYER_ATTACK_WEAK, 1.0F, 1.5F);
                }
            });
        }
        return true;
    }

    static boolean tornado(SkillContext c) {
        ServerPlayer p = c.player();
        for (int i = 0; i < 20; i++) {
            double a = i * 0.7;
            c.level().sendParticles(ParticleTypes.CLOUD, p.getX() + Math.cos(a) * 1.5, p.getY() + i * 0.15, p.getZ() + Math.sin(a) * 1.5, 1, 0, 0, 0, 0);
        }
        sound(c, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.5F, 0.8F);
        for (LivingEntity e : enemiesAround(c, p.position(), 6.0)) {
            if (hurt(c, e, 5.0F * c.power(), false)) {
                push(e, p.position(), 0.3, 1.2);
            }
        }
        return true;
    }

    // =================================================================== rogue

    static boolean shadowStep(SkillContext c) {
        ServerPlayer p = c.player();
        LivingEntity target = targetInFront(c, 16.0);
        if (target == null) {
            return false;
        }
        c.level().sendParticles(SHADOW_DUST, p.getX(), p.getY(1.0), p.getZ(), 20, 0.3, 0.6, 0.3, 0);
        Vec3 behind = target.position().subtract(target.getLookAngle().multiply(1, 0, 1).normalize().scale(1.3));
        p.teleportTo(behind.x, target.getY(), behind.z);
        p.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
        hurt(c, target, 8.0F * c.power(), false);
        effect(target, MobEffects.BLINDNESS, 40, 0);
        c.level().sendParticles(SHADOW_DUST, p.getX(), p.getY(1.0), p.getZ(), 20, 0.3, 0.6, 0.3, 0);
        sound(c, SoundEvents.ENDERMAN_TELEPORT, 0.8F, 1.4F);
        return true;
    }

    static boolean smokeBomb(SkillContext c) {
        ServerPlayer p = c.player();
        effect(p, MobEffects.INVISIBILITY, ticks(c, 120), 0);
        effect(p, MobEffects.MOVEMENT_SPEED, ticks(c, 120), 1);
        for (LivingEntity e : enemiesAround(c, p.position(), 8.0)) {
            effect(e, MobEffects.BLINDNESS, 100, 0);
            effect(e, MobEffects.MOVEMENT_SLOWDOWN, 100, 1);
            if (e instanceof Mob mob && mob.getTarget() == p) {
                mob.setTarget(null);
            }
        }
        c.level().sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY(0.5), p.getZ(), 80, 2.0, 1.0, 2.0, 0.02);
        sound(c, SoundEvents.FIRECHARGE_USE, 1.0F, 0.6F);
        return true;
    }

    static boolean poisonBlade(SkillContext c) {
        SkillRuntime.addBuff(c.player(), Buff.POISON_BLADE, ticks(c, 240), c.power());
        c.level().sendParticles(ParticleTypes.ITEM_SLIME, c.player().getX(), c.player().getY(1.0), c.player().getZ(), 15, 0.4, 0.4, 0.4, 0.05);
        sound(c, SoundEvents.BREWING_STAND_BREW, 1.0F, 1.2F);
        return true;
    }

    // =================================================================== archer

    static void shootArrow(SkillContext c, Vec3 from, Vec3 direction, float speed, double damage, boolean critical) {
        Arrow arrow = new Arrow(c.level(), c.player(), new ItemStack(Items.ARROW), null);
        arrow.setPos(from.x, from.y, from.z);
        arrow.shoot(direction.x, direction.y, direction.z, speed, 1.0F);
        arrow.setBaseDamage(damage);
        arrow.setCritArrow(critical);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        c.level().addFreshEntity(arrow);
    }

    static boolean multishot(SkillContext c) {
        ServerPlayer p = c.player();
        for (int i = -3; i <= 3; i++) {
            shootArrow(c, p.getEyePosition().add(0, -0.1, 0), p.getLookAngle().yRot(i * 0.1F), 2.8F, 3.0 * c.power(), true);
        }
        sound(c, SoundEvents.CROSSBOW_SHOOT, 1.0F, 0.9F);
        return true;
    }

    static boolean arrowRain(SkillContext c) {
        Vec3 center = lookedAt(c, 32.0);
        for (int i = 0; i < 15; i++) {
            SkillRuntime.later(i * 2, () -> {
                for (int j = 0; j < 2; j++) {
                    double a = c.level().random.nextDouble() * Math.PI * 2;
                    double r = c.level().random.nextDouble() * 4.0;
                    Vec3 from = center.add(Math.cos(a) * r, 14.0, Math.sin(a) * r);
                    shootArrow(c, from, new Vec3(0, -1, 0), 2.5F, 4.0 * c.power(), true);
                }
            });
        }
        ring(c, center, 4.0, ParticleTypes.CRIT, 24);
        sound(c, SoundEvents.ARROW_SHOOT, 1.5F, 0.6F);
        return true;
    }

    static boolean piercingShot(SkillContext c) {
        ServerPlayer p = c.player();
        Vec3 from = p.getEyePosition();
        Vec3 to = lookedAt(c, 40.0);
        line(c, from.add(p.getLookAngle()), to, ParticleTypes.CRIT);
        // A beam: everything on the line is hit
        AABB area = new AABB(from, to).inflate(1.0);
        for (LivingEntity e : c.level().getEntitiesOfClass(LivingEntity.class, area, e -> isEnemy(p, e))) {
            if (e.getBoundingBox().inflate(0.4).clip(from, to).isPresent()) {
                hurt(c, e, 10.0F * c.power(), false);
            }
        }
        sound(c, SoundEvents.TRIDENT_THROW.value(), 1.2F, 1.4F);
        return true;
    }

    static boolean trap(SkillContext c) {
        ServerPlayer p = c.player();
        List<LivingEntity> enemies = enemiesAround(c, p.position(), 6.0);
        for (LivingEntity e : enemies) {
            effect(e, MobEffects.MOVEMENT_SLOWDOWN, ticks(c, 80), 5);
            hurt(c, e, 3.0F * c.power(), false);
            c.level().sendParticles(ParticleTypes.COMPOSTER, e.getX(), e.getY() + 0.2, e.getZ(), 15, 0.4, 0.2, 0.4, 0);
        }
        ring(c, p.position(), 6.0, ParticleTypes.COMPOSTER, 30);
        sound(c, SoundEvents.SWEET_BERRY_BUSH_PLACE, 1.5F, 0.6F);
        return true;
    }

    static boolean deadeye(SkillContext c) {
        SkillRuntime.addBuff(c.player(), Buff.DEADEYE, ticks(c, 200), c.power());
        effect(c.player(), MobEffects.NIGHT_VISION, ticks(c, 200), 0);
        sound(c, SoundEvents.SPYGLASS_USE, 1.5F, 0.8F);
        return true;
    }

    static boolean summonWolves(SkillContext c) {
        spawnWolves(c, 2, 30.0, 5.0, 1200);
        sound(c, SoundEvents.WOLF_HOWL, 1.5F, 1.0F);
        return true;
    }

    /** Spirit wolves fighting for the player, fading after {@code lifetime} ticks. Health and damage grow with power. */
    static List<Wolf> spawnWolves(SkillContext c, int count, double health, double damage, int lifetime) {
        ServerPlayer p = c.player();
        List<Wolf> wolves = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Wolf wolf = EntityType.WOLF.create(c.level());
            if (wolf == null) {
                continue;
            }
            double a = i * Math.PI * 2 / count;
            wolf.moveTo(p.getX() + Math.cos(a), p.getY(), p.getZ() + Math.sin(a), p.getYRot(), 0);
            wolf.tame(p);
            wolf.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.min(200.0, health * c.power()));
            wolf.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage * c.power());
            wolf.setHealth(wolf.getMaxHealth());
            c.level().addFreshEntity(wolf);
            c.level().sendParticles(ParticleTypes.POOF, wolf.getX(), wolf.getY(0.5), wolf.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
            SkillRuntime.later(lifetime, () -> {
                if (wolf.isAlive()) {
                    c.level().sendParticles(ParticleTypes.POOF, wolf.getX(), wolf.getY(0.5), wolf.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
                    wolf.discard();
                }
            });
            wolves.add(wolf);
        }
        return wolves;
    }

    // =================================================================== spells

    static boolean fireball(SkillContext c) {
        SpellProjectile.shoot(c.player(), SpellProjectile.Element.FIRE, 12.0F * c.power(), c.player().getLookAngle(), 1.8F);
        sound(c, SoundEvents.BLAZE_SHOOT, 1.0F, 0.9F);
        return true;
    }

    static boolean frostNova(SkillContext c) {
        ServerPlayer p = c.player();
        for (int r = 1; r <= 6; r++) {
            ring(c, p.position(), r, ParticleTypes.SNOWFLAKE, r * 6);
        }
        for (LivingEntity e : enemiesAround(c, p.position(), 6.0)) {
            hurt(c, e, 6.0F * c.power(), true);
            effect(e, MobEffects.MOVEMENT_SLOWDOWN, ticks(c, 80), 3);
            e.setTicksFrozen(Math.max(e.getTicksFrozen(), 200));
        }
        sound(c, SoundEvents.GLASS_BREAK, 1.2F, 0.6F);
        return true;
    }

    static boolean chainLightning(SkillContext c) {
        ServerPlayer p = c.player();
        LivingEntity current = targetInFront(c, 16.0);
        if (current == null) {
            return false;
        }
        Set<LivingEntity> hit = new HashSet<>();
        Vec3 from = p.getEyePosition();
        for (int jump = 0; jump < 5 && current != null; jump++) {
            hit.add(current);
            Vec3 to = current.getBoundingBox().getCenter();
            line(c, from, to, ParticleTypes.ELECTRIC_SPARK);
            hurt(c, current, 8.0F * c.power(), true);
            from = to;
            LivingEntity last = current;
            current = enemiesAround(c, to, 6.0).stream().filter(e -> !hit.contains(e))
                    .min(Comparator.comparingDouble(last::distanceToSqr)).orElse(null);
        }
        sound(c, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.6F, 1.6F);
        return true;
    }

    static boolean meteorStrike(SkillContext c) {
        Vec3 center = lookedAt(c, 40.0);
        sound(c, SoundEvents.BLAZE_SHOOT, 1.5F, 0.5F);
        for (int t = 0; t < 30; t += 3) {
            int tick = t;
            SkillRuntime.later(t, () -> {
                ring(c, center, 4.0, ParticleTypes.FLAME, 20);
                double h = 15.0 * (30 - tick) / 30.0;
                c.level().sendParticles(ParticleTypes.FLAME, center.x, center.y + h, center.z, 10, 0.3, 0.3, 0.3, 0.02);
                c.level().sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y + h + 0.6, center.z, 3, 0.3, 0.3, 0.3, 0);
            });
        }
        SkillRuntime.later(30, () -> {
            c.level().sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y + 0.5, center.z, 1, 0, 0, 0, 0);
            c.level().playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), c.player().getSoundSource(), 2.0F, 0.7F);
            for (LivingEntity e : enemiesAround(c, center, 4.0)) {
                if (hurt(c, e, 16.0F * c.power(), true)) {
                    e.igniteForSeconds(5);
                    push(e, center, 0.8, 0.5);
                }
            }
        });
        return true;
    }

    static boolean arcaneBarrage(SkillContext c) {
        for (int i = -2; i <= 2; i++) {
            SpellProjectile.shoot(c.player(), SpellProjectile.Element.ARCANE, 6.0F * c.power(), c.player().getLookAngle().yRot(i * 0.12F), 2.2F);
        }
        sound(c, SoundEvents.EVOKER_CAST_SPELL, 1.0F, 1.4F);
        return true;
    }

    static boolean blink(SkillContext c) {
        ServerPlayer p = c.player();
        Vec3 look = p.getLookAngle();
        for (double d = 10.0; d >= 2.0; d -= 0.5) {
            Vec3 feet = p.position().add(look.scale(d));
            AABB box = p.getBoundingBox().move(feet.subtract(p.position()));
            if (c.level().noCollision(p, box)) {
                c.level().sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY(1.0), p.getZ(), 30, 0.3, 0.6, 0.3, 0.5);
                p.teleportTo(feet.x, feet.y, feet.z);
                p.fallDistance = 0;
                c.level().sendParticles(ParticleTypes.REVERSE_PORTAL, feet.x, feet.y + 1.0, feet.z, 30, 0.3, 0.6, 0.3, 0.05);
                sound(c, SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.6F);
                return true;
            }
        }
        return false;
    }

    static boolean timeStop(SkillContext c) {
        ServerPlayer p = c.player();
        int ticks = ticks(c, 80);
        for (LivingEntity e : enemiesAround(c, p.position(), 12.0)) {
            effect(e, MobEffects.MOVEMENT_SLOWDOWN, ticks, 9);
            effect(e, MobEffects.DIG_SLOWDOWN, ticks, 9);
            effect(e, MobEffects.WEAKNESS, ticks, 9);
            if (e instanceof Mob mob) {
                mob.getNavigation().stop();
            }
            c.level().sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY(1.0), e.getZ(), 6, 0.3, 0.5, 0.3, 0);
        }
        ring(c, p.position().add(0, 1, 0), 12.0, ParticleTypes.END_ROD, 48);
        sound(c, SoundEvents.BEACON_DEACTIVATE, 2.0F, 0.5F);
        return true;
    }

    static boolean rewind(SkillContext c) {
        ServerPlayer p = c.player();
        // Other skills are ready again (this one keeps its cooldown, set after it runs)
        SkillRuntime.resetCooldowns(p);
        p.heal(p.getMaxHealth() * 0.25F);
        c.level().sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY(1.0), p.getZ(), 50, 0.5, 0.8, 0.5, 0.05);
        sound(c, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.5F, 1.2F);
        return true;
    }

    static boolean manaShield(SkillContext c) {
        int amplifier = Math.min(4, 1 + (int) (c.stats().getMana() / 25));
        effect(c.player(), MobEffects.ABSORPTION, ticks(c, 200), amplifier);
        ring(c, c.player().position().add(0, 1, 0), 1.3, ParticleTypes.GLOW, 20);
        sound(c, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5F, 1.0F);
        return true;
    }

    static boolean lifeDrain(SkillContext c) {
        ServerPlayer p = c.player();
        LivingEntity target = targetInFront(c, 16.0);
        if (target == null) {
            return false;
        }
        float amount = 8.0F * c.power();
        line(c, target.getBoundingBox().getCenter(), p.getEyePosition().add(0, -0.4, 0), ParticleTypes.DAMAGE_INDICATOR);
        if (hurt(c, target, amount, true)) {
            p.heal(amount);
        }
        sound(c, SoundEvents.WITCH_DRINK, 1.0F, 0.6F);
        return true;
    }

    static boolean curse(SkillContext c) {
        ServerPlayer p = c.player();
        for (LivingEntity e : enemiesAround(c, p.position(), 8.0)) {
            effect(e, MobEffects.WITHER, ticks(c, 160), 1);
            effect(e, MobEffects.WEAKNESS, ticks(c, 160), 1);
            c.level().sendParticles(ParticleTypes.SOUL, e.getX(), e.getY(1.0), e.getZ(), 8, 0.3, 0.4, 0.3, 0.02);
        }
        ring(c, p.position(), 8.0, ParticleTypes.WITCH, 36);
        sound(c, SoundEvents.WITHER_AMBIENT, 0.7F, 1.4F);
        return true;
    }

    static boolean runeBlade(SkillContext c) {
        SkillRuntime.addBuff(c.player(), Buff.RUNE_BLADE, ticks(c, 240), c.power());
        c.level().sendParticles(ParticleTypes.ENCHANT, c.player().getX(), c.player().getY(1.2), c.player().getZ(), 40, 0.5, 0.6, 0.5, 0.5);
        sound(c, SoundEvents.ENCHANTMENT_TABLE_USE, 1.5F, 1.0F);
        return true;
    }

    // =================================================================== holy

    static boolean healAura(SkillContext c) {
        ServerPlayer p = c.player();
        float amount = healAmount(c, 8.0F);
        for (Player ally : c.level().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(8.0))) {
            ally.heal(amount);
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            c.level().sendParticles(ParticleTypes.HEART, ally.getX(), ally.getY(1.2), ally.getZ(), 6, 0.4, 0.4, 0.4, 0);
        }
        ring(c, p.position(), 8.0, HOLY_DUST, 40);
        sound(c, SoundEvents.AMETHYST_BLOCK_CHIME, 2.0F, 1.2F);
        return true;
    }

    static float holy(LivingEntity target, float amount) {
        return target.getType().is(EntityTypeTags.UNDEAD) ? amount * 2.0F : amount;
    }

    static void pillar(SkillContext c, LivingEntity target) {
        for (int i = 0; i < 12; i++) {
            c.level().sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + i * 0.4, target.getZ(), 1, 0.05, 0, 0.05, 0);
        }
        c.level().sendParticles(HOLY_DUST, target.getX(), target.getY(0.5), target.getZ(), 15, 0.4, 0.5, 0.4, 0);
    }

    static boolean smite(SkillContext c) {
        LivingEntity target = targetInFront(c, 20.0);
        if (target == null) {
            return false;
        }
        pillar(c, target);
        hurt(c, target, holy(target, 12.0F * c.power()), true);
        sound(c, SoundEvents.TRIDENT_THUNDER.value(), 0.6F, 1.6F);
        return true;
    }

    static boolean judgment(SkillContext c) {
        List<LivingEntity> enemies = enemiesAround(c, c.player().position(), 10.0);
        if (enemies.isEmpty()) {
            return false;
        }
        for (int i = 0; i < enemies.size(); i++) {
            LivingEntity e = enemies.get(i);
            SkillRuntime.later(i * 2, () -> {
                if (e.isAlive()) {
                    pillar(c, e);
                    hurt(c, e, holy(e, 10.0F * c.power()), true);
                }
            });
        }
        sound(c, SoundEvents.BEACON_POWER_SELECT, 2.0F, 1.0F);
        return true;
    }

    static boolean divineShield(SkillContext c) {
        effect(c.player(), MobEffects.DAMAGE_RESISTANCE, ticks(c, 60), 4);
        effect(c.player(), MobEffects.GLOWING, ticks(c, 60), 0);
        ring(c, c.player().position().add(0, 1, 0), 1.2, HOLY_DUST, 24);
        sound(c, SoundEvents.BEACON_ACTIVATE, 2.0F, 1.4F);
        return true;
    }

    static boolean sanctuary(SkillContext c) {
        Vec3 center = c.player().position();
        for (int pulse = 0; pulse < 10; pulse++) {
            SkillRuntime.later(pulse * 20, () -> {
                ring(c, center, 6.0, HOLY_DUST, 36);
                for (Player ally : c.level().getEntitiesOfClass(Player.class, new AABB(center, center).inflate(6.0))) {
                    ally.heal(healAmount(c, 2.0F));
                }
                for (LivingEntity e : enemiesAround(c, center, 6.0)) {
                    hurt(c, e, holy(e, 3.0F * c.power()), true);
                }
            });
        }
        sound(c, SoundEvents.BEACON_AMBIENT, 2.0F, 1.2F);
        return true;
    }

    // =================================================================== shadows

    static boolean summonShadows(SkillContext c) {
        boolean monarch = c.stats().getPlayerClass().bonuses().shadowLifetime() == 0;
        for (int i = 0; i < 2; i++) {
            ShadowSoldier.summon(c.player(), c.stats(), c.power(), monarch ? 0 : 600);
        }
        return true;
    }

    /**
     * Shadow Extraction: the corpses of the monsters the player killed nearby (last minute) rise as shadows.
     * Each corpse may resist; it can be tried {@link Corpses#MAX_ATTEMPTS} times, like in Solo Leveling.
     */
    static boolean arise(SkillContext c) {
        ServerPlayer p = c.player();
        PlayerClass.Bonuses bonus = c.stats().getPlayerClass().bonuses();
        List<Corpses.Corpse> corpses = Corpses.take(p, 12.0, Math.max(3, bonus.shadowMax() / 2 + 1));
        if (corpses.isEmpty()) {
            p.displayClientMessage(Component.translatable("message.arcanequest.arise_none").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        boolean monarch = bonus.shadowLifetime() == 0;
        float power = bonus.shadowPower() * Math.min(1.5F, 0.8F + 0.2F * c.power());
        int raised = 0;
        p.sendSystemMessage(Component.translatable("message.arcanequest.arise").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        for (Corpses.Corpse corpse : corpses) {
            Vec3 pos = corpse.position();
            c.level().sendParticles(ParticleTypes.SCULK_SOUL, pos.x, pos.y + 0.2, pos.z, 20, 0.4, 0.1, 0.4, 0.05);
            if (p.getRandom().nextFloat() < extractionChance(corpse, monarch)) {
                ShadowSoldier.extract(p, c.stats(), pos, corpse.name(), corpse.maxHealth(), corpse.rank(), power, bonus.shadowLifetime());
                raised++;
            } else {
                Corpses.retry(corpse);
                c.level().sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y + 0.5, pos.z, 8, 0.3, 0.3, 0.3, 0.02);
            }
        }
        p.displayClientMessage(Component.translatable("message.arcanequest.arise_result", raised, corpses.size())
                .withStyle(raised > 0 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY), true);
        sound(c, SoundEvents.WARDEN_EMERGE, 1.0F, 1.2F);
        return true;
    }

    /** Weak monsters always answer the Monarch; strong ones and bosses may resist. */
    static float extractionChance(Corpses.Corpse corpse, boolean monarch) {
        float chance = monarch ? 1.1F - corpse.maxHealth() / 600.0F : 0.9F - corpse.maxHealth() / 300.0F;
        return Mth.clamp(chance, monarch ? 0.4F : 0.25F, 1.0F);
    }

    /** The shadow closest to where the player looks (else the farthest one): the player appears at its place. */
    static boolean shadowExchange(SkillContext c) {
        ServerPlayer p = c.player();
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        List<ShadowSoldier> army = ShadowSoldier.armyOf(p).stream().filter(s -> s.distanceToSqr(p) > 4.0).toList();
        ShadowSoldier aimed = army.stream()
                .max(Comparator.comparingDouble((ShadowSoldier s) -> s.getBoundingBox().getCenter().subtract(eye).normalize().dot(look)))
                .filter(s -> s.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) > 0.85)
                .orElse(army.stream().max(Comparator.comparingDouble(p::distanceToSqr)).orElse(null));
        if (aimed == null) {
            return false;
        }
        c.level().sendParticles(SHADOW_DUST, p.getX(), p.getY() + 1, p.getZ(), 25, 0.3, 0.6, 0.3, 0);
        c.level().sendParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 0.1, p.getZ(), 15, 0.4, 0.05, 0.4, 0.02);
        p.teleportTo(aimed.getX(), aimed.getY(), aimed.getZ());
        p.fallDistance = 0;
        c.level().sendParticles(SHADOW_DUST, p.getX(), p.getY() + 1, p.getZ(), 25, 0.3, 0.6, 0.3, 0);
        sound(c, SoundEvents.ENDERMAN_TELEPORT, 1.0F, 0.6F);
        return true;
    }

    static boolean monarchDomain(SkillContext c) {
        ServerPlayer p = c.player();
        int ticks = ticks(c, 300);
        effect(p, MobEffects.DAMAGE_BOOST, ticks, 1);
        effect(p, MobEffects.MOVEMENT_SPEED, ticks, 1);
        for (ShadowSoldier shadow : ShadowSoldier.armyOf(p)) {
            shadow.setHealth(shadow.getMaxHealth());
            effect(shadow, MobEffects.DAMAGE_BOOST, ticks, 2);
            effect(shadow, MobEffects.MOVEMENT_SPEED, ticks, 1);
            effect(shadow, MobEffects.DAMAGE_RESISTANCE, ticks, 1);
            effect(shadow, MobEffects.REGENERATION, ticks, 1);
        }
        SkillRuntime.addBuff(p, Buff.DOMAIN, ticks, c.power());
        for (int r = 2; r <= 12; r += 2) {
            ring(c, p.position(), r, SHADOW_DUST, r * 5);
        }
        for (LivingEntity e : enemiesAround(c, p.position(), 12.0)) {
            effect(e, MobEffects.DARKNESS, ticks, 0);
            effect(e, MobEffects.WEAKNESS, ticks, 1);
        }
        sound(c, SoundEvents.WARDEN_ROAR, 1.0F, 0.8F);
        return true;
    }

    // =================================================================== fighters (new)

    /** A wide swing; ×2 on weakened enemies, each kill heals. */
    static boolean reapingCleave(SkillContext c) {
        ServerPlayer p = c.player();
        Vec3 look = flatLook(p);
        for (int i = -2; i <= 2; i++) {
            Vec3 pos = p.position().add(look.yRot(i * 0.4F).scale(2.5));
            c.level().sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, p.getY(0.6), pos.z, 1, 0, 0, 0, 0);
        }
        for (LivingEntity e : inCone(c, 5.0, 0.5)) {
            boolean weak = e.getHealth() < e.getMaxHealth() * 0.3F;
            if (hurt(c, e, 9.0F * c.power() * (weak ? 2.0F : 1.0F), false) && !e.isAlive()) {
                p.heal(4.0F);
            }
        }
        sound(c, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.6F);
        return true;
    }

    /** Ultimate: marks every enemy around; 3 s later the sentence falls, heavier on the wounded. */
    static boolean deathSentence(SkillContext c) {
        List<LivingEntity> enemies = enemiesAround(c, c.player().position(), 12.0);
        if (enemies.isEmpty()) {
            return false;
        }
        for (LivingEntity e : enemies) {
            effect(e, MobEffects.GLOWING, 60, 0);
            effect(e, MobEffects.MOVEMENT_SLOWDOWN, 60, 2);
            c.level().sendParticles(ParticleTypes.SOUL, e.getX(), e.getY(1.2), e.getZ(), 8, 0.2, 0.3, 0.2, 0.01);
        }
        sound(c, SoundEvents.BELL_RESONATE, 2.0F, 0.5F);
        SkillRuntime.later(60, () -> {
            for (LivingEntity e : enemies) {
                if (!e.isAlive()) {
                    continue;
                }
                // Bosses don't take the "missing health" part
                float missing = e instanceof ArcaneBoss ? 0 : (e.getMaxHealth() - e.getHealth()) * 0.4F;
                hurt(c, e, 20.0F * c.power() + missing, false);
                for (int i = 0; i < 10; i++) {
                    c.level().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, e.getX(), e.getY() + 3.0 - i * 0.3, e.getZ(), 1, 0.05, 0, 0.05, 0);
                }
            }
            sound(c, SoundEvents.PLAYER_ATTACK_CRIT, 1.5F, 0.5F);
        });
        return true;
    }

    /** Ultimate: leaps where the player looks and crashes down in three shockwaves that terrify. */
    static boolean cataclysm(SkillContext c) {
        ServerPlayer p = c.player();
        Vec3 target = lookedAt(c, 16.0);
        Vec3 jump = target.subtract(p.position()).multiply(1, 0, 1).scale(0.12);
        p.setDeltaMovement(jump.x, 1.1, jump.z);
        p.hurtMarked = true;
        effect(p, MobEffects.DAMAGE_RESISTANCE, 40, 2);
        // The landing is the attack, not a fall
        temporary(p, Attributes.SAFE_FALL_DISTANCE, "cataclysm_fall", 32.0, AttributeModifier.Operation.ADD_VALUE, 40);
        sound(c, SoundEvents.RAVAGER_ROAR, 1.5F, 0.6F);
        SkillRuntime.later(14, () -> {
            p.setDeltaMovement(0, -2.5, 0);
            p.hurtMarked = true;
        });
        SkillRuntime.later(20, () -> {
            p.fallDistance = 0;
            Vec3 center = p.position();
            float[] damage = {14.0F, 8.0F, 5.0F};
            for (int wave = 0; wave < 3; wave++) {
                int w = wave;
                SkillRuntime.later(wave * 6, () -> {
                    double radius = 4.0 + w * 3.0;
                    ring(c, center, radius, ParticleTypes.EXPLOSION, 16);
                    ring(c, center, radius, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), 30);
                    c.level().playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), p.getSoundSource(), 1.5F, 0.5F);
                    for (LivingEntity e : enemiesAround(c, center, radius)) {
                        if (hurt(c, e, damage[w] * c.power(), false)) {
                            push(e, center, 0.6, 0.7);
                            if (w == 0) {
                                frighten(c, e, 60);
                            }
                        }
                    }
                });
            }
        });
        return true;
    }

    // =================================================================== tanks (new)

    static boolean shieldBash(SkillContext c) {
        ServerPlayer p = c.player();
        LivingEntity target = targetInFront(c, 4.0);
        if (target == null) {
            return false;
        }
        if (hurt(c, target, 6.0F * c.power(), false)) {
            push(target, p.position(), 1.2, 0.3);
        }
        // Stunned for a moment, and angry at the guardian
        effect(target, MobEffects.MOVEMENT_SLOWDOWN, 30, 9);
        effect(target, MobEffects.WEAKNESS, 40, 1);
        if (target instanceof Mob mob) {
            mob.setTarget(p);
        }
        c.level().sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.8), target.getZ(), 15, 0.3, 0.3, 0.3, 0.2);
        sound(c, SoundEvents.SHIELD_BLOCK, 1.5F, 0.6F);
        return true;
    }

    /** Taunts every monster and shields the nearby players. */
    static boolean bulwark(SkillContext c) {
        ServerPlayer p = c.player();
        for (Mob mob : c.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(12.0), m -> isEnemy(p, m))) {
            mob.setTarget(p);
        }
        for (Player ally : c.level().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(8.0))) {
            effect(ally, MobEffects.DAMAGE_RESISTANCE, ticks(c, 120), 1);
            effect(ally, MobEffects.ABSORPTION, ticks(c, 200), 1);
            c.level().sendParticles(ParticleTypes.WAX_ON, ally.getX(), ally.getY(1.0), ally.getZ(), 10, 0.4, 0.5, 0.4, 0);
        }
        ring(c, p.position().add(0, 0.2, 0), 8.0, ParticleTypes.WAX_ON, 40);
        sound(c, SoundEvents.ANVIL_PLACE, 1.0F, 0.6F);
        return true;
    }

    /** Ultimate, Thomas Andre's Goliath: the player grows into a giant, tougher and heavier. */
    static boolean goliath(SkillContext c) {
        ServerPlayer p = c.player();
        int ticks = ticks(c, 300);
        temporary(p, Attributes.SCALE, "goliath_scale", 0.6, AttributeModifier.Operation.ADD_VALUE, ticks);
        temporary(p, Attributes.MAX_HEALTH, "goliath_health", 20.0, AttributeModifier.Operation.ADD_VALUE, ticks);
        temporary(p, Attributes.ARMOR, "goliath_armor", 8.0, AttributeModifier.Operation.ADD_VALUE, ticks);
        temporary(p, Attributes.KNOCKBACK_RESISTANCE, "goliath_knockback", 1.0, AttributeModifier.Operation.ADD_VALUE, ticks);
        temporary(p, Attributes.ATTACK_DAMAGE, "goliath_damage", 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, ticks);
        temporary(p, Attributes.ENTITY_INTERACTION_RANGE, "goliath_reach", 1.5, AttributeModifier.Operation.ADD_VALUE, ticks);
        p.heal(20.0F);
        c.level().sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY(), p.getZ(), 4, 1.0, 0.2, 1.0, 0);
        ring(c, p.position(), 3.0, ParticleTypes.CAMPFIRE_COSY_SMOKE, 24);
        sound(c, SoundEvents.IRON_GOLEM_REPAIR, 2.0F, 0.5F);
        sound(c, SoundEvents.RAVAGER_ROAR, 1.0F, 0.5F);
        return true;
    }

    /** Glyph on the ground: allies inside resist, enemies are slowed and burnt by the runes. */
    static boolean runicWard(SkillContext c) {
        Vec3 center = c.player().position();
        for (int pulse = 0; pulse < 8; pulse++) {
            SkillRuntime.later(pulse * 20, () -> {
                ring(c, center, 6.0, ParticleTypes.ENCHANT, 40);
                ring(c, center, 3.0, ParticleTypes.GLOW, 16);
                for (Player ally : c.level().getEntitiesOfClass(Player.class, new AABB(center, center).inflate(6.0))) {
                    effect(ally, MobEffects.DAMAGE_RESISTANCE, 30, 0);
                }
                for (LivingEntity e : enemiesAround(c, center, 6.0)) {
                    effect(e, MobEffects.MOVEMENT_SLOWDOWN, 30, 1);
                    hurt(c, e, 2.0F * c.power(), true);
                }
            });
        }
        sound(c, SoundEvents.ENCHANTMENT_TABLE_USE, 2.0F, 0.6F);
        return true;
    }

    /** Ultimate: everything that hits the player is sent back to the attacker. */
    static boolean aegis(SkillContext c) {
        ServerPlayer p = c.player();
        int ticks = ticks(c, 160);
        SkillRuntime.addBuff(p, Buff.AEGIS, ticks, c.power());
        effect(p, MobEffects.DAMAGE_RESISTANCE, ticks, 1);
        for (int r = 1; r <= 3; r++) {
            ring(c, p.position().add(0, r * 0.6, 0), 1.3, ParticleTypes.END_ROD, 16);
        }
        sound(c, SoundEvents.BEACON_ACTIVATE, 2.0F, 0.8F);
        return true;
    }

    // =================================================================== assassins (new)

    /** Sung Jinwoo's Mutilation: three strikes at the vital points, critical, then bleeding. */
    static boolean mutilation(SkillContext c) {
        LivingEntity target = targetInFront(c, 4.5);
        if (target == null) {
            return false;
        }
        float classCrit = c.stats().getPlayerClass().bonuses().crit();
        float crit = classCrit > 0 ? classCrit : 1.5F;
        for (int i = 0; i < 3; i++) {
            SkillRuntime.later(i * 4, () -> {
                if (target.isAlive()) {
                    hurt(c, target, 4.0F * c.power() * crit, false);
                    c.level().sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 10, 0.2, 0.3, 0.2, 0.3);
                    c.level().sendParticles(BLOOD_DUST, target.getX(), target.getY(0.6), target.getZ(), 6, 0.2, 0.3, 0.2, 0);
                    sound(c, SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 1.4F);
                }
            });
        }
        for (int i = 1; i <= 4; i++) {
            SkillRuntime.later(10 + i * 20, () -> {
                if (target.isAlive()) {
                    hurt(c, target, 1.0F * c.power(), false);
                    c.level().sendParticles(BLOOD_DUST, target.getX(), target.getY(0.5), target.getZ(), 4, 0.2, 0.2, 0.2, 0);
                }
            });
        }
        return true;
    }

    /** Sung Jinwoo's Stealth: unseen and forgotten; the first hit is an ambush (×2). */
    static boolean stealth(SkillContext c) {
        ServerPlayer p = c.player();
        int ticks = ticks(c, 160);
        effect(p, MobEffects.INVISIBILITY, ticks, 0);
        effect(p, MobEffects.MOVEMENT_SPEED, ticks, 0);
        SkillRuntime.addBuff(p, Buff.STEALTH, ticks, c.power());
        for (Mob mob : c.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(16.0), m -> m.getTarget() == p)) {
            mob.setTarget(null);
        }
        c.level().sendParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY(0.5), p.getZ(), 25, 0.4, 0.6, 0.4, 0.02);
        sound(c, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 1.0F);
        return true;
    }

    /** Sung Jinwoo's Dagger Rush: daggers strike the target from every direction. */
    static boolean daggerRush(SkillContext c) {
        LivingEntity target = targetInFront(c, 14.0);
        if (target == null) {
            return false;
        }
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            SkillRuntime.later(i * 2, () -> {
                if (!target.isAlive()) {
                    return;
                }
                Vec3 to = target.getBoundingBox().getCenter();
                Vec3 from = to.add(Math.cos(a) * 3.0, 1.0, Math.sin(a) * 3.0);
                line(c, from, to, ParticleTypes.CRIT);
                hurt(c, target, 3.0F * c.power(), false);
                sound(c, SoundEvents.TRIDENT_HIT, 0.6F, 1.6F);
            });
        }
        return true;
    }

    static boolean shurikenStorm(SkillContext c) {
        ServerPlayer p = c.player();
        for (int i = -4; i <= 4; i++) {
            shootArrow(c, p.getEyePosition().add(0, -0.1, 0), p.getLookAngle().yRot(i * 0.12F), 3.2F, 2.0 * c.power(), false);
        }
        sound(c, SoundEvents.TRIDENT_THROW.value(), 1.0F, 1.8F);
        return true;
    }

    /** Ultimate: untouchable, the ninja flashes between the enemies around, then comes back. */
    static boolean thousandCuts(SkillContext c) {
        ServerPlayer p = c.player();
        List<LivingEntity> targets = new ArrayList<>(enemiesAround(c, p.position(), 10.0));
        if (targets.isEmpty()) {
            return false;
        }
        targets.sort(Comparator.comparingDouble(p::distanceToSqr));
        Vec3 start = p.position();
        effect(p, MobEffects.DAMAGE_RESISTANCE, 50, 4);
        for (int i = 0; i < 12; i++) {
            LivingEntity e = targets.get(i % Math.min(8, targets.size()));
            SkillRuntime.later(i * 3, () -> {
                if (!e.isAlive()) {
                    return;
                }
                Vec3 behind = e.position().subtract(e.getLookAngle().multiply(1, 0, 1).normalize().scale(1.2));
                c.level().sendParticles(SHADOW_DUST, p.getX(), p.getY(1.0), p.getZ(), 6, 0.2, 0.4, 0.2, 0);
                p.teleportTo(behind.x, e.getY(), behind.z);
                hurt(c, e, 5.0F * c.power(), false);
                c.level().sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY(0.6), e.getZ(), 1, 0, 0, 0, 0);
                sound(c, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.6F);
            });
        }
        SkillRuntime.later(38, () -> {
            p.teleportTo(start.x, start.y, start.z);
            p.fallDistance = 0;
            c.level().sendParticles(SHADOW_DUST, p.getX(), p.getY(1.0), p.getZ(), 20, 0.3, 0.6, 0.3, 0);
        });
        return true;
    }

    /** Ultimate: a single killing blow from behind, doubled from invisibility; a kill hides the assassin again. */
    static boolean assassination(SkillContext c) {
        ServerPlayer p = c.player();
        LivingEntity target = targetInFront(c, 24.0);
        if (target == null) {
            return false;
        }
        boolean hidden = p.isInvisible() || SkillRuntime.buff(p, Buff.STEALTH) > 0;
        SkillRuntime.removeBuff(p, Buff.STEALTH);
        c.level().sendParticles(SHADOW_DUST, p.getX(), p.getY(1.0), p.getZ(), 20, 0.3, 0.6, 0.3, 0);
        Vec3 behind = target.position().subtract(target.getLookAngle().multiply(1, 0, 1).normalize().scale(1.3));
        p.teleportTo(behind.x, target.getY(), behind.z);
        p.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
        hurt(c, target, 30.0F * c.power() * (hidden ? 2.0F : 1.0F), false);
        c.level().sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY(0.6), target.getZ(), 30, 0.3, 0.5, 0.3, 0.1);
        if (!target.isAlive()) {
            effect(p, MobEffects.INVISIBILITY, 100, 0);
            SkillRuntime.addBuff(p, Buff.STEALTH, 100, c.power());
        }
        sound(c, SoundEvents.PLAYER_ATTACK_CRIT, 1.5F, 0.5F);
        return true;
    }

    // =================================================================== mages (new)

    /** Choi Jong-In's Flame Spear: a lance of fire through everything in line. */
    static boolean flameSpear(SkillContext c) {
        ServerPlayer p = c.player();
        Vec3 from = p.getEyePosition();
        Vec3 to = lookedAt(c, 30.0);
        line(c, from.add(p.getLookAngle()), to, ParticleTypes.FLAME);
        line(c, from.add(p.getLookAngle()), to, ParticleTypes.SMALL_FLAME);
        for (LivingEntity e : onLine(c, from, to, 0.6)) {
            if (hurt(c, e, 14.0F * c.power(), true)) {
                e.igniteForSeconds(6);
            }
        }
        c.level().sendParticles(ParticleTypes.EXPLOSION, to.x, to.y, to.z, 1, 0, 0, 0, 0);
        sound(c, SoundEvents.BLAZE_SHOOT, 1.2F, 0.6F);
        sound(c, SoundEvents.FIRECHARGE_USE, 1.0F, 0.8F);
        return true;
    }

    /** Ultimate: fire rains on the area for three seconds. */
    static boolean inferno(SkillContext c) {
        Vec3 center = lookedAt(c, 40.0);
        ring(c, center, 6.0, ParticleTypes.FLAME, 40);
        for (int i = 0; i < 12; i++) {
            SkillRuntime.later(i * 5, () -> {
                double a = c.level().random.nextDouble() * Math.PI * 2;
                double r = c.level().random.nextDouble() * 6.0;
                Vec3 hit = center.add(Math.cos(a) * r, 0, Math.sin(a) * r);
                line(c, hit.add(0, 10, 0), hit, ParticleTypes.FLAME);
                c.level().sendParticles(ParticleTypes.EXPLOSION, hit.x, hit.y + 0.5, hit.z, 1, 0, 0, 0, 0);
                c.level().playSound(null, hit.x, hit.y, hit.z, SoundEvents.GENERIC_EXPLODE.value(), c.player().getSoundSource(), 1.0F, 1.0F);
                for (LivingEntity e : enemiesAround(c, hit, 3.0)) {
                    if (hurt(c, e, 10.0F * c.power(), true)) {
                        e.igniteForSeconds(4);
                    }
                }
            });
        }
        sound(c, SoundEvents.BLAZE_AMBIENT, 2.0F, 0.5F);
        return true;
    }

    /** The Frost Monarch's ice: spikes burst out of the ground in a line, freezing what they pierce. */
    static boolean glacialSpikes(SkillContext c) {
        ServerPlayer p = c.player();
        Vec3 dir = flatLook(p);
        Vec3 start = p.position();
        Set<LivingEntity> hit = new HashSet<>();
        BlockParticleOption ice = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState());
        for (int i = 1; i <= 14; i++) {
            Vec3 pos = start.add(dir.scale(i));
            SkillRuntime.later(i, () -> {
                c.level().sendParticles(ice, pos.x, pos.y + 0.5, pos.z, 12, 0.3, 0.6, 0.3, 0.1);
                c.level().sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y + 1.0, pos.z, 4, 0.2, 0.5, 0.2, 0.02);
                for (LivingEntity e : enemiesAround(c, pos, 1.6)) {
                    if (hit.add(e) && hurt(c, e, 10.0F * c.power(), true)) {
                        effect(e, MobEffects.MOVEMENT_SLOWDOWN, 60, 4);
                        e.setTicksFrozen(Math.max(e.getTicksFrozen(), 300));
                        e.push(0, 0.5, 0);
                        e.hurtMarked = true;
                    }
                }
            });
        }
        sound(c, SoundEvents.GLASS_BREAK, 1.5F, 0.5F);
        return true;
    }

    /** Ultimate, the white flames of Baran, Monarch of White Flames: lightning falls on every enemy, five times. */
    static boolean whiteFlames(SkillContext c) {
        ServerPlayer p = c.player();
        if (enemiesAround(c, p.position(), 16.0).isEmpty()) {
            return false;
        }
        for (int wave = 0; wave < 5; wave++) {
            SkillRuntime.later(wave * 10, () -> {
                List<LivingEntity> targets = new ArrayList<>(enemiesAround(c, p.position(), 16.0));
                targets.sort(Comparator.comparingDouble(p::distanceToSqr));
                for (LivingEntity e : targets.subList(0, Math.min(6, targets.size()))) {
                    lightning(c, e.position());
                    c.level().sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY(0.5), e.getZ(), 10, 0.3, 0.6, 0.3, 0.05);
                    if (hurt(c, e, 12.0F * c.power(), true)) {
                        e.igniteForSeconds(3);
                    }
                }
            });
        }
        return true;
    }

    static boolean arcaneSlash(SkillContext c) {
        ServerPlayer p = c.player();
        Vec3 look = flatLook(p);
        for (int i = -4; i <= 4; i++) {
            Vec3 pos = p.position().add(look.yRot(i * 0.2F).scale(3.5));
            c.level().sendParticles(ARCANE_DUST, pos.x, p.getY(0.6), pos.z, 2, 0.1, 0.1, 0.1, 0);
            c.level().sendParticles(ParticleTypes.WITCH, pos.x, p.getY(0.6), pos.z, 1, 0.1, 0.1, 0.1, 0);
        }
        for (LivingEntity e : inCone(c, 6.0, 0.6)) {
            if (hurt(c, e, 9.0F * c.power(), true)) {
                effect(e, MobEffects.MOVEMENT_SLOWDOWN, 40, 1);
            }
        }
        sound(c, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.4F);
        sound(c, SoundEvents.EVOKER_CAST_SPELL, 0.8F, 1.6F);
        return true;
    }

    /** Ultimate, the Demon King Baran's lightning: three bolts fall along the blade's path, then the runes stay. */
    static boolean thunderBlade(SkillContext c) {
        ServerPlayer p = c.player();
        Vec3 dir = flatLook(p);
        Vec3 start = p.position();
        for (int i = 1; i <= 3; i++) {
            Vec3 pos = start.add(dir.scale(4.0 * i));
            SkillRuntime.later(i * 5, () -> {
                lightning(c, pos);
                ring(c, pos, 2.5, ParticleTypes.ELECTRIC_SPARK, 16);
                for (LivingEntity e : enemiesAround(c, pos, 2.5)) {
                    hurt(c, e, 16.0F * c.power(), true);
                }
            });
        }
        SkillRuntime.addBuff(p, Buff.RUNE_BLADE, ticks(c, 200), c.power());
        sound(c, SoundEvents.TRIDENT_THUNDER.value(), 1.0F, 1.0F);
        return true;
    }

    /** Ultimate: a quarter of the warlock's life for fifteen seconds of devastating, life stealing spells. */
    static boolean demonicPact(SkillContext c) {
        ServerPlayer p = c.player();
        p.setHealth(Math.max(1.0F, p.getHealth() * 0.75F));
        SkillRuntime.addBuff(p, Buff.PACT, ticks(c, 300), c.power());
        c.level().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY(1.0), p.getZ(), 40, 0.5, 0.8, 0.5, 0.05);
        ring(c, p.position(), 2.0, ParticleTypes.SOUL, 20);
        sound(c, SoundEvents.WITHER_SPAWN, 0.5F, 1.5F);
        return true;
    }

    /** Space folds around a point: the enemies are pulled in, then crushed. */
    static boolean gravityWell(SkillContext c) {
        Vec3 center = lookedAt(c, 24.0);
        for (int i = 0; i < 12; i++) {
            int pulse = i;
            SkillRuntime.later(i * 5, () -> {
                ring(c, center, 8.0 - pulse * 0.5, ParticleTypes.PORTAL, 24);
                for (LivingEntity e : enemiesAround(c, center, 8.0)) {
                    push(e, center, -0.35, 0.05);
                    hurt(c, e, 1.5F * c.power(), true);
                }
            });
        }
        SkillRuntime.later(62, () -> {
            c.level().sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y + 0.5, center.z, 1, 0, 0, 0, 0);
            c.level().playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), c.player().getSoundSource(), 1.5F, 1.4F);
            for (LivingEntity e : enemiesAround(c, center, 4.0)) {
                hurt(c, e, 8.0F * c.power(), true);
            }
        });
        sound(c, SoundEvents.PORTAL_TRIGGER, 0.6F, 1.8F);
        return true;
    }

    /** Ultimate: time collapses; the frozen enemies take every blow at once when it starts again. */
    static boolean temporalCollapse(SkillContext c) {
        ServerPlayer p = c.player();
        List<LivingEntity> enemies = enemiesAround(c, p.position(), 16.0);
        if (enemies.isEmpty()) {
            return false;
        }
        int ticks = 100;
        for (LivingEntity e : enemies) {
            effect(e, MobEffects.MOVEMENT_SLOWDOWN, ticks, 9);
            effect(e, MobEffects.DIG_SLOWDOWN, ticks, 9);
            effect(e, MobEffects.WEAKNESS, ticks, 9);
            if (e instanceof Mob mob) {
                mob.getNavigation().stop();
            }
        }
        for (int r = 4; r <= 16; r += 4) {
            ring(c, p.position().add(0, 1, 0), r, ParticleTypes.END_ROD, r * 4);
        }
        sound(c, SoundEvents.BEACON_DEACTIVATE, 2.0F, 0.3F);
        SkillRuntime.later(ticks, () -> {
            for (LivingEntity e : enemies) {
                if (e.isAlive()) {
                    c.level().sendParticles(ParticleTypes.REVERSE_PORTAL, e.getX(), e.getY(0.5), e.getZ(), 20, 0.3, 0.5, 0.3, 0.1);
                    hurt(c, e, 25.0F * c.power(), true);
                }
            }
            sound(c, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), 2.0F, 0.8F);
        });
        return true;
    }

    /** Ultimate, Esil Radiru's battle magic: faster, stronger, and every hit bursts with arcane force. */
    static boolean arcaneAvatar(SkillContext c) {
        ServerPlayer p = c.player();
        int ticks = ticks(c, 300);
        effect(p, MobEffects.MOVEMENT_SPEED, ticks, 1);
        effect(p, MobEffects.DAMAGE_BOOST, ticks, 0);
        effect(p, MobEffects.DAMAGE_RESISTANCE, ticks, 0);
        SkillRuntime.addBuff(p, Buff.AVATAR, ticks, c.power());
        c.level().sendParticles(ARCANE_DUST, p.getX(), p.getY(1.0), p.getZ(), 40, 0.5, 0.8, 0.5, 0);
        sound(c, SoundEvents.EVOKER_PREPARE_SUMMON, 1.5F, 1.2F);
        return true;
    }

    // =================================================================== paladins and healers (new)

    /** A holy banner: allies around fight stronger and heal, enemies burn. */
    static boolean holyBanner(SkillContext c) {
        Vec3 center = c.player().position();
        for (int pulse = 0; pulse < 10; pulse++) {
            SkillRuntime.later(pulse * 20, () -> {
                ring(c, center, 8.0, HOLY_DUST, 36);
                for (int i = 0; i < 8; i++) {
                    c.level().sendParticles(ParticleTypes.END_ROD, center.x, center.y + i * 0.4, center.z, 1, 0, 0, 0, 0);
                }
                for (Player ally : c.level().getEntitiesOfClass(Player.class, new AABB(center, center).inflate(8.0))) {
                    effect(ally, MobEffects.DAMAGE_BOOST, 30, 0);
                    effect(ally, MobEffects.REGENERATION, 30, 0);
                }
                for (LivingEntity e : enemiesAround(c, center, 8.0)) {
                    hurt(c, e, holy(e, 2.0F * c.power()), true);
                }
            });
        }
        sound(c, SoundEvents.ARMOR_EQUIP_GOLD.value(), 1.5F, 0.8F);
        return true;
    }

    /** Ultimate: the crusader and every ally around rush into battle; the crusader's hits smite. */
    static boolean crusade(SkillContext c) {
        ServerPlayer p = c.player();
        int ticks = ticks(c, 300);
        for (Player ally : c.level().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(12.0))) {
            effect(ally, MobEffects.DAMAGE_BOOST, ticks, 1);
            effect(ally, MobEffects.DAMAGE_RESISTANCE, ticks, 0);
            effect(ally, MobEffects.MOVEMENT_SPEED, ticks, 0);
            c.level().sendParticles(HOLY_DUST, ally.getX(), ally.getY(1.0), ally.getZ(), 15, 0.4, 0.6, 0.4, 0);
        }
        SkillRuntime.addBuff(p, Buff.CRUSADE, ticks, c.power());
        ring(c, p.position(), 12.0, HOLY_DUST, 60);
        sound(c, SoundEvents.RAID_HORN.value(), 1.0F, 1.2F);
        return true;
    }

    /** Ultimate, the light of the Rulers: columns of light strike every enemy, the allies are healed and shielded. */
    static boolean rulersLight(SkillContext c) {
        ServerPlayer p = c.player();
        for (LivingEntity e : enemiesAround(c, p.position(), 12.0)) {
            pillar(c, e);
            hurt(c, e, holy(e, 16.0F * c.power()), true);
            effect(e, MobEffects.BLINDNESS, 60, 0);
        }
        for (Player ally : c.level().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(12.0))) {
            ally.heal(healAmount(c, 6.0F));
            effect(ally, MobEffects.ABSORPTION, 200, 1);
        }
        for (int i = 0; i < 30; i++) {
            c.level().sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + i * 0.5, p.getZ(), 2, 0.2, 0, 0.2, 0);
        }
        ring(c, p.position(), 12.0, HOLY_DUST, 60);
        sound(c, SoundEvents.BEACON_POWER_SELECT, 2.0F, 0.6F);
        sound(c, SoundEvents.TRIDENT_THUNDER.value(), 0.8F, 1.6F);
        return true;
    }

    /** Lee Joohee's care: harmful effects are washed away from the allies, undead are burnt. */
    static boolean purify(SkillContext c) {
        ServerPlayer p = c.player();
        for (Player ally : c.level().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(10.0))) {
            for (MobEffectInstance active : new ArrayList<>(ally.getActiveEffects())) {
                if (active.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                    ally.removeEffect(active.getEffect());
                }
            }
            ally.clearFire();
            ally.heal(healAmount(c, 4.0F));
            effect(ally, MobEffects.DAMAGE_RESISTANCE, 100, 0);
            c.level().sendParticles(ParticleTypes.END_ROD, ally.getX(), ally.getY(1.0), ally.getZ(), 12, 0.4, 0.6, 0.4, 0.02);
        }
        for (LivingEntity e : enemiesAround(c, p.position(), 10.0)) {
            if (e.getType().is(EntityTypeTags.UNDEAD)) {
                pillar(c, e);
                hurt(c, e, holy(e, 6.0F * c.power()), true);
            }
        }
        ring(c, p.position(), 10.0, ParticleTypes.GLOW, 40);
        sound(c, SoundEvents.AMETHYST_BLOCK_CHIME, 2.0F, 1.6F);
        return true;
    }

    /** Ultimate, Min Byung-Gu's blessing: allies are healed, and survive the next deadly blow. */
    static boolean savingGrace(SkillContext c) {
        ServerPlayer p = c.player();
        for (Player ally : c.level().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(16.0))) {
            ally.heal(healAmount(c, 10.0F));
            SkillRuntime.addBuff(ally, Buff.GRACE, ticks(c, 400), c.power());
            c.level().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, ally.getX(), ally.getY(1.0), ally.getZ(), 30, 0.4, 0.6, 0.4, 0.2);
        }
        ring(c, p.position(), 16.0, HOLY_DUST, 64);
        sound(c, SoundEvents.BEACON_ACTIVATE, 2.0F, 1.6F);
        return true;
    }

    /** Ultimate, Tusk the shaman: a sacred grove heals the allies and chokes the enemies. */
    static boolean verdantGrove(SkillContext c) {
        Vec3 center = c.player().position();
        for (int pulse = 0; pulse < 10; pulse++) {
            SkillRuntime.later(pulse * 20, () -> {
                ring(c, center, 7.0, ParticleTypes.HAPPY_VILLAGER, 40);
                ring(c, center, 4.0, ParticleTypes.SPORE_BLOSSOM_AIR, 16);
                for (Player ally : c.level().getEntitiesOfClass(Player.class, new AABB(center, center).inflate(7.0))) {
                    ally.heal(healAmount(c, 2.0F));
                    effect(ally, MobEffects.REGENERATION, 40, 0);
                }
                for (LivingEntity e : enemiesAround(c, center, 7.0)) {
                    effect(e, MobEffects.POISON, 40, 0);
                    effect(e, MobEffects.MOVEMENT_SLOWDOWN, 40, 2);
                    hurt(c, e, 2.0F * c.power(), true);
                }
            });
        }
        sound(c, SoundEvents.AZALEA_LEAVES_PLACE, 2.0F, 0.6F);
        return true;
    }

    // =================================================================== duelists (new)

    /** Cha Hae-In's Sword Dance: five flowing slashes while gliding forward. */
    static boolean swordDance(SkillContext c) {
        ServerPlayer p = c.player();
        effect(p, MobEffects.DAMAGE_RESISTANCE, 24, 1);
        for (int i = 0; i < 5; i++) {
            SkillRuntime.later(i * 4, () -> {
                Vec3 dir = flatLook(p);
                p.setDeltaMovement(dir.x * 0.6, p.getDeltaMovement().y, dir.z * 0.6);
                p.hurtMarked = true;
                ring(c, p.position().add(0, 0.9, 0), 2.0, ParticleTypes.SWEEP_ATTACK, 4);
                c.level().sendParticles(ParticleTypes.CHERRY_LEAVES, p.getX(), p.getY(1.0), p.getZ(), 8, 0.8, 0.5, 0.8, 0);
                for (LivingEntity e : enemiesAround(c, p.position(), 3.0)) {
                    hurt(c, e, 5.0F * c.power(), false);
                }
                sound(c, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9F, 1.5F);
            });
        }
        return true;
    }

    /** A wave of sword energy that cuts through a line of enemies. */
    static boolean swordWave(SkillContext c) {
        ServerPlayer p = c.player();
        Vec3 from = p.position().add(0, 1.0, 0);
        Vec3 to = from.add(flatLook(p).scale(12.0));
        for (int i = 1; i <= 12; i += 2) {
            Vec3 pos = from.add(flatLook(p).scale(i));
            c.level().sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        }
        for (LivingEntity e : onLine(c, from, to, 1.5)) {
            hurt(c, e, 10.0F * c.power(), false);
        }
        sound(c, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.7F);
        return true;
    }

    /** Ultimate, Liu Zhigang's sword: one second to gather, then a cut that splits everything in front. */
    static boolean heavenSplitter(SkillContext c) {
        ServerPlayer p = c.player();
        effect(p, MobEffects.MOVEMENT_SLOWDOWN, 20, 3);
        c.level().sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY(1.0), p.getZ(), 40, 0.5, 0.8, 0.5, 0.5);
        sound(c, SoundEvents.ENCHANTMENT_TABLE_USE, 1.5F, 0.5F);
        SkillRuntime.later(20, () -> {
            Vec3 dir = flatLook(p);
            Vec3 from = p.position().add(0, 1.0, 0);
            Vec3 to = from.add(dir.scale(24.0));
            for (int i = 1; i <= 24; i++) {
                Vec3 pos = from.add(dir.scale(i));
                c.level().sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
                c.level().sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 1.0, pos.z, 2, 0.05, 1.0, 0.05, 0);
            }
            for (LivingEntity e : onLine(c, from, to, 2.0)) {
                if (hurt(c, e, 30.0F * c.power(), false)) {
                    push(e, p.position(), 1.5, 0.4);
                }
            }
            sound(c, SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.4F);
            sound(c, SoundEvents.TRIDENT_THUNDER.value(), 0.6F, 1.4F);
        });
        return true;
    }

    /** Ultimate: the windblade becomes a storm, cutting and pulling everything around; half of the attacks miss. */
    static boolean tempestDance(SkillContext c) {
        ServerPlayer p = c.player();
        int ticks = ticks(c, 100);
        effect(p, MobEffects.MOVEMENT_SPEED, ticks, 2);
        SkillRuntime.addBuff(p, Buff.TEMPEST, ticks, c.power());
        for (int t = 0; t < ticks; t += 10) {
            SkillRuntime.later(t, () -> {
                if (!p.isAlive()) {
                    return;
                }
                ring(c, p.position().add(0, 0.8, 0), 3.5, ParticleTypes.CLOUD, 16);
                ring(c, p.position().add(0, 1.2, 0), 3.0, ParticleTypes.SWEEP_ATTACK, 4);
                for (LivingEntity e : enemiesAround(c, p.position(), 4.0)) {
                    if (hurt(c, e, 4.0F * c.power(), false)) {
                        push(e, p.position(), -0.3, 0.05);
                    }
                }
            });
        }
        sound(c, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 2.0F, 0.6F);
        return true;
    }

    // =================================================================== rangers (new)

    /** Jumps back while shooting three arrows; enemies close by are slowed. */
    static boolean evasiveShot(SkillContext c) {
        ServerPlayer p = c.player();
        for (LivingEntity e : enemiesAround(c, p.position(), 4.0)) {
            effect(e, MobEffects.MOVEMENT_SLOWDOWN, 60, 2);
        }
        Vec3 back = flatLook(p).scale(-1.4);
        p.setDeltaMovement(back.x, 0.45, back.z);
        p.hurtMarked = true;
        c.level().sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.2, p.getZ(), 10, 0.3, 0.1, 0.3, 0.02);
        SkillRuntime.later(3, () -> {
            for (int i = -1; i <= 1; i++) {
                shootArrow(c, p.getEyePosition().add(0, -0.1, 0), p.getLookAngle().yRot(i * 0.08F), 3.0F, 3.0 * c.power(), true);
            }
            sound(c, SoundEvents.ARROW_SHOOT, 1.0F, 1.2F);
        });
        return true;
    }

    static boolean explosiveArrow(SkillContext c) {
        ServerPlayer p = c.player();
        LivingEntity aimed = targetInFront(c, 40.0);
        Vec3 to = aimed != null ? aimed.getBoundingBox().getCenter() : lookedAt(c, 40.0);
        line(c, p.getEyePosition().add(p.getLookAngle()), to, ParticleTypes.CRIT);
        c.level().sendParticles(ParticleTypes.EXPLOSION, to.x, to.y, to.z, 2, 0.5, 0.5, 0.5, 0);
        c.level().playSound(null, to.x, to.y, to.z, SoundEvents.GENERIC_EXPLODE.value(), p.getSoundSource(), 1.2F, 1.2F);
        for (LivingEntity e : enemiesAround(c, to, 3.0)) {
            if (hurt(c, e, 12.0F * c.power(), false)) {
                push(e, to, 0.8, 0.4);
            }
        }
        sound(c, SoundEvents.CROSSBOW_SHOOT, 1.0F, 0.6F);
        return true;
    }

    /** Ultimate: a one second aim, then an arrow of light pierces everything on 64 blocks. */
    static boolean heavensArrow(SkillContext c) {
        ServerPlayer p = c.player();
        effect(p, MobEffects.MOVEMENT_SLOWDOWN, 20, 4);
        ring(c, p.position().add(0, 1, 0), 1.5, ParticleTypes.END_ROD, 16);
        sound(c, SoundEvents.CROSSBOW_LOADING_END.value(), 1.5F, 0.5F);
        SkillRuntime.later(20, () -> {
            Vec3 from = p.getEyePosition();
            Vec3 to = lookedAt(c, 64.0);
            line(c, from.add(p.getLookAngle()), to, ParticleTypes.END_ROD);
            for (LivingEntity e : onLine(c, from, to, 1.2)) {
                hurt(c, e, 40.0F * c.power(), false);
                effect(e, MobEffects.GLOWING, 100, 0);
            }
            sound(c, SoundEvents.TRIDENT_RIPTIDE_3.value(), 1.5F, 1.4F);
        });
        return true;
    }

    /** Marks the prey: it glows, and takes more damage from the hunter and their wolves. */
    static boolean huntersMark(SkillContext c) {
        ServerPlayer p = c.player();
        LivingEntity target = targetInFront(c, 30.0);
        if (target == null) {
            return false;
        }
        int ticks = ticks(c, 200);
        SkillRuntime.mark(p, target, ticks);
        effect(target, MobEffects.GLOWING, ticks, 0);
        for (Wolf wolf : c.level().getEntitiesOfClass(Wolf.class, p.getBoundingBox().inflate(32.0), w -> w.isOwnedBy(p))) {
            wolf.setTarget(target);
        }
        c.level().sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(1.1), target.getZ(), 20, 0.3, 0.3, 0.3, 0.1);
        sound(c, SoundEvents.ARROW_HIT_PLAYER, 1.2F, 0.8F);
        return true;
    }

    /** Ultimate, Rakan, Monarch of Fangs: a pack of great wolves answers the howl, the enemies tremble. */
    static boolean rakansHowl(SkillContext c) {
        ServerPlayer p = c.player();
        spawnWolves(c, 4, 40.0, 7.0, 600);
        int ticks = ticks(c, 300);
        effect(p, MobEffects.MOVEMENT_SPEED, ticks, 1);
        effect(p, MobEffects.DAMAGE_BOOST, ticks, 0);
        for (LivingEntity e : enemiesAround(c, p.position(), 12.0)) {
            frighten(c, e, 60);
        }
        sound(c, SoundEvents.WOLF_HOWL, 2.0F, 0.6F);
        sound(c, SoundEvents.RAVAGER_ROAR, 0.8F, 1.3F);
        return true;
    }

    // =================================================================== monks (new)

    /** A palm strike whose shockwave throws the enemies in front far away; stronger bare-handed. */
    static boolean palmStrike(SkillContext c) {
        ServerPlayer p = c.player();
        Vec3 from = p.position().add(0, 1.0, 0);
        Vec3 to = from.add(flatLook(p).scale(6.0));
        float unarmed = p.getMainHandItem().isEmpty() ? 2.0F * c.stats().getPlayerClass().bonuses().unarmed() : 0;
        line(c, from, to, ParticleTypes.CLOUD);
        c.level().sendParticles(ParticleTypes.SONIC_BOOM, to.x, to.y, to.z, 1, 0, 0, 0, 0);
        for (LivingEntity e : onLine(c, from, to, 1.5)) {
            if (hurt(c, e, (10.0F + unarmed) * c.power(), false)) {
                push(e, p.position(), 1.8, 0.4);
            }
        }
        sound(c, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.5F, 0.5F);
        return true;
    }

    /** Ultimate, Baek Yoonho's beast form: bigger, faster, wilder. */
    static boolean beastForm(SkillContext c) {
        ServerPlayer p = c.player();
        int ticks = ticks(c, 400);
        temporary(p, Attributes.SCALE, "beast_scale", 0.3, AttributeModifier.Operation.ADD_VALUE, ticks);
        temporary(p, Attributes.ATTACK_DAMAGE, "beast_damage", 0.4, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, ticks);
        temporary(p, Attributes.ATTACK_SPEED, "beast_attack_speed", 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, ticks);
        temporary(p, Attributes.MOVEMENT_SPEED, "beast_speed", 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, ticks);
        temporary(p, Attributes.JUMP_STRENGTH, "beast_jump", 0.2, AttributeModifier.Operation.ADD_VALUE, ticks);
        temporary(p, Attributes.SAFE_FALL_DISTANCE, "beast_fall", 6.0, AttributeModifier.Operation.ADD_VALUE, ticks);
        effect(p, MobEffects.REGENERATION, ticks, 0);
        c.level().sendParticles(ParticleTypes.ANGRY_VILLAGER, p.getX(), p.getY(1.2), p.getZ(), 10, 0.5, 0.5, 0.5, 0);
        c.level().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.WHITE_WOOL.defaultBlockState()),
                p.getX(), p.getY(1.0), p.getZ(), 40, 0.5, 0.8, 0.5, 0.1);
        sound(c, SoundEvents.RAVAGER_ROAR, 1.5F, 1.3F);
        return true;
    }

    /** Ultimate, the shadow Iron: unbreakable and unmovable, it hits back, then everything is released at once. */
    static boolean adamantineBody(SkillContext c) {
        ServerPlayer p = c.player();
        int ticks = ticks(c, 160);
        effect(p, MobEffects.DAMAGE_RESISTANCE, ticks, 2);
        temporary(p, Attributes.KNOCKBACK_RESISTANCE, "adamantine_knockback", 1.0, AttributeModifier.Operation.ADD_VALUE, ticks);
        SkillRuntime.addBuff(p, Buff.ADAMANT, ticks, c.power());
        c.level().sendParticles(ParticleTypes.WAX_OFF, p.getX(), p.getY(1.0), p.getZ(), 30, 0.4, 0.7, 0.4, 0);
        sound(c, SoundEvents.ANVIL_LAND, 1.0F, 0.5F);
        SkillRuntime.later(ticks, () -> {
            if (!p.isAlive()) {
                return;
            }
            ring(c, p.position(), 6.0, ParticleTypes.EXPLOSION, 16);
            sound(c, SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 0.8F);
            for (LivingEntity e : enemiesAround(c, p.position(), 6.0)) {
                if (hurt(c, e, 10.0F * c.power(), false)) {
                    push(e, p.position(), 1.0, 0.5);
                }
            }
        });
        return true;
    }

    // =================================================================== shadows (new)

    /** A shadow soldier bursts into dark flames: the closer to the crosshair, the better. */
    static boolean corpseExplosion(SkillContext c) {
        ServerPlayer p = c.player();
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        ShadowSoldier shadow = ShadowSoldier.armyOf(p).stream().filter(s -> s.distanceToSqr(p) < 32 * 32)
                .max(Comparator.comparingDouble(s -> s.getBoundingBox().getCenter().subtract(eye).normalize().dot(look)))
                .orElse(null);
        if (shadow == null) {
            return false;
        }
        Vec3 center = shadow.position();
        float damage = 10.0F * c.power() + shadow.getMaxHealth() * 0.3F;
        shadow.fade();
        c.level().sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.5, center.z, 2, 0.5, 0.5, 0.5, 0);
        c.level().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y + 0.5, center.z, 40, 1.5, 0.5, 1.5, 0.05);
        c.level().playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), p.getSoundSource(), 1.2F, 0.6F);
        for (LivingEntity e : enemiesAround(c, center, 4.0)) {
            if (hurt(c, e, damage, true)) {
                push(e, center, 0.6, 0.4);
            }
        }
        return true;
    }

    /** Souls are torn from every enemy around: life and mana for the necromancer. */
    static boolean soulHarvest(SkillContext c) {
        ServerPlayer p = c.player();
        List<LivingEntity> enemies = enemiesAround(c, p.position(), 8.0);
        if (enemies.isEmpty()) {
            return false;
        }
        for (LivingEntity e : enemies) {
            line(c, e.getBoundingBox().getCenter(), p.getEyePosition().add(0, -0.4, 0), ParticleTypes.SOUL);
            hurt(c, e, 5.0F * c.power(), true);
        }
        p.heal(1.5F * enemies.size());
        c.stats().setMana(Math.min(Mana.max(p, c.stats()), c.stats().getMana() + 4.0F * enemies.size()));
        sound(c, SoundEvents.SOUL_ESCAPE.value(), 2.0F, 0.8F);
        return true;
    }

    /** Ultimate, Yogumunt's plague: a cloud of death; for its duration, every monster killed rises as a shadow. */
    static boolean deathPlague(SkillContext c) {
        Vec3 center = lookedAt(c, 24.0);
        SkillRuntime.addBuff(c.player(), Buff.PLAGUE, 200, c.power());
        for (int pulse = 0; pulse < 10; pulse++) {
            SkillRuntime.later(pulse * 20, () -> {
                ring(c, center, 6.0, ParticleTypes.SOUL, 30);
                c.level().sendParticles(ParticleTypes.SQUID_INK, center.x, center.y + 0.5, center.z, 30, 3.0, 0.5, 3.0, 0.01);
                for (LivingEntity e : enemiesAround(c, center, 6.0)) {
                    effect(e, MobEffects.WITHER, 40, 1);
                    hurt(c, e, 3.0F * c.power(), true);
                }
            });
        }
        sound(c, SoundEvents.WITHER_AMBIENT, 1.5F, 0.5F);
        return true;
    }

    /**
     * Sung Jinwoo's Ruler's Authority (telekinesis): the enemies in front are lifted, then slammed into the ground.
     * Sneaking: everything around is thrown away instead.
     */
    static boolean rulersAuthority(SkillContext c) {
        ServerPlayer p = c.player();
        if (p.isShiftKeyDown()) {
            for (LivingEntity e : enemiesAround(c, p.position(), 7.0)) {
                if (hurt(c, e, 6.0F * c.power(), true)) {
                    push(e, p.position(), 2.0, 0.5);
                }
            }
            ring(c, p.position().add(0, 1, 0), 7.0, SHADOW_DUST, 40);
            sound(c, SoundEvents.WARDEN_SONIC_BOOM, 0.8F, 1.4F);
            return true;
        }
        List<LivingEntity> lifted = inCone(c, 10.0, 0.7);
        if (lifted.isEmpty()) {
            return false;
        }
        for (LivingEntity e : lifted) {
            e.setDeltaMovement(0, 1.1, 0);
            e.hurtMarked = true;
            c.level().sendParticles(SHADOW_DUST, e.getX(), e.getY(0.5), e.getZ(), 15, 0.3, 0.5, 0.3, 0);
        }
        sound(c, SoundEvents.SHULKER_SHOOT, 1.5F, 0.5F);
        SkillRuntime.later(12, () -> {
            for (LivingEntity e : lifted) {
                if (!e.isAlive()) {
                    continue;
                }
                e.setDeltaMovement(0, -2.5, 0);
                e.hurtMarked = true;
                hurt(c, e, 8.0F * c.power(), true);
                c.level().sendParticles(ParticleTypes.EXPLOSION, e.getX(), e.getY(), e.getZ(), 1, 0, 0, 0, 0);
            }
            sound(c, SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 1.2F);
        });
        return true;
    }

    /** Dragon's Fear, the roar of Kamish: every enemy around is seized with terror. */
    static boolean dragonsFear(SkillContext c) {
        ServerPlayer p = c.player();
        int ticks = ticks(c, 100);
        for (LivingEntity e : enemiesAround(c, p.position(), 16.0)) {
            frighten(c, e, ticks);
            effect(e, MobEffects.DIG_SLOWDOWN, ticks, 1);
            hurt(c, e, 4.0F * c.power(), true);
        }
        for (int r = 4; r <= 16; r += 4) {
            ring(c, p.position().add(0, 0.5, 0), r, SHADOW_DUST, r * 5);
        }
        sound(c, SoundEvents.ENDER_DRAGON_GROWL, 1.5F, 0.8F);
        return true;
    }

    // ---------------------------------------------------------------- spells of the martial classes

    /** Blood Aura: for 6 seconds, enemies close by bleed every second and the berserker drinks their blood. */
    static boolean bloodAura(SkillContext c) {
        ServerPlayer p = c.player();
        for (int second = 0; second < 6; second++) {
            SkillRuntime.later(second * 20, () -> {
                if (!p.isAlive()) {
                    return;
                }
                float drained = 0;
                for (LivingEntity e : enemiesAround(c, p.position(), 5.0)) {
                    float damage = 2.5F * c.power();
                    if (hurt(c, e, damage, true)) {
                        drained += damage;
                    }
                    c.level().sendParticles(new DustParticleOptions(new Vector3f(0.7F, 0.05F, 0.05F), 1.2F),
                            e.getX(), e.getY(0.6), e.getZ(), 6, 0.3, 0.4, 0.3, 0);
                }
                p.heal(drained * 0.3F);
                ring(c, p.position(), 5.0, new DustParticleOptions(new Vector3f(0.6F, 0.0F, 0.0F), 1.4F), 24);
            });
        }
        sound(c, SoundEvents.WARDEN_HEARTBEAT, 2.0F, 0.8F);
        return true;
    }

    /** Earth Spikes: rock spikes burst out of the ground in a line, throwing enemies up. */
    static boolean earthSpikes(SkillContext c) {
        ServerPlayer p = c.player();
        Vec3 dir = flatLook(p);
        for (int i = 1; i <= 10; i++) {
            Vec3 at = p.position().add(dir.scale(i));
            int step = i;
            SkillRuntime.later(i, () -> {
                c.level().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DRIPSTONE_BLOCK.defaultBlockState()),
                        at.x, at.y + 0.3, at.z, 12, 0.4, 0.5, 0.4, 0.1);
                for (LivingEntity e : enemiesAround(c, at, 1.6)) {
                    hurt(c, e, 6.0F * c.power(), true);
                    e.setDeltaMovement(e.getDeltaMovement().add(0, 0.8, 0));
                    e.hurtMarked = true;
                    effect(e, MobEffects.MOVEMENT_SLOWDOWN, ticks(c, 60), 2);
                }
                if (step % 3 == 0) {
                    sound(c, SoundEvents.POINTED_DRIPSTONE_LAND, 1.2F, 0.6F);
                }
            });
        }
        return true;
    }

    /** Wind Guard: a gust throws enemies back, then a wind barrier turns arrows and spells away and quickens the step. */
    static boolean windGuard(SkillContext c) {
        ServerPlayer p = c.player();
        for (LivingEntity e : enemiesAround(c, p.position(), 4.0)) {
            push(e, p.position(), 1.2, 0.3);
        }
        SkillRuntime.addBuff(p, Buff.WIND_GUARD, ticks(c, 160), c.power());
        effect(p, MobEffects.MOVEMENT_SPEED, ticks(c, 160), 1);
        c.level().sendParticles(ParticleTypes.GUST_EMITTER_SMALL, p.getX(), p.getY(0.5), p.getZ(), 1, 0, 0, 0, 0);
        ring(c, p.position(), 2.0, ParticleTypes.CLOUD, 20);
        sound(c, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.2F, 1.0F);
        return true;
    }

    /** Frost Arrow: an arrow of ice flies straight ahead and bursts on the first enemy, freezing everything around. */
    static boolean frostArrow(SkillContext c) {
        ServerPlayer p = c.player();
        Vec3 from = p.getEyePosition();
        Vec3 to = lookedAt(c, 32.0);
        LivingEntity first = onLine(c, from, to, 1.0).stream().min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
        Vec3 impact = first == null ? to : first.getBoundingBox().getCenter();
        line(c, from.add(p.getLookAngle()), impact, ParticleTypes.SNOWFLAKE);
        for (LivingEntity e : enemiesAround(c, impact, 2.5)) {
            hurt(c, e, (e == first ? 7.0F : 3.0F) * c.power(), true);
            effect(e, MobEffects.MOVEMENT_SLOWDOWN, ticks(c, 80), 3);
            e.setTicksFrozen(Math.max(e.getTicksFrozen(), 160));
        }
        c.level().sendParticles(ParticleTypes.ITEM_SNOWBALL, impact.x, impact.y, impact.z, 25, 0.6, 0.6, 0.6, 0.1);
        sound(c, SoundEvents.ARROW_SHOOT, 1.0F, 1.6F);
        sound(c, SoundEvents.GLASS_BREAK, 0.8F, 1.4F);
        return true;
    }

    /** Meditation: the monk gathers their ki and heals quickly, harder to hurt for a moment. */
    static boolean meditation(SkillContext c) {
        ServerPlayer p = c.player();
        effect(p, MobEffects.REGENERATION, ticks(c, 100), 2);
        effect(p, MobEffects.DAMAGE_RESISTANCE, ticks(c, 100), 0);
        p.heal(healAmount(c, 4.0F));
        for (int i = 0; i < 5; i++) {
            SkillRuntime.later(i * 20, () -> ring(c, p.position().add(0, 0.1, 0), 1.5, ParticleTypes.END_ROD, 16));
        }
        sound(c, SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.6F);
        return true;
    }
}
