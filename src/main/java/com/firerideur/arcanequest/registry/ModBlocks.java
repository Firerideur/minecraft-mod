package com.firerideur.arcanequest.registry;

import com.firerideur.arcanequest.ArcaneQuest;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ArcaneQuest.MODID);

    // Mithril: mid tier metal, between iron and diamond
    public static final RegistryObject<Block> MITHRIL_ORE = register("mithril_ore", () -> ore(MapColor.STONE, 3.0F, SoundType.STONE));
    public static final RegistryObject<Block> DEEPSLATE_MITHRIL_ORE = register("deepslate_mithril_ore", () -> ore(MapColor.DEEPSLATE, 4.5F, SoundType.DEEPSLATE));
    public static final RegistryObject<Block> MITHRIL_BLOCK = register("mithril_block", () -> metal(MapColor.COLOR_LIGHT_BLUE, 5.0F));
    public static final RegistryObject<Block> RAW_MITHRIL_BLOCK = register("raw_mithril_block", () -> metal(MapColor.COLOR_LIGHT_BLUE, 5.0F));

    // Arcanite: magic crystal used for staffs and mage armor
    public static final RegistryObject<Block> ARCANITE_ORE = register("arcanite_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 7), oreProps(MapColor.STONE, 3.0F, SoundType.STONE)));
    public static final RegistryObject<Block> DEEPSLATE_ARCANITE_ORE = register("deepslate_arcanite_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 7), oreProps(MapColor.DEEPSLATE, 4.5F, SoundType.DEEPSLATE)));
    public static final RegistryObject<Block> ARCANITE_BLOCK = register("arcanite_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops()
                    .strength(5.0F, 6.0F).sound(SoundType.AMETHYST).lightLevel(state -> 7)));

    // Adamantite: end game metal, deep underground only
    public static final RegistryObject<Block> ADAMANTITE_ORE = register("adamantite_ore", () -> ore(MapColor.DEEPSLATE, 6.0F, SoundType.DEEPSLATE));
    public static final RegistryObject<Block> ADAMANTITE_BLOCK = register("adamantite_block", () -> metal(MapColor.COLOR_RED, 7.0F));
    public static final RegistryObject<Block> RAW_ADAMANTITE_BLOCK = register("raw_adamantite_block", () -> metal(MapColor.COLOR_RED, 7.0F));

    private ModBlocks() {}

    private static RegistryObject<Block> register(String name, Supplier<Block> block) {
        RegistryObject<Block> object = BLOCKS.register(name, block);
        ModItems.registerBlockItem(name, object);
        return object;
    }

    private static BlockBehaviour.Properties oreProps(MapColor color, float strength, SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(color).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(strength, 3.0F).sound(sound);
    }

    private static Block ore(MapColor color, float strength, SoundType sound) {
        return new Block(oreProps(color, strength, sound));
    }

    private static Block metal(MapColor color, float strength) {
        return new Block(BlockBehaviour.Properties.of().mapColor(color).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                .requiresCorrectToolForDrops().strength(strength, 6.0F).sound(SoundType.METAL));
    }
}
