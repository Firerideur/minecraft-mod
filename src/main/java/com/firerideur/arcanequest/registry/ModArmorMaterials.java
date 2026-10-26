package com.firerideur.arcanequest.registry;

import com.firerideur.arcanequest.ArcaneQuest;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, ArcaneQuest.MODID);

    public static final RegistryObject<ArmorMaterial> MITHRIL = register("mithril", 3, 6, 7, 3, 18,
            SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.0F, () -> Ingredient.of(ModItems.MITHRIL_INGOT.get()));

    /** Low defense, but each piece gives bonus mana, see {@link com.firerideur.arcanequest.magic.Mana}. */
    public static final RegistryObject<ArmorMaterial> ARCANITE = register("arcanite", 2, 4, 5, 2, 30,
            SoundEvents.ARMOR_EQUIP_GOLD, 0.0F, 0.0F, () -> Ingredient.of(ModItems.ARCANITE.get()));

    public static final RegistryObject<ArmorMaterial> ADAMANTITE = register("adamantite", 4, 7, 9, 4, 15,
            SoundEvents.ARMOR_EQUIP_NETHERITE, 3.5F, 0.15F, () -> Ingredient.of(ModItems.ADAMANTITE_INGOT.get()));

    /** Durability multipliers, used with {@link ArmorItem.Type#getDurability(int)}. */
    public static final int MITHRIL_DURABILITY = 28;
    public static final int ARCANITE_DURABILITY = 20;
    public static final int ADAMANTITE_DURABILITY = 42;

    private ModArmorMaterials() {}

    private static RegistryObject<ArmorMaterial> register(String name, int boots, int leggings, int chestplate, int helmet,
                                                          int enchantability, Holder<SoundEvent> sound, float toughness,
                                                          float knockbackResistance, Supplier<Ingredient> repair) {
        Map<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.BOOTS, boots);
        defense.put(ArmorItem.Type.LEGGINGS, leggings);
        defense.put(ArmorItem.Type.CHESTPLATE, chestplate);
        defense.put(ArmorItem.Type.HELMET, helmet);
        defense.put(ArmorItem.Type.BODY, chestplate + 2);
        List<ArmorMaterial.Layer> layers = List.of(new ArmorMaterial.Layer(ArcaneQuest.id(name)));
        return ARMOR_MATERIALS.register(name, () -> new ArmorMaterial(defense, enchantability, sound, repair, layers,
                toughness, knockbackResistance));
    }
}
