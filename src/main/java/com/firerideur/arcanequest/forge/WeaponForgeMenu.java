package com.firerideur.arcanequest.forge;

import com.firerideur.arcanequest.registry.ModBlocks;
import com.firerideur.arcanequest.registry.ModItems;
import com.firerideur.arcanequest.registry.ModMenus;
import com.firerideur.arcanequest.registry.ModRecipes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** Weapon Forge: template (kept) + ingots + grip/shaft, result taken like a smithing table. */
public class WeaponForgeMenu extends ItemCombinerMenu {
    public static final int TEMPLATE_X = 8, MATERIAL_X = 26, HANDLE_X = 44, RESULT_X = 98, SLOT_Y = 48;

    @Nullable
    private RecipeHolder<WeaponForgingRecipe> selected;

    /** Client side. */
    public WeaponForgeMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public WeaponForgeMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.WEAPON_FORGE.get(), id, inventory, access);
    }

    @Override
    protected ItemCombinerMenuSlotDefinition createInputSlotDefinitions() {
        return ItemCombinerMenuSlotDefinition.create()
                .withSlot(0, TEMPLATE_X, SLOT_Y, stack -> true)
                .withSlot(1, MATERIAL_X, SLOT_Y, stack -> true)
                .withSlot(2, HANDLE_X, SLOT_Y, stack -> true)
                .withResultSlot(3, RESULT_X, SLOT_Y)
                .build();
    }

    private WeaponForgingRecipe.Input input() {
        return new WeaponForgingRecipe.Input(inputSlots.getItem(0), inputSlots.getItem(1), inputSlots.getItem(2));
    }

    @Override
    public void createResult() {
        Optional<RecipeHolder<WeaponForgingRecipe>> recipe = player.level().getRecipeManager()
                .getRecipeFor(ModRecipes.WEAPON_FORGING.get(), input(), player.level());
        selected = recipe.orElse(null);
        resultSlots.setItem(0, recipe.map(r -> r.value().assemble(input(), player.level().registryAccess())).orElse(ItemStack.EMPTY));
        resultSlots.setRecipeUsed(selected);
    }

    @Override
    protected boolean mayPickup(Player player, boolean hasStack) {
        return selected != null && selected.value().matches(input(), player.level());
    }

    @Override
    protected void onTake(Player player, ItemStack stack) {
        stack.onCraftedBy(player.level(), player, stack.getCount());
        if (selected != null) {
            WeaponForgingRecipe recipe = selected.value();
            shrink(1, recipe.material().count());
            shrink(2, 1);
        }
        // Anvil sound
        access.execute((level, pos) -> level.levelEvent(1030, pos, 0));
        createResult();
    }

    private void shrink(int slot, int amount) {
        ItemStack stack = inputSlots.getItem(slot);
        if (!stack.isEmpty()) {
            stack.shrink(amount);
            inputSlots.setItem(slot, stack);
        }
    }

    /** True when something is in every slot but it isn't a valid recipe (the screen shows a red cross). */
    public boolean hasInvalidRecipe() {
        return !inputSlots.getItem(0).isEmpty() && !inputSlots.getItem(1).isEmpty() && !inputSlots.getItem(2).isEmpty()
                && resultSlots.getItem(0).isEmpty();
    }

    /** Shift-click sends blueprints, grips/shafts and ingots each to their own slot. */
    @Override
    public int getSlotToQuickMoveTo(ItemStack stack) {
        if (stack.is(ModItems.HAMMER_TEMPLATE.get())
                || ModItems.TEMPLATES.values().stream().anyMatch(template -> stack.is(template.get()))) {
            return 0;
        }
        if (stack.is(ModItems.WEAPON_GRIP.get()) || stack.is(ModItems.WEAPON_SHAFT.get())) {
            return 2;
        }
        return 1;
    }

    @Override
    protected boolean isValidBlock(BlockState state) {
        return state.is(ModBlocks.WEAPON_FORGE.get());
    }
}
