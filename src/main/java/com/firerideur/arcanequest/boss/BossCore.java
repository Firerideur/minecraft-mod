package com.firerideur.arcanequest.boss;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Shared boss behavior (boss bar, phase 2 at half health, special attacks), used by composition
 * because each boss extends a different vanilla mob.
 *
 * <p>Special attacks are telegraphed: each one has a wind-up the client animates, then its effect.
 * The current attack is synced so models can play it; {@link #attackTime(float)} gives its progress.
 */
public class BossCore {
    private static final double MESSAGE_RADIUS = 48.0;
    /** Minimum pause between two special attacks. */
    private static final int ATTACK_GAP = 20;

    private final Mob boss;
    private final ServerBossEvent bossEvent;
    private final EntityDataAccessor<Byte> attackData;
    private final EntityDataAccessor<Boolean> enragedData;
    private boolean phaseTwo;

    private int attackTick;
    private int attackLength;
    private int idleTicks;
    private final int[] cooldowns = new int[8];
    /** Client side: tickCount when the current attack started. */
    private int clientAttackStart;

    public BossCore(Mob boss, BossEvent.BossBarColor color, EntityDataAccessor<Byte> attackData,
                    EntityDataAccessor<Boolean> enragedData) {
        this.boss = boss;
        this.attackData = attackData;
        this.enragedData = enragedData;
        this.bossEvent = new ServerBossEvent(boss.getDisplayName(), color, BossEvent.BossBarOverlay.NOTCHED_10);
        this.bossEvent.setDarkenScreen(true);
    }

    /**
     * Call first thing in customServerAiStep: updates the bar, timers and the running attack.
     *
     * @return true the tick the boss enters phase 2
     */
    public boolean tick() {
        bossEvent.setProgress(boss.getHealth() / boss.getMaxHealth());
        for (int i = 0; i < cooldowns.length; i++) {
            if (cooldowns[i] > 0) {
                cooldowns[i]--;
            }
        }
        if (isAttacking()) {
            if (++attackTick > attackLength) {
                stopAttack();
            }
        } else {
            idleTicks++;
        }
        if (!phaseTwo && boss.getHealth() < boss.getMaxHealth() / 2.0F) {
            phaseTwo = true;
            boss.getEntityData().set(enragedData, true);
            announce(Component.translatable("message.arcanequest.boss_enraged", boss.getDisplayName())
                    .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
            return true;
        }
        return false;
    }

    public boolean isPhaseTwo() {
        return phaseTwo;
    }

    /** Synced phase 2 flag, for the client. */
    public boolean isEnraged() {
        return boss.getEntityData().get(enragedData);
    }

    /** True every {@code interval} ticks (twice as often in phase 2). */
    public boolean every(int interval) {
        int actual = phaseTwo ? Math.max(1, interval / 2) : interval;
        return boss.tickCount % actual == 0;
    }

    // ---------------------------------------------------------------- attacks

    public int getAttack() {
        return boss.getEntityData().get(attackData);
    }

    public boolean isAttacking() {
        return getAttack() != 0;
    }

    /** Ticks since the running attack started, server side (1 on the first tick after start). */
    public int getAttackTick() {
        return attackTick;
    }

    /** True when no attack runs, the gap after the last one is over and {@code attack} is off cooldown. */
    public boolean ready(int attack) {
        return !isAttacking() && idleTicks >= ATTACK_GAP && cooldowns[attack] <= 0;
    }

    /** Starts {@code attack} for {@code length} ticks; it can be used again after {@code cooldown} (shorter in phase 2). */
    public void startAttack(int attack, int length, int cooldown) {
        attackTick = 0;
        attackLength = length;
        cooldowns[attack] = phaseTwo ? cooldown * 3 / 5 : cooldown;
        boss.getEntityData().set(attackData, (byte) attack);
    }

    public void stopAttack() {
        attackTick = 0;
        idleTicks = 0;
        boss.getEntityData().set(attackData, (byte) 0);
    }

    /** Client: call from onSyncedDataUpdated. */
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (key.equals(attackData)) {
            clientAttackStart = boss.tickCount;
        }
    }

    /** Client: ticks since the current attack started, for animations. */
    public float attackTime(float partialTick) {
        return boss.tickCount - clientAttackStart + partialTick;
    }

    // ---------------------------------------------------------------- helpers for attacks

    public List<Player> playersWithin(Vec3 center, double radius) {
        return boss.level().getEntitiesOfClass(Player.class, new AABB(center, center).inflate(radius),
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative() && p.position().distanceTo(center) <= radius);
    }

    /** Hurts {@code player} and pushes it along {@code push}. */
    public boolean hit(Player player, float damage, Vec3 push) {
        if (!player.hurt(boss.damageSources().mobAttack(boss), damage)) {
            return false;
        }
        player.setDeltaMovement(player.getDeltaMovement().add(push));
        player.hurtMarked = true;
        return true;
    }

    /** Horizontal unit vector from the boss to {@code pos}. */
    public Vec3 directionTo(Vec3 pos) {
        Vec3 d = new Vec3(pos.x - boss.getX(), 0, pos.z - boss.getZ());
        return d.lengthSqr() < 1.0E-4 ? boss.getLookAngle().multiply(1, 0, 1).normalize() : d.normalize();
    }

    /** Top of the ground near {@code pos} (searching a few blocks up and down), or null. */
    public static Vec3 groundAt(Level level, Vec3 pos) {
        BlockPos.MutableBlockPos p = BlockPos.containing(pos.x, pos.y + 3, pos.z).mutable();
        for (int i = 0; i < 8; i++, p.move(0, -1, 0)) {
            BlockPos below = p.below();
            if (level.getBlockState(p).getCollisionShape(level, p).isEmpty()
                    && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                return new Vec3(pos.x, p.getY(), pos.z);
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- boss bar and messages

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
        boss.getEntityData().set(enragedData, phaseTwo);
        onNameChanged();
    }
}
