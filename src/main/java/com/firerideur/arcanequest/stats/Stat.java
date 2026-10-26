package com.firerideur.arcanequest.stats;

import net.minecraft.network.chat.Component;

/**
 * The stats a player can improve by spending points earned on level up.
 */
public enum Stat {
    /** Bonus melee damage. */
    STRENGTH("strength", 0xE05050),
    /** Bonus max health. */
    VITALITY("vitality", 0x50C050),
    /** Bonus movement and attack speed. */
    AGILITY("agility", 0x50B0E0),
    /** Will power magic (mana, spell damage) in a later phase. */
    INTELLIGENCE("intelligence", 0xB070E0);

    private final String key;
    private final int color;

    Stat(String key, int color) {
        this.key = key;
        this.color = color;
    }

    public String key() {
        return key;
    }

    public int color() {
        return color;
    }

    public Component displayName() {
        return Component.translatable("stat.arcanequest." + key);
    }

    public Component description() {
        return Component.translatable("stat.arcanequest." + key + ".desc");
    }

    public static Stat byId(int id) {
        Stat[] values = values();
        return id >= 0 && id < values.length ? values[id] : null;
    }
}
