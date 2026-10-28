package com.firerideur.arcanequest.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

/**
 * Adds carvers to biomes (Forge only ships a modifier for features):
 * {@code {"type": "arcanequest:add_carvers", "biomes": "#minecraft:is_overworld", "carvers": [...], "step": "air"}}.
 */
public record AddCarversBiomeModifier(HolderSet<Biome> biomes, HolderSet<ConfiguredWorldCarver<?>> carvers,
                                      GenerationStep.Carving step) implements BiomeModifier {
    public static final MapCodec<AddCarversBiomeModifier> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(AddCarversBiomeModifier::biomes),
            RegistryCodecs.homogeneousList(Registries.CONFIGURED_CARVER).fieldOf("carvers").forGetter(AddCarversBiomeModifier::carvers),
            GenerationStep.Carving.CODEC.optionalFieldOf("step", GenerationStep.Carving.AIR).forGetter(AddCarversBiomeModifier::step)
    ).apply(builder, AddCarversBiomeModifier::new));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase == Phase.ADD && biomes.contains(biome)) {
            for (Holder<ConfiguredWorldCarver<?>> carver : carvers) {
                builder.getGenerationSettings().addCarver(step, carver);
            }
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
