package com.firerideur.arcanequest.registry;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.enchant.ArcaneBookshelfBlock;
import com.firerideur.arcanequest.enchant.ArcaneEnchanting;
import com.firerideur.arcanequest.enchant.ArcaneEnchantingTableBlock;
import com.firerideur.arcanequest.forge.WeaponForgeBlock;
import com.firerideur.arcanequest.fusion.FusionForgeBlock;
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

    // Workstations
    public static final RegistryObject<Block> FUSION_FORGE = register("fusion_forge",
            () -> new FusionForgeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops()
                    .strength(4.0F, 8.0F).sound(SoundType.NETHER_BRICKS)
                    .lightLevel(state -> state.getValue(FusionForgeBlock.LIT) ? 13 : 0)));
    public static final RegistryObject<Block> WEAPON_FORGE = register("weapon_forge",
            () -> new WeaponForgeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops()
                    .strength(4.0F, 1200.0F).sound(SoundType.ANVIL)));

    // Arcane enchanting: the table and bookshelves with enchanting stats (power, max power, chaos, arcana)
    public static final RegistryObject<Block> ARCANE_ENCHANTING_TABLE = register("arcane_enchanting_table",
            () -> new ArcaneEnchantingTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .requiresCorrectToolForDrops().strength(5.0F, 1200.0F).lightLevel(state -> 9).sound(SoundType.AMETHYST)));
    public static final RegistryObject<Block> REINFORCED_BOOKSHELF = shelf("reinforced_bookshelf", 1.5F, 20, 3, 0, SoundType.DEEPSLATE_BRICKS);
    public static final RegistryObject<Block> MITHRIL_BOOKSHELF = shelf("mithril_bookshelf", 2.0F, 25, 0, 4, SoundType.METAL);
    public static final RegistryObject<Block> ANCHORED_BOOKSHELF = shelf("anchored_bookshelf", 1.0F, 30, -12, 0, SoundType.STONE);
    public static final RegistryObject<Block> INFERNAL_BOOKSHELF = shelf("infernal_bookshelf", 2.5F, 30, 8, 0, SoundType.NETHER_BRICKS);
    public static final RegistryObject<Block> ABYSSAL_BOOKSHELF = shelf("abyssal_bookshelf", 2.5F, 30, 0, 6, SoundType.STONE);
    public static final RegistryObject<Block> ARCANITE_BOOKSHELF = shelf("arcanite_bookshelf", 3.0F, 35, 2, 8, SoundType.AMETHYST);
    public static final RegistryObject<Block> ADAMANTITE_BOOKSHELF = shelf("adamantite_bookshelf", 4.0F, 40, 4, 4, SoundType.METAL);
    public static final RegistryObject<Block> DRACONIC_BOOKSHELF = shelf("draconic_bookshelf", 5.0F, 50, 5, 12, SoundType.STONE);

    /** Magic wall closing a dungeon's boss arena until its monsters are dead (no item: it can't be obtained). */
    public static final RegistryObject<Block> DUNGEON_SEAL = BLOCKS.register("dungeon_seal",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(-1.0F, 3600000.0F)
                    .noLootTable().lightLevel(state -> 12).sound(SoundType.AMETHYST).isValidSpawn((state, level, pos, type) -> false)));

    private ModBlocks() {}

    private static RegistryObject<Block> register(String name, Supplier<Block> block) {
        RegistryObject<Block> object = BLOCKS.register(name, block);
        ModItems.registerBlockItem(name, object);
        return object;
    }

    private static RegistryObject<Block> shelf(String name, float power, float maxPower, float chaos, float arcana, SoundType sound) {
        return register(name, () -> new ArcaneBookshelfBlock(new ArcaneEnchanting.ShelfStats(power, maxPower, chaos, arcana),
                BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F).sound(sound)));
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
