package com.firerideur.arcanequest.gametest;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.enchant.EnchantmentEvents;
import com.firerideur.arcanequest.enchant.ModEnchantments;
import com.firerideur.arcanequest.magic.Mana;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;

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
}
