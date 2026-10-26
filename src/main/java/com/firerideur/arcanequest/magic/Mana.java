package com.firerideur.arcanequest.magic;

import com.firerideur.arcanequest.registry.ModArmorMaterials;
import com.firerideur.arcanequest.stats.PlayerClass;
import com.firerideur.arcanequest.stats.PlayerStats;
import com.firerideur.arcanequest.stats.Stat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

/** Mana formulas. Works on both sides since armor is visible to the client. */
public final class Mana {
    public static final float BASE_MANA = 20.0F;
    public static final float MANA_PER_INTELLIGENCE = 5.0F;
    public static final float MANA_PER_ARCANITE_PIECE = 15.0F;

    private Mana() {}

    public static float max(Player player, PlayerStats stats) {
        float max = BASE_MANA + MANA_PER_INTELLIGENCE * stats.get(Stat.INTELLIGENCE)
                + MANA_PER_ARCANITE_PIECE * arcanitePieces(player);
        return stats.getPlayerClass() == PlayerClass.MAGE ? max * 1.5F : max;
    }

    /** Mana regenerated each tick: 1/s at base, +0.1/s per intelligence, +0.25/s per arcanite piece. */
    public static float regenPerTick(Player player, PlayerStats stats) {
        float perSecond = 1.0F + 0.1F * stats.get(Stat.INTELLIGENCE) + 0.25F * arcanitePieces(player);
        if (stats.getPlayerClass() == PlayerClass.MAGE) {
            perSecond *= 1.5F;
        }
        return perSecond / 20.0F;
    }

    public static int arcanitePieces(Player player) {
        int count = 0;
        for (ItemStack stack : player.getArmorSlots()) {
            if (stack.getItem() instanceof ArmorItem armor && ModArmorMaterials.ARCANITE.isPresent()
                    && armor.getMaterial().value() == ModArmorMaterials.ARCANITE.get()) {
                count++;
            }
        }
        return count;
    }
}
