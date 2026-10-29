package com.firerideur.arcanequest.stats;

import com.firerideur.arcanequest.enchant.EnchantmentEvents;
import com.firerideur.arcanequest.jobs.JobEvents;
import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.command.ArcaneQuestCommand;
import com.firerideur.arcanequest.mobs.MobLevels;
import com.firerideur.arcanequest.magic.Mana;
import com.firerideur.arcanequest.network.ModNetwork;
import com.firerideur.arcanequest.shadow.ShadowSoldier;
import com.firerideur.arcanequest.skill.ClassSkills;
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
        StatsCapability.get(event.getEntity()).ifPresent(stats -> {
            if (PlayerClass.canChoose(stats)) {
                event.getEntity().sendSystemMessage(Component.translatable("message.arcanequest.choose_class")
                        .withStyle(ChatFormatting.AQUA));
            }
        });
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        StatsCapability.get(player).ifPresent(stats -> stats.setMana(Mana.max(player, stats)));
        refresh(player);
        player.setHealth(player.getMaxHealth());
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
            addXp(player, Math.round(MobLevels.xpForKill(victim) * EnchantmentEvents.wisdomBonus(player) * JobEvents.classXpBonus(player)));
        } else if (event.getSource().getEntity() instanceof ShadowSoldier shadow && shadow.getOwner() instanceof ServerPlayer owner) {
            // Shadows fight for their master: the experience is theirs
            addXp(owner, MobLevels.xpForKill(victim));
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        ArcaneQuestCommand.register(event.getDispatcher());
        ArcaneQuest.LOGGER.info("Registered /arcanequest and /aq commands ({})", event.getCommandSelection());
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
                int previous = stats.getLevel() - gained;
                var skills = ClassSkills.skillsOf(stats.getPlayerClass());
                for (int slot = 0; slot < skills.size(); slot++) {
                    int unlock = ClassSkills.UNLOCK_LEVELS[slot];
                    if (previous < unlock && stats.getLevel() >= unlock) {
                        player.sendSystemMessage(Component.translatable("message.arcanequest.skill_unlocked",
                                skills.get(slot).displayName(), Component.keybind("key.arcanequest.skill_" + (slot + 1)))
                                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
                    }
                }
                if (PlayerClass.canChoose(stats)) {
                    if (stats.getPlayerClass() == PlayerClass.NONE && previous < PlayerClass.UNLOCK_LEVEL) {
                        player.sendSystemMessage(Component.translatable("message.arcanequest.class_unlocked")
                                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
                    } else if (stats.getPlayerClass().isFirstClass() && previous < PlayerClass.AWAKENING_LEVEL) {
                        player.sendSystemMessage(Component.translatable("message.arcanequest.awakening")
                                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
                    }
                }
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
