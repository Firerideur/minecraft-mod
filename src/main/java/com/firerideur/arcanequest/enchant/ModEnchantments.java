package com.firerideur.arcanequest.enchant;

import com.firerideur.arcanequest.ArcaneQuest;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * The mod's enchantments. They are data (data/arcanequest/enchantment, made by tools/generate_resources.py); the
 * simple ones work by themselves (attributes, effects on hit), the others are done in {@link EnchantmentEvents}.
 */
public final class ModEnchantments {
    // Weapons
    public static final ResourceKey<Enchantment> LIFESTEAL = key("lifesteal");
    public static final ResourceKey<Enchantment> EXECUTIONER = key("executioner");
    public static final ResourceKey<Enchantment> CRITICAL = key("critical");
    public static final ResourceKey<Enchantment> GIANT_SLAYER = key("giant_slayer");
    public static final ResourceKey<Enchantment> BLEEDING = key("bleeding");
    public static final ResourceKey<Enchantment> THUNDERSTRIKE = key("thunderstrike");
    public static final ResourceKey<Enchantment> CLEAVE = key("cleave");
    public static final ResourceKey<Enchantment> SOUL_HARVEST = key("soul_harvest");
    // Tools
    public static final ResourceKey<Enchantment> VEIN_MINER = key("vein_miner");
    public static final ResourceKey<Enchantment> TIMBER = key("timber");
    public static final ResourceKey<Enchantment> AUTOSMELT = key("autosmelt");
    public static final ResourceKey<Enchantment> TELEKINESIS = key("telekinesis");
    public static final ResourceKey<Enchantment> WISDOM = key("wisdom");
    // Armor
    public static final ResourceKey<Enchantment> ARCANE_MIND = key("arcane_mind");
    public static final ResourceKey<Enchantment> REJUVENATION = key("rejuvenation");
    public static final ResourceKey<Enchantment> EVASION = key("evasion");
    public static final ResourceKey<Enchantment> NIGHT_SIGHT = key("night_sight");
    public static final ResourceKey<Enchantment> SECOND_WIND = key("second_wind");
    // Bows
    public static final ResourceKey<Enchantment> EXPLOSIVE_ARROWS = key("explosive_arrows");
    public static final ResourceKey<Enchantment> SNIPER = key("sniper");
    // Staffs
    public static final ResourceKey<Enchantment> SPELL_POWER = key("spell_power");
    public static final ResourceKey<Enchantment> MANA_EFFICIENCY = key("mana_efficiency");
    public static final ResourceKey<Enchantment> QUICK_CAST = key("quick_cast");
    // Anything
    public static final ResourceKey<Enchantment> SOULBOUND = key("soulbound");

    private ModEnchantments() {}

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, ArcaneQuest.id(name));
    }

    private static Optional<Holder.Reference<Enchantment>> holder(Level level, ResourceKey<Enchantment> key) {
        return level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(key);
    }

    /** Level of {@code key} on {@code stack} (0 if absent, or if a datapack removed the enchantment). */
    public static int level(Level level, ItemStack stack, ResourceKey<Enchantment> key) {
        return stack.isEmpty() ? 0 : holder(level, key).map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, stack)).orElse(0);
    }

    /** Highest level of {@code key} on what {@code entity} wears or holds, in the slots the enchantment works in. */
    public static int level(LivingEntity entity, ResourceKey<Enchantment> key) {
        return holder(entity.level(), key).map(h -> EnchantmentHelper.getEnchantmentLevel(h, entity)).orElse(0);
    }
}
