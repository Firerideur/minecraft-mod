package com.firerideur.arcanequest.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Locale;

/**
 * Gives each region of the underground its own look, like the cave regions of Better Caves: mossy caves with glow
 * berries, amethyst crystal caves, frozen caves with ice stalactites, mushroom caves, magma caves in the depths and
 * old catacombs. Runs once per chunk and only reshapes the surface of the caves (floors, walls, ceilings).
 */
public class CaveDecoratorFeature extends Feature<NoneFeatureConfiguration> {
    /** Size of a cave region, in blocks. */
    private static final int REGION = 96;
    /** Caves this close to the surface keep their vanilla look. */
    private static final int SURFACE_MARGIN = 12;

    public CaveDecoratorFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    public enum Theme {
        PLAIN(32, false), MOSSY(14, false), CRYSTAL(10, false), FROZEN(10, false), FUNGAL(12, false), MAGMA(14, true),
        CATACOMBS(10, false);

        final int weight;
        /** Only below y = 0. */
        final boolean deep;

        Theme(int weight, boolean deep) {
            this.weight = weight;
            this.deep = deep;
        }

        public Component displayName() {
            return Component.translatable("cave.arcanequest." + name().toLowerCase(Locale.ROOT));
        }
    }

    /** The theme of the region around (x, y, z): regions are {@link #REGION} blocks wide, one layer above y = 0 and one below. */
    public static Theme themeAt(long seed, int x, int y, int z) {
        boolean deep = y < 0;
        long cell = Mth.getSeed(Math.floorDiv(x, REGION), deep ? -1 : 1, Math.floorDiv(z, REGION)) ^ seed * 0x5DEECE66DL;
        RandomSource random = RandomSource.create(cell);
        int total = 0;
        for (Theme theme : Theme.values()) {
            if (deep || !theme.deep) {
                total += theme.weight;
            }
        }
        int pick = random.nextInt(total);
        for (Theme theme : Theme.values()) {
            if (deep || !theme.deep) {
                pick -= theme.weight;
                if (pick < 0) {
                    return theme;
                }
            }
        }
        return Theme.PLAIN;
    }

    /** Center of the closest region of {@code theme} on the layer of {@code y}, or null if none within ~1000 blocks. */
    public static BlockPos nearest(long seed, Theme theme, int x, int y, int z) {
        int layerY = y < 0 ? -1 : 0;
        int cellX = Math.floorDiv(x, REGION), cellZ = Math.floorDiv(z, REGION);
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -11; dx <= 11; dx++) {
            for (int dz = -11; dz <= 11; dz++) {
                int cx = (cellX + dx) * REGION + REGION / 2, cz = (cellZ + dz) * REGION + REGION / 2;
                double distance = (double) (cx - x) * (cx - x) + (double) (cz - z) * (cz - z);
                if (distance < bestDistance && themeAt(seed, cx, layerY, cz) == theme) {
                    best = new BlockPos(cx, y, cz);
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    /** Whether {@code state} is one of the floor blocks this decorator lays down for {@code theme}. */
    public static boolean isFloorOf(Theme theme, BlockState state) {
        return switch (theme) {
            case MOSSY -> state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.MOSSY_COBBLESTONE);
            case CRYSTAL -> state.is(Blocks.CALCITE) || state.is(Blocks.AMETHYST_BLOCK);
            case FROZEN -> state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE);
            case FUNGAL -> state.is(Blocks.MYCELIUM) || state.is(Blocks.ROOTED_DIRT);
            case MAGMA -> state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.BASALT) || state.is(Blocks.BLACKSTONE);
            case CATACOMBS -> state.is(Blocks.GRAVEL) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.MOSSY_COBBLESTONE);
            default -> false;
        };
    }

    /**
     * A spot to stand in a decorated cave of the region centered on {@code center}: a themed floor with two blocks of
     * air above. Generates the chunks it looks at.
     */
    public static BlockPos findCaveSpot(WorldGenLevel level, Theme theme, BlockPos center) {
        boolean deep = center.getY() < 0;
        int low = deep ? level.getMinBuildHeight() + 8 : 0, high = deep ? -1 : 64;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int ring = 0; ring <= REGION / 2; ring += 4) {
            for (int dx = -ring; dx <= ring; dx += 4) {
                for (int dz = -ring; dz <= ring; dz += 4) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    int x = center.getX() + dx, z = center.getZ() + dz;
                    for (int y = high; y >= low; y--) {
                        if (isFloorOf(theme, level.getBlockState(pos.set(x, y, z)))
                                && level.isEmptyBlock(pos.set(x, y + 1, z)) && level.isEmptyBlock(pos.set(x, y + 2, z))) {
                            return new BlockPos(x, y + 1, z);
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        int chunkX = context.origin().getX() & ~15, chunkZ = context.origin().getZ() & ~15;
        int bottom = level.getMinBuildHeight() + 6;
        long seed = level.getSeed();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = chunkX + dx, z = chunkZ + dz;
                int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - SURFACE_MARGIN;
                Theme upper = themeAt(seed, x, 0, z), lower = themeAt(seed, x, -1, z);
                if (upper == Theme.PLAIN && lower == Theme.PLAIN) {
                    continue;
                }
                BlockState above = level.getBlockState(pos.set(x, top + 1, z));
                for (int y = top; y >= bottom; y--) {
                    Theme theme = y < 0 ? lower : upper;
                    BlockState state = level.getBlockState(pos.set(x, y, z));
                    boolean air = state.isAir();
                    if (theme != Theme.PLAIN && (air || above.isAir()) && !vanillaCaveBiome(level, pos)) {
                        BlockPos here = pos.immutable();
                        if (air && isBaseStone(above)) {
                            placed |= ceiling(level, theme, random, here.above());
                        } else if (!air && isBaseStone(state)) {
                            placed |= floor(level, theme, random, here);
                        }
                        if (air) {
                            placed |= walls(level, theme, random, here, chunkX, chunkZ);
                        }
                    }
                    // Re-read: decorating may have changed the block at y (vines, icicles...)
                    above = level.getBlockState(pos.set(x, y, z));
                }
            }
        }
        return placed;
    }

    private static boolean vanillaCaveBiome(WorldGenLevel level, BlockPos pos) {
        var biome = level.getBiome(pos);
        return biome.is(Biomes.LUSH_CAVES) || biome.is(Biomes.DRIPSTONE_CAVES) || biome.is(Biomes.DEEP_DARK);
    }

    private static boolean isBaseStone(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Floors: the top block is replaced, then something may grow on it
    // ------------------------------------------------------------------------------------------------------------

    private static boolean floor(WorldGenLevel level, Theme theme, RandomSource random, BlockPos pos) {
        BlockPos up = pos.above();
        switch (theme) {
            case MOSSY -> {
                set(level, pos, random.nextInt(6) == 0 ? Blocks.MOSSY_COBBLESTONE : Blocks.MOSS_BLOCK);
                int roll = random.nextInt(100);
                if (roll < 25) {
                    set(level, up, Blocks.MOSS_CARPET);
                } else if (roll < 33) {
                    set(level, up, random.nextBoolean() ? Blocks.SHORT_GRASS : Blocks.FERN);
                } else if (roll < 35) {
                    set(level, up, random.nextInt(3) == 0 ? Blocks.FLOWERING_AZALEA : Blocks.AZALEA);
                }
            }
            case CRYSTAL -> {
                set(level, pos, random.nextInt(3) == 0 ? Blocks.AMETHYST_BLOCK : Blocks.CALCITE);
                if (random.nextInt(9) == 0) {
                    level.setBlock(up, crystal(random, Direction.UP), 2);
                }
            }
            case FROZEN -> {
                int roll = random.nextInt(10);
                set(level, pos, roll < 5 ? Blocks.SNOW_BLOCK : roll < 9 ? Blocks.PACKED_ICE : Blocks.BLUE_ICE);
                if (roll < 5 && random.nextInt(3) > 0) {
                    level.setBlock(up, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1 + random.nextInt(3)), 2);
                } else if (random.nextInt(30) == 0) {
                    column(level, up, Direction.UP, Blocks.PACKED_ICE, 1 + random.nextInt(3));
                }
            }
            case FUNGAL -> {
                set(level, pos, random.nextInt(5) == 0 ? Blocks.ROOTED_DIRT : Blocks.MYCELIUM);
                int roll = random.nextInt(100);
                if (roll < 12) {
                    set(level, up, random.nextBoolean() ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM);
                } else if (roll < 13) {
                    hugeMushroom(level, random, up);
                }
            }
            case MAGMA -> {
                int roll = random.nextInt(10);
                set(level, pos, roll < 2 ? Blocks.MAGMA_BLOCK : roll < 6 ? Blocks.BASALT : Blocks.BLACKSTONE);
                if (random.nextInt(40) == 0 && enclosed(level, pos)) {
                    set(level, pos, Blocks.LAVA);
                } else if (random.nextInt(60) == 0) {
                    column(level, up, Direction.UP, Blocks.BASALT, 1 + random.nextInt(3));
                }
            }
            case CATACOMBS -> {
                int roll = random.nextInt(10);
                set(level, pos, roll < 4 ? Blocks.GRAVEL : roll < 7 ? Blocks.COARSE_DIRT : Blocks.MOSSY_COBBLESTONE);
                int deco = random.nextInt(200);
                if (deco < 6) {
                    set(level, up, Blocks.BONE_BLOCK);
                } else if (deco < 8) {
                    level.setBlock(up, Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, random.nextInt(16)), 2);
                } else if (deco < 10) {
                    set(level, up, Blocks.COBWEB);
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ceilings: the block above the air is replaced, then things hang from it
    // ------------------------------------------------------------------------------------------------------------

    private static boolean ceiling(WorldGenLevel level, Theme theme, RandomSource random, BlockPos pos) {
        BlockPos down = pos.below();
        switch (theme) {
            case MOSSY -> {
                set(level, pos, Blocks.MOSS_BLOCK);
                int roll = random.nextInt(200);
                if (roll < 10) {
                    glowBerries(level, random, down);
                } else if (roll < 18) {
                    set(level, down, Blocks.HANGING_ROOTS);
                } else if (roll < 19) {
                    set(level, down, Blocks.SPORE_BLOSSOM);
                }
            }
            case CRYSTAL -> {
                set(level, pos, random.nextInt(3) == 0 ? Blocks.AMETHYST_BLOCK : Blocks.CALCITE);
                if (random.nextInt(8) == 0) {
                    level.setBlock(down, crystal(random, Direction.DOWN), 2);
                }
            }
            case FROZEN -> {
                set(level, pos, random.nextInt(4) == 0 ? Blocks.BLUE_ICE : Blocks.PACKED_ICE);
                if (random.nextInt(14) == 0) {
                    // Icicle
                    column(level, down, Direction.DOWN, random.nextBoolean() ? Blocks.ICE : Blocks.PACKED_ICE, 1 + random.nextInt(4));
                }
            }
            case FUNGAL -> {
                if (random.nextInt(60) == 0) {
                    set(level, pos, Blocks.SHROOMLIGHT);
                } else if (random.nextInt(6) == 0) {
                    level.setBlock(down, lichen(Direction.UP), 2);
                }
            }
            case MAGMA -> {
                set(level, pos, random.nextInt(3) == 0 ? Blocks.BASALT : Blocks.BLACKSTONE);
                if (random.nextInt(25) == 0) {
                    column(level, down, Direction.DOWN, Blocks.BASALT, 1 + random.nextInt(3));
                }
            }
            case CATACOMBS -> {
                if (random.nextInt(5) > 0) {
                    set(level, pos, masonry(random, pos.getY() < 0));
                }
                int roll = random.nextInt(250);
                if (roll < 12) {
                    set(level, down, Blocks.COBWEB);
                } else if (roll < 14) {
                    hangingLantern(level, random, down);
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Walls: the stone around an air block (inside this chunk) is reskinned
    // ------------------------------------------------------------------------------------------------------------

    private static boolean walls(WorldGenLevel level, Theme theme, RandomSource random, BlockPos air, int chunkX, int chunkZ) {
        boolean placed = false;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos wall = air.relative(side);
            if (wall.getX() < chunkX || wall.getX() > chunkX + 15 || wall.getZ() < chunkZ || wall.getZ() > chunkZ + 15
                    || !isBaseStone(level.getBlockState(wall))) {
                continue;
            }
            Block block = switch (theme) {
                case MOSSY -> random.nextInt(3) > 0 ? Blocks.MOSS_BLOCK : Blocks.MOSSY_COBBLESTONE;
                case CRYSTAL -> random.nextInt(4) == 0 ? Blocks.AMETHYST_BLOCK : Blocks.CALCITE;
                case FROZEN -> random.nextInt(3) == 0 ? Blocks.ICE : Blocks.PACKED_ICE;
                case FUNGAL -> random.nextInt(4) == 0 ? Blocks.ROOTED_DIRT : null;
                case MAGMA -> random.nextInt(20) == 0 ? Blocks.MAGMA_BLOCK : random.nextBoolean() ? Blocks.BASALT : Blocks.BLACKSTONE;
                case CATACOMBS -> random.nextInt(6) == 0 ? null : masonry(random, air.getY() < 0);
                default -> null;
            };
            if (block != null) {
                set(level, wall, block);
                placed = true;
            }
            if (theme == Theme.CRYSTAL && random.nextInt(14) == 0) {
                level.setBlock(air, crystal(random, side.getOpposite()), 2);
            } else if ((theme == Theme.MOSSY || theme == Theme.FUNGAL) && random.nextInt(12) == 0) {
                level.setBlock(air, lichen(side), 2);
            }
        }
        return placed;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------------------------

    /** Old cut-stone walls of the catacombs: stone bricks, or deepslate bricks and tiles down deep. */
    private static Block masonry(RandomSource random, boolean deep) {
        int roll = random.nextInt(10);
        if (deep) {
            return roll < 4 ? Blocks.DEEPSLATE_BRICKS : roll < 7 ? Blocks.CRACKED_DEEPSLATE_BRICKS : roll < 9 ? Blocks.DEEPSLATE_TILES
                    : Blocks.CHISELED_DEEPSLATE;
        }
        return roll < 4 ? Blocks.STONE_BRICKS : roll < 7 ? Blocks.MOSSY_STONE_BRICKS : roll < 9 ? Blocks.CRACKED_STONE_BRICKS
                : Blocks.CHISELED_STONE_BRICKS;
    }

    private static void set(WorldGenLevel level, BlockPos pos, Block block) {
        level.setBlock(pos, block.defaultBlockState(), 2);
    }

    /** A block of lava only where it can't flow out: solid on every side but up. */
    private static boolean enclosed(WorldGenLevel level, BlockPos pos) {
        for (Direction side : Direction.values()) {
            if (side != Direction.UP && !level.getBlockState(pos.relative(side)).isSolid()) {
                return false;
            }
        }
        return true;
    }

    /** Up to {@code length} blocks from {@code start}, stopping before anything that isn't air. */
    private static void column(WorldGenLevel level, BlockPos start, Direction direction, Block block, int length) {
        BlockPos.MutableBlockPos pos = start.mutable();
        // Leave a block of air at the end so caves aren't sealed off
        for (int i = 0; i < length && level.isEmptyBlock(pos) && level.isEmptyBlock(pos.relative(direction)); i++) {
            set(level, pos, block);
            pos.move(direction);
        }
    }

    private static BlockState crystal(RandomSource random, Direction facing) {
        Block block = switch (random.nextInt(4)) {
            case 0 -> Blocks.AMETHYST_CLUSTER;
            case 1 -> Blocks.LARGE_AMETHYST_BUD;
            case 2 -> Blocks.MEDIUM_AMETHYST_BUD;
            default -> Blocks.AMETHYST_CLUSTER;
        };
        return block.defaultBlockState().setValue(AmethystClusterBlock.FACING, facing);
    }

    /** Glow lichen stuck on the face of the block in direction {@code side}. */
    private static BlockState lichen(Direction side) {
        return Blocks.GLOW_LICHEN.defaultBlockState().setValue(MultifaceBlock.getFaceProperty(side), true);
    }

    private static void glowBerries(WorldGenLevel level, RandomSource random, BlockPos top) {
        int length = 1 + random.nextInt(5);
        BlockPos.MutableBlockPos pos = top.mutable();
        for (int i = 0; i < length; i++) {
            boolean last = i == length - 1 || !level.isEmptyBlock(pos.below()) || !level.isEmptyBlock(pos.below(2));
            Block block = last ? Blocks.CAVE_VINES : Blocks.CAVE_VINES_PLANT;
            level.setBlock(pos, block.defaultBlockState().setValue(CaveVines.BERRIES, random.nextInt(3) == 0), 2);
            if (last) {
                return;
            }
            pos.move(Direction.DOWN);
        }
    }

    private static void hangingLantern(WorldGenLevel level, RandomSource random, BlockPos top) {
        int chain = 1 + random.nextInt(2);
        BlockPos.MutableBlockPos pos = top.mutable();
        for (int i = 0; i < chain; i++) {
            if (!level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.below(2))) {
                return;
            }
            level.setBlock(pos, Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y), 2);
            pos.move(Direction.DOWN);
        }
        level.setBlock(pos, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 2);
    }

    /** A small giant mushroom: a stem with a cap, if there is room for it. */
    private static void hugeMushroom(WorldGenLevel level, RandomSource random, BlockPos base) {
        int height = 3 + random.nextInt(3);
        for (int y = 0; y <= height + 1; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (!level.isEmptyBlock(base.offset(dx, y, dz))) {
                        return;
                    }
                }
            }
        }
        for (int y = 0; y < height; y++) {
            set(level, base.above(y), Blocks.MUSHROOM_STEM);
        }
        Block cap = random.nextBoolean() ? Blocks.RED_MUSHROOM_BLOCK : Blocks.BROWN_MUSHROOM_BLOCK;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                set(level, base.offset(dx, height, dz), cap);
            }
        }
    }
}
