package com.firerideur.arcanequest.fusion;

import com.firerideur.arcanequest.registry.ModItems;
import com.firerideur.arcanequest.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class FusionForgeMenu extends AbstractContainerMenu {
    // Slot positions, shared with the screen
    public static final int A_X = 38, A_Y = 17;
    public static final int B_X = 38, B_Y = 53;
    public static final int CATALYST_X = 8, CATALYST_Y = 35;
    public static final int RESULT_X = 116, RESULT_Y = 35;
    private static final int CONTAINER_SLOTS = 4;

    private final Container container;
    private final ContainerData data;

    /** Client side. */
    public FusionForgeMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(CONTAINER_SLOTS), new SimpleContainerData(4));
    }

    public FusionForgeMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(ModMenus.FUSION_FORGE.get(), id);
        checkContainerSize(container, CONTAINER_SLOTS);
        checkContainerDataCount(data, 4);
        this.container = container;
        this.data = data;
        container.startOpen(inventory.player);

        addSlot(new Slot(container, FusionForgeBlockEntity.SLOT_A, A_X, A_Y));
        addSlot(new Slot(container, FusionForgeBlockEntity.SLOT_B, B_X, B_Y));
        addSlot(new Slot(container, FusionForgeBlockEntity.SLOT_CATALYST, CATALYST_X, CATALYST_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.FUSION_CATALYST.get());
            }
        });
        addSlot(new Slot(container, FusionForgeBlockEntity.SLOT_RESULT, RESULT_X, RESULT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
        addDataSlots(data);
    }

    public float progress() {
        int max = data.get(1);
        return max > 0 ? (float) data.get(0) / max : 0;
    }

    public int charge() {
        return data.get(2);
    }

    public int maxCharge() {
        return Math.max(1, data.get(3));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int playerStart = CONTAINER_SLOTS;
        int playerEnd = slots.size();
        if (index < CONTAINER_SLOTS) {
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (stack.is(ModItems.FUSION_CATALYST.get())) {
            if (!moveItemStackTo(stack, FusionForgeBlockEntity.SLOT_CATALYST, FusionForgeBlockEntity.SLOT_CATALYST + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, FusionForgeBlockEntity.SLOT_A, FusionForgeBlockEntity.SLOT_B + 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}
