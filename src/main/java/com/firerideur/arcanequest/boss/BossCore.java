package com.firerideur.arcanequest.boss;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Mob;

/**
 * Shared boss behavior (boss bar, phase 2 at half health, ability timers), used by composition
 * because each boss extends a different vanilla mob.
 */
public class BossCore {
    private static final double MESSAGE_RADIUS = 48.0;

    private final Mob boss;
    private final ServerBossEvent bossEvent;
    private boolean phaseTwo;

    public BossCore(Mob boss, BossEvent.BossBarColor color) {
        this.boss = boss;
        this.bossEvent = new ServerBossEvent(boss.getDisplayName(), color, BossEvent.BossBarOverlay.NOTCHED_10);
        this.bossEvent.setDarkenScreen(true);
    }

    /**
     * Call from customServerAiStep.
     *
     * @return true the tick the boss enters phase 2
     */
    public boolean tick() {
        bossEvent.setProgress(boss.getHealth() / boss.getMaxHealth());
        if (!phaseTwo && boss.getHealth() < boss.getMaxHealth() / 2.0F) {
            phaseTwo = true;
            announce(Component.translatable("message.arcanequest.boss_enraged", boss.getDisplayName())
                    .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
            return true;
        }
        return false;
    }

    public boolean isPhaseTwo() {
        return phaseTwo;
    }

    /** True every {@code interval} ticks (twice as often in phase 2). */
    public boolean every(int interval) {
        int actual = phaseTwo ? Math.max(1, interval / 2) : interval;
        return boss.tickCount % actual == 0;
    }

    public void announce(Component message) {
        if (boss.level() instanceof ServerLevel level) {
            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(boss) < MESSAGE_RADIUS * MESSAGE_RADIUS) {
                    player.sendSystemMessage(message);
                }
            }
        }
    }

    public void onNameChanged() {
        bossEvent.setName(boss.getDisplayName());
    }

    public void startSeenByPlayer(ServerPlayer player) {
        bossEvent.addPlayer(player);
    }

    public void stopSeenByPlayer(ServerPlayer player) {
        bossEvent.removePlayer(player);
    }

    public void save(CompoundTag tag) {
        tag.putBoolean("arcanequest_phase_two", phaseTwo);
    }

    public void load(CompoundTag tag) {
        phaseTwo = tag.getBoolean("arcanequest_phase_two");
        onNameChanged();
    }
}
