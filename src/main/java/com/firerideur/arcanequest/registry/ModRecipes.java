package com.firerideur.arcanequest.registry;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.forge.WeaponForgingRecipe;
import com.firerideur.arcanequest.fusion.FusionRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, ArcaneQuest.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, ArcaneQuest.MODID);

    public static final RegistryObject<RecipeType<FusionRecipe>> FUSION = TYPES.register("fusion",
            () -> RecipeType.simple(ArcaneQuest.id("fusion")));
    public static final RegistryObject<RecipeSerializer<FusionRecipe>> FUSION_SERIALIZER = SERIALIZERS.register("fusion",
            FusionRecipe.Serializer::new);

    public static final RegistryObject<RecipeType<WeaponForgingRecipe>> WEAPON_FORGING = TYPES.register("weapon_forging",
            () -> RecipeType.simple(ArcaneQuest.id("weapon_forging")));
    public static final RegistryObject<RecipeSerializer<WeaponForgingRecipe>> WEAPON_FORGING_SERIALIZER = SERIALIZERS.register("weapon_forging",
            WeaponForgingRecipe.Serializer::new);

    private ModRecipes() {}
}
