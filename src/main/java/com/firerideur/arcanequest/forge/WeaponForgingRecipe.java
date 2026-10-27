package com.firerideur.arcanequest.forge;

import com.firerideur.arcanequest.fusion.CountedIngredient;
import com.firerideur.arcanequest.registry.ModRecipes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/** Weapon Forge recipe: template (kept) + ingots + grip or shaft = weapon. */
public record WeaponForgingRecipe(Ingredient template, CountedIngredient material, Ingredient handle, ItemStack result)
        implements Recipe<WeaponForgingRecipe.Input> {

    public record Input(ItemStack template, ItemStack material, ItemStack handle) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return switch (index) {
                case 0 -> template;
                case 1 -> material;
                case 2 -> handle;
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public int size() {
            return 3;
        }
    }

    @Override
    public boolean matches(Input input, Level level) {
        return template.test(input.template()) && material.test(input.material()) && handle.test(input.handle());
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, template, material.ingredient(), handle);
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.WEAPON_FORGING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.WEAPON_FORGING.get();
    }

    public static class Serializer implements RecipeSerializer<WeaponForgingRecipe> {
        private static final MapCodec<WeaponForgingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("template").forGetter(WeaponForgingRecipe::template),
                CountedIngredient.CODEC.fieldOf("material").forGetter(WeaponForgingRecipe::material),
                Ingredient.CODEC_NONEMPTY.fieldOf("handle").forGetter(WeaponForgingRecipe::handle),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(WeaponForgingRecipe::result)
        ).apply(instance, WeaponForgingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, WeaponForgingRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, WeaponForgingRecipe::template,
                CountedIngredient.STREAM_CODEC, WeaponForgingRecipe::material,
                Ingredient.CONTENTS_STREAM_CODEC, WeaponForgingRecipe::handle,
                ItemStack.STREAM_CODEC, WeaponForgingRecipe::result,
                WeaponForgingRecipe::new);

        @Override
        public MapCodec<WeaponForgingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, WeaponForgingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
