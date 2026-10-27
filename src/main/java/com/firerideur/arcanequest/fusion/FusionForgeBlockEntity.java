package com.firerideur.arcanequest.fusion;

import com.firerideur.arcanequest.registry.ModBlockEntities;
import com.firerideur.arcanequest.registry.ModItems;
import com.firerideur.arcanequest.registry.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Fusion Forge: melts two ingredients into an alloy. Each Fusion Catalyst powers {@link #CHARGES_PER_CATALYST} fusions.
 */
public class FusionForgeBlockEntity extends BaseContainerBlockEntity {
    public static final int SLOT_A = 0;
    public static final int SLOT_B = 1;
    public static final int SLOT_CATALYST = 2;
    public static final int SLOT_RESULT = 3;
    public static final int CHARGES_PER_CATALYST = 4;

    private NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
    private int progress;
    private int maxProgress = 200;
    private int charge;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> charge;
                default -> CHARGES_PER_CATALYST;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> maxProgress = value;
                case 2 -> charge = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public FusionForgeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_FORGE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FusionForgeBlockEntity forge) {
        boolean wasWorking = forge.progress > 0;
        FusionRecipe.Input input = new FusionRecipe.Input(forge.items.get(SLOT_A), forge.items.get(SLOT_B));
        Optional<RecipeHolder<FusionRecipe>> recipe = input.isEmpty() ? Optional.empty()
                : level.getRecipeManager().getRecipeFor(ModRecipes.FUSION.get(), input, level);

        if (recipe.isPresent() && forge.canOutput(recipe.get().value().result())) {
            if (forge.charge <= 0 && forge.items.get(SLOT_CATALYST).is(ModItems.FUSION_CATALYST.get())) {
                forge.items.get(SLOT_CATALYST).shrink(1);
                forge.charge = CHARGES_PER_CATALYST;
            }
            if (forge.charge > 0) {
                FusionRecipe fusion = recipe.get().value();
                forge.maxProgress = fusion.time();
                if (++forge.progress >= forge.maxProgress) {
                    forge.fuse(fusion, input);
                }
            } else {
                forge.progress = 0;
            }
        } else {
            forge.progress = 0;
        }

        boolean working = forge.progress > 0;
        if (wasWorking != working) {
            level.setBlock(pos, state.setValue(FusionForgeBlock.LIT, working), 3);
        }
        setChanged(level, pos, state);
    }

    private boolean canOutput(ItemStack result) {
        ItemStack current = items.get(SLOT_RESULT);
        return current.isEmpty() || (ItemStack.isSameItemSameComponents(current, result)
                && current.getCount() + result.getCount() <= current.getMaxStackSize());
    }

    private void fuse(FusionRecipe recipe, FusionRecipe.Input input) {
        boolean inOrder = recipe.inOrder(input);
        items.get(inOrder ? SLOT_A : SLOT_B).shrink(recipe.first().count());
        items.get(inOrder ? SLOT_B : SLOT_A).shrink(recipe.second().count());
        ItemStack current = items.get(SLOT_RESULT);
        if (current.isEmpty()) {
            items.set(SLOT_RESULT, recipe.result().copy());
        } else {
            current.grow(recipe.result().getCount());
        }
        charge--;
        progress = 0;
    }

    public ContainerData getData() {
        return data;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.arcanequest.fusion_forge");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_RESULT -> false;
            case SLOT_CATALYST -> stack.is(ModItems.FUSION_CATALYST.get());
            default -> true;
        };
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new FusionForgeMenu(id, inventory, this, data);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        progress = tag.getInt("progress");
        maxProgress = Math.max(1, tag.getInt("max_progress"));
        charge = tag.getInt("charge");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("progress", progress);
        tag.putInt("max_progress", maxProgress);
        tag.putInt("charge", charge);
    }
}
