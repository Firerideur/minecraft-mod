package com.firerideur.arcanequest.dungeon;

import com.firerideur.arcanequest.mobs.MobLevels;
import com.firerideur.arcanequest.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds a dungeon: a chain of rooms joined by corridors, laid out by a random walk on a grid.
 * The first room holds the way out, the last one the gate boss; the rooms in between hold monsters
 * (more and stronger with the stars) and sometimes a chest.
 */
public final class DungeonGenerator {
    /** Distance between room centers. */
    static final int CELL = 24;
    /** Rooms are 15x15 inside, walls at +-8. */
    static final int HALF = 8;
    static final int HEIGHT = 8;
    private static final int CORRIDOR_HALF = 2;
    private static final int CORRIDOR_HEIGHT = 5;

    private DungeonGenerator() {}

    /** Rooms in a dungeon of this rank. */
    public static int roomCount(int stars) {
        return 3 + stars;
    }

    /** Level of the monsters behind a gate of this rank (1 star: 4-9 ... 7 stars: 76-81). */
    public static int mobLevel(int stars, RandomSource random) {
        return stars * 12 - 8 + random.nextInt(6);
    }

    public static void generate(ServerLevel level, DungeonData.Dungeon dungeon, RandomSource random) {
        DungeonTheme theme = DungeonTheme.of(dungeon.stars);
        BlockPos origin = dungeon.origin();
        List<BlockPos> rooms = layout(origin, roomCount(dungeon.stars), random);
        for (BlockPos room : rooms) {
            room(level, theme, room, random);
        }
        for (int i = 1; i < rooms.size(); i++) {
            corridor(level, theme, rooms.get(i - 1), rooms.get(i));
        }
        for (int i = 1; i < rooms.size(); i++) {
            BlockPos center = rooms.get(i);
            boolean boss = i == rooms.size() - 1;
            if (boss) {
                pillars(level, theme, center);
                spawnBoss(level, dungeon, theme, center, random);
                for (int m = 0; m < 2; m++) {
                    spawnMob(level, dungeon, theme.randomMob(random), center, random);
                }
            } else {
                int count = 2 + dungeon.stars / 2 + random.nextInt(3);
                for (int m = 0; m < count; m++) {
                    spawnMob(level, dungeon, theme.randomMob(random), center, random);
                }
                if (random.nextFloat() < 0.35F) {
                    chest(level, center.offset(HALF - 2, 1, HALF - 2), Rewards.roomLoot(dungeon.stars, random));
                }
            }
        }
        // Rooms never go west, so the west wall of the first room is free for the way out
        dungeon.entry = rooms.get(0).offset(-2, 1, 0);
        dungeon.bossRoom = rooms.get(rooms.size() - 1);
        // The way out, against the wall of the first room
        GateEntity exit = new GateEntity(ModEntities.GATE.get(), level);
        exit.setup(dungeon.id, dungeon.stars, true);
        BlockPos exitPos = rooms.get(0).offset(-HALF + 2, 1, 0);
        exit.moveTo(exitPos.getX() + 0.5, exitPos.getY(), exitPos.getZ() + 0.5, 0, 0);
        level.addFreshEntity(exit);
        dungeon.generated = true;
    }

    /** Room centers: a random walk that never goes back on itself or west, so rooms never overlap. */
    static List<BlockPos> layout(BlockPos origin, int count, RandomSource random) {
        List<BlockPos> rooms = new ArrayList<>();
        Set<Long> used = new HashSet<>();
        int gx = 0, gz = 0;
        rooms.add(origin);
        used.add(key(0, 0));
        Direction[] choices = {Direction.EAST, Direction.EAST, Direction.NORTH, Direction.SOUTH};
        while (rooms.size() < count) {
            Direction dir = choices[random.nextInt(choices.length)];
            int nx = gx + dir.getStepX(), nz = gz + dir.getStepZ();
            if (!used.add(key(nx, nz))) {
                // Taken: east is always free since we never go west
                nx = gx + 1;
                nz = gz;
                used.add(key(nx, nz));
            }
            gx = nx;
            gz = nz;
            rooms.add(origin.offset(gx * CELL, 0, gz * CELL));
        }
        return rooms;
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 2);
    }

    private static void room(ServerLevel level, DungeonTheme theme, BlockPos center, RandomSource random) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -HALF; x <= HALF; x++) {
            for (int z = -HALF; z <= HALF; z++) {
                for (int y = 0; y <= HEIGHT; y++) {
                    pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    boolean wall = Math.abs(x) == HALF || Math.abs(z) == HALF;
                    BlockState state;
                    if (y == 0) {
                        state = (random.nextFloat() < 0.12F ? theme.accent : theme.floor).defaultBlockState();
                    } else if (y == HEIGHT) {
                        boolean light = (Math.abs(x) == 4 || x == 0) && (Math.abs(z) == 4 || z == 0) && (x == 0) == (z == 0);
                        state = (light ? theme.light : theme.ceiling).defaultBlockState();
                    } else if (wall) {
                        state = (random.nextFloat() < 0.15F ? theme.accent : theme.wall).defaultBlockState();
                    } else if (theme.cobwebs && y == HEIGHT - 1 && Math.abs(x) == HALF - 1 && Math.abs(z) == HALF - 1) {
                        state = Blocks.COBWEB.defaultBlockState();
                    } else {
                        state = Blocks.AIR.defaultBlockState();
                    }
                    set(level, pos, state);
                }
            }
        }
    }

    private static void corridor(ServerLevel level, DungeonTheme theme, BlockPos from, BlockPos to) {
        boolean alongX = from.getZ() == to.getZ();
        BlockPos a = alongX ? (from.getX() < to.getX() ? from : to) : (from.getZ() < to.getZ() ? from : to);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int t = HALF; t <= CELL - HALF; t++) {
            for (int w = -CORRIDOR_HALF; w <= CORRIDOR_HALF; w++) {
                for (int y = 0; y <= CORRIDOR_HEIGHT; y++) {
                    if (alongX) {
                        pos.set(a.getX() + t, a.getY() + y, a.getZ() + w);
                    } else {
                        pos.set(a.getX() + w, a.getY() + y, a.getZ() + t);
                    }
                    boolean inRoomWall = t == HALF || t == CELL - HALF;
                    BlockState state;
                    if (y == 0) {
                        state = theme.floor.defaultBlockState();
                    } else if (inRoomWall && Math.abs(w) < CORRIDOR_HALF && y < CORRIDOR_HEIGHT) {
                        state = Blocks.AIR.defaultBlockState();
                    } else if (inRoomWall) {
                        continue;
                    } else if (y == CORRIDOR_HEIGHT) {
                        state = (w == 0 && t % 4 == 0 ? theme.light : theme.ceiling).defaultBlockState();
                    } else if (Math.abs(w) == CORRIDOR_HALF) {
                        state = theme.wall.defaultBlockState();
                    } else {
                        state = Blocks.AIR.defaultBlockState();
                    }
                    set(level, pos, state);
                }
            }
        }
    }

    private static void pillars(ServerLevel level, DungeonTheme theme, BlockPos center) {
        for (int sx : new int[]{-5, 5}) {
            for (int sz : new int[]{-5, 5}) {
                for (int y = 1; y < HEIGHT; y++) {
                    set(level, center.offset(sx, y, sz), theme.pillar.defaultBlockState());
                }
            }
        }
    }

    private static DungeonMob spawnMob(ServerLevel level, DungeonData.Dungeon dungeon, EntityType<? extends DungeonMob> type,
                                       BlockPos center, RandomSource random) {
        DungeonMob mob = type.create(level);
        if (mob == null) {
            return null;
        }
        double x = center.getX() + 0.5 + random.nextInt(11) - 5;
        double z = center.getZ() + 0.5 + random.nextInt(11) - 5;
        mob.moveTo(x, center.getY() + 1, z, random.nextFloat() * 360, 0);
        mob.setDungeonId(dungeon.id);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.STRUCTURE, null);
        MobLevels.setLevel(mob, mobLevel(dungeon.stars, random), false);
        level.addFreshEntity(mob);
        return mob;
    }

    private static void spawnBoss(ServerLevel level, DungeonData.Dungeon dungeon, DungeonTheme theme, BlockPos center, RandomSource random) {
        DungeonMob boss = theme.boss.get().create(level);
        if (boss == null) {
            return;
        }
        boss.moveTo(center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5, 0, 0);
        boss.setDungeonId(dungeon.id);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(center), MobSpawnType.STRUCTURE, null);
        MobLevels.setLevel(boss, mobLevel(dungeon.stars, random) + 3, false);
        boss.makeGateBoss(dungeon.stars);
        level.addFreshEntity(boss);
    }

    static void chest(ServerLevel level, BlockPos pos, List<ItemStack> loot) {
        set(level, pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH));
        if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
            for (int i = 0; i < loot.size() && i < chest.getContainerSize(); i++) {
                chest.setItem(i * 2 % chest.getContainerSize(), loot.get(i));
            }
        }
    }
}
