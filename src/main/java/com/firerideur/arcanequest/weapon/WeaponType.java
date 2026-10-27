package com.firerideur.arcanequest.weapon;

import net.minecraft.network.chat.Component;

/**
 * Melee weapon families. Damage and speed are added on top of the material tier, like vanilla swords
 * (a sword is +3 damage, -2.4 speed).
 */
public enum WeaponType {
    /** Very fast, short reach, x1.75 damage when hitting from behind. */
    DAGGER("dagger", 1, -1.4F, -0.5F, 1),
    /** Fast and precise; right click to dash forward slashing everything on the way. */
    KATANA("katana", 3, -2.0F, 0.0F, 3),
    /** Two-handed and slow, every hit also cleaves enemies in front of you. */
    LONGSWORD("longsword", 6, -3.0F, 0.5F, 4),
    /** Long reach thrusts. */
    SPEAR("spear", 4, -2.8F, 2.0F, 2),
    /** Wide spinning sweep around you that heals you a little. */
    SCYTHE("scythe", 5, -3.0F, 1.0F, 3);

    private final String key;
    private final int damage;
    private final float speed;
    private final float reach;
    private final int materialCost;

    WeaponType(String key, int damage, float speed, float reach, int materialCost) {
        this.key = key;
        this.damage = damage;
        this.speed = speed;
        this.reach = reach;
        this.materialCost = materialCost;
    }

    public String key() {
        return key;
    }

    /** Bonus attack damage over the tier's. */
    public int damage() {
        return damage;
    }

    /** Attack speed modifier (4 + speed attacks per second). */
    public float speed() {
        return speed;
    }

    /** Extra entity reach in blocks. */
    public float reach() {
        return reach;
    }

    /** Ingots used in the Weapon Forge. */
    public int materialCost() {
        return materialCost;
    }

    /** Long hafted weapons need a shaft instead of a grip in the Weapon Forge. */
    public boolean usesShaft() {
        return this == SPEAR || this == SCYTHE;
    }

    public boolean twoHanded() {
        return this == LONGSWORD || this == SCYTHE || this == SPEAR;
    }

    public Component description() {
        return Component.translatable("weapon.arcanequest." + key + ".desc");
    }
}
