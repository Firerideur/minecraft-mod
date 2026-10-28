package com.firerideur.arcanequest.client;

import com.firerideur.arcanequest.ArcaneQuest;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

/** Enchanted books of the mod say what their enchantments do. */
public final class EnchantmentTooltips {
    private EnchantmentTooltips() {}

    public static void onTooltip(ItemTooltipEvent event) {
        ItemEnchantments stored = event.getItemStack().get(DataComponents.STORED_ENCHANTMENTS);
        if (stored == null || stored.isEmpty()) {
            return;
        }
        for (Holder<Enchantment> enchantment : stored.keySet()) {
            enchantment.unwrapKey().filter(key -> key.location().getNamespace().equals(ArcaneQuest.MODID)).ifPresent(key ->
                    event.getToolTip().add(Component.literal("  ").append(Component.translatable(
                            "enchantment." + ArcaneQuest.MODID + "." + key.location().getPath() + ".desc"))
                            .withStyle(ChatFormatting.DARK_GRAY)));
        }
    }
}
