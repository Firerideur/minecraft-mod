package com.firerideur.arcanequest.stats;

import com.firerideur.arcanequest.magic.Mana;
import com.firerideur.arcanequest.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Class passives that aren't plain attributes, and mana regeneration. */
public final class ClassEvents {
    /** Berserker: extra melee damage below half health. */
    public static final float BERSERKER_RAGE_BONUS = 1.3F;
    /** Paladin: health healed by each melee hit. */
    public static final float PALADIN_HEAL = 1.0F;
    /** Spellblade: mana restored by each melee hit. */
    public static final float SPELLBLADE_MANA = 2.0F;
    /** Cleric: heals 1 health every this many ticks. */
    public static final int CLERIC_REGEN_INTERVAL = 80;
    /** Sync mana to the client at most this often while it regenerates. */
    private static final int MANA_SYNC_INTERVAL = 10;

    private ClassEvents() {}

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        PlayerClass playerClass = classOf(player);
        if (event.getSource().getDirectEntity() instanceof AbstractArrow) {
            event.setAmount(event.getAmount() * playerClass.bonuses().arrows());
            return;
        }
        if (event.getSource().getDirectEntity() != player) {
            return;
        }
        // Melee hits
        switch (playerClass) {
            case BERSERKER -> {
                if (player.getHealth() < player.getMaxHealth() / 2) {
                    event.setAmount(event.getAmount() * BERSERKER_RAGE_BONUS);
                }
            }
            case PALADIN -> player.heal(PALADIN_HEAL);
            case SPELLBLADE -> {
                if (player instanceof ServerPlayer serverPlayer) {
                    StatsCapability.get(serverPlayer).ifPresent(stats -> {
                        stats.setMana(Math.min(Mana.max(serverPlayer, stats), stats.getMana() + SPELLBLADE_MANA));
                        ModNetwork.sync(serverPlayer);
                    });
                }
            }
            default -> {
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

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player instanceof ServerPlayer player) || !player.isAlive()) {
            return;
        }
        StatsCapability.get(player).ifPresent(stats -> {
            if (stats.getPlayerClass() == PlayerClass.CLERIC && player.tickCount % CLERIC_REGEN_INTERVAL == 0
                    && player.getHealth() < player.getMaxHealth()) {
                player.heal(1.0F);
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
