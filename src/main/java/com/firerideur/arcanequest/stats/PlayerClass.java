package com.firerideur.arcanequest.stats;

import net.minecraft.network.chat.Component;

/**
 * The class a player picks once. Each class has passive bonuses, see {@link StatEffects} and {@link ClassEvents}.
 */
public enum PlayerClass {
    NONE("none", 0xAAAAAA),
    /** +4 health, +2 armor, +10% melee damage, +10% knockback resistance. */
    WARRIOR("warrior", 0xE05050),
    /** +50% max mana and mana regen, +25% spell power. */
    MAGE("mage", 0xB070E0),
    /** +30% arrow damage, +5% movement speed. */
    ARCHER("archer", 0x50C050),
    /** Critical hits deal x2 instead of x1.5, +10% attack speed, +5% movement speed. */
    ASSASSIN("assassin", 0x50B0E0);

    private final String key;
    private final int color;

    PlayerClass(String key, int color) {
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
        return Component.translatable("class.arcanequest." + key);
    }

    public Component description() {
        return Component.translatable("class.arcanequest." + key + ".desc");
    }

    public static PlayerClass byId(int id) {
        PlayerClass[] values = values();
        return id >= 0 && id < values.length ? values[id] : NONE;
    }

    public static PlayerClass byKey(String key) {
        for (PlayerClass value : values()) {
            if (value.key.equals(key)) {
                return value;
            }
        }
        return NONE;
    }
}
