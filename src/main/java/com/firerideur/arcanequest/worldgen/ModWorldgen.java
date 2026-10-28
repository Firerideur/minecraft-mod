package com.firerideur.arcanequest.worldgen;

import com.firerideur.arcanequest.ArcaneQuest;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.carver.CaveCarverConfiguration;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Cave generation: giant caverns, the carver biome modifier, underground lakes and the cave decorator. */
public final class ModWorldgen {
    public static final DeferredRegister<WorldCarver<?>> CARVERS = DeferredRegister.create(Registries.CARVER, ArcaneQuest.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, ArcaneQuest.MODID);
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, ArcaneQuest.MODID);

    public static final RegistryObject<WorldCarver<CaveCarverConfiguration>> CAVERN =
            CARVERS.register("cavern", () -> new CavernCarver(CaveCarverConfiguration.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> CAVE_DECORATOR =
            FEATURES.register("cave_decorator", () -> new CaveDecoratorFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> UNDERGROUND_LAKE =
            FEATURES.register("underground_lake", () -> new UndergroundLakeFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<MapCodec<AddCarversBiomeModifier>> ADD_CARVERS =
            BIOME_MODIFIERS.register("add_carvers", () -> AddCarversBiomeModifier.CODEC);

    private ModWorldgen() {}

    public static void register(IEventBus modEventBus) {
        CARVERS.register(modEventBus);
        FEATURES.register(modEventBus);
        BIOME_MODIFIERS.register(modEventBus);
    }
}
