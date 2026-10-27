package com.firerideur.arcanequest.character;

import com.firerideur.arcanequest.registry.ModMenus;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * The Arcane Quest character menu (K): inventory, equipment and a 2x2 crafting grid, like the vanilla
 * inventory but laid out for {@link com.firerideur.arcanequest.client.CharacterScreen}.
 * Slot order matches {@link InventoryMenu}: 0 result, 1-4 grid, 5-8 armor, 9-35 inventory, 36-44 hotbar, 45 offhand.
 */
public class CharacterMenu extends AbstractContainerMenu {
    public enum View { INVENTORY, CRAFTING, SKILLS, MAP }

    // Layout, relative to the screen's top-left corner (slot frames; items are 1 pixel inside)
    public static final int INVENTORY_X = 100, INVENTORY_W = 176, PANEL_H = 130;
    public static final int EQUIPMENT_X = 280, EQUIPMENT_W = 124;
    public static final int GRID_X = INVENTORY_X + 7, GRID_Y = 26, HOTBAR_Y = 104;
    public static final int ARMOR_X = EQUIPMENT_X + 8, ARMOR_Y = 26;
    public static final int OFFHAND_X = EQUIPMENT_X + 98, OFFHAND_Y = 80;
    public static final int CRAFT_X = EQUIPMENT_X + 12, CRAFT_Y = 40;
    public static final int RESULT_X = EQUIPMENT_X + 82, RESULT_Y = 48;

    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final ResourceLocation[] ARMOR_ICONS = {InventoryMenu.EMPTY_ARMOR_SLOT_HELMET,
            InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS};

    private final CraftingContainer craftSlots = new TransientCraftingContainer(this, 2, 2);
    private final ResultContainer resultSlots = new ResultContainer();
    private final Player owner;
    /** Which tab the client shows; hidden slots can't be clicked. Server side it stays INVENTORY. */
    private View view = View.INVENTORY;

    public CharacterMenu(int id, Inventory inventory) {
        super(ModMenus.CHARACTER.get(), id);
        this.owner = inventory.player;

        addSlot(new ResultSlot(owner, craftSlots, resultSlots, 0, RESULT_X + 1, RESULT_Y + 1) {
            @Override
            public boolean isActive() {
                return view == View.CRAFTING;
            }
        });
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                addSlot(new Slot(craftSlots, col + row * 2, CRAFT_X + col * 18 + 1, CRAFT_Y + row * 18 + 1) {
                    @Override
                    public boolean isActive() {
                        return view == View.CRAFTING;
                    }
                });
            }
        }
        for (int i = 0; i < 4; i++) {
            addSlot(new EquipmentSlotView(inventory, owner, ARMOR_SLOTS[i], 39 - i, ARMOR_X + 1, ARMOR_Y + i * 18 + 1, ARMOR_ICONS[i]));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + (row + 1) * 9, GRID_X + col * 18 + 1, GRID_Y + row * 18 + 1) {
                    @Override
                    public boolean isActive() {
                        return view == View.INVENTORY || view == View.CRAFTING;
                    }
                });
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, GRID_X + col * 18 + 1, HOTBAR_Y + 1) {
                @Override
                public boolean isActive() {
                    return view == View.INVENTORY || view == View.CRAFTING;
                }
            });
        }
        addSlot(new EquipmentSlotView(inventory, owner, EquipmentSlot.OFFHAND, 40, OFFHAND_X + 1, OFFHAND_Y + 1,
                InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD));
    }

    public View getView() {
        return view;
    }

    public void setView(View view) {
        this.view = view;
    }

    /** Armor and offhand slots, only shown on the inventory tab. */
    private class EquipmentSlotView extends Slot {
        private final Player player;
        private final EquipmentSlot equipmentSlot;
        private final ResourceLocation emptyIcon;

        EquipmentSlotView(Container container, Player player, EquipmentSlot slot, int index, int x, int y, ResourceLocation emptyIcon) {
            super(container, index, x, y);
            this.player = player;
            this.equipmentSlot = slot;
            this.emptyIcon = emptyIcon;
        }

        @Override
        public boolean isActive() {
            return view == View.INVENTORY;
        }

        @Override
        public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
            player.onEquipItem(equipmentSlot, oldStack, newStack);
            super.setByPlayer(newStack, oldStack);
        }

        @Override
        public int getMaxStackSize() {
            return equipmentSlot == EquipmentSlot.OFFHAND ? super.getMaxStackSize() : 1;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return equipmentSlot == EquipmentSlot.OFFHAND || stack.canEquip(equipmentSlot, player);
        }

        @Override
        public boolean mayPickup(Player player) {
            ItemStack stack = getItem();
            // Curse of Binding
            boolean bound = !stack.isEmpty() && !player.isCreative()
                    && EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE);
            return !bound && super.mayPickup(player);
        }

        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            return Pair.of(InventoryMenu.BLOCK_ATLAS, emptyIcon);
        }
    }

    @Override
    public void slotsChanged(Container container) {
        Level level = owner.level();
        if (level.isClientSide() || !(owner instanceof ServerPlayer serverPlayer)) {
            return;
        }
        CraftingInput input = craftSlots.asCraftInput();
        ItemStack result = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> recipe = level.getServer().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, level);
        if (recipe.isPresent() && resultSlots.setRecipeUsed(level, serverPlayer, recipe.get())) {
            ItemStack crafted = recipe.get().value().assemble(input, level.registryAccess());
            if (crafted.isItemEnabled(level.enabledFeatures())) {
                result = crafted;
            }
        }
        resultSlots.setItem(0, result);
        setRemoteSlot(0, result);
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), 0, result));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        resultSlots.clearContent();
        if (!player.level().isClientSide()) {
            clearContainer(player, craftSlots);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    /** Same rules as the vanilla inventory. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            original = stack.copy();
            EquipmentSlot equipment = player.getEquipmentSlotForItem(original);
            if (index == 0) {
                if (!moveItemStackTo(stack, 9, 45, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, original);
            } else if (index < 9) {
                if (!moveItemStackTo(stack, 9, 45, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (equipment.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && !slots.get(8 - equipment.getIndex()).hasItem()) {
                int target = 8 - equipment.getIndex();
                if (!moveItemStackTo(stack, target, target + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (equipment == EquipmentSlot.OFFHAND && !slots.get(45).hasItem()) {
                if (!moveItemStackTo(stack, 45, 46, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 36) {
                if (!moveItemStackTo(stack, 36, 45, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 45) {
                if (!moveItemStackTo(stack, 9, 36, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 9, 45, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY, original);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == original.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
            if (index == 0) {
                player.drop(stack, false);
            }
        }
        return original;
    }
}
