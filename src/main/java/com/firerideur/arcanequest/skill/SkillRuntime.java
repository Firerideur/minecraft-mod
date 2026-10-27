package com.firerideur.arcanequest.skill;

import com.firerideur.arcanequest.network.ModNetwork;
import com.firerideur.arcanequest.network.SkillCooldownPacket;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

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
    public enum Buff { LIFESTEAL, POISON_BLADE, RUNE_BLADE, PARRY, DEADEYE, BERSERK, DOMAIN }

    private record BuffState(long end, float power) {}

    private record Task(long runAt, Runnable action) {}

    private static final Map<UUID, Map<Skill, Long>> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Map<Buff, BuffState>> BUFFS = new HashMap<>();
    private static final List<Task> TASKS = new ArrayList<>();
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
        float cost = skill.manaCost() * stats.getPlayerClass().bonuses().manaCost();
        if (!free && stats.getMana() < cost) {
            player.displayClientMessage(Component.translatable("message.arcanequest.no_mana").withStyle(ChatFormatting.BLUE), true);
            return false;
        }
        SkillContext context = new SkillContext(player, stats, player.serverLevel(), skill, power(stats, skill));
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

    /** Buffs on the damage players deal (lifesteal, poison, rune blade, deadeye) and take (berserk). */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player victim && buff(victim, Buff.BERSERK) > 0) {
            event.setAmount(event.getAmount() * 1.3F);
        }
        if (!(event.getSource().getEntity() instanceof Player player) || event.getEntity() == player) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (event.getSource().getDirectEntity() instanceof AbstractArrow && buff(player, Buff.DEADEYE) > 0) {
            event.setAmount(event.getAmount() * 2.0F);
        }
        if (event.getSource().getDirectEntity() != player) {
            return;
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
    }

    /** Parry: the next attack is blocked and countered. */
    @SubscribeEvent
    public static void onAttacked(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
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

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        COOLDOWNS.remove(event.getEntity().getUUID());
        BUFFS.remove(event.getEntity().getUUID());
    }
}
