package com.firerideur.arcanequest.enchant;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The Arcane Enchanting Table: enchants with the stats of the bookshelves around it (see {@link ArcaneEnchanting}). */
public class ArcaneEnchantingTableBlock extends Block {
    private static final Component TITLE = Component.translatable("container.arcanequest.arcane_enchanting");
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);

    public ArcaneEnchantingTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            player.openMenu(state.getMenuProvider(level, pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inventory, player) ->
                new ArcaneEnchantingMenu(id, inventory, ContainerLevelAccess.create(level, pos)), TITLE);
    }

    /** Runes fly from the bookshelves to the table, and arcane sparks rise above it. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (random.nextInt(12) == 0 && EnchantingTableBlock.isValidBookShelf(level, pos, offset)) {
                level.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5,
                        offset.getX() + random.nextFloat() - 0.5, offset.getY() - random.nextFloat() - 1.0,
                        offset.getZ() + random.nextFloat() - 0.5);
            }
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.WITCH, pos.getX() + 0.2 + random.nextFloat() * 0.6, pos.getY() + 0.9,
                    pos.getZ() + 0.2 + random.nextFloat() * 0.6, 0, 0.05, 0);
        }
    }
}
