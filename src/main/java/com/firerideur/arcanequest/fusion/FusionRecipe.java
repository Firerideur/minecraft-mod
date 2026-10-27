package com.firerideur.arcanequest.fusion;

import com.firerideur.arcanequest.registry.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/** Fusion Forge recipe: two ingredients (in any slot order) melted together into an alloy. */
public record FusionRecipe(CountedIngredient first, CountedIngredient second, ItemStack result, int time)
        implements Recipe<FusionRecipe.Input> {

    public record Input(ItemStack a, ItemStack b) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return index == 0 ? a : b;
        }

        @Override
        public int size() {
            return 2;
        }
    }

    /** True if slot A holds the first ingredient (else the recipe matches with the slots swapped). */
    public boolean inOrder(Input input) {
        return first.test(input.a()) && second.test(input.b());
    }

    @Override
    public boolean matches(Input input, Level level) {
        return inOrder(input) || (first.test(input.b()) && second.test(input.a()));
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
        return NonNullList.of(Ingredient.EMPTY, first.ingredient(), second.ingredient());
    }

    @Override
    public boolean isSpecial() {
        // Keeps it out of the vanilla recipe book
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.FUSION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.FUSION.get();
    }

    public static class Serializer implements RecipeSerializer<FusionRecipe> {
        private static final MapCodec<FusionRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                CountedIngredient.CODEC.fieldOf("first").forGetter(FusionRecipe::first),
                CountedIngredient.CODEC.fieldOf("second").forGetter(FusionRecipe::second),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(FusionRecipe::result),
                Codec.intRange(1, 6000).optionalFieldOf("time", 200).forGetter(FusionRecipe::time)
        ).apply(instance, FusionRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, FusionRecipe> STREAM_CODEC = StreamCodec.composite(
                CountedIngredient.STREAM_CODEC, FusionRecipe::first,
                CountedIngredient.STREAM_CODEC, FusionRecipe::second,
                ItemStack.STREAM_CODEC, FusionRecipe::result,
                ByteBufCodecs.VAR_INT, FusionRecipe::time,
                FusionRecipe::new);

        @Override
        public MapCodec<FusionRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FusionRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
