package com.firerideur.arcanequest.enchant;

import com.firerideur.arcanequest.registry.ModBlocks;
import com.firerideur.arcanequest.registry.ModMenus;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.common.Tags;

import java.util.List;

/**
 * Like the vanilla enchanting menu (item, lapis, three choices), with the stats of {@link ArcaneEnchanting}: choices up
 * to level 100, enchantments past their usual maximum, more of them with Arcana.
 */
public class ArcaneEnchantingMenu extends AbstractContainerMenu {
    public static final int ITEM_X = 15, LAPIS_X = 35, SLOT_Y = 47;
    public static final int INVENTORY_Y = 104;

    private final Container slotsContainer = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };
    private final ContainerLevelAccess access;
    private final RandomSource random = RandomSource.create();
    private final DataSlot seed = DataSlot.standalone();
    public final int[] costs = new int[3];
    public final int[] clueEnchantment = {-1, -1, -1};
    public final int[] clueLevel = {-1, -1, -1};
    /** Power ×10, chaos and arcana of the table (for the screen). */
    public final int[] stats = new int[3];

    /** Client side. */
    public ArcaneEnchantingMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public ArcaneEnchantingMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.ARCANE_ENCHANTING.get(), id);
        this.access = access;
        addSlot(new Slot(slotsContainer, 0, ITEM_X, SLOT_Y) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(slotsContainer, 1, LAPIS_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Tags.Items.ENCHANTING_FUELS);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_Y + 58));
        }
        for (int i = 0; i < 3; i++) {
            addDataSlot(DataSlot.shared(costs, i));
            addDataSlot(DataSlot.shared(clueEnchantment, i));
            addDataSlot(DataSlot.shared(clueLevel, i));
            addDataSlot(DataSlot.shared(stats, i));
        }
        addDataSlot(seed).set(inventory.player.getEnchantmentSeed());
    }

    private ArcaneEnchanting.Stats tableStats() {
        return new ArcaneEnchanting.Stats(stats[0] / 10.0F, stats[1], stats[2]);
    }

    @Override
    public void slotsChanged(Container container) {
        if (container != slotsContainer) {
            return;
        }
        ItemStack stack = container.getItem(0);
        access.execute((level, pos) -> {
            ArcaneEnchanting.Stats table = ArcaneEnchanting.gather(level, pos);
            stats[0] = Math.round(table.power() * 10);
            stats[1] = Math.round(table.chaos());
            stats[2] = Math.round(table.arcana());
            IdMap<Holder<Enchantment>> ids = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).asHolderIdMap();
            for (int slot = 0; slot < 3; slot++) {
                costs[slot] = 0;
                clueEnchantment[slot] = -1;
                clueLevel[slot] = -1;
                if (stack.isEmpty() || !stack.isEnchantable()) {
                    continue;
                }
                random.setSeed(seed.get() + slot * 31L);
                costs[slot] = ArcaneEnchanting.cost(random, slot, table, stack);
                if (costs[slot] > 0) {
                    List<EnchantmentInstance> list = enchantments(level.registryAccess(), stack, slot, table);
                    if (list.isEmpty()) {
                        costs[slot] = 0;
                    } else {
                        clueEnchantment[slot] = ids.getId(list.get(0).enchantment);
                        clueLevel[slot] = list.get(0).level;
                    }
                }
            }
            broadcastChanges();
        });
    }

    private List<EnchantmentInstance> enchantments(RegistryAccess registries, ItemStack stack, int slot,
                                                   ArcaneEnchanting.Stats table) {
        random.setSeed(seed.get() + slot * 7919L + costs[slot]);
        return ArcaneEnchanting.select(registries, random, stack, costs[slot], table);
    }

    @Override
    public boolean clickMenuButton(Player player, int slot) {
        if (slot < 0 || slot >= 3) {
            return false;
        }
        ItemStack stack = slotsContainer.getItem(0);
        ItemStack lapis = slotsContainer.getItem(1);
        int lapisCost = slot + 1;
        boolean free = player.hasInfiniteMaterials();
        if (costs[slot] <= 0 || stack.isEmpty() || !free && (lapis.getCount() < lapisCost
                || player.experienceLevel < costs[slot] || player.experienceLevel < lapisCost)) {
            return false;
        }
        access.execute((level, pos) -> {
            List<EnchantmentInstance> list = enchantments(level.registryAccess(), stack, slot, tableStats());
            if (list.isEmpty()) {
                return;
            }
            player.onEnchantmentPerformed(stack, lapisCost);
            ItemStack result = stack.is(Items.BOOK) ? stack.transmuteCopy(Items.ENCHANTED_BOOK) : stack;
            for (EnchantmentInstance instance : list) {
                result.enchant(instance.enchantment, instance.level);
            }
            slotsContainer.setItem(0, result);
            lapis.consume(lapisCost, player);
            if (lapis.isEmpty()) {
                slotsContainer.setItem(1, ItemStack.EMPTY);
            }
            player.awardStat(Stats.ENCHANT_ITEM);
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.ENCHANTED_ITEM.trigger(serverPlayer, result, lapisCost);
            }
            seed.set(player.getEnchantmentSeed());
            slotsChanged(slotsContainer);
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 0.8F + level.random.nextFloat() * 0.2F);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.2F);
        });
        return true;
    }

    public int lapisCount() {
        ItemStack lapis = slotsContainer.getItem(1);
        return lapis.isEmpty() ? 0 : lapis.getCount();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, slotsContainer));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.ARCANE_ENCHANTING_TABLE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, 38, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(Tags.Items.ENCHANTING_FUELS)) {
            if (!moveItemStackTo(stack, 1, 2, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (slots.get(0).hasItem()) {
                return ItemStack.EMPTY;
            }
            slots.get(0).setByPlayer(stack.split(1));
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return stack.getCount() == copy.getCount() ? ItemStack.EMPTY : copy;
    }
}
