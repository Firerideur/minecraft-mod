package com.firerideur.arcanequest.stats;

import com.firerideur.arcanequest.ArcaneQuest;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Turns allocated stats into vanilla attribute modifiers.
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

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
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
