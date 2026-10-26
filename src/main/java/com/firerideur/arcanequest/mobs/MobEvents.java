package com.firerideur.arcanequest.mobs;

import com.firerideur.arcanequest.network.MobLevelPacket;
import com.firerideur.arcanequest.network.ModNetwork;
import com.firerideur.arcanequest.registry.ModItems;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MobEvents {
    private static final double ELITE_BAR_RANGE = 32.0;

    /** Boss bars of the Champions currently loaded. Server side only. */
    private static final Map<UUID, ServerBossEvent> ELITE_BARS = new HashMap<>();

    private MobEvents() {}

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && MobLevels.isLeveled(event.getEntity())) {
            MobLevels.assign((Mob) event.getEntity(), level);
        }
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        ServerBossEvent bar = ELITE_BARS.remove(event.getEntity().getUUID());
        if (bar != null) {
            bar.removeAllPlayers();
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        Entity target = event.getTarget();
        int level = MobLevels.getLevel(target);
        if (level > 0 && event.getEntity() instanceof ServerPlayer player) {
            ModNetwork.CHANNEL.send(new MobLevelPacket(target.getId(), level, MobLevels.isElite(target)),
                    PacketDistributor.PLAYER.with(player));
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || entity.tickCount % 10 != 0 || !MobLevels.isElite(entity)) {
            return;
        }
        ServerBossEvent bar = ELITE_BARS.computeIfAbsent(entity.getUUID(), uuid ->
                new ServerBossEvent(entity.getDisplayName(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS));
        bar.setProgress(entity.getHealth() / entity.getMaxHealth());
        for (ServerPlayer player : ((ServerLevel) entity.level()).players()) {
            boolean near = entity.isAlive() && player.distanceToSqr(entity) < ELITE_BAR_RANGE * ELITE_BAR_RANGE;
            if (near) {
                bar.addPlayer(player);
            } else {
                bar.removePlayer(player);
            }
        }
    }

    /** Champions drop ore materials. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (!MobLevels.isElite(entity) || !event.isRecentlyHit()) {
            return;
        }
        var random = entity.getRandom();
        drop(event, ModItems.RAW_MITHRIL.get(), 1 + random.nextInt(3));
        drop(event, ModItems.ARCANITE.get(), random.nextInt(3));
        if (random.nextFloat() < 0.15F) {
            drop(event, ModItems.RAW_ADAMANTITE.get(), 1);
        }
    }

    private static void drop(LivingDropsEvent event, Item item, int count) {
        if (count <= 0) {
            return;
        }
        LivingEntity entity = event.getEntity();
        event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), new ItemStack(item, count)));
    }
}
