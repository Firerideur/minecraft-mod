package com.firerideur.arcanequest.dungeon;

import com.firerideur.arcanequest.dungeon.DungeonScenes.Scene;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import static com.firerideur.arcanequest.registry.ModEntities.*;

/** Look and inhabitants of a dungeon. Each gate rank (stars) has one or two themes, picked at random. */
public enum DungeonTheme {
    GOBLIN_CAVE(1, Blocks.COBBLESTONE, Blocks.MOSSY_STONE_BRICKS, Blocks.MOSSY_COBBLESTONE, Blocks.STONE_BRICKS, Blocks.GLOWSTONE,
            Blocks.STONE_BRICKS, true, GOBLIN, mobs(GOBLIN, GOBLIN, GOBLIN, GOBLIN_SHAMAN)),
    WOLF_DEN(2, Blocks.COBBLED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_TILES, Blocks.DEEPSLATE_BRICKS, Blocks.SHROOMLIGHT,
            Blocks.POLISHED_DEEPSLATE, true, STEELFANG_WOLF, mobs(GOBLIN, GOBLIN_SHAMAN, STEELFANG_WOLF, STEELFANG_WOLF, WEREWOLF)),
    BEAST_LAIR(2, Blocks.COARSE_DIRT, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.OAK_PLANKS, Blocks.JACK_O_LANTERN,
            Blocks.OAK_LOG, true, WEREWOLF, mobs(RATMAN, RATMAN, RATMAN, LIZARDMAN, WEREWOLF)),
    CURSED_FOREST(2, Blocks.MOSS_BLOCK, Blocks.DARK_OAK_LOG, Blocks.MUDDY_MANGROVE_ROOTS, Blocks.DARK_OAK_PLANKS, Blocks.VERDANT_FROGLIGHT,
            Blocks.DARK_OAK_LOG, true, TREANT, mobs(TREANT, STEELFANG_WOLF, GOBLIN, GOBLIN)),
    ICE_FORTRESS(3, Blocks.SNOW_BLOCK, Blocks.PACKED_ICE, Blocks.BLUE_ICE, Blocks.PACKED_ICE, Blocks.SEA_LANTERN,
            Blocks.BLUE_ICE, false, ICE_ELF, mobs(ICE_ELF, ICE_ELF, ICE_ELF, STEELFANG_WOLF, TROLL)),
    SPIDER_NEST(3, Blocks.COARSE_DIRT, Blocks.TUFF, Blocks.TUFF_BRICKS, Blocks.TUFF, Blocks.SHROOMLIGHT,
            Blocks.POLISHED_TUFF, true, GIANT_SPIDER, mobs(GIANT_SPIDER, GIANT_SPIDER, GIANT_SPIDER, GIANT_WASP)),
    GHOUL_CRYPT(3, Blocks.SOUL_SOIL, Blocks.CRACKED_DEEPSLATE_BRICKS, Blocks.CHISELED_DEEPSLATE, Blocks.DEEPSLATE_TILES,
            Blocks.VERDANT_FROGLIGHT, Blocks.BONE_BLOCK, true, GHOUL, mobs(GHOUL, GHOUL, GHOUL, RATMAN)),
    ANT_NEST(4, Blocks.PACKED_MUD, Blocks.MUD_BRICKS, Blocks.ROOTED_DIRT, Blocks.MUD_BRICKS, Blocks.OCHRE_FROGLIGHT,
            Blocks.MUD_BRICKS, true, ANT_SOLDIER, mobs(ANT_SOLDIER, ANT_SOLDIER, ANT_SOLDIER, STEELFANG_WOLF, BEETLE)),
    INSECT_JUNGLE(4, Blocks.MOSS_BLOCK, Blocks.JUNGLE_LOG, Blocks.MOSSY_COBBLESTONE, Blocks.JUNGLE_PLANKS, Blocks.OCHRE_FROGLIGHT,
            Blocks.JUNGLE_LOG, false, MANTIS, mobs(MANTIS, BEETLE, GIANT_WASP, GIANT_WASP, ANT_SOLDIER)),
    SUNKEN_TEMPLE(4, Blocks.PRISMARINE_BRICKS, Blocks.PRISMARINE, Blocks.DARK_PRISMARINE, Blocks.PRISMARINE_BRICKS, Blocks.SEA_LANTERN,
            Blocks.DARK_PRISMARINE, false, NAGA, mobs(NAGA, NAGA, BASILISK, BASILISK, LIZARDMAN, LIZARDMAN)),
    ORC_CAMP(5, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_LOG, Blocks.COARSE_DIRT, Blocks.DARK_OAK_PLANKS, Blocks.SHROOMLIGHT,
            Blocks.DARK_OAK_LOG, false, HIGH_ORC, mobs(HIGH_ORC, HIGH_ORC, GOBLIN_SHAMAN, GOBLIN, TROLL)),
    LABYRINTH(5, Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS, Blocks.SHROOMLIGHT,
            Blocks.CHISELED_STONE_BRICKS, true, MINOTAUR, mobs(MINOTAUR, TROLL, TROLL, BASILISK)),
    BLACK_CITADEL(6, Blocks.POLISHED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.GILDED_BLACKSTONE, Blocks.BLACKSTONE,
            Blocks.PEARLESCENT_FROGLIGHT, Blocks.CHISELED_POLISHED_BLACKSTONE, false, DEMON_KNIGHT,
            mobs(HIGH_ORC, HIGH_ORC, DEMON_KNIGHT, DEMON_KNIGHT, SUCCUBUS)),
    GARGOYLE_CATHEDRAL(6, Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_TILES, Blocks.CHISELED_DEEPSLATE, Blocks.DEEPSLATE_TILES,
            Blocks.SEA_LANTERN, Blocks.POLISHED_BASALT, true, LICH, mobs(GARGOYLE, GARGOYLE, GARGOYLE, LICH, DEMON_KNIGHT, GHOUL)),
    DEMON_ABYSS(7, Blocks.RED_NETHER_BRICKS, Blocks.NETHER_BRICKS, Blocks.MAGMA_BLOCK, Blocks.NETHER_BRICKS, Blocks.SHROOMLIGHT,
            Blocks.CRYING_OBSIDIAN, false, DEMON_KNIGHT, mobs(DEMON_KNIGHT, DEMON_KNIGHT, DEMON_KNIGHT, ANT_SOLDIER, HIGH_ORC, LICH, MINOTAUR, IMP)),
    INFERNAL_THRONE(7, Blocks.BLACKSTONE, Blocks.RED_NETHER_BRICKS, Blocks.MAGMA_BLOCK, Blocks.NETHER_WART_BLOCK, Blocks.SHROOMLIGHT,
            Blocks.OBSIDIAN, false, GREATER_DEMON, mobs(IMP, IMP, IMP, SUCCUBUS, SUCCUBUS, DEMON_KNIGHT));

    public final int stars;
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

    DungeonTheme(int stars, Block floor, Block wall, Block accent, Block ceiling, Block light, Block pillar, boolean cobwebs,
                 Supplier<? extends EntityType<? extends DungeonMob>> boss,
                 List<Supplier<? extends EntityType<? extends DungeonMob>>> mobs) {
        this.stars = stars;
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

    /** The first theme of this rank (used for dungeons saved before themes could vary). */
    public static DungeonTheme of(int stars) {
        return forStars(stars).get(0);
    }

    public static List<DungeonTheme> forStars(int stars) {
        int clamped = Math.max(1, Math.min(Gates.MAX_STARS, stars));
        return Arrays.stream(values()).filter(t -> t.stars == clamped).toList();
    }

    public static DungeonTheme random(int stars, RandomSource random) {
        List<DungeonTheme> themes = forStars(stars);
        return themes.get(random.nextInt(themes.size()));
    }

    public static DungeonTheme byName(String name, int stars) {
        for (DungeonTheme theme : values()) {
            if (theme.name().equals(name)) {
                return theme;
            }
        }
        return of(stars);
    }

    /** A random inhabitant (the list repeats common ones). */
    public EntityType<? extends DungeonMob> randomMob(RandomSource random) {
        return mobs.get(random.nextInt(mobs.size())).get();
    }

    /** The scenes the zones of this dungeon are made of (an abandoned village, a war camp...). */
    public List<DungeonScenes.Scene> scenes() {
        return switch (this) {
            case GOBLIN_CAVE -> List.of(Scene.CAMP, Scene.VILLAGE, Scene.RUINS);
            case WOLF_DEN -> List.of(Scene.GROVE, Scene.VILLAGE, Scene.RUINS);
            case BEAST_LAIR -> List.of(Scene.CAMP, Scene.NEST, Scene.VILLAGE);
            case CURSED_FOREST -> List.of(Scene.GROVE, Scene.VILLAGE, Scene.CRYPT);
            case ICE_FORTRESS -> List.of(Scene.ICE_SPIKES, Scene.RUINS, Scene.VILLAGE);
            case SPIDER_NEST -> List.of(Scene.NEST, Scene.RUINS, Scene.VILLAGE);
            case GHOUL_CRYPT -> List.of(Scene.CRYPT, Scene.RUINS, Scene.VILLAGE);
            case ANT_NEST -> List.of(Scene.NEST, Scene.RUINS);
            case INSECT_JUNGLE -> List.of(Scene.GROVE, Scene.NEST, Scene.RUINS);
            case SUNKEN_TEMPLE -> List.of(Scene.SHRINE, Scene.RUINS);
            case ORC_CAMP -> List.of(Scene.CAMP, Scene.VILLAGE, Scene.RUINS);
            case LABYRINTH -> List.of(Scene.RUINS, Scene.SHRINE, Scene.CRYPT);
            case BLACK_CITADEL -> List.of(Scene.RUINS, Scene.CAMP, Scene.CRYPT);
            case GARGOYLE_CATHEDRAL -> List.of(Scene.SHRINE, Scene.CRYPT, Scene.RUINS);
            case DEMON_ABYSS -> List.of(Scene.HELLSCAPE, Scene.RUINS);
            case INFERNAL_THRONE -> List.of(Scene.HELLSCAPE, Scene.SHRINE);
        };
    }

    /** Natural ground and rock of the landscape (the theme's own blocks are kept for what was built). */
    record Terrain(Block ground, Block groundAccent, Block rock, Block rockAccent) {}

    Terrain terrain() {
        return switch (this) {
            case GOBLIN_CAVE -> new Terrain(Blocks.COARSE_DIRT, Blocks.GRAVEL, Blocks.STONE, Blocks.ANDESITE);
            case WOLF_DEN -> new Terrain(Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.STONE, Blocks.MOSSY_COBBLESTONE);
            case BEAST_LAIR -> new Terrain(Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT, Blocks.STONE, Blocks.GRANITE);
            case CURSED_FOREST -> new Terrain(Blocks.PODZOL, Blocks.MOSS_BLOCK, Blocks.TUFF, Blocks.MOSSY_COBBLESTONE);
            case ICE_FORTRESS -> new Terrain(Blocks.SNOW_BLOCK, Blocks.PACKED_ICE, Blocks.PACKED_ICE, Blocks.SNOW_BLOCK);
            case SPIDER_NEST -> new Terrain(Blocks.COARSE_DIRT, Blocks.GRAVEL, Blocks.TUFF, Blocks.DEEPSLATE);
            case GHOUL_CRYPT -> new Terrain(Blocks.SOUL_SOIL, Blocks.COARSE_DIRT, Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE);
            case ANT_NEST -> new Terrain(Blocks.PACKED_MUD, Blocks.ROOTED_DIRT, Blocks.DRIPSTONE_BLOCK, Blocks.PACKED_MUD);
            case INSECT_JUNGLE -> new Terrain(Blocks.GRASS_BLOCK, Blocks.MOSS_BLOCK, Blocks.STONE, Blocks.MOSSY_COBBLESTONE);
            case SUNKEN_TEMPLE -> new Terrain(Blocks.SAND, Blocks.CLAY, Blocks.STONE, Blocks.PRISMARINE);
            case ORC_CAMP -> new Terrain(Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT, Blocks.STONE, Blocks.GRANITE);
            case LABYRINTH -> new Terrain(Blocks.GRAVEL, Blocks.COBBLESTONE, Blocks.STONE, Blocks.ANDESITE);
            case BLACK_CITADEL -> new Terrain(Blocks.BLACKSTONE, Blocks.BASALT, Blocks.BLACKSTONE, Blocks.BASALT);
            case GARGOYLE_CATHEDRAL -> new Terrain(Blocks.COBBLED_DEEPSLATE, Blocks.DEEPSLATE, Blocks.DEEPSLATE, Blocks.TUFF);
            case DEMON_ABYSS -> new Terrain(Blocks.NETHERRACK, Blocks.SOUL_SOIL, Blocks.NETHERRACK, Blocks.BASALT);
            case INFERNAL_THRONE -> new Terrain(Blocks.NETHERRACK, Blocks.CRIMSON_NYLIUM, Blocks.BLACKSTONE, Blocks.NETHERRACK);
        };
    }

    /** Demon lands: lava instead of water. */
    boolean hellish() {
        return this == DEMON_ABYSS || this == INFERNAL_THRONE;
    }

    /** Under an open (dark) sky instead of a rock ceiling. */
    public boolean open() {
        return switch (this) {
            case WOLF_DEN, CURSED_FOREST, ICE_FORTRESS, INSECT_JUNGLE, ORC_CAMP, DEMON_ABYSS, INFERNAL_THRONE -> true;
            default -> false;
        };
    }

    DungeonScenes.Wood wood() {
        return switch (this) {
            case ICE_FORTRESS, GOBLIN_CAVE, BEAST_LAIR, ORC_CAMP -> DungeonScenes.Wood.SPRUCE;
            case WOLF_DEN, CURSED_FOREST, GHOUL_CRYPT, SPIDER_NEST, BLACK_CITADEL, GARGOYLE_CATHEDRAL -> DungeonScenes.Wood.DARK_OAK;
            case INSECT_JUNGLE, ANT_NEST, SUNKEN_TEMPLE -> DungeonScenes.Wood.JUNGLE;
            case DEMON_ABYSS, INFERNAL_THRONE -> DungeonScenes.Wood.CRIMSON;
            default -> DungeonScenes.Wood.OAK;
        };
    }

    Block leaves() {
        return switch (this) {
            case INSECT_JUNGLE -> Blocks.JUNGLE_LEAVES;
            case WOLF_DEN, ICE_FORTRESS -> Blocks.SPRUCE_LEAVES;
            default -> Blocks.DARK_OAK_LEAVES;
        };
    }

    Block tentCloth() {
        return switch (this) {
            case ORC_CAMP, BLACK_CITADEL -> Blocks.RED_WOOL;
            case BEAST_LAIR -> Blocks.BROWN_WOOL;
            default -> Blocks.GREEN_WOOL;
        };
    }

    /** Braziers of the boss arena: soul fire for the undead and the cold, normal fire otherwise. */
    boolean soulFire() {
        return switch (this) {
            case GHOUL_CRYPT, GARGOYLE_CATHEDRAL, ICE_FORTRESS, CURSED_FOREST, SPIDER_NEST -> true;
            default -> false;
        };
    }

    public Component displayName() {
        return Component.translatable("dungeon.arcanequest." + name().toLowerCase(Locale.ROOT));
    }

    /** Every monster type that lives in this theme (boss included). */
    public List<EntityType<? extends DungeonMob>> inhabitants() {
        List<EntityType<? extends DungeonMob>> all = new ArrayList<>(mobs.stream().<EntityType<? extends DungeonMob>>map(Supplier::get).distinct().toList());
        if (!all.contains(boss.get())) {
            all.add(boss.get());
        }
        return all;
    }
}
