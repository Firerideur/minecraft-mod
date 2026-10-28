package com.firerideur.arcanequest.enchant;

import com.firerideur.arcanequest.ArcaneQuest;
import com.mojang.serialization.MapCodec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, ArcaneQuest.MODID);

    public static final RegistryObject<MapCodec<EnchantedLootModifier>> ENCHANTED_LOOT =
            LOOT_MODIFIERS.register("enchanted_loot", () -> EnchantedLootModifier.CODEC);

    private ModLootModifiers() {}
}
