package com.firerideur.arcanequest.dungeon;

import com.firerideur.arcanequest.registry.ModEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/** Look and inhabitants of a dungeon, one per gate rank (stars). */
public enum DungeonTheme {
    GOBLIN_CAVE(Blocks.COBBLESTONE, Blocks.MOSSY_STONE_BRICKS, Blocks.MOSSY_COBBLESTONE, Blocks.STONE_BRICKS, Blocks.GLOWSTONE,
            Blocks.STONE_BRICKS, true, ModEntities.GOBLIN, mobs(ModEntities.GOBLIN, ModEntities.GOBLIN, ModEntities.GOBLIN, ModEntities.GOBLIN_SHAMAN)),
    WOLF_DEN(Blocks.COBBLED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_TILES, Blocks.DEEPSLATE_BRICKS, Blocks.SHROOMLIGHT,
            Blocks.POLISHED_DEEPSLATE, true, ModEntities.STEELFANG_WOLF, mobs(ModEntities.GOBLIN, ModEntities.GOBLIN_SHAMAN, ModEntities.STEELFANG_WOLF, ModEntities.STEELFANG_WOLF)),
    ICE_FORTRESS(Blocks.SNOW_BLOCK, Blocks.PACKED_ICE, Blocks.BLUE_ICE, Blocks.PACKED_ICE, Blocks.SEA_LANTERN,
            Blocks.BLUE_ICE, false, ModEntities.ICE_ELF, mobs(ModEntities.ICE_ELF, ModEntities.ICE_ELF, ModEntities.ICE_ELF, ModEntities.STEELFANG_WOLF)),
    ANT_NEST(Blocks.PACKED_MUD, Blocks.MUD_BRICKS, Blocks.ROOTED_DIRT, Blocks.MUD_BRICKS, Blocks.OCHRE_FROGLIGHT,
            Blocks.MUD_BRICKS, true, ModEntities.ANT_SOLDIER, mobs(ModEntities.ANT_SOLDIER, ModEntities.ANT_SOLDIER, ModEntities.ANT_SOLDIER, ModEntities.STEELFANG_WOLF)),
    ORC_CAMP(Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_LOG, Blocks.COARSE_DIRT, Blocks.DARK_OAK_PLANKS, Blocks.SHROOMLIGHT,
            Blocks.DARK_OAK_LOG, false, ModEntities.HIGH_ORC, mobs(ModEntities.HIGH_ORC, ModEntities.HIGH_ORC, ModEntities.GOBLIN_SHAMAN, ModEntities.GOBLIN)),
    BLACK_CITADEL(Blocks.POLISHED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.GILDED_BLACKSTONE, Blocks.BLACKSTONE,
            Blocks.PEARLESCENT_FROGLIGHT, Blocks.CHISELED_POLISHED_BLACKSTONE, false, ModEntities.DEMON_KNIGHT,
            mobs(ModEntities.HIGH_ORC, ModEntities.HIGH_ORC, ModEntities.DEMON_KNIGHT, ModEntities.DEMON_KNIGHT)),
    DEMON_ABYSS(Blocks.RED_NETHER_BRICKS, Blocks.NETHER_BRICKS, Blocks.MAGMA_BLOCK, Blocks.NETHER_BRICKS, Blocks.SHROOMLIGHT,
            Blocks.CRYING_OBSIDIAN, false, ModEntities.DEMON_KNIGHT,
            mobs(ModEntities.DEMON_KNIGHT, ModEntities.DEMON_KNIGHT, ModEntities.DEMON_KNIGHT, ModEntities.ANT_SOLDIER, ModEntities.HIGH_ORC));

    public final Block floor;
    public final Block wall;
    public final Block accent;
    public final Block ceiling;
    public final Block light;
    public final Block pillar;
    /** Old, abandoned places get cobwebs in the corners. */
    public final boolean cobwebs;
    public final Supplier<? extends EntityType<? extends DungeonMob>> boss;
    private final List<Supplier<? extends EntityType<? extends DungeonMob>>> mobs;

    DungeonTheme(Block floor, Block wall, Block accent, Block ceiling, Block light, Block pillar, boolean cobwebs,
                 Supplier<? extends EntityType<? extends DungeonMob>> boss,
                 List<Supplier<? extends EntityType<? extends DungeonMob>>> mobs) {
        this.floor = floor;
        this.wall = wall;
        this.accent = accent;
        this.ceiling = ceiling;
        this.light = light;
        this.pillar = pillar;
        this.cobwebs = cobwebs;
        this.boss = boss;
        this.mobs = mobs;
    }

    @SafeVarargs
    private static List<Supplier<? extends EntityType<? extends DungeonMob>>> mobs(Supplier<? extends EntityType<? extends DungeonMob>>... mobs) {
        return List.of(mobs);
    }

    public static DungeonTheme of(int stars) {
        DungeonTheme[] values = values();
        return values[Math.max(1, Math.min(values.length, stars)) - 1];
    }

    /** A random inhabitant (the list repeats common ones). */
    public EntityType<? extends DungeonMob> randomMob(RandomSource random) {
        return mobs.get(random.nextInt(mobs.size())).get();
    }

    public Component displayName() {
        return Component.translatable("dungeon.arcanequest." + name().toLowerCase(Locale.ROOT));
    }
}
