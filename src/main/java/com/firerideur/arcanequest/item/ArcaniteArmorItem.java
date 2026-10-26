package com.firerideur.arcanequest.item;

import com.firerideur.arcanequest.magic.Mana;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Mage armor: each worn piece adds max mana and mana regen. */
public class ArcaniteArmorItem extends ArmorItem {
    public ArcaniteArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.arcanequest.arcanite_armor", (int) Mana.MANA_PER_ARCANITE_PIECE)
                .withStyle(ChatFormatting.BLUE));
    }
}
