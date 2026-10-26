package com.firerideur.arcanequest.magic;

import net.minecraft.network.chat.Component;

/**
 * Spells cast by staffs. Damage and healing are multiplied by the caster's spell power.
 */
public enum Spell {
    FIRE_BOLT("fire_bolt", 8, 15, 6.0F),
    FROST_BOLT("frost_bolt", 8, 15, 4.0F),
    ARCANE_MISSILE("arcane_missile", 12, 10, 9.0F),
    LIGHTNING("lightning", 25, 60, 10.0F),
    HEAL("heal", 20, 80, 4.0F),
    ARCANE_BARRAGE("arcane_barrage", 30, 40, 8.0F);

    private final String key;
    private final int manaCost;
    private final int cooldownTicks;
    private final float basePower;

    Spell(String key, int manaCost, int cooldownTicks, float basePower) {
        this.key = key;
        this.manaCost = manaCost;
        this.cooldownTicks = cooldownTicks;
        this.basePower = basePower;
    }

    public String key() {
        return key;
    }

    public int manaCost() {
        return manaCost;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    /** Damage for attack spells, health restored for {@link #HEAL}. */
    public float basePower() {
        return basePower;
    }

    public Component displayName() {
        return Component.translatable("spell.arcanequest." + key);
    }

    public Component description() {
        return Component.translatable("spell.arcanequest." + key + ".desc");
    }
}
