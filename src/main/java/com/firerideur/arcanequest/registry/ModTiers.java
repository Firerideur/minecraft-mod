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

    /** Boss weapons. */
    public static final Tier LEGENDARY = new ForgeTier(3000, 10.0F, 5.0F, 22, BlockTags.NEEDS_DIAMOND_TOOL,
            () -> Ingredient.of(ModItems.ADAMANTITE_INGOT.get()), BlockTags.INCORRECT_FOR_NETHERITE_TOOL);

    private ModTiers() {}
}
