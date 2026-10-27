package com.firerideur.arcanequest.stats;

import com.firerideur.arcanequest.boss.ArcaneBoss;
import com.firerideur.arcanequest.magic.Mana;
import com.firerideur.arcanequest.magic.SpellProjectile;
import com.firerideur.arcanequest.network.ModNetwork;
import com.firerideur.arcanequest.shadow.Corpses;
import com.firerideur.arcanequest.shadow.ShadowSoldier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Class passives that aren't plain attributes (driven by {@link PlayerClass.Bonuses}), and mana regeneration. */
public final class ClassEvents {
    /** Execute bonus applies to targets below this part of their health. */
    public static final float EXECUTE_THRESHOLD = 0.3F;
    /** Tenacity reduces the damage taken below this part of max health. */
    public static final float TENACITY_THRESHOLD = 0.3F;
    public static final float SPELL_CRIT_DAMAGE = 1.5F;
    /** Combo stacks of the Monk line, lost after {@link #COMBO_WINDOW} ticks without hitting. */
    public static final int MAX_COMBO = 5;
    private static final int COMBO_WINDOW = 40;
    private static final double AURA_RADIUS = 8.0;

    private record Combo(long lastHit, int stacks) {}

    private static final Map<UUID, Combo> COMBOS = new HashMap<>();
    /** Sync mana to the client at most this often while it regenerates. */
    private static final int MANA_SYNC_INTERVAL = 10;

    private ClassEvents() {}

    /** Damage dealt by players: class multipliers and on-hit effects. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target instanceof Player victim) {
            onPlayerHurt(event, victim);
        }
        if (!(event.getSource().getEntity() instanceof Player player) || target == player) {
            return;
        }
        PlayerStats stats = StatsCapability.get(player).orElse(null);
        if (stats == null) {
            return;
        }
        PlayerClass.Bonuses bonus = stats.getPlayerClass().bonuses();
        float amount = event.getAmount();
        boolean melee = event.getSource().getDirectEntity() == player;
        if (event.getSource().getDirectEntity() instanceof AbstractArrow) {
            amount *= bonus.arrows();
        }
        if (melee) {
            if (player.getMainHandItem().isEmpty()) {
                amount += bonus.unarmed();
            }
            if (player.getHealth() < player.getMaxHealth() / 2) {
                amount *= bonus.rage();
            }
            if (bonus.healOnHit() > 0) {
                player.heal(bonus.healOnHit());
            }
            if (bonus.manaOnHit() > 0 && player instanceof ServerPlayer serverPlayer) {
                stats.setMana(Math.min(Mana.max(serverPlayer, stats), stats.getMana() + bonus.manaOnHit()));
                ModNetwork.sync(serverPlayer);
            }
            if (bonus.combo() > 0) {
                amount *= 1 + bonus.combo() * combo(player);
            }
            if (bonus.backstab() > 1 && isBehind(player, target)) {
                amount *= bonus.backstab();
                if (player.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY(0.7), target.getZ(), 3, 0.2, 0.2, 0.2, 0.1);
                }
            }
        }
        if (bonus.firstStrike() > 1 && target.getHealth() >= target.getMaxHealth()) {
            amount *= bonus.firstStrike();
        }
        if (bonus.spellCrit() > 0 && isMagic(event.getSource()) && player.getRandom().nextFloat() < bonus.spellCrit()) {
            amount *= SPELL_CRIT_DAMAGE;
            if (player.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY(0.6), target.getZ(), 12, 0.3, 0.4, 0.3, 0.2);
            }
        }
        if (target.getHealth() < target.getMaxHealth() * EXECUTE_THRESHOLD) {
            amount *= bonus.execute();
        }
        if (target.getType().is(EntityTypeTags.UNDEAD)) {
            amount *= bonus.undead();
        }
        if (event.getSource().getDirectEntity() instanceof SpellProjectile && bonus.spellLifesteal() > 0) {
            player.heal(amount * bonus.spellLifesteal());
        }
        if (bonus.ignite() > 0) {
            target.igniteForSeconds(bonus.ignite());
        }
        if (bonus.slow() > 0) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, bonus.slow(), 1), player);
        }
        if (bonus.poison() > 0) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, bonus.poison(), 0), player);
        }
        event.setAmount(amount);
    }

    /** Hits in a row (Monk line): returns the stacks before this hit, then counts it. */
    private static int combo(Player player) {
        long now = player.level().getGameTime();
        Combo combo = COMBOS.get(player.getUUID());
        int stacks = combo != null && now - combo.lastHit <= COMBO_WINDOW ? combo.stacks : 0;
        COMBOS.put(player.getUUID(), new Combo(now, Math.min(MAX_COMBO, stacks + 1)));
        return stacks;
    }

    /** True when {@code attacker} is in the back half of {@code target}. */
    public static boolean isBehind(LivingEntity attacker, LivingEntity target) {
        Vec3 facing = Vec3.directionFromRotation(0, target.getYHeadRot());
        Vec3 toAttacker = attacker.position().subtract(target.position()).multiply(1, 0, 1);
        return toAttacker.lengthSqr() > 1.0E-4 && facing.dot(toAttacker.normalize()) < -0.2;
    }

    public static boolean isMagic(DamageSource source) {
        return source.getDirectEntity() instanceof SpellProjectile || source.is(DamageTypes.MAGIC)
                || source.is(DamageTypes.INDIRECT_MAGIC);
    }

    private static void onPlayerHurt(LivingHurtEvent event, Player victim) {
        PlayerClass.Bonuses bonus = classOf(victim).bonuses();
        event.setAmount(event.getAmount() * bonus.damageTaken());
        if (bonus.tenacity() > 0 && victim.getHealth() < victim.getMaxHealth() * TENACITY_THRESHOLD) {
            event.setAmount(event.getAmount() * (1 - bonus.tenacity()));
        }
        if (bonus.reflect() > 0 && event.getSource().getDirectEntity() instanceof LivingEntity attacker && attacker != victim) {
            attacker.hurt(victim.damageSources().thorns(victim), event.getAmount() * bonus.reflect());
        }
    }

    /** Dodge: some classes avoid attacks completely. */
    @SubscribeEvent
    public static void onAttacked(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        float dodge = classOf(player).bonuses().dodge();
        if (dodge > 0 && event.getSource().getEntity() != null && player.getRandom().nextFloat() < dodge) {
            event.setCanceled(true);
            if (player.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0, player.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_NODAMAGE, player.getSoundSource(), 1.0F, 1.4F);
            }
        }
    }

    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        float crit = classOf(event.getEntity()).bonuses().crit();
        if (event.isVanillaCritical() && crit > 0) {
            event.setDamageModifier(crit);
        }
    }

    /**
     * Necromancer line: monsters killed by the player or their shadows may rise as shadows.
     * Bosses defeated are remembered (hidden classes).
     */
    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        ServerPlayer player;
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            player = killer;
        } else if (event.getSource().getEntity() instanceof ShadowSoldier shadow && shadow.getOwner() instanceof ServerPlayer owner) {
            player = owner;
        } else {
            return;
        }
        StatsCapability.get(player).ifPresent(stats -> {
            if (event.getEntity() instanceof ArcaneBoss boss && !stats.hasKilledBoss(boss.getBossId())) {
                stats.markBossKill(boss.getBossId());
                ModNetwork.sync(player);
            }
            if (event.getSource().getEntity() == player) {
                onKillPassives(player, stats);
            }
            ShadowSoldier raised = ShadowSoldier.tryRaise(player, stats, event.getEntity());
            if (raised == null) {
                // Shadow Extraction (Arise) can still raise it for a while
                Corpses.add(player, event.getEntity());
            }
        });
    }

    private static void onKillPassives(ServerPlayer player, PlayerStats stats) {
        PlayerClass.Bonuses bonus = stats.getPlayerClass().bonuses();
        if (bonus.killFrenzy() > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, bonus.killFrenzy(), 0));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, bonus.killFrenzy(), 0));
        }
        if (bonus.healOnKill() > 0) {
            player.heal(bonus.healOnKill());
        }
        if (bonus.manaOnKill() > 0) {
            stats.setMana(Math.min(Mana.max(player, stats), stats.getMana() + bonus.manaOnKill()));
            ModNetwork.sync(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        COMBOS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player instanceof ServerPlayer player) || !player.isAlive()) {
            return;
        }
        StatsCapability.get(player).ifPresent(stats -> {
            PlayerClass.Bonuses bonus = stats.getPlayerClass().bonuses();
            if (bonus.regen() > 0 && player.tickCount % bonus.regen() == 0 && player.getHealth() < player.getMaxHealth()) {
                player.heal(1.0F);
            }
            if (bonus.aura() > 0 && player.tickCount % bonus.aura() == 0) {
                for (Player ally : player.level().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(AURA_RADIUS),
                        p -> p != player && p.isAlive() && p.getHealth() < p.getMaxHealth())) {
                    ally.heal(1.0F);
                    player.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, ally.getX(), ally.getY(1.0), ally.getZ(), 3, 0.3, 0.3, 0.3, 0);
                }
            }
            if (bonus.sneakInvisible() && player.isCrouching()) {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 10, 0, true, false, true));
            }
            float max = Mana.max(player, stats);
            float mana = stats.getMana();
            if (mana < max) {
                stats.setMana(Math.min(max, mana + Mana.regenPerTick(player, stats)));
                if (stats.getMana() >= max || player.tickCount % MANA_SYNC_INTERVAL == 0) {
                    ModNetwork.sync(player);
                }
            } else if (mana > max) {
                // Arcanite armor taken off
                stats.setMana(max);
                ModNetwork.sync(player);
            }
        });
    }

    private static PlayerClass classOf(Player player) {
        return StatsCapability.get(player).map(PlayerStats::getPlayerClass).orElse(PlayerClass.NONE);
    }
}
