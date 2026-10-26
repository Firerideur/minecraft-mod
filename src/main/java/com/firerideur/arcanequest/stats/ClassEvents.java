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
    public static final float ARCHER_ARROW_BONUS = 1.3F;
    public static final float ASSASSIN_CRIT_MULTIPLIER = 2.0F;
    /** Sync mana to the client at most this often while it regenerates. */
    private static final int MANA_SYNC_INTERVAL = 10;

    private ClassEvents() {}

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getSource().getDirectEntity() instanceof AbstractArrow
                && event.getSource().getEntity() instanceof Player player
                && classOf(player) == PlayerClass.ARCHER) {
            event.setAmount(event.getAmount() * ARCHER_ARROW_BONUS);
        }
    }

    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        if (event.isVanillaCritical() && classOf(event.getEntity()) == PlayerClass.ASSASSIN) {
            event.setDamageModifier(ASSASSIN_CRIT_MULTIPLIER);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player instanceof ServerPlayer player) || !player.isAlive()) {
            return;
        }
        StatsCapability.get(player).ifPresent(stats -> {
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
