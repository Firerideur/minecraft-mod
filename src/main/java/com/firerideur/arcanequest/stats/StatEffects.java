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

import java.util.List;

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
    private static final ResourceLocation CLASS_HEALTH = ArcaneQuest.id("class_health");
    private static final ResourceLocation CLASS_ARMOR = ArcaneQuest.id("class_armor");
    private static final ResourceLocation CLASS_TOUGHNESS = ArcaneQuest.id("class_toughness");
    private static final ResourceLocation CLASS_KNOCKBACK = ArcaneQuest.id("class_knockback");
    private static final ResourceLocation CLASS_DAMAGE = ArcaneQuest.id("class_damage");
    private static final ResourceLocation CLASS_ATTACK_SPEED = ArcaneQuest.id("class_attack_speed");
    private static final ResourceLocation CLASS_SPEED = ArcaneQuest.id("class_speed");
    /** Modifiers of the classes of older versions, removed from saved players. */
    private static final String[] LEGACY = {"warrior_health", "warrior_armor", "warrior_damage", "warrior_knockback",
            "archer_speed", "assassin_speed", "assassin_attack_speed"};

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

        PlayerClass.Bonuses bonus = stats.getPlayerClass().bonuses();
        set(player, Attributes.MAX_HEALTH, CLASS_HEALTH, bonus.health(), AttributeModifier.Operation.ADD_VALUE);
        set(player, Attributes.ARMOR, CLASS_ARMOR, bonus.armor(), AttributeModifier.Operation.ADD_VALUE);
        set(player, Attributes.ARMOR_TOUGHNESS, CLASS_TOUGHNESS, bonus.toughness(), AttributeModifier.Operation.ADD_VALUE);
        set(player, Attributes.KNOCKBACK_RESISTANCE, CLASS_KNOCKBACK, bonus.knockback(), AttributeModifier.Operation.ADD_VALUE);
        set(player, Attributes.ATTACK_DAMAGE, CLASS_DAMAGE, bonus.melee(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(player, Attributes.ATTACK_SPEED, CLASS_ATTACK_SPEED, bonus.attackSpeed(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(player, Attributes.MOVEMENT_SPEED, CLASS_SPEED, bonus.speed(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        for (String legacy : LEGACY) {
            for (var attribute : List.of(Attributes.MAX_HEALTH, Attributes.ARMOR, Attributes.ATTACK_DAMAGE,
                    Attributes.KNOCKBACK_RESISTANCE, Attributes.MOVEMENT_SPEED, Attributes.ATTACK_SPEED)) {
                set(player, attribute, ArcaneQuest.id(legacy), 0, AttributeModifier.Operation.ADD_VALUE);
            }
        }

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
