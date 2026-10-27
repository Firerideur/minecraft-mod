package com.firerideur.arcanequest.gametest;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.forge.WeaponForgeMenu;
import com.firerideur.arcanequest.fusion.FusionForgeBlockEntity;
import com.firerideur.arcanequest.registry.ModBlocks;
import com.firerideur.arcanequest.registry.ModItems;
import com.firerideur.arcanequest.registry.ModItems.WeaponMaterial;
import com.firerideur.arcanequest.registry.ModRecipes;
import com.firerideur.arcanequest.weapon.WeaponEvents;
import com.firerideur.arcanequest.weapon.WeaponType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraftforge.gametest.GameTestHolder;

/** Weapons, Fusion Forge and Weapon Forge. Run with {@code ./gradlew runGameTestServer}. */
@GameTestHolder(ArcaneQuest.MODID)
@SuppressWarnings("removal") // makeMockServerPlayerInLevel is the only mock player helper in 1.21.1
public class ForgingGameTests {
    private static final String EMPTY = "forge:empty3x3x3";

    private static Item weapon(WeaponMaterial material, WeaponType type) {
        return ModItems.WEAPONS.get(material).get(type).get();
    }

    private static double modifier(ItemStack stack, Holder<Attribute> attribute) {
        ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        return modifiers.modifiers().stream().filter(e -> e.attribute().equals(attribute))
                .mapToDouble(e -> e.modifier().amount()).sum();
    }

    @GameTest(template = EMPTY)
    public static void weaponStats(GameTestHelper helper) {
        ItemStack spear = new ItemStack(weapon(WeaponMaterial.MITHRIL, WeaponType.SPEAR));
        ItemStack dagger = new ItemStack(weapon(WeaponMaterial.IRON, WeaponType.DAGGER));
        ItemStack longsword = new ItemStack(weapon(WeaponMaterial.MITHRIL, WeaponType.LONGSWORD));
        helper.assertTrue(modifier(spear, Attributes.ENTITY_INTERACTION_RANGE) == 2.0, "spear should have +2 reach");
        helper.assertTrue(modifier(dagger, Attributes.ENTITY_INTERACTION_RANGE) == -0.5, "dagger should have shorter reach");
        helper.assertTrue(modifier(longsword, Attributes.ATTACK_DAMAGE) == 6 + 2.5, "mithril longsword damage");
        helper.assertTrue(modifier(dagger, Attributes.ATTACK_SPEED) > modifier(longsword, Attributes.ATTACK_SPEED),
                "dagger should be faster than longsword");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void daggerBackstab(GameTestHelper helper) {
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        zombie.setYRot(0);
        zombie.setYHeadRot(0); // facing +Z
        player.setPos(zombie.getX(), zombie.getY(), zombie.getZ() - 2);
        helper.assertTrue(WeaponEvents.isBehind(player, zombie), "player at -Z is behind a zombie facing +Z");
        player.setPos(zombie.getX(), zombie.getY(), zombie.getZ() + 2);
        helper.assertTrue(!WeaponEvents.isBehind(player, zombie), "player at +Z is in front");
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void fusionForgeMakesSteel(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, ModBlocks.FUSION_FORGE.get());
        FusionForgeBlockEntity forge = helper.getBlockEntity(pos);
        forge.setItem(FusionForgeBlockEntity.SLOT_A, new ItemStack(Items.COAL, 2)); // order doesn't matter
        forge.setItem(FusionForgeBlockEntity.SLOT_B, new ItemStack(Items.IRON_INGOT, 1));
        forge.setItem(FusionForgeBlockEntity.SLOT_CATALYST, new ItemStack(ModItems.FUSION_CATALYST.get()));
        helper.succeedWhen(() -> {
            helper.assertTrue(forge.getItem(FusionForgeBlockEntity.SLOT_RESULT).is(ModItems.STEEL_INGOT.get()), "no steel yet");
            helper.assertTrue(forge.getItem(FusionForgeBlockEntity.SLOT_A).isEmpty(), "coal should be used");
            helper.assertTrue(forge.getItem(FusionForgeBlockEntity.SLOT_B).isEmpty(), "iron should be used");
            helper.assertTrue(forge.getItem(FusionForgeBlockEntity.SLOT_CATALYST).isEmpty(), "catalyst should be used");
            helper.assertTrue(forge.getData().get(2) == FusionForgeBlockEntity.CHARGES_PER_CATALYST - 1, "3 charges left");
        });
    }

    @GameTest(template = EMPTY)
    public static void weaponForgeMakesKatana(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        WeaponForgeMenu menu = new WeaponForgeMenu(1, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), pos));
        menu.getSlot(0).set(new ItemStack(ModItems.TEMPLATES.get(WeaponType.KATANA).get()));
        menu.getSlot(1).set(new ItemStack(ModItems.MITHRIL_INGOT.get(), 5));
        menu.getSlot(2).set(new ItemStack(ModItems.WEAPON_GRIP.get(), 2));
        menu.createResult();

        ItemStack result = menu.getSlot(3).getItem();
        helper.assertTrue(result.is(weapon(WeaponMaterial.MITHRIL, WeaponType.KATANA)), "should make a mithril katana, got " + result);
        menu.getSlot(3).onTake(player, result);
        helper.assertTrue(menu.getSlot(0).getItem().getCount() == 1, "template is kept");
        helper.assertTrue(menu.getSlot(1).getItem().getCount() == 2, "katana costs 3 ingots");
        helper.assertTrue(menu.getSlot(2).getItem().getCount() == 1, "one grip used");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void forgingRecipesLoad(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        int fusions = recipes.getAllRecipesFor(ModRecipes.FUSION.get()).size();
        int weapons = recipes.getAllRecipesFor(ModRecipes.WEAPON_FORGING.get()).size();
        helper.assertTrue(fusions == 4, "expected 4 fusion recipes, got " + fusions);
        helper.assertTrue(weapons == WeaponMaterial.values().length * WeaponType.values().length,
                "expected one forging recipe per weapon, got " + weapons);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void weaponForgeShiftClick(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getInventory().setItem(0, new ItemStack(ModItems.WEAPON_GRIP.get(), 2));
        player.getInventory().setItem(1, new ItemStack(ModItems.MITHRIL_INGOT.get(), 5));
        player.getInventory().setItem(2, new ItemStack(ModItems.TEMPLATES.get(WeaponType.KATANA).get()));
        WeaponForgeMenu menu = new WeaponForgeMenu(1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 1))));
        // Hotbar slots come after the 4 forge slots and the 27 inventory slots
        int hotbarStart = 4 + 27;
        menu.quickMoveStack(player, hotbarStart);
        menu.quickMoveStack(player, hotbarStart + 1);
        menu.quickMoveStack(player, hotbarStart + 2);
        helper.assertTrue(menu.getSlot(0).getItem().is(ModItems.TEMPLATES.get(WeaponType.KATANA).get()), "blueprint should go to slot 1");
        helper.assertTrue(menu.getSlot(1).getItem().is(ModItems.MITHRIL_INGOT.get()), "ingots should go to slot 2");
        helper.assertTrue(menu.getSlot(2).getItem().is(ModItems.WEAPON_GRIP.get()), "grip should go to slot 3");
        helper.succeed();
    }
}
