package com.firerideur.arcanequest.stats;

import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Classes, Solo Leveling style: a first class at level {@link #UNLOCK_LEVEL}, then an "awakening" into a stronger
 * class at level {@link #AWAKENING_LEVEL}.
 * <ul>
 *   <li>First classes (ranks C and B): the {@link #OFFERED} that best match how the stat points were spent.</li>
 *   <li>Awakened classes (rank A): each first class can evolve into two of them.</li>
 *   <li>Hidden classes (rank S): only offered when a condition is met, like the Shadow Monarch who must have
 *       defeated the Skeleton King.</li>
 * </ul>
 * Bonuses are data ({@link Bonuses}): attributes are applied by {@link StatEffects}, the rest by {@link ClassEvents}.
 */
public enum PlayerClass {
    NONE("none", 0xAAAAAA, Rank.NONE, affinity(0, 0, 0, 0), new Bonuses()),

    // ---------------------------------------------------------------- first classes (level 20)
    BERSERKER("berserker", 0xD83A3A, Rank.C, affinity(1, 0, 0, 0),
            new Bonuses().melee(0.15).attackSpeed(0.10).rage(1.3F)),
    GUARDIAN("guardian", 0x7C9CC0, Rank.C, affinity(0, 1, 0, 0),
            new Bonuses().health(8).armor(4).toughness(2).knockback(0.3)),
    ASSASSIN("assassin", 0x50B0E0, Rank.C, affinity(0, 0, 1, 0),
            new Bonuses().attackSpeed(0.10).speed(0.10).crit(2.0F)),
    MAGE("mage", 0xB070E0, Rank.C, affinity(0, 0, 0, 1),
            new Bonuses().mana(1.5F).spellPower(1.3F)),
    PALADIN("paladin", 0xF0C040, Rank.B, affinity(1, 1, 0, 0),
            new Bonuses().health(4).armor(2).melee(0.10).healOnHit(1)),
    DUELIST("duelist", 0xE88A30, Rank.B, affinity(1, 0, 1, 0),
            new Bonuses().melee(0.10).attackSpeed(0.15).crit(1.75F)),
    RANGER("ranger", 0x50C050, Rank.B, affinity(0, 1, 1, 0),
            new Bonuses().health(2).speed(0.10).arrows(1.35F)),
    SPELLBLADE("spellblade", 0x8A60F0, Rank.B, affinity(1, 0, 0, 1),
            new Bonuses().melee(0.10).spellPower(1.15F).manaOnHit(2)),
    CLERIC("cleric", 0xF4EEA0, Rank.B, affinity(0, 1, 0, 1),
            new Bonuses().health(4).spellPower(1.1F).heals(1.5F).regen(80)),
    ARCANIST("arcanist", 0x40D0D0, Rank.B, affinity(0, 0, 1, 1),
            new Bonuses().speed(0.10).spellPower(1.1F).manaCost(0.7F)),
    MONK("monk", 0xE0B080, Rank.B, affinity(0.7F, 0.7F, 1, 0),
            new Bonuses().unarmed(5).attackSpeed(0.10).speed(0.05)),
    NECROMANCER("necromancer", 0x6A4C9C, Rank.B, affinity(0.4F, 0.4F, 0.4F, 1),
            new Bonuses().spellPower(1.1F).shadows(0.15F, 3, 1200, 1.0F)),

    // ---------------------------------------------------------------- awakened classes (level 50)
    WARLORD("warlord", 0xB01818, Rank.A, from("berserker"),
            new Bonuses().health(4).melee(0.25).attackSpeed(0.15).rage(1.5F)),
    EXECUTIONER("executioner", 0x8C1F2A, Rank.A, from("berserker"),
            new Bonuses().melee(0.20).attackSpeed(0.10).execute(1.6F)),
    FORTRESS("fortress", 0x5A7090, Rank.A, from("guardian"),
            new Bonuses().health(12).armor(6).toughness(4).knockback(0.5).damageTaken(0.85F)),
    RUNE_GUARDIAN("rune_guardian", 0x4FA0D0, Rank.A, from("guardian"),
            new Bonuses().health(10).armor(5).toughness(2).knockback(0.4).reflect(0.3F)),
    NINJA("ninja", 0x3A3A4A, Rank.A, from("assassin"),
            new Bonuses().attackSpeed(0.15).speed(0.20).crit(2.0F).dodge(0.25F)),
    SHADOW_ASSASSIN("shadow_assassin", 0x2A6A9A, Rank.A, from("assassin"),
            new Bonuses().attackSpeed(0.15).speed(0.10).crit(2.5F).invisibleWhenSneaking()),
    ARCHMAGE("archmage", 0x9A40F0, Rank.A, from("mage"),
            new Bonuses().mana(2.0F).spellPower(1.6F)),
    ELEMENTALIST("elementalist", 0xF06030, Rank.A, from("mage"),
            new Bonuses().mana(1.5F).spellPower(1.4F).ignite(3).slow(40)),
    CRUSADER("crusader", 0xF8D860, Rank.A, from("paladin"),
            new Bonuses().health(8).armor(4).melee(0.15).healOnHit(2)),
    HOLY_KNIGHT("holy_knight", 0xFFF2B0, Rank.A, from("paladin"),
            new Bonuses().health(6).armor(3).melee(0.15).healOnHit(1).undead(1.6F)),
    WEAPON_MASTER("weapon_master", 0xD07020, Rank.A, from("duelist"),
            new Bonuses().melee(0.20).attackSpeed(0.25).crit(2.0F)),
    WINDBLADE("windblade", 0x90E0D0, Rank.A, from("duelist"),
            new Bonuses().melee(0.15).attackSpeed(0.20).speed(0.20).crit(1.75F).dodge(0.10F)),
    SNIPER("sniper", 0x309030, Rank.A, from("ranger"),
            new Bonuses().health(2).speed(0.10).arrows(1.7F)),
    HUNTER("hunter", 0x70A040, Rank.A, from("ranger"),
            new Bonuses().health(4).speed(0.15).arrows(1.45F).slow(40)),
    RUNE_KNIGHT("rune_knight", 0x6070F0, Rank.A, from("spellblade"),
            new Bonuses().armor(2).melee(0.20).spellPower(1.3F).manaOnHit(4)),
    WARLOCK("warlock", 0x7A2A8A, Rank.A, from("spellblade"),
            new Bonuses().melee(0.10).spellPower(1.35F).spellLifesteal(0.25F)),
    SAINT("saint", 0xFFFFE0, Rank.A, from("cleric"),
            new Bonuses().health(6).spellPower(1.2F).heals(2.5F).regen(40)),
    DRUID("druid", 0x40A050, Rank.A, from("cleric"),
            new Bonuses().health(8).spellPower(1.15F).heals(1.75F).regen(60).poison(60)),
    CHRONOMANCER("chronomancer", 0x30B0B0, Rank.A, from("arcanist"),
            new Bonuses().speed(0.15).spellPower(1.2F).manaCost(0.5F)),
    BATTLEMAGE("battlemage", 0x5090E0, Rank.A, from("arcanist"),
            new Bonuses().speed(0.15).melee(0.10).attackSpeed(0.10).spellPower(1.3F).manaCost(0.75F)),
    GRANDMASTER("grandmaster", 0xF0C090, Rank.A, from("monk"),
            new Bonuses().unarmed(10).attackSpeed(0.20).speed(0.10)),
    IRON_MONK("iron_monk", 0xA09080, Rank.A, from("monk"),
            new Bonuses().unarmed(7).health(6).armor(6).knockback(0.3)),
    LICH_LORD("lich_lord", 0x50D0A0, Rank.A, from("necromancer"),
            new Bonuses().mana(1.5F).spellPower(1.25F).shadows(0.30F, 5, 2400, 1.2F)),

    // ---------------------------------------------------------------- hidden class
    /** Only after defeating the Skeleton King: permanent shadow army. */
    SHADOW_MONARCH("shadow_monarch", 0x4040FF, Rank.S, from("necromancer", "assassin"),
            new Bonuses().health(6).melee(0.15).speed(0.10).spellPower(1.2F).shadows(0.5F, 8, 0, 1.6F)
                    .requiresBoss(BossKills.SKELETON_KING));

    public static final int UNLOCK_LEVEL = 20;
    public static final int AWAKENING_LEVEL = 50;
    public static final int OFFERED = 3;

    private final String key;
    private final int color;
    private final Rank rank;
    /** Stats affinity of first classes, null for awakened ones. */
    private final float[] affinity;
    /** Keys of the classes this one awakens from (empty for first classes). */
    private final List<String> parents;
    private final Bonuses bonuses;

    PlayerClass(String key, int color, Rank rank, float[] affinity, Bonuses bonuses) {
        this.key = key;
        this.color = color;
        this.rank = rank;
        this.affinity = affinity;
        this.parents = List.of();
        this.bonuses = bonuses;
    }

    PlayerClass(String key, int color, Rank rank, String[] parents, Bonuses bonuses) {
        this.key = key;
        this.color = color;
        this.rank = rank;
        this.affinity = null;
        this.parents = List.of(parents);
        this.bonuses = bonuses;
    }

    private static float[] affinity(float strength, float vitality, float agility, float intelligence) {
        return new float[]{strength, vitality, agility, intelligence};
    }

    private static String[] from(String... parents) {
        return parents;
    }

    public String key() {
        return key;
    }

    public int color() {
        return color;
    }

    public Rank rank() {
        return rank;
    }

    public Bonuses bonuses() {
        return bonuses;
    }

    /** True for the classes picked at level 20. */
    public boolean isFirstClass() {
        return affinity != null && this != NONE;
    }

    public boolean isAwakened() {
        return !parents.isEmpty();
    }

    /** The first class an awakened class comes from (itself for first classes). */
    public PlayerClass firstClass() {
        return parents.isEmpty() ? this : byKey(parents.get(0));
    }

    public Component displayName() {
        return Component.translatable("class.arcanequest." + key);
    }

    /** Name followed by the rank, like "Mage [C]". */
    public Component displayNameWithRank() {
        return displayName().copy().withColor(color).append(Component.literal(" [" + rank.name() + "]").withColor(rank.color()));
    }

    public Component description() {
        return Component.translatable("class.arcanequest." + key + ".desc");
    }

    /**
     * How well spent stats fit this first class, from 0 to 1: the cosine between the stats and the class affinity,
     * so it's the distribution of the points that counts, not their number.
     */
    public float match(PlayerStats stats) {
        if (affinity == null) {
            return 0;
        }
        double dot = 0, statsNorm = 0, affinityNorm = 0;
        for (Stat stat : Stat.values()) {
            float value = stats.get(stat);
            dot += value * affinity[stat.ordinal()];
            statsNorm += value * value;
            affinityNorm += affinity[stat.ordinal()] * affinity[stat.ordinal()];
        }
        return statsNorm == 0 || affinityNorm == 0 ? 0 : (float) (dot / Math.sqrt(statsNorm * affinityNorm));
    }

    /** True if the special condition of this class (if any) is fulfilled. */
    public boolean isUnlocked(PlayerStats stats) {
        return bonuses.requiredBoss < 0 || stats.hasKilledBoss(bonuses.requiredBoss);
    }

    /**
     * What the player can pick (or will be able to pick, for previews):
     * without class, the first classes best matching the stats (empty if no point was spent);
     * with a first class, its awakenings, plus hidden classes whose condition is met.
     */
    public static List<PlayerClass> offers(PlayerStats stats) {
        PlayerClass current = stats.getPlayerClass();
        if (current == NONE) {
            if (Arrays.stream(Stat.values()).allMatch(stat -> stats.get(stat) == 0)) {
                return List.of();
            }
            // Stable sort: ties keep the declaration order
            return Arrays.stream(values())
                    .filter(PlayerClass::isFirstClass)
                    .sorted(Comparator.comparingDouble((PlayerClass c) -> c.match(stats)).reversed())
                    .limit(OFFERED)
                    .toList();
        }
        if (current.isFirstClass()) {
            return Arrays.stream(values())
                    .filter(c -> c.parents.contains(current.key) && c.isUnlocked(stats))
                    .toList();
        }
        return List.of();
    }

    /** All the awakenings of {@code first}, including locked hidden ones (for display). */
    public static List<PlayerClass> awakeningsOf(PlayerClass first) {
        return Arrays.stream(values()).filter(c -> c.parents.contains(first.key)).toList();
    }

    /** True when the player can pick a class right now: first class at level 20, awakening at level 50. */
    public static boolean canChoose(PlayerStats stats) {
        PlayerClass current = stats.getPlayerClass();
        return current == NONE && stats.getLevel() >= UNLOCK_LEVEL
                || current.isFirstClass() && stats.getLevel() >= AWAKENING_LEVEL;
    }

    /**
     * Sets the class if the player may pick it now (see {@link #canChoose}) and it is one of their offers.
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

    /** Hunter ranks, like in Solo Leveling. */
    public enum Rank {
        NONE(0xAAAAAA), C(0x55AAFF), B(0xB070FF), A(0xFFAA00), S(0xFF4040);

        private final int color;

        Rank(int color) {
            this.color = color;
        }

        public int color() {
            return color;
        }
    }

    /** Bit numbers of the bosses in {@link PlayerStats#hasKilledBoss}. */
    public static final class BossKills {
        public static final int SKELETON_KING = 0;
        public static final int CORRUPTED_ARCHMAGE = 1;
        public static final int ADAMANTITE_GOLEM = 2;

        private BossKills() {}
    }

    /** Class bonuses. Percentages are fractions (0.1 = +10%), multipliers are 1 when the class has no bonus. */
    public static final class Bonuses {
        private double health, armor, toughness, knockback, melee, attackSpeed, speed;
        private float spellPower = 1, mana = 1, manaCost = 1, heals = 1, arrows = 1, crit, rage = 1, execute = 1,
                undead = 1, damageTaken = 1, dodge, reflect, spellLifesteal, healOnHit, manaOnHit, unarmed;
        private int regen, ignite, slow, poison;
        private boolean sneakInvisible;
        private float shadowChance, shadowPower = 1;
        private int shadowMax, shadowLifetime;
        private int requiredBoss = -1;

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
        Bonuses rage(float v) { rage = v; return this; }
        Bonuses execute(float v) { execute = v; return this; }
        Bonuses undead(float v) { undead = v; return this; }
        Bonuses damageTaken(float v) { damageTaken = v; return this; }
        Bonuses dodge(float v) { dodge = v; return this; }
        Bonuses reflect(float v) { reflect = v; return this; }
        Bonuses spellLifesteal(float v) { spellLifesteal = v; return this; }
        Bonuses healOnHit(float v) { healOnHit = v; return this; }
        Bonuses manaOnHit(float v) { manaOnHit = v; return this; }
        Bonuses unarmed(float v) { unarmed = v; return this; }
        Bonuses regen(int ticks) { regen = ticks; return this; }
        Bonuses ignite(int seconds) { ignite = seconds; return this; }
        Bonuses slow(int ticks) { slow = ticks; return this; }
        Bonuses poison(int ticks) { poison = ticks; return this; }
        Bonuses invisibleWhenSneaking() { sneakInvisible = true; return this; }
        Bonuses requiresBoss(int boss) { requiredBoss = boss; return this; }

        /**
         * Killed monsters may rise as shadow soldiers.
         *
         * @param lifetime ticks before a shadow fades, 0 = until it dies
         * @param power    multiplier of the shadows' health and damage
         */
        Bonuses shadows(float chance, int max, int lifetime, float power) {
            shadowChance = chance;
            shadowMax = max;
            shadowLifetime = lifetime;
            shadowPower = power;
            return this;
        }

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
        /** Melee damage multiplier below half health. */
        public float rage() { return rage; }
        /** Damage multiplier against targets below 30% health. */
        public float execute() { return execute; }
        /** Damage multiplier against undead. */
        public float undead() { return undead; }
        public float damageTaken() { return damageTaken; }
        /** Chance to avoid an attack completely. */
        public float dodge() { return dodge; }
        /** Part of melee damage sent back to the attacker. */
        public float reflect() { return reflect; }
        /** Part of spell damage healed. */
        public float spellLifesteal() { return spellLifesteal; }
        public float healOnHit() { return healOnHit; }
        public float manaOnHit() { return manaOnHit; }
        /** Bonus damage of bare-handed hits. */
        public float unarmed() { return unarmed; }
        /** Heals 1 health every this many ticks, 0 = no regeneration. */
        public int regen() { return regen; }
        /** Seconds of fire set by hits. */
        public int ignite() { return ignite; }
        /** Ticks of slowness put by hits. */
        public int slow() { return slow; }
        /** Ticks of poison put by hits. */
        public int poison() { return poison; }
        public boolean sneakInvisible() { return sneakInvisible; }
        public float shadowChance() { return shadowChance; }
        public int shadowMax() { return shadowMax; }
        public int shadowLifetime() { return shadowLifetime; }
        public float shadowPower() { return shadowPower; }
    }
}
