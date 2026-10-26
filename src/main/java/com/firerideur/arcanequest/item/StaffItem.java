package com.firerideur.arcanequest.item;

import com.firerideur.arcanequest.magic.Spell;
import com.firerideur.arcanequest.magic.SpellCaster;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Right click to cast the staff's spell. */
public class StaffItem extends Item {
    private final Spell spell;

    public StaffItem(Spell spell, Properties properties) {
        super(properties);
        this.spell = spell;
    }

    public Spell getSpell() {
        return spell;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            if (!SpellCaster.tryCast(serverPlayer, spell)) {
                return InteractionResultHolder.fail(stack);
            }
            player.getCooldowns().addCooldown(this, spell.cooldownTicks());
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }
        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(spell.displayName().copy().withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(spell.description().copy().withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.arcanequest.mana_cost", spell.manaCost()).withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.arcanequest.cooldown",
                String.format("%.1f", spell.cooldownTicks() / 20.0F)).withStyle(ChatFormatting.DARK_GRAY));
    }
}
