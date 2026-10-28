package com.firerideur.arcanequest.skill;

import com.firerideur.arcanequest.enchant.EnchantmentEvents;
import com.firerideur.arcanequest.network.ModNetwork;
import com.firerideur.arcanequest.network.SkillCooldownPacket;
import com.firerideur.arcanequest.shadow.Corpses;
import com.firerideur.arcanequest.shadow.ShadowSoldier;
import com.firerideur.arcanequest.stats.ClassEvents;
import com.firerideur.arcanequest.stats.PlayerStats;
import com.firerideur.arcanequest.stats.Stat;
import com.firerideur.arcanequest.stats.StatsCapability;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server side state of skills: cooldowns, temporary buffs and delayed effects. */
public final class SkillRuntime {
    /** Temporary effects some skills give, checked when the player hits or gets hit. */
    public enum Buff {
        LIFESTEAL, POISON_BLADE, RUNE_BLADE, PARRY, DEADEYE, BERSERK, DOMAIN, STEALTH, AEGIS, CRUSADE, TEMPEST, PACT,
        GRACE, AVATAR, ADAMANT, PLAGUE, WIND_GUARD
    }

    /** Shadows of a player under Monarch's Domain deal this much more damage (+50%, as in Solo Leveling). */
    public static final float DOMAIN_DAMAGE = 1.5F;
    /** Damage multiplier of hits on a target marked by its hunter (Hunter's Mark). */
    public static final float MARK_DAMAGE = 1.3F;
    public static final float STEALTH_DAMAGE = 2.0F;
    public static final float PACT_DAMAGE = 1.5F;

    private record BuffState(long end, float power) {}

    private record Mark(UUID hunter, long end) {}

    private record Task(long runAt, Runnable action) {}

    private static final Map<UUID, Map<Skill, Long>> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Map<Buff, BuffState>> BUFFS = new HashMap<>();
    private static final List<Task> TASKS = new ArrayList<>();
    private static final Map<UUID, Mark> MARKS = new HashMap<>();
    /** Terrified monsters (Bloodlust, Dragon's Fear) and the game time their fear ends. */
    private static final Map<UUID, Long> FEARED = new HashMap<>();
    /** Set while Arcane Avatar bursts, so that the burst doesn't trigger itself. */
    private static boolean bursting;
    private static long serverTicks;

    private SkillRuntime() {}

    /**
     * Uses the skill in {@code slot} if it is unlocked, ready and affordable.
     *
     * @return true if the skill was used
     */
    public static boolean use(ServerPlayer player, int slot) {
        PlayerStats stats = StatsCapability.get(player).orElse(null);
        if (stats == null || !player.isAlive() || player.isSpectator()) {
            return false;
        }
        Skill skill = ClassSkills.unlocked(stats, slot);
        if (skill == null) {
            return false;
        }
        long now = player.level().getGameTime();
        long ready = COOLDOWNS.getOrDefault(player.getUUID(), Map.of()).getOrDefault(skill, 0L);
        if (ready > now) {
            player.displayClientMessage(Component.translatable("message.arcanequest.skill_cooldown", skill.displayName(),
                    (ready - now + 19) / 20).withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        boolean free = player.getAbilities().instabuild;
        float cost = skill.manaCost() * stats.getPlayerClass().bonuses().manaCost()
                * (skill.isMagic() ? EnchantmentEvents.manaCostBonus(player) : 1.0F);
        if (!free && stats.getMana() < cost) {
            player.displayClientMessage(Component.translatable("message.arcanequest.no_mana").withStyle(ChatFormatting.BLUE), true);
            return false;
        }
        // A staff's Spell Power also strengthens the spells of the class
        float power = power(stats, skill) * (skill.isMagic() ? EnchantmentEvents.spellPowerBonus(player) : 1.0F);
        SkillContext context = new SkillContext(player, stats, player.serverLevel(), skill, power);
        if (!skill.run(context)) {
            return false;
        }
        if (!free) {
            stats.setMana(stats.getMana() - cost);
        }
        COOLDOWNS.computeIfAbsent(player.getUUID(), id -> new EnumMap<>(Skill.class)).put(skill, now + skill.cooldownTicks());
        ModNetwork.CHANNEL.send(new SkillCooldownPacket(skill.ordinal(), skill.cooldownTicks()), PacketDistributor.PLAYER.with(player));
        ModNetwork.sync(player);
        return true;
    }

    /** Skill strength: +2% per level above 20, then spell power for magic skills or +2% per strength point. */
    public static float power(PlayerStats stats, Skill skill) {
        float level = 1.0F + 0.02F * Math.max(0, stats.getLevel() - 20);
        return level * (skill.isMagic() ? stats.spellPower() : 1.0F + 0.02F * stats.get(Stat.STRENGTH));
    }

    /** Makes every skill of {@code player} ready again (Rewind). */
    public static void resetCooldowns(ServerPlayer player) {
        COOLDOWNS.remove(player.getUUID());
        ModNetwork.CHANNEL.send(new SkillCooldownPacket(-1, 0), PacketDistributor.PLAYER.with(player));
    }

    public static void addBuff(Player player, Buff buff, int ticks, float power) {
        BUFFS.computeIfAbsent(player.getUUID(), id -> new EnumMap<>(Buff.class))
                .put(buff, new BuffState(player.level().getGameTime() + ticks, power));
    }

    /** Power of an active buff, or 0 if {@code player} doesn't have it. */
    public static float buff(Player player, Buff buff) {
        BuffState state = BUFFS.getOrDefault(player.getUUID(), Map.of()).get(buff);
        return state != null && state.end > player.level().getGameTime() ? state.power : 0.0F;
    }

    public static void removeBuff(Player player, Buff buff) {
        Map<Buff, BuffState> buffs = BUFFS.get(player.getUUID());
        if (buffs != null) {
            buffs.remove(buff);
        }
    }

    /** Hunter's Mark: {@code target} takes more damage from {@code hunter} and their pets for {@code ticks}. */
    public static void mark(Player hunter, LivingEntity target, int ticks) {
        MARKS.put(target.getUUID(), new Mark(hunter.getUUID(), hunter.level().getGameTime() + ticks));
    }

    public static boolean isMarkedBy(LivingEntity target, @Nullable Entity attacker) {
        Mark mark = MARKS.get(target.getUUID());
        if (mark == null || attacker == null) {
            return false;
        }
        if (mark.end <= target.level().getGameTime()) {
            MARKS.remove(target.getUUID());
            return false;
        }
        return mark.hunter.equals(attacker.getUUID()) || attacker instanceof TamableAnimal pet && mark.hunter.equals(pet.getOwnerUUID());
    }

    /** Fear: {@code mob} can't pick a target for {@code ticks}. */
    public static void fear(LivingEntity mob, int ticks) {
        long now = mob.level().getGameTime();
        if (FEARED.size() > 256) {
            FEARED.values().removeIf(end -> end <= now);
        }
        FEARED.put(mob.getUUID(), now + ticks);
    }

    public static boolean isFeared(LivingEntity mob) {
        Long end = FEARED.get(mob.getUUID());
        return end != null && end > mob.level().getGameTime();
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() != null && isFeared(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /** Runs {@code action} on the server thread in {@code delay} ticks (skills with several hits or a delay). */
    public static void later(int delay, Runnable action) {
        TASKS.add(new Task(serverTicks + delay, action));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        serverTicks++;
        if (TASKS.isEmpty()) {
            return;
        }
        List<Task> due = new ArrayList<>();
        for (Iterator<Task> it = TASKS.iterator(); it.hasNext(); ) {
            Task task = it.next();
            if (task.runAt <= serverTicks) {
                due.add(task);
                it.remove();
            }
        }
        due.forEach(task -> task.action.run());
    }

    /** Buffs on the damage players deal (lifesteal, poison, rune blade, deadeye...) and take (berserk, aegis...). */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        Entity source = event.getSource().getEntity();
        if (target instanceof Player victim) {
            onPlayerHurt(event, victim, source);
        }
        if (source instanceof ShadowSoldier shadow && shadow.getOwner() != null && buff(shadow.getOwner(), Buff.DOMAIN) > 0) {
            event.setAmount(event.getAmount() * DOMAIN_DAMAGE);
        }
        if (isMarkedBy(target, source)) {
            event.setAmount(event.getAmount() * MARK_DAMAGE);
        }
        if (!(source instanceof Player player) || target == player) {
            return;
        }
        if (event.getSource().getDirectEntity() instanceof AbstractArrow && buff(player, Buff.DEADEYE) > 0) {
            event.setAmount(event.getAmount() * 2.0F);
        }
        if (buff(player, Buff.PACT) > 0 && ClassEvents.isMagic(event.getSource())) {
            event.setAmount(event.getAmount() * PACT_DAMAGE);
            player.heal(event.getAmount() * 0.2F);
        }
        if (event.getSource().getDirectEntity() != player) {
            return;
        }
        if (buff(player, Buff.STEALTH) > 0) {
            // Out of Stealth: the first hit is a critical ambush
            event.setAmount(event.getAmount() * STEALTH_DAMAGE);
            removeBuff(player, Buff.STEALTH);
            player.removeEffect(MobEffects.INVISIBILITY);
            if (player.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY(0.6), target.getZ(), 15, 0.3, 0.4, 0.3, 0.05);
            }
        }
        if (buff(player, Buff.LIFESTEAL) > 0) {
            player.heal(event.getAmount() * 0.3F);
        }
        if (buff(player, Buff.POISON_BLADE) > 0) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1), player);
        }
        float rune = buff(player, Buff.RUNE_BLADE);
        if (rune > 0) {
            event.setAmount(event.getAmount() + 4.0F * rune);
            StatsCapability.get(player).ifPresent(stats -> stats.setMana(stats.getMana() + 2));
            if (player.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY(0.5), target.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
            }
        }
        float crusade = buff(player, Buff.CRUSADE);
        if (crusade > 0) {
            event.setAmount(event.getAmount() + 3.0F * crusade);
            player.heal(1.0F);
        }
        float avatar = buff(player, Buff.AVATAR);
        if (avatar > 0 && !bursting && player.level() instanceof ServerLevel level) {
            // Arcane Avatar: every hit bursts around the target
            bursting = true;
            try {
                level.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY(0.5), target.getZ(), 20, 0.8, 0.5, 0.8, 0.1);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(2.5),
                        e -> e != target && SkillActions.isEnemy(player, e))) {
                    e.invulnerableTime = 0;
                    e.hurt(player.damageSources().indirectMagic(player, player), 4.0F * avatar);
                }
            } finally {
                bursting = false;
            }
        }
    }

    private static void onPlayerHurt(LivingHurtEvent event, Player victim, @Nullable Entity source) {
        if (buff(victim, Buff.BERSERK) > 0) {
            event.setAmount(event.getAmount() * 1.3F);
        }
        if (!(source instanceof LivingEntity attacker) || attacker == victim || attacker instanceof Player) {
            return;
        }
        // Aegis sends everything back, Adamantine Body half of the melee hits
        float aegis = buff(victim, Buff.AEGIS);
        float reflected = aegis > 0 ? event.getAmount() * Math.min(2.0F, 0.5F + 0.5F * aegis)
                : buff(victim, Buff.ADAMANT) > 0 && event.getSource().getDirectEntity() == attacker ? event.getAmount() * 0.5F : 0;
        if (reflected > 0) {
            attacker.invulnerableTime = 0;
            attacker.hurt(victim.damageSources().thorns(victim), reflected);
            if (victim.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, attacker.getX(), attacker.getY(0.6), attacker.getZ(), 10, 0.3, 0.4, 0.3, 0.2);
            }
        }
    }

    /** Saving Grace: a blessed player survives a deadly blow once. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && buff(player, Buff.GRACE) > 0) {
            event.setCanceled(true);
            removeBuff(player, Buff.GRACE);
            player.setHealth(player.getMaxHealth() * 0.3F);
            player.removeAllEffects();
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 2));
            player.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY(1.0), player.getZ(), 40, 0.5, 0.8, 0.5, 0.3);
            player.level().playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, player.getSoundSource(), 1.0F, 1.2F);
        }
    }

    /** Parry: the next attack is blocked and countered. */
    @SubscribeEvent
    public static void onAttacked(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (buff(player, Buff.TEMPEST) > 0 && event.getSource().getEntity() != null && player.getRandom().nextFloat() < 0.5F) {
            // Tempest Dance: half of the attacks miss
            event.setCanceled(true);
            player.serverLevel().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(1.0), player.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
            return;
        }
        if (buff(player, Buff.WIND_GUARD) > 0 && event.getSource().getDirectEntity() instanceof Projectile projectile) {
            // Wind Guard: arrows and spells are blown away
            event.setCanceled(true);
            projectile.setDeltaMovement(projectile.getDeltaMovement().scale(-0.6));
            projectile.hurtMarked = true;
            player.serverLevel().sendParticles(ParticleTypes.GUST, player.getX(), player.getY(1.0), player.getZ(), 1, 0, 0, 0, 0);
            return;
        }
        float parry = buff(player, Buff.PARRY);
        if (parry > 0 && event.getSource().getEntity() instanceof LivingEntity attacker && attacker != player) {
            event.setCanceled(true);
            removeBuff(player, Buff.PARRY);
            attacker.invulnerableTime = 0;
            attacker.hurt(player.damageSources().playerAttack(player), 8.0F * parry);
            attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
            player.level().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, player.getSoundSource(), 1.0F, 1.3F);
            player.serverLevel().sendParticles(ParticleTypes.CRIT, player.getX(), player.getY(1.0), player.getZ(), 15, 0.4, 0.4, 0.4, 0.3);
        }
    }

    /** Nothing must leak from a world to the next one (singleplayer). */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TASKS.clear();
        COOLDOWNS.clear();
        BUFFS.clear();
        MARKS.clear();
        FEARED.clear();
        Corpses.clear();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        COOLDOWNS.remove(event.getEntity().getUUID());
        BUFFS.remove(event.getEntity().getUUID());
        MARKS.values().removeIf(mark -> mark.hunter.equals(event.getEntity().getUUID()));
    }
}
