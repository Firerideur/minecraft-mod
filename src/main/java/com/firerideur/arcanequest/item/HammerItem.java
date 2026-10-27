package com.firerideur.arcanequest.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Mining hammer: breaks a square of blocks facing the player (3x3 up to 9x9). Sneak to break a single block.
 * Slower than a pickaxe, more so the bigger the square.
 */
public class HammerItem extends PickaxeItem {
    /** Neighbors may be at most this much harder than the mined block (so stone never takes obsidian with it). */
    private static final float HARDNESS_RATIO = 2.0F;

    private static boolean breakingArea;

    private final int radius;

    public HammerItem(Tier tier, int radius, Properties properties) {
        super(tier, properties.attributes(DiggerItem.createAttributes(tier, 6.0F, -3.4F)));
        this.radius = radius;
    }

    public int getRadius() {
        return radius;
    }

    public int getSize() {
        return radius * 2 + 1;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return super.getDestroySpeed(stack, state) / (1.0F + 0.5F * radius);
    }

    /** Blocks around {@code center} in the plane facing the player, without the center itself. */
    public List<BlockPos> areaAround(Player player, BlockPos center) {
        Direction facing = Direction.orderedByNearest(player)[0];
        List<BlockPos> area = new ArrayList<>();
        for (int a = -radius; a <= radius; a++) {
            for (int b = -radius; b <= radius; b++) {
                if (a == 0 && b == 0) {
                    continue;
                }
                area.add(switch (facing.getAxis()) {
                    case X -> center.offset(0, a, b);
                    case Y -> center.offset(a, 0, b);
                    case Z -> center.offset(a, b, 0);
                });
            }
        }
        return area;
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (breakingArea || !(event.getPlayer() instanceof ServerPlayer player) || player.isShiftKeyDown()) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof HammerItem hammer) || !hammer.isCorrectToolForDrops(stack, event.getState())) {
            return;
        }
        Level level = player.level();
        float centerHardness = event.getState().getDestroySpeed(level, event.getPos());
        breakingArea = true;
        try {
            for (BlockPos pos : hammer.areaAround(player, event.getPos())) {
                if (stack.isEmpty()) {
                    break; // the hammer broke
                }
                BlockState state = level.getBlockState(pos);
                float hardness = state.getDestroySpeed(level, pos);
                if (state.isAir() || hardness < 0 || hardness > centerHardness * HARDNESS_RATIO + 0.5F
                        || !hammer.isCorrectToolForDrops(stack, state)) {
                    continue;
                }
                player.gameMode.destroyBlock(pos);
            }
        } finally {
            breakingArea = false;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.arcanequest.hammer", getSize(), getSize()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.arcanequest.hammer.sneak").withStyle(ChatFormatting.DARK_GRAY));
    }
}
