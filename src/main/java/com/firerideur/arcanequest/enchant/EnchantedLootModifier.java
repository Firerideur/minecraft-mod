package com.firerideur.arcanequest.enchant;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/**
 * Block drops of enchanted tools: Autosmelt smelts them (ores into ingots, sand into glass...), Telekinesis puts them
 * straight into the miner's inventory.
 */
public class EnchantedLootModifier extends LootModifier {
    public static final MapCodec<EnchantedLootModifier> CODEC =
            RecordCodecBuilder.mapCodec(instance -> codecStart(instance).apply(instance, EnchantedLootModifier::new));

    public EnchantedLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        ItemStack tool = context.getParamOrNull(LootContextParams.TOOL);
        if (tool == null || tool.isEmpty() || !context.hasParam(LootContextParams.BLOCK_STATE)) {
            return loot;
        }
        ServerLevel level = context.getLevel();
        if (ModEnchantments.level(level, tool, ModEnchantments.AUTOSMELT) > 0) {
            ObjectArrayList<ItemStack> smelted = new ObjectArrayList<>();
            for (ItemStack stack : loot) {
                smelted.add(level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level)
                        .map(recipe -> recipe.value().getResultItem(level.registryAccess()).copyWithCount(
                                recipe.value().getResultItem(level.registryAccess()).getCount() * stack.getCount()))
                        .orElse(stack));
            }
            loot = smelted;
        }
        Entity miner = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (miner instanceof Player player && ModEnchantments.level(level, tool, ModEnchantments.TELEKINESIS) > 0) {
            ObjectArrayList<ItemStack> left = new ObjectArrayList<>();
            for (ItemStack stack : loot) {
                ItemStack copy = stack.copy();
                player.getInventory().add(copy);
                if (!copy.isEmpty()) {
                    left.add(copy);
                }
            }
            loot = left;
        }
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
