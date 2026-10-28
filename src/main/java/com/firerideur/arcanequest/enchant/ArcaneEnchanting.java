package com.firerideur.arcanequest.enchant;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Enchanting at the Arcane Enchanting Table, in the spirit of Apotheosis. The bookshelves around the table give three
 * stats:
 * <ul>
 *   <li><b>Power</b> (Eterna): twice the highest enchanting level, up to {@link #MAX_POWER} (level 100). Each kind of
 *       bookshelf only adds power up to its own ceiling, so better shelves are needed to go higher;</li>
 *   <li><b>Chaos</b> (Quanta): how much the power of an enchanting varies, up or down;</li>
 *   <li><b>Arcana</b>: rare enchantments become as likely as common ones, extra enchantments, and treasure
 *       enchantments (Mending, Soulbound...) from {@link #TREASURE_ARCANA}.</li>
 * </ul>
 * A strong enough enchanting goes past the usual maximum level of an enchantment (up to +{@link #MAX_OVER_LEVEL}).
 */
public final class ArcaneEnchanting {
    public static final float MAX_POWER = 50.0F;
    /** Chaos every table has (vanilla tables vary by ±15% too). */
    public static final float BASE_CHAOS = 15.0F;
    public static final float TREASURE_ARCANA = 25.0F;
    public static final int MAX_OVER_LEVEL = 3;
    /** Power needed past the maximum level's cost for each level beyond it. */
    private static final int POWER_PER_OVER_LEVEL = 20;

    /** What a bookshelf brings: power (up to {@code maxPower} in total), chaos and arcana, in percent. */
    public record ShelfStats(float power, float maxPower, float chaos, float arcana) {
        public static final ShelfStats VANILLA = new ShelfStats(1.0F, 15.0F, 0, 0);
    }

    /** The stats of a table. */
    public record Stats(float power, float chaos, float arcana) {
        /** Highest level an enchanting can cost (and reach). */
        public int maxLevel() {
            return Math.round(power * 2);
        }
    }

    private ArcaneEnchanting() {}

    /** Stats of the bookshelves around the table at {@code table} (same places as for a vanilla table). */
    public static Stats gather(Level level, BlockPos table) {
        List<ShelfStats> shelves = new ArrayList<>();
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (EnchantingTableBlock.isValidBookShelf(level, table, offset)) {
                BlockPos pos = table.offset(offset);
                BlockState state = level.getBlockState(pos);
                shelves.add(state.getBlock() instanceof ArcaneBookshelfBlock shelf ? shelf.stats()
                        : new ShelfStats(state.getEnchantPowerBonus(level, pos), ShelfStats.VANILLA.maxPower(), 0, 0));
            }
        }
        return combine(shelves);
    }

    /** Power adds up shelf by shelf, from the lowest ceiling to the highest; chaos and arcana simply add up. */
    public static Stats combine(List<ShelfStats> shelves) {
        float power = 0, chaos = BASE_CHAOS, arcana = 0;
        List<ShelfStats> sorted = new ArrayList<>(shelves);
        sorted.sort(Comparator.comparingDouble(ShelfStats::maxPower));
        for (ShelfStats shelf : sorted) {
            power = Math.max(power, Math.min(shelf.maxPower(), power + shelf.power()));
            chaos += shelf.chaos();
            arcana += shelf.arcana();
        }
        return new Stats(Math.min(MAX_POWER, power), Mth.clamp(chaos, 0, 100), Mth.clamp(arcana, 0, 100));
    }

    /** Level cost of each of the three choices: about a third, two thirds and all of the table's power. */
    public static int cost(RandomSource random, int slot, Stats stats, ItemStack stack) {
        if (stack.getEnchantmentValue() <= 0 || stats.maxLevel() <= 0) {
            return 0;
        }
        int max = stats.maxLevel();
        int cost = Math.round(max * (slot + 1) / 3.0F * (0.8F + 0.2F * random.nextFloat()));
        return Mth.clamp(cost, slot + 1, max);
    }

    /** The enchantments an enchanting costing {@code cost} levels gives (the same for the same random seed). */
    public static List<EnchantmentInstance> select(RegistryAccess registries, RandomSource random, ItemStack stack, int cost,
                                                   Stats stats) {
        int enchantability = stack.getEnchantmentValue();
        if (enchantability <= 0) {
            return List.of();
        }
        var registry = registries.registryOrThrow(Registries.ENCHANTMENT);
        Set<Holder<Enchantment>> pool = new LinkedHashSet<>();
        registry.getTag(EnchantmentTags.IN_ENCHANTING_TABLE).ifPresent(tag -> tag.forEach(pool::add));
        if (stats.arcana() >= TREASURE_ARCANA) {
            registry.getTag(EnchantmentTags.TREASURE).ifPresent(tag -> tag.forEach(pool::add));
        }
        registry.getTag(EnchantmentTags.CURSE).ifPresent(tag -> tag.forEach(pool::remove));

        // Chaos makes the power vary, enchantability raises it a little (as at a vanilla table)
        float swing = (random.nextFloat() * 2 - 1) * stats.chaos() / 100.0F;
        int power = Math.max(1, Math.round((cost + 1 + random.nextInt(enchantability / 4 + 1)) * (1 + swing)));
        boolean book = stack.is(Items.BOOK);
        List<EnchantmentInstance> available = new ArrayList<>();
        for (Holder<Enchantment> holder : pool) {
            Enchantment enchantment = holder.value();
            if (!book && !stack.canApplyAtEnchantingTable(enchantment)) {
                continue;
            }
            int level = 0;
            for (int i = enchantment.getMaxLevel(); i >= enchantment.getMinLevel(); i--) {
                if (power >= enchantment.getMinCost(i)) {
                    level = i;
                    break;
                }
            }
            if (level == 0) {
                continue;
            }
            if (level == enchantment.getMaxLevel() && enchantment.getMaxLevel() > 1) {
                level += Math.min(MAX_OVER_LEVEL, (power - enchantment.getMinCost(level)) / POWER_PER_OVER_LEVEL);
            }
            available.add(new EnchantmentInstance(holder, level));
        }
        List<EnchantmentInstance> chosen = new ArrayList<>();
        pick(random, available, stats, chosen);
        int extra = power;
        while (!available.isEmpty() && random.nextInt(50) <= extra) {
            pick(random, available, stats, chosen);
            extra /= 2;
        }
        if (random.nextFloat() * 100 < stats.arcana()) {
            pick(random, available, stats, chosen);
        }
        if (book && chosen.size() > 1) {
            chosen.remove(random.nextInt(chosen.size()));
        }
        return chosen;
    }

    /** Picks one enchantment by weight (Arcana evens the weights out), then drops what isn't compatible with it. */
    private static void pick(RandomSource random, List<EnchantmentInstance> available, Stats stats, List<EnchantmentInstance> chosen) {
        if (available.isEmpty()) {
            return;
        }
        float total = 0;
        float[] weights = new float[available.size()];
        for (int i = 0; i < weights.length; i++) {
            int weight = available.get(i).enchantment.value().getWeight();
            weights[i] = weight + stats.arcana() / 100.0F * (10 - weight);
            total += weights[i];
        }
        float roll = random.nextFloat() * total;
        int index = 0;
        while (index < weights.length - 1 && roll >= weights[index]) {
            roll -= weights[index];
            index++;
        }
        EnchantmentInstance picked = available.remove(index);
        chosen.add(picked);
        available.removeIf(other -> !Enchantment.areCompatible(picked.enchantment, other.enchantment));
    }
}
