package com.firerideur.arcanequest.dungeon;

import com.firerideur.arcanequest.registry.ModItems;
import com.firerideur.arcanequest.weapon.WeaponType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** What dungeons give: experience when cleared, and loot that gets better with the stars. */
public final class Rewards {
    private Rewards() {}

    /** Bonus experience for each player in the dungeon when its boss falls. */
    public static long clearXp(int stars) {
        return 400L * stars * stars;
    }

    /** Small chest found in a room. */
    public static List<ItemStack> roomLoot(int stars, RandomSource random) {
        List<ItemStack> loot = new ArrayList<>();
        loot.add(new ItemStack(Items.BREAD, 2 + random.nextInt(4)));
        loot.add(new ItemStack(stars <= 2 ? Items.IRON_INGOT : stars <= 4 ? ModItems.MITHRIL_INGOT.get() : ModItems.ADAMANTITE_INGOT.get(),
                1 + random.nextInt(3)));
        if (random.nextFloat() < 0.5F) {
            loot.add(new ItemStack(Items.ARROW, 8 + random.nextInt(16)));
        }
        if (stars >= 3 && random.nextFloat() < 0.5F) {
            loot.add(new ItemStack(ModItems.ARCANITE.get(), 1 + random.nextInt(2)));
        }
        if (random.nextFloat() < 0.15F * stars) {
            loot.add(new ItemStack(Items.GOLDEN_APPLE));
        }
        return loot;
    }

    /** The boss chest: materials, a forged weapon of the gate's tier, and rare treasures at high ranks. */
    public static List<ItemStack> bossLoot(int stars, RandomSource random) {
        List<ItemStack> loot = new ArrayList<>();
        loot.add(new ItemStack(Items.DIAMOND, stars + random.nextInt(stars + 1)));
        loot.add(new ItemStack(ModItems.MITHRIL_INGOT.get(), 2 + stars));
        loot.add(new ItemStack(ModItems.ARCANITE.get(), 1 + stars));
        if (stars >= 3) {
            loot.add(new ItemStack(ModItems.ADAMANTITE_INGOT.get(), stars - 1));
        }
        loot.add(new ItemStack(ModItems.FUSION_CATALYST.get(), 1 + stars / 2));
        ModItems.WeaponMaterial material = weaponMaterial(stars);
        WeaponType[] types = WeaponType.values();
        Item weapon = ModItems.WEAPONS.get(material).get(types[random.nextInt(types.length)]).get();
        loot.add(new ItemStack(weapon));
        if (stars >= 5) {
            loot.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
        }
        if (stars >= 6) {
            loot.add(new ItemStack(Items.NETHERITE_INGOT, stars - 5));
        }
        if (stars == 7) {
            loot.add(new ItemStack(Items.TOTEM_OF_UNDYING));
        }
        loot.add(new ItemStack(Items.EXPERIENCE_BOTTLE, 4 * stars));
        return loot;
    }

    static ModItems.WeaponMaterial weaponMaterial(int stars) {
        return switch (stars) {
            case 1 -> ModItems.WeaponMaterial.STEEL;
            case 2 -> ModItems.WeaponMaterial.MITHRIL;
            case 3 -> ModItems.WeaponMaterial.ORICHALCUM;
            case 4 -> ModItems.WeaponMaterial.ADAMANTITE;
            case 5 -> ModItems.WeaponMaterial.ARCANIUM;
            default -> ModItems.WeaponMaterial.STARSTEEL;
        };
    }
}
