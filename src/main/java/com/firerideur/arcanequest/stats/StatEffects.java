package com.firerideur.arcanequest.stats;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.magic.Mana;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Turns allocated stats and class passives into vanilla attribute modifiers.
 */
public final class StatEffects {
    public static final double DAMAGE_PER_STRENGTH = 0.5;
    public static final double HEALTH_PER_VITALITY = 1.0;
    public static final double SPEED_PER_AGILITY = 0.005;
    public static final double ATTACK_SPEED_PER_AGILITY = 0.01;

    private static final ResourceLocation STRENGTH_DAMAGE = ArcaneQuest.id("strength_damage");
    private static final ResourceLocation VITALITY_HEALTH = ArcaneQuest.id("vitality_health");
    private static final ResourceLocation AGILITY_SPEED = ArcaneQuest.id("agility_speed");
    private static final ResourceLocation AGILITY_ATTACK_SPEED = ArcaneQuest.id("agility_attack_speed");
    private static final ResourceLocation WARRIOR_HEALTH = ArcaneQuest.id("warrior_health");
    private static final ResourceLocation WARRIOR_ARMOR = ArcaneQuest.id("warrior_armor");
    private static final ResourceLocation WARRIOR_DAMAGE = ArcaneQuest.id("warrior_damage");
    private static final ResourceLocation WARRIOR_KNOCKBACK = ArcaneQuest.id("warrior_knockback");
    private static final ResourceLocation ARCHER_SPEED = ArcaneQuest.id("archer_speed");
    private static final ResourceLocation ASSASSIN_SPEED = ArcaneQuest.id("assassin_speed");
    private static final ResourceLocation ASSASSIN_ATTACK_SPEED = ArcaneQuest.id("assassin_attack_speed");

    private StatEffects() {}

    public static void apply(Player player, PlayerStats stats) {
        set(player, Attributes.ATTACK_DAMAGE, STRENGTH_DAMAGE,
                stats.get(Stat.STRENGTH) * DAMAGE_PER_STRENGTH, AttributeModifier.Operation.ADD_VALUE);
        set(player, Attributes.MAX_HEALTH, VITALITY_HEALTH,
                stats.get(Stat.VITALITY) * HEALTH_PER_VITALITY, AttributeModifier.Operation.ADD_VALUE);
        set(player, Attributes.MOVEMENT_SPEED, AGILITY_SPEED,
                stats.get(Stat.AGILITY) * SPEED_PER_AGILITY, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(player, Attributes.ATTACK_SPEED, AGILITY_ATTACK_SPEED,
                stats.get(Stat.AGILITY) * ATTACK_SPEED_PER_AGILITY, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        PlayerClass playerClass = stats.getPlayerClass();
        boolean warrior = playerClass == PlayerClass.WARRIOR;
        boolean archer = playerClass == PlayerClass.ARCHER;
        boolean assassin = playerClass == PlayerClass.ASSASSIN;
        set(player, Attributes.MAX_HEALTH, WARRIOR_HEALTH, warrior ? 4.0 : 0, AttributeModifier.Operation.ADD_VALUE);
        set(player, Attributes.ARMOR, WARRIOR_ARMOR, warrior ? 2.0 : 0, AttributeModifier.Operation.ADD_VALUE);
        set(player, Attributes.ATTACK_DAMAGE, WARRIOR_DAMAGE, warrior ? 0.1 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(player, Attributes.KNOCKBACK_RESISTANCE, WARRIOR_KNOCKBACK, warrior ? 0.1 : 0, AttributeModifier.Operation.ADD_VALUE);
        set(player, Attributes.MOVEMENT_SPEED, ARCHER_SPEED, archer ? 0.05 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(player, Attributes.MOVEMENT_SPEED, ASSASSIN_SPEED, assassin ? 0.05 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(player, Attributes.ATTACK_SPEED, ASSASSIN_ATTACK_SPEED, assassin ? 0.1 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        stats.setMana(Math.min(stats.getMana(), Mana.max(player, stats)));
    }

    // Permanent modifiers are saved with the player, so max health is already right when health is loaded.
    private static void set(Player player, Holder<Attribute> attribute, ResourceLocation id, double amount,
                            AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        if (amount == 0) {
            instance.removeModifier(id);
        } else {
            instance.addOrReplacePermanentModifier(new AttributeModifier(id, amount, operation));
        }
    }
}
