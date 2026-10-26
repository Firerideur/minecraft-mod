package com.firerideur.arcanequest.stats;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

import java.util.Arrays;

/**
 * RPG data of a player: level, experience, unspent points and allocated stats.
 * The same class is used on the server (authoritative) and on the client (synced copy).
 */
@AutoRegisterCapability
public class PlayerStats {
    public static final int MAX_LEVEL = 100;
    public static final int POINTS_PER_LEVEL = 3;

    private int level = 1;
    private long xp;
    private int points;
    private final int[] stats = new int[Stat.values().length];

    /** Experience needed to go from {@code level} to {@code level + 1}. */
    public static long xpForNextLevel(int level) {
        return 50L * level + 10L * level * level;
    }

    public int getLevel() {
        return level;
    }

    public long getXp() {
        return xp;
    }

    public int getPoints() {
        return points;
    }

    public int get(Stat stat) {
        return stats[stat.ordinal()];
    }

    public boolean isMaxLevel() {
        return level >= MAX_LEVEL;
    }

    /**
     * Adds experience and handles level ups.
     *
     * @return the number of levels gained
     */
    public int addXp(long amount) {
        if (amount <= 0 || isMaxLevel()) {
            return 0;
        }
        xp += amount;
        int gained = 0;
        while (!isMaxLevel() && xp >= xpForNextLevel(level)) {
            xp -= xpForNextLevel(level);
            level++;
            points += POINTS_PER_LEVEL;
            gained++;
        }
        if (isMaxLevel()) {
            xp = 0;
        }
        return gained;
    }

    /** Sets the level directly, giving back all points that level would have earned. */
    public void setLevel(int newLevel) {
        level = Math.max(1, Math.min(MAX_LEVEL, newLevel));
        xp = 0;
        resetStats();
    }

    public boolean spendPoint(Stat stat) {
        if (points <= 0) {
            return false;
        }
        points--;
        stats[stat.ordinal()]++;
        return true;
    }

    /** Refunds every allocated point. */
    public void resetStats() {
        Arrays.fill(stats, 0);
        points = (level - 1) * POINTS_PER_LEVEL;
    }

    public void copyFrom(PlayerStats other) {
        level = other.level;
        xp = other.xp;
        points = other.points;
        System.arraycopy(other.stats, 0, stats, 0, stats.length);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("level", level);
        tag.putLong("xp", xp);
        tag.putInt("points", points);
        for (Stat stat : Stat.values()) {
            tag.putInt(stat.key(), get(stat));
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        level = Math.max(1, tag.contains("level") ? tag.getInt("level") : 1);
        xp = tag.getLong("xp");
        points = tag.getInt("points");
        for (Stat stat : Stat.values()) {
            stats[stat.ordinal()] = tag.getInt(stat.key());
        }
    }
}
