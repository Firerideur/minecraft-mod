package com.firerideur.arcanequest.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * The places inside a dungeon: each zone of the dungeon is one of these scenes, built on flat ground around its
 * center. They give the dungeon its story (a village the goblins raided, an orc war camp, a graveyard...).
 */
public final class DungeonScenes {
    private DungeonScenes() {}

    public enum Scene {
        /** Abandoned village: ruined houses with collapsed roofs around a well. */
        VILLAGE,
        /** Broken pillars, arches and fallen columns. */
        RUINS,
        /** War camp: tents, a campfire, a palisade and trophies. */
        CAMP,
        /** Dark trees, boulders and mushrooms. */
        GROVE,
        /** Graveyard: tombstones, open graves and a small mausoleum. */
        CRYPT,
        /** Webs, egg clutches, mounds and bones. */
        NEST,
        /** Ice spikes, snow and frozen statues. */
        ICE_SPIKES,
        /** Temple: rows of pillars, an altar and pools. */
        SHRINE,
        /** Lava pools, fire, basalt and netherrack spikes. */
        HELLSCAPE
    }

    /** Wood used by the houses and camps of a theme. */
    record Wood(Block planks, Block log, Block stairs, Block fence) {
        static final Wood OAK = new Wood(Blocks.OAK_PLANKS, Blocks.OAK_LOG, Blocks.OAK_STAIRS, Blocks.OAK_FENCE);
        static final Wood SPRUCE = new Wood(Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_LOG, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_FENCE);
        static final Wood DARK_OAK = new Wood(Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_FENCE);
        static final Wood CRIMSON = new Wood(Blocks.CRIMSON_PLANKS, Blocks.CRIMSON_STEM, Blocks.CRIMSON_STAIRS, Blocks.CRIMSON_FENCE);
        static final Wood JUNGLE = new Wood(Blocks.JUNGLE_PLANKS, Blocks.JUNGLE_LOG, Blocks.JUNGLE_STAIRS, Blocks.JUNGLE_FENCE);
    }

    /**
     * Builds {@code scene} around {@code center} (a ground block) within {@code radius}, keeping the middle free for the
     * monsters. Places where a chest could go are added to {@code chests}.
     */
    static void build(ServerLevel level, Scene scene, DungeonTheme theme, BlockPos center, int radius, RandomSource random,
                      List<BlockPos> chests) {
        switch (scene) {
            case VILLAGE -> village(level, theme, center, radius, random, chests);
            case RUINS -> ruins(level, theme, center, radius, random, chests);
            case CAMP -> camp(level, theme, center, radius, random, chests);
            case GROVE -> grove(level, theme, center, radius, random);
            case CRYPT -> crypt(level, theme, center, radius, random, chests);
            case NEST -> nest(level, theme, center, radius, random);
            case ICE_SPIKES -> iceSpikes(level, center, radius, random);
            case SHRINE -> shrine(level, theme, center, radius, random, chests);
            case HELLSCAPE -> hellscape(level, theme, center, radius, random);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Abandoned village
    // ------------------------------------------------------------------------------------------------------------

    private static void village(ServerLevel level, DungeonTheme theme, BlockPos center, int radius, RandomSource random,
                                List<BlockPos> chests) {
        Wood wood = theme.wood();
        well(level, center, random);
        int houses = 3 + random.nextInt(2);
        float start = random.nextFloat() * Mth.TWO_PI;
        for (int i = 0; i < houses; i++) {
            float angle = start + i * Mth.TWO_PI / houses + (random.nextFloat() - 0.5F) * 0.4F;
            int distance = radius - 6;
            BlockPos at = center.offset(Math.round(Mth.cos(angle) * distance), 0, Math.round(Mth.sin(angle) * distance));
            // The door faces the well
            int dx = center.getX() - at.getX(), dz = center.getZ() - at.getZ();
            Direction door = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST)
                    : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
            house(level, at, 2, 2 + random.nextInt(2), door, wood, random, chests);
        }
        // Lanterns on posts, hay, barrels and a dead garden
        for (int i = 0; i < 4; i++) {
            BlockPos post = ground(center, radius - 3, random);
            if (free(level, post, 3)) {
                set(level, post, wood.fence());
                set(level, post.above(), wood.fence());
                set(level, post.above(2), Blocks.LANTERN);
            }
        }
        scatter(level, center, radius - 2, random, 8, Blocks.HAY_BLOCK, Blocks.BARREL, Blocks.COMPOSTER, Blocks.DEAD_BUSH);
        groundPatch(level, center, radius - 1, random, 0.25F, Blocks.COARSE_DIRT, Blocks.GRAVEL, Blocks.DIRT_PATH);
    }

    private static void well(ServerLevel level, BlockPos center, RandomSource random) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos pos = center.offset(x, 0, z);
                set(level, pos.below(), Blocks.COBBLESTONE);
                if (x == 0 && z == 0) {
                    set(level, pos, Blocks.WATER);
                } else {
                    set(level, pos, Blocks.COBBLESTONE);
                    set(level, pos.above(), random.nextInt(4) == 0 ? Blocks.MOSSY_COBBLESTONE_WALL : Blocks.COBBLESTONE_WALL);
                }
            }
        }
        // Two posts, a roof and a hanging bucket rope
        for (int x : new int[]{-1, 1}) {
            set(level, center.offset(x, 2, 0), Blocks.OAK_FENCE);
            set(level, center.offset(x, 3, 0), Blocks.OAK_FENCE);
        }
        for (int x = -1; x <= 1; x++) {
            set(level, center.offset(x, 4, 0), Blocks.OAK_SLAB);
        }
        level.setBlock(center.offset(0, 3, 0), Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y), 2);
    }

    /**
     * A ruined house: cobblestone footing, log corners, plank walls with holes, broken windows and a gable roof partly
     * fallen in. {@code half} is half its width across the door, {@code halfLength} half its depth.
     */
    private static void house(ServerLevel level, BlockPos center, int half, int halfLength, Direction door, Wood wood,
                              RandomSource random, List<BlockPos> chests) {
        boolean alongX = door.getAxis() == Direction.Axis.Z;
        int hx = alongX ? halfLength : half, hz = alongX ? half : halfLength;
        // Clear and floor the footprint
        for (int x = -hx; x <= hx; x++) {
            for (int z = -hz; z <= hz; z++) {
                set(level, center.offset(x, 0, z), random.nextInt(4) == 0 ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE);
                for (int y = 1; y <= 8; y++) {
                    set(level, center.offset(x, y, z), Blocks.AIR);
                }
            }
        }
        int wallTop = 3;
        for (int x = -hx; x <= hx; x++) {
            for (int z = -hz; z <= hz; z++) {
                boolean edgeX = Math.abs(x) == hx, edgeZ = Math.abs(z) == hz;
                if (!edgeX && !edgeZ) {
                    continue;
                }
                for (int y = 1; y <= wallTop; y++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (edgeX && edgeZ) {
                        set(level, pos, wood.log());
                    } else if (y == 1) {
                        set(level, pos, Blocks.COBBLESTONE);
                    } else if (y == 2 && (edgeX ? Math.abs(z) == 1 : Math.abs(x) == 1) && random.nextBoolean()) {
                        // Broken window
                        set(level, pos, Blocks.AIR);
                    } else if (random.nextFloat() < 0.15F) {
                        // A hole in the wall
                        set(level, pos, Blocks.AIR);
                    } else {
                        set(level, pos, wood.planks());
                    }
                }
            }
        }
        // Doorway in the middle of the door side
        BlockPos doorway = center.relative(door, door.getAxis() == Direction.Axis.X ? hx : hz);
        set(level, doorway.above(), Blocks.AIR);
        set(level, doorway.above(2), Blocks.AIR);
        // Gable roof rising across the door axis, with collapsed parts
        Direction up = alongX ? Direction.SOUTH : Direction.EAST;
        int across = alongX ? hz : hx, along = alongX ? hx : hz;
        for (int k = 0; k <= across; k++) {
            int y = wallTop + 1 + k;
            for (int a = -along - 1; a <= along + 1; a++) {
                for (int side : new int[]{-1, 1}) {
                    int c = side * (across + 1 - k);
                    BlockPos pos = alongX ? center.offset(a, y, c) : center.offset(c, y, a);
                    if (random.nextFloat() < 0.22F) {
                        continue;
                    }
                    Direction facing = side < 0 ? up : up.getOpposite();
                    level.setBlock(pos, wood.stairs().defaultBlockState().setValue(StairBlock.FACING, facing), 2);
                }
                // Gable ends
                if (Math.abs(a) == along) {
                    for (int c = -(across - k); c <= across - k; c++) {
                        BlockPos pos = alongX ? center.offset(a, y, c) : center.offset(c, y, a);
                        set(level, pos, wood.planks());
                    }
                }
            }
            if (k == across) {
                for (int a = -along - 1; a <= along + 1; a++) {
                    BlockPos pos = alongX ? center.offset(a, y, 0) : center.offset(0, y, a);
                    if (random.nextFloat() > 0.25F) {
                        set(level, pos, wood.planks());
                    }
                }
            }
        }
        // Inside: cobwebs, rubble and a place for a chest against the back wall
        set(level, center.offset(hx - 1, wallTop, hz - 1), Blocks.COBWEB);
        set(level, center.offset(-hx + 1, wallTop, -hz + 1), Blocks.COBWEB);
        BlockPos back = center.relative(door.getOpposite(), (door.getAxis() == Direction.Axis.X ? hx : hz) - 1).above();
        chests.add(back);
        BlockPos side = center.relative(door.getClockWise(), (door.getClockWise().getAxis() == Direction.Axis.X ? hx : hz) - 1).above();
        set(level, side, random.nextBoolean() ? Blocks.CRAFTING_TABLE : Blocks.BARREL);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ruins
    // ------------------------------------------------------------------------------------------------------------

    private static void ruins(ServerLevel level, DungeonTheme theme, BlockPos center, int radius, RandomSource random,
                              List<BlockPos> chests) {
        Block pillar = theme.pillar;
        int pillars = 6 + random.nextInt(5);
        for (int i = 0; i < pillars; i++) {
            BlockPos base = ground(center, radius - 2, random);
            if (base.distSqr(center) < 16 || !free(level, base, 6)) {
                continue;
            }
            int height = 1 + random.nextInt(6);
            for (int y = 1; y <= height; y++) {
                set(level, base.above(y), y == height && random.nextBoolean() ? theme.accent : pillar);
            }
        }
        // An arch still standing
        BlockPos arch = ground(center, radius - 5, random);
        Direction across = random.nextBoolean() ? Direction.EAST : Direction.SOUTH;
        for (int y = 1; y <= 5; y++) {
            set(level, arch.relative(across, -2).above(y), pillar);
            set(level, arch.relative(across, 2).above(y), pillar);
        }
        for (int t = -2; t <= 2; t++) {
            set(level, arch.relative(across, t).above(6), theme.wall);
        }
        // A fallen column
        BlockPos fallen = ground(center, radius - 4, random);
        Direction.Axis axis = random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z;
        for (int t = 0; t < 5; t++) {
            BlockPos pos = fallen.relative(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE), t).above();
            if (level.getBlockState(pos).isAir()) {
                BlockState state = pillar.defaultBlockState();
                if (state.hasProperty(RotatedPillarBlock.AXIS)) {
                    state = state.setValue(RotatedPillarBlock.AXIS, axis);
                }
                level.setBlock(pos, state, 2);
            }
        }
        scatter(level, center, radius - 1, random, 14, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.GRAVEL, theme.accent);
        groundPatch(level, center, radius - 1, random, 0.15F, Blocks.MOSS_BLOCK, Blocks.GRAVEL);
        chests.add(arch.above());
    }

    // ------------------------------------------------------------------------------------------------------------
    // War camp
    // ------------------------------------------------------------------------------------------------------------

    private static void camp(ServerLevel level, DungeonTheme theme, BlockPos center, int radius, RandomSource random,
                             List<BlockPos> chests) {
        Wood wood = theme.wood();
        level.setBlock(center.above(), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), 2);
        // Log seats around the fire
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos seat = center.relative(side, 3).above();
            BlockState log = wood.log().defaultBlockState();
            if (log.hasProperty(RotatedPillarBlock.AXIS)) {
                log = log.setValue(RotatedPillarBlock.AXIS, side.getClockWise().getAxis());
            }
            level.setBlock(seat, log, 2);
        }
        int tents = 3 + random.nextInt(2);
        float start = random.nextFloat() * Mth.TWO_PI;
        Block cloth = theme.tentCloth();
        for (int i = 0; i < tents; i++) {
            float angle = start + i * Mth.TWO_PI / tents;
            BlockPos at = center.offset(Math.round(Mth.cos(angle) * (radius - 5)), 0, Math.round(Mth.sin(angle) * (radius - 5)));
            tent(level, at, random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z, cloth, wood);
            chests.add(at.above());
        }
        // Palisade of sharpened logs on part of the edge, with skulls as trophies
        float gapStart = random.nextFloat() * Mth.TWO_PI;
        for (int step = 0; step < 48; step++) {
            float angle = step * Mth.TWO_PI / 48;
            float gap = Math.abs(Mth.wrapDegrees((angle - gapStart) * Mth.RAD_TO_DEG));
            if (gap < 70) {
                continue;
            }
            BlockPos post = center.offset(Math.round(Mth.cos(angle) * (radius - 1)), 0, Math.round(Mth.sin(angle) * (radius - 1)));
            if (!free(level, post, 4)) {
                continue;
            }
            for (int y = 1; y <= 3; y++) {
                set(level, post.above(y), wood.log());
            }
            if (step % 7 == 0) {
                level.setBlock(post.above(4), Blocks.SKELETON_SKULL.defaultBlockState()
                        .setValue(SkullBlock.ROTATION, random.nextInt(16)), 2);
            } else {
                set(level, post.above(4), wood.fence());
            }
        }
        scatter(level, center, radius - 3, random, 8, Blocks.BARREL, Blocks.HAY_BLOCK, Blocks.CAULDRON, Blocks.SMITHING_TABLE);
        groundPatch(level, center, radius - 1, random, 0.3F, Blocks.COARSE_DIRT, Blocks.PACKED_MUD);
    }

    /** An A-frame tent of cloth on a wooden ridge, 5 wide and 4 long, open at both ends. */
    private static void tent(ServerLevel level, BlockPos center, Direction.Axis axis, Block cloth, Wood wood) {
        for (int l = -2; l <= 1; l++) {
            for (int w = -2; w <= 2; w++) {
                int y = 3 - Math.abs(w);
                BlockPos pos = axis == Direction.Axis.X ? center.offset(l, y, w) : center.offset(w, y, l);
                set(level, pos, w == 0 ? wood.planks() : cloth);
                // Clear inside
                for (int under = 1; under < y; under++) {
                    BlockPos inside = axis == Direction.Axis.X ? center.offset(l, under, w) : center.offset(w, under, l);
                    set(level, inside, Blocks.AIR);
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Grove
    // ------------------------------------------------------------------------------------------------------------

    private static void grove(ServerLevel level, DungeonTheme theme, BlockPos center, int radius, RandomSource random) {
        Wood wood = theme.wood();
        Block leaves = theme.leaves();
        int trees = 5 + random.nextInt(4);
        for (int i = 0; i < trees; i++) {
            BlockPos base = ground(center, radius - 2, random);
            if (base.distSqr(center) < 25 || !free(level, base, 8)) {
                continue;
            }
            int height = 5 + random.nextInt(4);
            for (int y = 1; y <= height; y++) {
                set(level, base.above(y), wood.log());
            }
            int crown = 2 + random.nextInt(2);
            BlockPos top = base.above(height);
            for (int x = -crown; x <= crown; x++) {
                for (int y = -1; y <= crown; y++) {
                    for (int z = -crown; z <= crown; z++) {
                        if (x * x + y * y * 2 + z * z <= crown * crown + 1 && random.nextFloat() < 0.9F) {
                            BlockPos pos = top.offset(x, y, z);
                            if (level.getBlockState(pos).isAir()) {
                                level.setBlock(pos, leaves.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true), 2);
                            }
                        }
                    }
                }
            }
        }
        // Mossy boulders
        for (int i = 0; i < 3; i++) {
            BlockPos rock = ground(center, radius - 3, random);
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (random.nextFloat() < 0.75F) {
                        set(level, rock.offset(x, 1, z), random.nextBoolean() ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE);
                    }
                }
            }
            set(level, rock.above(2), Blocks.MOSSY_COBBLESTONE);
        }
        scatter(level, center, radius - 1, random, 30, Blocks.FERN, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.RED_MUSHROOM,
                Blocks.BROWN_MUSHROOM, Blocks.SWEET_BERRY_BUSH);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Graveyard
    // ------------------------------------------------------------------------------------------------------------

    private static void crypt(ServerLevel level, DungeonTheme theme, BlockPos center, int radius, RandomSource random,
                              List<BlockPos> chests) {
        // Rows of tombstones with their graves
        for (int row = -radius + 3; row <= radius - 3; row += 4) {
            for (int col = -radius + 3; col <= radius - 3; col += 3) {
                if (Math.abs(row) < 4 && Math.abs(col) < 4 || row * row + col * col > (radius - 2) * (radius - 2) || random.nextInt(4) == 0) {
                    continue;
                }
                BlockPos stone = center.offset(col, 0, row);
                if (!free(level, stone, 3)) {
                    continue;
                }
                set(level, stone.above(), random.nextInt(3) == 0 ? Blocks.CHISELED_STONE_BRICKS : Blocks.STONE_BRICK_WALL);
                if (random.nextBoolean()) {
                    set(level, stone.above(2), Blocks.STONE_BRICK_WALL);
                }
                set(level, stone.south(), random.nextInt(3) == 0 ? Blocks.ROOTED_DIRT : Blocks.COARSE_DIRT);
                set(level, stone.south(2), Blocks.COARSE_DIRT);
                if (random.nextInt(6) == 0) {
                    level.setBlock(stone.south().above(), Blocks.SKELETON_SKULL.defaultBlockState()
                            .setValue(SkullBlock.ROTATION, random.nextInt(16)), 2);
                }
            }
        }
        // A small mausoleum with a chest inside
        BlockPos tomb = ground(center, radius - 6, random);
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 1; y <= 4; y++) {
                    boolean wall = Math.abs(x) == 2 || Math.abs(z) == 2 || y == 4;
                    BlockState state = wall ? (random.nextInt(4) == 0 ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS).defaultBlockState()
                            : Blocks.AIR.defaultBlockState();
                    level.setBlock(tomb.offset(x, y, z), state, 2);
                }
            }
        }
        set(level, tomb.offset(0, 1, -2), Blocks.AIR);
        set(level, tomb.offset(0, 2, -2), Blocks.IRON_BARS);
        chests.add(tomb.offset(0, 1, 1));
        // Soul lanterns on posts
        for (int i = 0; i < 4; i++) {
            BlockPos post = ground(center, radius - 2, random);
            if (free(level, post, 3)) {
                set(level, post.above(), Blocks.DARK_OAK_FENCE);
                set(level, post.above(2), Blocks.SOUL_LANTERN);
            }
        }
        scatter(level, center, radius - 1, random, 10, Blocks.COBWEB, Blocks.BONE_BLOCK, Blocks.DEAD_BUSH, Blocks.CANDLE);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Nest
    // ------------------------------------------------------------------------------------------------------------

    private static void nest(ServerLevel level, DungeonTheme theme, BlockPos center, int radius, RandomSource random) {
        // Mounds of earth
        for (int i = 0; i < 5; i++) {
            BlockPos mound = ground(center, radius - 4, random);
            if (mound.distSqr(center) < 25) {
                continue;
            }
            int size = 2 + random.nextInt(2);
            for (int x = -size; x <= size; x++) {
                for (int z = -size; z <= size; z++) {
                    int height = size - (int) Math.sqrt(x * x + z * z);
                    for (int y = 1; y <= height; y++) {
                        set(level, mound.offset(x, y, z), random.nextInt(3) == 0 ? Blocks.MUD : Blocks.COARSE_DIRT);
                    }
                }
            }
        }
        // Clutches of eggs, webs and bones
        for (int i = 0; i < 6; i++) {
            BlockPos clutch = ground(center, radius - 2, random);
            for (int j = 0; j < 4; j++) {
                BlockPos egg = clutch.offset(random.nextInt(3) - 1, 1, random.nextInt(3) - 1);
                if (level.getBlockState(egg).isAir() && !level.getBlockState(egg.below()).isAir()) {
                    set(level, egg, Blocks.TURTLE_EGG);
                }
            }
        }
        scatter(level, center, radius - 1, random, 22, Blocks.COBWEB, Blocks.COBWEB, Blocks.BONE_BLOCK, Blocks.SLIME_BLOCK);
        groundPatch(level, center, radius - 1, random, 0.3F, Blocks.PACKED_MUD, Blocks.MUD, Blocks.COARSE_DIRT);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ice spikes
    // ------------------------------------------------------------------------------------------------------------

    private static void iceSpikes(ServerLevel level, BlockPos center, int radius, RandomSource random) {
        int spikes = 5 + random.nextInt(4);
        for (int i = 0; i < spikes; i++) {
            BlockPos base = ground(center, radius - 2, random);
            if (base.distSqr(center) < 25) {
                continue;
            }
            int height = 4 + random.nextInt(7);
            for (int y = 1; y <= height; y++) {
                int width = y < height / 3 ? 1 : 0;
                for (int x = -width; x <= width; x++) {
                    for (int z = -width; z <= width; z++) {
                        set(level, base.offset(x, y, z), random.nextInt(6) == 0 ? Blocks.BLUE_ICE : Blocks.PACKED_ICE);
                    }
                }
            }
        }
        // Frozen statues of fallen hunters
        for (int i = 0; i < 2; i++) {
            BlockPos statue = ground(center, radius - 4, random);
            set(level, statue.above(), Blocks.ICE);
            set(level, statue.above(2), Blocks.ICE);
            level.setBlock(statue.above(3), Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, random.nextInt(16)), 2);
        }
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos pos = center.offset(x, 1, z);
                if (x * x + z * z <= radius * radius && random.nextInt(3) > 0 && level.getBlockState(pos).isAir()
                        && level.getBlockState(pos.below()).isSolid()) {
                    level.setBlock(pos, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1 + random.nextInt(2)), 2);
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Temple
    // ------------------------------------------------------------------------------------------------------------

    private static void shrine(ServerLevel level, DungeonTheme theme, BlockPos center, int radius, RandomSource random,
                               List<BlockPos> chests) {
        // Two rows of pillars along a processional way
        for (int t = -radius + 3; t <= radius - 3; t += 4) {
            for (int side : new int[]{-4, 4}) {
                BlockPos base = center.offset(t, 0, side);
                int height = random.nextInt(5) == 0 ? 2 + random.nextInt(3) : 6;
                for (int y = 1; y <= height; y++) {
                    set(level, base.above(y), theme.pillar);
                }
                if (height == 6) {
                    set(level, base.above(7), theme.light);
                }
            }
        }
        // Altar at the end of the way, with pools on both sides
        BlockPos altar = center.offset(radius - 4, 0, 0);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                set(level, altar.offset(x, 1, z), theme.accent);
            }
        }
        set(level, altar.above(2), Blocks.CANDLE);
        chests.add(altar.offset(-2, 1, 0));
        for (int side : new int[]{-8, 8}) {
            BlockPos pool = center.offset(0, 0, side);
            for (int x = -2; x <= 2; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (Math.abs(pool.getZ() + z - center.getZ()) < radius - 2) {
                        set(level, pool.offset(x, 0, z), theme.hellish() ? Blocks.LAVA : Blocks.WATER);
                        set(level, pool.offset(x, -1, z), theme.accent);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Hellscape
    // ------------------------------------------------------------------------------------------------------------

    private static void hellscape(ServerLevel level, DungeonTheme theme, BlockPos center, int radius, RandomSource random) {
        // Lava pools sunk into the ground
        for (int i = 0; i < 3; i++) {
            BlockPos pool = ground(center, radius - 5, random);
            if (pool.distSqr(center) < 25) {
                continue;
            }
            int size = 1 + random.nextInt(2);
            for (int x = -size; x <= size; x++) {
                for (int z = -size; z <= size; z++) {
                    set(level, pool.offset(x, 0, z), Blocks.LAVA);
                    set(level, pool.offset(x, -1, z), Blocks.MAGMA_BLOCK);
                }
            }
        }
        // Spikes of netherrack and basalt
        for (int i = 0; i < 7; i++) {
            BlockPos base = ground(center, radius - 2, random);
            if (base.distSqr(center) < 25 || !free(level, base, 6)) {
                continue;
            }
            int height = 3 + random.nextInt(6);
            Block block = random.nextBoolean() ? Blocks.NETHERRACK : Blocks.BASALT;
            for (int y = 1; y <= height; y++) {
                set(level, base.above(y), block);
            }
            if (block == Blocks.NETHERRACK) {
                set(level, base.above(height + 1), Blocks.FIRE);
            }
        }
        // Burning patches and soul fires
        for (int i = 0; i < 10; i++) {
            BlockPos patch = ground(center, radius - 2, random);
            if (level.getBlockState(patch.above()).isAir() && level.getBlockState(patch).isSolid()) {
                boolean soul = random.nextBoolean();
                set(level, patch, soul ? Blocks.SOUL_SOIL : Blocks.NETHERRACK);
                set(level, patch.above(), soul ? Blocks.SOUL_FIRE : Blocks.FIRE);
            }
        }
        scatter(level, center, radius - 1, random, 8, Blocks.BONE_BLOCK, Blocks.MAGMA_BLOCK, Blocks.CRYING_OBSIDIAN);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------------------------

    static void set(ServerLevel level, BlockPos pos, Block block) {
        level.setBlock(pos, block.defaultBlockState(), 2);
    }

    /** A random ground block within {@code radius} of {@code center}. */
    static BlockPos ground(BlockPos center, int radius, RandomSource random) {
        float angle = random.nextFloat() * Mth.TWO_PI;
        float distance = Mth.sqrt(random.nextFloat()) * Math.max(1, radius);
        return center.offset(Math.round(Mth.cos(angle) * distance), 0, Math.round(Mth.sin(angle) * distance));
    }

    /** Whether the {@code height} blocks above this ground block are empty. */
    static boolean free(ServerLevel level, BlockPos ground, int height) {
        for (int y = 1; y <= height; y++) {
            if (!level.getBlockState(ground.above(y)).isAir()) {
                return false;
            }
        }
        return level.getBlockState(ground).isSolid();
    }

    /** Puts {@code count} random props on free ground. */
    private static void scatter(ServerLevel level, BlockPos center, int radius, RandomSource random, int count, Block... props) {
        for (int i = 0; i < count; i++) {
            BlockPos at = ground(center, radius, random);
            if (at.distSqr(center) >= 9 && free(level, at, 2)) {
                Block prop = props[random.nextInt(props.length)];
                if (prop == Blocks.SWEET_BERRY_BUSH || prop == Blocks.DEAD_BUSH || prop == Blocks.FERN || prop == Blocks.SHORT_GRASS
                        || prop == Blocks.RED_MUSHROOM || prop == Blocks.BROWN_MUSHROOM) {
                    // Plants need soil under them
                    if (!level.getBlockState(at).is(Blocks.GRASS_BLOCK) && !level.getBlockState(at).is(Blocks.MOSS_BLOCK)
                            && !level.getBlockState(at).is(Blocks.PODZOL)) {
                        set(level, at, Blocks.PODZOL);
                    }
                }
                set(level, at.above(), prop);
            }
        }
    }

    /** Replaces some ground blocks with patches of other blocks (paths, mud, moss...). */
    private static void groundPatch(ServerLevel level, BlockPos center, int radius, RandomSource random, float chance, Block... blocks) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos pos = center.offset(x, 0, z);
                if (x * x + z * z <= radius * radius && random.nextFloat() < chance && level.getBlockState(pos).isSolid()
                        && level.getBlockState(pos.above()).isAir()) {
                    set(level, pos, blocks[random.nextInt(blocks.length)]);
                }
            }
        }
    }

    /** A hanging lantern on a chain from a ceiling block above {@code below}, if there is one within reach. */
    static void hangingLantern(ServerLevel level, BlockPos below, int maxChain, boolean soul) {
        BlockPos.MutableBlockPos pos = below.mutable();
        int chain = 0;
        while (level.getBlockState(pos.above()).isAir() && chain < 16) {
            pos.move(Direction.UP);
            chain++;
        }
        if (chain >= 16 || level.getBlockState(pos.above()).isAir()) {
            return;
        }
        int length = Math.min(maxChain, chain - 3);
        for (int i = 0; i < length; i++) {
            level.setBlock(pos, Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y), 2);
            pos.move(Direction.DOWN);
        }
        level.setBlock(pos, (soul ? Blocks.SOUL_LANTERN : Blocks.LANTERN).defaultBlockState().setValue(LanternBlock.HANGING, true), 2);
    }
}
