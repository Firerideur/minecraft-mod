package com.firerideur.arcanequest.stats;

import com.firerideur.arcanequest.command.ArcaneQuestCommand;
import com.firerideur.arcanequest.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class StatsEvents {
    private StatsEvents() {}

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            StatsCapability.Provider provider = new StatsCapability.Provider();
            event.addCapability(StatsCapability.ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    /** Keep stats when the player dies or comes back from the End. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        StatsCapability.get(original).ifPresent(oldStats ->
                StatsCapability.get(event.getEntity()).ifPresent(newStats -> newStats.copyFrom(oldStats)));
        original.invalidateCaps();
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        refresh(event.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        refresh(event.getEntity());
        event.getEntity().setHealth(event.getEntity().getMaxHealth());
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        refresh(event.getEntity());
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || victim instanceof Player) {
            return;
        }
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            addXp(player, xpForKill(victim));
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        ArcaneQuestCommand.register(event.getDispatcher());
    }

    /** Experience given for killing an entity: tougher mobs give more. */
    public static long xpForKill(LivingEntity victim) {
        return Math.max(1, Math.round(victim.getMaxHealth() / 2.0));
    }

    public static void addXp(ServerPlayer player, long amount) {
        StatsCapability.get(player).ifPresent(stats -> {
            int gained = stats.addXp(amount);
            if (gained > 0) {
                StatEffects.apply(player, stats);
                player.sendSystemMessage(Component.translatable("message.arcanequest.level_up",
                        stats.getLevel(), gained * PlayerStats.POINTS_PER_LEVEL).withStyle(ChatFormatting.GOLD));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            ModNetwork.sync(player);
        });
    }

    /** Re-applies attribute bonuses and sends the data to the client. */
    public static void refresh(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            StatsCapability.get(serverPlayer).ifPresent(stats -> StatEffects.apply(serverPlayer, stats));
            ModNetwork.sync(serverPlayer);
        }
    }
}
