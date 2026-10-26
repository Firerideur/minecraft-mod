package com.firerideur.arcanequest.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;
import java.util.function.Supplier;

/** Use on a block to summon a boss on top of it. */
public class BossSummonItem extends Item {
    private static final double ANNOUNCE_RADIUS = 64.0;

    private final Supplier<? extends EntityType<? extends Mob>> boss;

    public BossSummonItem(Supplier<? extends EntityType<? extends Mob>> boss, Properties properties) {
        super(properties);
        this.boss = boss;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Mob mob = boss.get().spawn(level, pos, MobSpawnType.MOB_SUMMONED);
        if (mob == null) {
            return InteractionResult.FAIL;
        }
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        Component message = Component.translatable("message.arcanequest.boss_summoned", mob.getDisplayName())
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(mob) < ANNOUNCE_RADIUS * ANNOUNCE_RADIUS) {
                player.sendSystemMessage(message);
            }
        }
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.arcanequest.summon", boss.get().getDescription())
                .withStyle(ChatFormatting.DARK_RED));
    }
}
