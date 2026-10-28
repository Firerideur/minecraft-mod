package com.firerideur.arcanequest.enchant;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** A bookshelf with enchanting stats (see {@link ArcaneEnchanting}); it also powers vanilla enchanting tables. */
public class ArcaneBookshelfBlock extends Block {
    private final ArcaneEnchanting.ShelfStats stats;

    public ArcaneBookshelfBlock(ArcaneEnchanting.ShelfStats stats, Properties properties) {
        super(properties);
        this.stats = stats;
    }

    public ArcaneEnchanting.ShelfStats stats() {
        return stats;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.arcanequest.shelf", format(stats.power()), format(stats.maxPower()),
                format(stats.chaos()), format(stats.arcana())).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private static String format(float value) {
        return value == Math.round(value) ? String.valueOf(Math.round(value)) : String.valueOf(value);
    }

    @Override
    public float getEnchantPowerBonus(BlockState state, LevelReader level, BlockPos pos) {
        return stats.power();
    }
}
