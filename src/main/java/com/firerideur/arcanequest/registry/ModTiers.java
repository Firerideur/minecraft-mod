package com.firerideur.arcanequest.registry;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;

public final class ModTiers {
    /** Between iron and diamond, but can already mine diamond and adamantite. */
    public static final Tier MITHRIL = new ForgeTier(900, 7.5F, 2.5F, 18, BlockTags.NEEDS_DIAMOND_TOOL,
            () -> Ingredient.of(ModItems.MITHRIL_INGOT.get()), BlockTags.INCORRECT_FOR_DIAMOND_TOOL);

    /** Better than netherite. */
    public static final Tier ADAMANTITE = new ForgeTier(2600, 10.0F, 4.5F, 16, BlockTags.NEEDS_DIAMOND_TOOL,
            () -> Ingredient.of(ModItems.ADAMANTITE_INGOT.get()), BlockTags.INCORRECT_FOR_NETHERITE_TOOL);

    // Alloys made in the Fusion Forge

    /** Iron + coal: a cheap upgrade over iron. */
    public static final Tier STEEL = new ForgeTier(500, 6.5F, 2.5F, 12, BlockTags.NEEDS_IRON_TOOL,
            () -> Ingredient.of(ModItems.STEEL_INGOT.get()), BlockTags.INCORRECT_FOR_IRON_TOOL);

    /** Mithril + gold: very enchantable. */
    public static final Tier ORICHALCUM = new ForgeTier(1200, 8.0F, 3.5F, 26, BlockTags.NEEDS_DIAMOND_TOOL,
            () -> Ingredient.of(ModItems.ORICHALCUM_INGOT.get()), BlockTags.INCORRECT_FOR_DIAMOND_TOOL);

    /** Arcanite + adamantite: weapons restore mana on hit. */
    public static final Tier ARCANIUM = new ForgeTier(2200, 9.0F, 4.0F, 30, BlockTags.NEEDS_DIAMOND_TOOL,
            () -> Ingredient.of(ModItems.ARCANIUM_INGOT.get()), BlockTags.INCORRECT_FOR_NETHERITE_TOOL);

    /** Mithril + adamantite: the strongest metal. */
    public static final Tier STARSTEEL = new ForgeTier(3500, 11.0F, 5.5F, 18, BlockTags.NEEDS_DIAMOND_TOOL,
            () -> Ingredient.of(ModItems.STARSTEEL_INGOT.get()), BlockTags.INCORRECT_FOR_NETHERITE_TOOL);

    /** Boss weapons. */
    public static final Tier LEGENDARY = new ForgeTier(3000, 10.0F, 5.0F, 22, BlockTags.NEEDS_DIAMOND_TOOL,
            () -> Ingredient.of(ModItems.ADAMANTITE_INGOT.get()), BlockTags.INCORRECT_FOR_NETHERITE_TOOL);

    private ModTiers() {}
}
