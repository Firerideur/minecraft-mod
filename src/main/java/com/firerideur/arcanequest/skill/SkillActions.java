package com.firerideur.arcanequest.skill;

import com.firerideur.arcanequest.magic.SpellProjectile;
import com.firerideur.arcanequest.shadow.ShadowSoldier;
import com.firerideur.arcanequest.skill.SkillRuntime.Buff;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
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

    static boolean bloodlust(SkillContext c) {
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
        ServerPlayer p = c.player();
        for (int i = 0; i < 2; i++) {
            Wolf wolf = EntityType.WOLF.create(c.level());
            if (wolf == null) {
                continue;
            }
            wolf.moveTo(p.getX() + (i == 0 ? 1 : -1), p.getY(), p.getZ(), p.getYRot(), 0);
            wolf.tame(p);
            wolf.getAttribute(Attributes.MAX_HEALTH).setBaseValue(30.0 * c.power());
            wolf.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(5.0 * c.power());
            wolf.setHealth(wolf.getMaxHealth());
            c.level().addFreshEntity(wolf);
            c.level().sendParticles(ParticleTypes.POOF, wolf.getX(), wolf.getY(0.5), wolf.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
            // Spirit wolves fade after a minute
            SkillRuntime.later(1200, () -> {
                if (wolf.isAlive()) {
                    c.level().sendParticles(ParticleTypes.POOF, wolf.getX(), wolf.getY(0.5), wolf.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
                    wolf.discard();
                }
            });
        }
        sound(c, SoundEvents.WOLF_HOWL, 1.5F, 1.0F);
        return true;
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
        float amount = 8.0F * c.power();
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
                    ally.heal(2.0F * c.power());
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

    static boolean arise(SkillContext c) {
        ServerPlayer p = c.player();
        int max = Math.max(4, c.stats().getPlayerClass().bonuses().shadowMax());
        int missing = max - ShadowSoldier.armyOf(p).size();
        if (missing <= 0) {
            return false;
        }
        int lifetime = c.stats().getPlayerClass().bonuses().shadowLifetime();
        for (int i = 0; i < missing; i++) {
            ShadowSoldier.summon(p, c.stats(), c.power() * 1.3F, lifetime);
        }
        c.level().sendParticles(ParticleTypes.SCULK_SOUL, p.getX(), p.getY() + 0.2, p.getZ(), 60, 3.0, 0.2, 3.0, 0.05);
        p.sendSystemMessage(Component.translatable("message.arcanequest.arise").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        sound(c, SoundEvents.WARDEN_EMERGE, 1.0F, 1.2F);
        return true;
    }

    static boolean shadowExchange(SkillContext c) {
        ServerPlayer p = c.player();
        List<ShadowSoldier> army = ShadowSoldier.armyOf(p);
        ShadowSoldier farthest = army.stream().max(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
        if (farthest == null) {
            return false;
        }
        Vec3 mine = p.position();
        c.level().sendParticles(SHADOW_DUST, mine.x, mine.y + 1, mine.z, 25, 0.3, 0.6, 0.3, 0);
        p.teleportTo(farthest.getX(), farthest.getY(), farthest.getZ());
        farthest.teleportTo(mine.x, mine.y, mine.z);
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
}
