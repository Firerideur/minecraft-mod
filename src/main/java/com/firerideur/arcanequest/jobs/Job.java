package com.firerideur.arcanequest.jobs;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Jobs, like on French servers (Paladium...): each one levels up by doing its work (mining, chopping, farming,
 * hunting, fishing) and gets better at it, with bonuses that grow with the level and perks unlocked at milestones.
 */
public enum Job {
    MINER(() -> Items.IRON_PICKAXE, ChatFormatting.GRAY, 15, 30, 50),
    LUMBERJACK(() -> Items.IRON_AXE, ChatFormatting.GOLD, 15, 30, 50),
    FARMER(() -> Items.WHEAT, ChatFormatting.GREEN, 10, 25, 40),
    HUNTER(() -> Items.BOW, ChatFormatting.RED, 15, 30, 45),
    FISHERMAN(() -> Items.FISHING_ROD, ChatFormatting.AQUA, 15, 30, 45);

    public static final int MAX_LEVEL = 50;

    private final Supplier<Item> icon;
    private final ChatFormatting color;
    /** Levels at which the job's three perks unlock. */
    private final int[] milestones;

    Job(Supplier<Item> icon, ChatFormatting color, int... milestones) {
        this.icon = icon;
        this.color = color;
        this.milestones = milestones;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Item icon() {
        return icon.get();
    }

    public ChatFormatting color() {
        return color;
    }

    public Component displayName() {
        return Component.translatable("job.arcanequest." + key());
    }

    /** Job experience needed to go from {@code level} to the next one. */
    public static long xpForNextLevel(int level) {
        return 60L + 30L * level + 3L * level * level;
    }

    public int milestone(int perk) {
        return milestones[perk];
    }

    public int perkCount() {
        return milestones.length;
    }

    public boolean hasPerk(int level, int perk) {
        return level >= milestones[perk];
    }

    /** "Mining speed +12%" style line of the job's always-on bonus, at {@code level}. */
    public Component bonus(int level) {
        return Component.translatable("job.arcanequest." + key() + ".bonus", format(speedBonus(level) * 100),
                format(doubleChance(level) * 100));
    }

    public Component perk(int perk) {
        return Component.translatable("job.arcanequest." + key() + ".perk" + (perk + 1), milestones[perk]);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Bonuses (they grow with the level)
    // ------------------------------------------------------------------------------------------------------------

    /** Faster mining (miner), chopping (lumberjack); more damage to monsters (hunter); otherwise 0. */
    public float speedBonus(int level) {
        return switch (this) {
            case MINER -> 0.01F * level;
            case LUMBERJACK -> 0.015F * level;
            case HUNTER -> 0.005F * level;
            default -> 0;
        };
    }

    /** Chance of a double harvest: ores, logs, crops, monster loot, fish. */
    public float doubleChance(int level) {
        return switch (this) {
            case MINER -> 0.006F * level;
            case LUMBERJACK -> 0.008F * level;
            case FARMER, HUNTER -> 0.01F * level;
            case FISHERMAN -> 0.008F * level;
        };
    }

    private static String format(float value) {
        return value == Math.round(value) ? String.valueOf(Math.round(value)) : String.format(Locale.ROOT, "%.1f", value);
    }

    public static Job byKey(String key) {
        for (Job job : values()) {
            if (job.key().equals(key)) {
                return job;
            }
        }
        return null;
    }
}
