package com.firerideur.arcanequest.stats;

import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * The class a player picks once, at level {@link #UNLOCK_LEVEL}. Only the {@link #OFFERED} classes that best match
 * the way the player spent their stat points are offered.
 *
 * <p>Each class has an affinity with one or two stats, attribute bonuses ({@link Bonuses}, applied by
 * {@link StatEffects}) and sometimes a special passive ({@link ClassEvents}).
 */
public enum PlayerClass {
    NONE("none", 0xAAAAAA, affinity(0, 0, 0, 0), new Bonuses()),
    /** Strength. Hits harder the more hurt he is. */
    BERSERKER("berserker", 0xD83A3A, affinity(1, 0, 0, 0), new Bonuses().melee(0.15).attackSpeed(0.10)),
    /** Strength + vitality. Melee hits heal. */
    PALADIN("paladin", 0xF0C040, affinity(1, 1, 0, 0), new Bonuses().health(4).armor(2).melee(0.10)),
    /** Vitality. A wall of armor. */
    GUARDIAN("guardian", 0x7C9CC0, affinity(0, 1, 0, 0), new Bonuses().health(8).armor(4).toughness(2).knockback(0.3)),
    /** Strength + agility. Fast blade, strong critical hits. */
    DUELIST("duelist", 0xE88A30, affinity(1, 0, 1, 0), new Bonuses().melee(0.10).attackSpeed(0.15).crit(1.75F)),
    /** Agility. Devastating critical hits. */
    ASSASSIN("assassin", 0x50B0E0, affinity(0, 0, 1, 0), new Bonuses().attackSpeed(0.10).speed(0.10).crit(2.0F)),
    /** Agility + vitality. Bow master. */
    RANGER("ranger", 0x50C050, affinity(0, 1, 1, 0), new Bonuses().health(2).speed(0.10).arrows(1.35F)),
    /** Intelligence. Pure spell power. */
    MAGE("mage", 0xB070E0, affinity(0, 0, 0, 1), new Bonuses().mana(1.5F).spellPower(1.3F)),
    /** Strength + intelligence. Melee hits restore mana. */
    SPELLBLADE("spellblade", 0x8A60F0, affinity(1, 0, 0, 1), new Bonuses().melee(0.10).spellPower(1.15F)),
    /** Vitality + intelligence. Stronger heals, slowly regenerates health. */
    CLERIC("cleric", 0xF4EEA0, affinity(0, 1, 0, 1), new Bonuses().health(4).spellPower(1.1F).heals(1.5F)),
    /** Agility + intelligence. Cheaper spells. */
    ARCANIST("arcanist", 0x40D0D0, affinity(0, 0, 1, 1), new Bonuses().speed(0.10).spellPower(1.1F).manaCost(0.7F));

    public static final int UNLOCK_LEVEL = 20;
    public static final int OFFERED = 3;

    private final String key;
    private final int color;
    private final float[] affinity;
    private final Bonuses bonuses;

    PlayerClass(String key, int color, float[] affinity, Bonuses bonuses) {
        this.key = key;
        this.color = color;
        this.affinity = affinity;
        this.bonuses = bonuses;
    }

    private static float[] affinity(float strength, float vitality, float agility, float intelligence) {
        return new float[]{strength, vitality, agility, intelligence};
    }

    public String key() {
        return key;
    }

    public int color() {
        return color;
    }

    public Bonuses bonuses() {
        return bonuses;
    }

    public Component displayName() {
        return Component.translatable("class.arcanequest." + key);
    }

    public Component description() {
        return Component.translatable("class.arcanequest." + key + ".desc");
    }

    /**
     * How well spent stats fit this class, from 0 to 1: the cosine between the stats and the class affinity,
     * so it's the distribution of the points that counts, not their number.
     */
    public float match(PlayerStats stats) {
        double dot = 0, statsNorm = 0, affinityNorm = 0;
        for (Stat stat : Stat.values()) {
            float value = stats.get(stat);
            dot += value * affinity[stat.ordinal()];
            statsNorm += value * value;
            affinityNorm += affinity[stat.ordinal()] * affinity[stat.ordinal()];
        }
        return statsNorm == 0 || affinityNorm == 0 ? 0 : (float) (dot / Math.sqrt(statsNorm * affinityNorm));
    }

    /**
     * The classes offered for these stats, best match first. Empty if no point was spent yet.
     * Ties keep the declaration order, so the result is stable.
     */
    public static List<PlayerClass> offers(PlayerStats stats) {
        if (Arrays.stream(Stat.values()).allMatch(stat -> stats.get(stat) == 0)) {
            return List.of();
        }
        return Arrays.stream(values())
                .filter(c -> c != NONE)
                .sorted(Comparator.comparingDouble((PlayerClass c) -> c.match(stats)).reversed())
                .limit(OFFERED)
                .toList();
    }

    public static boolean canChoose(PlayerStats stats) {
        return stats.getPlayerClass() == NONE && stats.getLevel() >= UNLOCK_LEVEL;
    }

    /**
     * Sets the class if the player may pick it now: level {@link #UNLOCK_LEVEL}, no class yet, and one of their offers.
     *
     * @return true if the class was set
     */
    public static boolean choose(PlayerStats stats, PlayerClass chosen) {
        if (!canChoose(stats) || !offers(stats).contains(chosen)) {
            return false;
        }
        stats.setPlayerClass(chosen);
        return true;
    }

    public static PlayerClass byId(int id) {
        PlayerClass[] values = values();
        return id >= 0 && id < values.length ? values[id] : NONE;
    }

    /** Unknown keys (like classes from older versions) give {@link #NONE}, so the player can choose again. */
    public static PlayerClass byKey(String key) {
        for (PlayerClass value : values()) {
            if (value.key.equals(key)) {
                return value;
            }
        }
        return NONE;
    }

    /** Class bonuses. Percentages are fractions (0.1 = +10%), multipliers are 1 when the class has no bonus. */
    public static final class Bonuses {
        private double health, armor, toughness, knockback, melee, attackSpeed, speed;
        private float spellPower = 1, mana = 1, manaCost = 1, heals = 1, arrows = 1, crit;

        Bonuses health(double v) { health = v; return this; }
        Bonuses armor(double v) { armor = v; return this; }
        Bonuses toughness(double v) { toughness = v; return this; }
        Bonuses knockback(double v) { knockback = v; return this; }
        Bonuses melee(double v) { melee = v; return this; }
        Bonuses attackSpeed(double v) { attackSpeed = v; return this; }
        Bonuses speed(double v) { speed = v; return this; }
        Bonuses spellPower(float v) { spellPower = v; return this; }
        Bonuses mana(float v) { mana = v; return this; }
        Bonuses manaCost(float v) { manaCost = v; return this; }
        Bonuses heals(float v) { heals = v; return this; }
        Bonuses arrows(float v) { arrows = v; return this; }
        Bonuses crit(float v) { crit = v; return this; }

        /** Bonus max health (half hearts). */
        public double health() { return health; }
        public double armor() { return armor; }
        public double toughness() { return toughness; }
        public double knockback() { return knockback; }
        /** Melee damage bonus, fraction of the base. */
        public double melee() { return melee; }
        public double attackSpeed() { return attackSpeed; }
        public double speed() { return speed; }
        public float spellPower() { return spellPower; }
        /** Multiplier of max mana and mana regeneration. */
        public float mana() { return mana; }
        public float manaCost() { return manaCost; }
        public float heals() { return heals; }
        public float arrows() { return arrows; }
        /** Critical hit multiplier, 0 for vanilla (x1.5). */
        public float crit() { return crit; }
    }
}
