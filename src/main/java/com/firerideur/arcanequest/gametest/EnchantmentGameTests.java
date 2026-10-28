package com.firerideur.arcanequest.gametest;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.enchant.ArcaneEnchanting;
import com.firerideur.arcanequest.enchant.EnchantmentEvents;
import com.firerideur.arcanequest.enchant.ModEnchantments;
import com.firerideur.arcanequest.magic.Mana;
import com.firerideur.arcanequest.registry.ModBlocks;
import com.firerideur.arcanequest.registry.ModItems;
import com.firerideur.arcanequest.stats.PlayerStats;
import com.firerideur.arcanequest.stats.StatsCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The mod's enchantments. */
@GameTestHolder(ArcaneQuest.MODID)
@SuppressWarnings("removal") // makeMockServerPlayerInLevel is the only mock player helper in 1.21.1
public class EnchantmentGameTests {
    private static final String EMPTY = "forge:empty3x3x3";

    private static ItemStack enchanted(GameTestHelper helper, ItemStack stack, ResourceKey<Enchantment> key, int level) {
        Holder<Enchantment> holder = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(key);
        stack.enchant(holder, level);
        return stack;
    }

    @GameTest(template = EMPTY)
    public static void enchantmentsAreLoaded(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        long ours = registry.keySet().stream().filter(id -> id.getNamespace().equals(ArcaneQuest.MODID)).count();
        helper.assertTrue(ours >= 30, "30 enchantments expected, found " + ours);
        helper.assertTrue(registry.getHolder(ModEnchantments.VEIN_MINER).isPresent(), "vein miner should exist");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void lifestealHeals(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, enchanted(helper, new ItemStack(Items.DIAMOND_SWORD), ModEnchantments.LIFESTEAL, 3));
        player.setHealth(10.0F);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        zombie.setNoAi(true);
        player.attack(zombie);
        helper.assertTrue(player.getHealth() > 10.0F, "the hit should heal, health " + player.getHealth());
        zombie.discard();
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 60)
    public static void veinMinerAndAutosmelt(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ItemStack pickaxe = enchanted(helper, new ItemStack(Items.DIAMOND_PICKAXE), ModEnchantments.VEIN_MINER, 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, enchanted(helper, pickaxe, ModEnchantments.AUTOSMELT, 1));
        BlockPos[] vein = {new BlockPos(0, 1, 0), new BlockPos(1, 1, 0), new BlockPos(2, 1, 0), new BlockPos(2, 2, 1)};
        for (BlockPos pos : vein) {
            helper.setBlock(pos, Blocks.IRON_ORE);
        }
        player.gameMode.destroyBlock(helper.absolutePos(vein[0]));
        for (BlockPos pos : vein) {
            helper.assertBlockNotPresent(Blocks.IRON_ORE, pos);
        }
        helper.runAfterDelay(2, () -> {
            AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(6);
            var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, area);
            helper.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(Items.IRON_INGOT)), "autosmelt should drop ingots");
            helper.assertFalse(drops.stream().anyMatch(e -> e.getItem().is(Items.RAW_IRON)), "no raw iron with autosmelt");
            drops.forEach(ItemEntity::discard);
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void staffAndHelmetBonuses(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        PlayerStats stats = StatsCapability.get(player).orElseThrow();
        float baseMana = Mana.max(player, stats);
        player.setItemSlot(EquipmentSlot.HEAD, enchanted(helper, new ItemStack(Items.IRON_HELMET), ModEnchantments.ARCANE_MIND, 3));
        helper.assertTrue(Mana.max(player, stats) == baseMana + 3 * Mana.MANA_PER_ARCANE_MIND, "arcane mind III: +30 mana");
        player.setItemInHand(InteractionHand.MAIN_HAND, enchanted(helper, new ItemStack(ModItems.FIRE_STAFF.get()),
                ModEnchantments.SPELL_POWER, 5));
        helper.assertTrue(Math.abs(EnchantmentEvents.spellPowerBonus(player) - 1.5F) < 1.0E-4F, "spell power V: +50%");
        helper.assertTrue(new ItemStack(ModItems.FIRE_STAFF.get()).isEnchantable(), "staffs should be enchantable");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void shelvesAddPowerUpToTheirCeiling(GameTestHelper helper) {
        ArcaneEnchanting.ShelfStats vanilla = ArcaneEnchanting.ShelfStats.VANILLA;
        ArcaneEnchanting.ShelfStats draconic = new ArcaneEnchanting.ShelfStats(5, 50, 5, 12);
        helper.assertTrue(ArcaneEnchanting.combine(Collections.nCopies(30, vanilla)).power() == 15, "vanilla shelves stop at 15");
        helper.assertTrue(ArcaneEnchanting.combine(Collections.nCopies(20, draconic)).power() == 50, "draconic shelves reach 50");
        List<ArcaneEnchanting.ShelfStats> mixed = new ArrayList<>(Collections.nCopies(15, vanilla));
        mixed.addAll(Collections.nCopies(3, draconic));
        helper.assertTrue(ArcaneEnchanting.combine(mixed).power() == 30, "15 + 3 draconic = 30, got " + ArcaneEnchanting.combine(mixed).power());
        helper.assertTrue(ArcaneEnchanting.combine(List.of()).chaos() == ArcaneEnchanting.BASE_CHAOS, "base chaos");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void strongTablesGoPastTheMaximumLevel(GameTestHelper helper) {
        ArcaneEnchanting.Stats table = new ArcaneEnchanting.Stats(50, 15, 0);
        boolean overLevel = false;
        for (int seed = 0; seed < 200 && !overLevel; seed++) {
            for (EnchantmentInstance instance : ArcaneEnchanting.select(helper.getLevel().registryAccess(), RandomSource.create(seed),
                    new ItemStack(Items.DIAMOND_SWORD), 100, table)) {
                overLevel |= instance.level > instance.enchantment.value().getMaxLevel();
                helper.assertTrue(instance.level <= instance.enchantment.value().getMaxLevel() + ArcaneEnchanting.MAX_OVER_LEVEL,
                        "never more than +" + ArcaneEnchanting.MAX_OVER_LEVEL);
            }
        }
        helper.assertTrue(overLevel, "a level 100 enchanting should go past the usual maximum");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void arcanaUnlocksTreasures(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        boolean treasure = false, treasureWithoutArcana = false;
        for (int seed = 0; seed < 300; seed++) {
            for (EnchantmentInstance instance : ArcaneEnchanting.select(helper.getLevel().registryAccess(), RandomSource.create(seed),
                    new ItemStack(Items.BOOK), 60, new ArcaneEnchanting.Stats(30, 15, 100))) {
                treasure |= instance.enchantment.is(EnchantmentTags.TREASURE);
            }
            for (EnchantmentInstance instance : ArcaneEnchanting.select(helper.getLevel().registryAccess(), RandomSource.create(seed),
                    new ItemStack(Items.BOOK), 60, new ArcaneEnchanting.Stats(30, 15, 0))) {
                treasureWithoutArcana |= instance.enchantment.is(EnchantmentTags.TREASURE);
            }
        }
        helper.assertTrue(registry.getTag(EnchantmentTags.TREASURE).isPresent(), "treasure tag");
        helper.assertTrue(treasure, "100% arcana should give treasure enchantments");
        helper.assertFalse(treasureWithoutArcana, "no treasure without arcana");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void tableCountsTheShelvesAroundIt(GameTestHelper helper) {
        // High above the grid: the shelves stand two blocks away from the table
        BlockPos table = helper.absolutePos(new BlockPos(1, 60, 1));
        var level = helper.getLevel();
        level.setBlock(table, ModBlocks.ARCANE_ENCHANTING_TABLE.get().defaultBlockState(), 3);
        int placed = 0;
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (placed < 8) {
                level.setBlock(table.offset(offset), ModBlocks.DRACONIC_BOOKSHELF.get().defaultBlockState(), 3);
                placed++;
            }
        }
        ArcaneEnchanting.Stats stats = ArcaneEnchanting.gather(level, table);
        helper.assertTrue(stats.power() == 40, "8 draconic shelves: power 40, got " + stats.power());
        helper.assertTrue(stats.maxLevel() == 80, "level 80 enchantings");
        helper.assertTrue(stats.arcana() == 96, "8 x 12% arcana, got " + stats.arcana());
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            level.setBlock(table.offset(offset), Blocks.AIR.defaultBlockState(), 3);
        }
        level.setBlock(table, Blocks.AIR.defaultBlockState(), 3);
        helper.succeed();
    }
}
