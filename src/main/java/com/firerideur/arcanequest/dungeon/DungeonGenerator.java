package com.firerideur.arcanequest.dungeon;

import com.firerideur.arcanequest.mobs.MobLevels;
import com.firerideur.arcanequest.registry.ModBlocks;
import com.firerideur.arcanequest.registry.ModEntities;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the world behind a gate: a landscape closed by cliffs (and a rock ceiling underground), made of zones joined
 * by paths. The first zone holds the way out; each following zone is a scene (an abandoned village, a war camp,
 * ruins...) with its monsters; the last one is the boss arena, sealed until every monster of the dungeon is dead.
 */
public final class DungeonGenerator {
    /** Monsters keep this far from the edge of their zone. */
    private static final int EDGE = 3;
    private static final int ENTRY_RADIUS = 9;
    private static final int ARENA_RADIUS = 17;
    private static final int PATH_HALF_WIDTH = 3;
    /** Width of the cliffs around the walkable area. */
    private static final int CLIFF = 5;
    /** Part of the chests that are mimics. */
    static final float MIMIC_CHANCE = 0.3F;

    private DungeonGenerator() {}

    /** Zones in a dungeon of this rank: the entry, one scene per star and one more, and the boss arena. */
    public static int roomCount(int stars) {
        return 3 + stars;
    }

    /** Level of the monsters behind a gate of this rank (1 star: 4-9 ... 7 stars: 76-81). */
    public static int mobLevel(int stars, RandomSource random) {
        return stars * 12 - 8 + random.nextInt(6);
    }

    enum Kind { ENTRY, SCENE, ARENA }

    record Zone(BlockPos center, int radius, Kind kind, DungeonScenes.Scene scene) {}

    public static void generate(ServerLevel level, DungeonData.Dungeon dungeon, RandomSource random) {
        DungeonTheme theme = dungeon.theme();
        BlockPos origin = dungeon.origin();
        List<Zone> zones = layout(origin, roomCount(dungeon.stars), theme, random);
        long seed = random.nextLong();
        terrain(level, theme, zones, seed);

        int required = 0;
        for (Zone zone : zones) {
            switch (zone.kind()) {
                case ENTRY -> entry(level, dungeon, theme, zone);
                case SCENE -> required += scene(level, dungeon, theme, zone, random);
                case ARENA -> arena(level, dungeon, theme, zone, random);
            }
        }
        seal(level, dungeon, zones.get(zones.size() - 2), zones.get(zones.size() - 1));
        dungeon.required = required;
        dungeon.killed = 0;
        dungeon.generated = true;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------------------------------------------------

    /** Zone centers: a winding chain heading east (never back west, so the way out stays behind the player). */
    static List<Zone> layout(BlockPos origin, int count, DungeonTheme theme, RandomSource random) {
        List<Zone> zones = new ArrayList<>();
        zones.add(new Zone(origin, ENTRY_RADIUS, Kind.ENTRY, null));
        List<DungeonScenes.Scene> scenes = new ArrayList<>();
        float heading = 0;
        for (int i = 1; i < count; i++) {
            boolean arena = i == count - 1;
            if (scenes.isEmpty()) {
                scenes.addAll(theme.scenes());
                Util.shuffle(scenes, random);
            }
            int radius = arena ? ARENA_RADIUS : 11 + random.nextInt(5);
            Zone previous = zones.get(zones.size() - 1);
            BlockPos center = null;
            for (int attempt = 0; attempt < 24 && center == null; attempt++) {
                float angle = Mth.clamp(heading * 0.5F + (random.nextFloat() - 0.5F) * 2.2F, -1.1F, 1.1F);
                int distance = previous.radius() + radius + 8 + random.nextInt(8);
                BlockPos candidate = previous.center().offset(Math.round(Mth.cos(angle) * distance), 0,
                        Math.round(Mth.sin(angle) * distance));
                if (clear(zones, candidate, radius, previous)) {
                    center = candidate;
                    heading = angle;
                }
            }
            if (center == null) {
                // Straight east always works: every other zone is further west
                center = previous.center().offset(previous.radius() + radius + 10, 0, 0);
                heading = 0;
            }
            zones.add(new Zone(center, radius, arena ? Kind.ARENA : Kind.SCENE, arena ? null : scenes.remove(0)));
        }
        return zones;
    }

    private static boolean clear(List<Zone> zones, BlockPos center, int radius, Zone previous) {
        for (Zone zone : zones) {
            double min = zone.radius() + radius + (zone == previous ? 6 : 12);
            if (zone.center().distSqr(center) < min * min) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Terrain
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Ground under every zone and path, cliffs all around (wobbly, so it looks natural) and, underground, a rock
     * ceiling with a few lights. The arena stays perfectly flat.
     */
    private static void terrain(ServerLevel level, DungeonTheme theme, List<Zone> zones, long seed) {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (Zone zone : zones) {
            minX = Math.min(minX, zone.center().getX() - zone.radius());
            maxX = Math.max(maxX, zone.center().getX() + zone.radius());
            minZ = Math.min(minZ, zone.center().getZ() - zone.radius());
            maxZ = Math.max(maxZ, zone.center().getZ() + zone.radius());
        }
        int margin = CLIFF + 4;
        int base = zones.get(0).center().getY();
        boolean open = theme.open();
        DungeonTheme.Terrain terrain = theme.terrain();
        RandomSource random = RandomSource.create(seed);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Zone entry = zones.get(0), arena = zones.get(zones.size() - 1);
        for (int x = minX - margin; x <= maxX + margin; x++) {
            for (int z = minZ - margin; z <= maxZ + margin; z++) {
                double distance = distance(zones, x, z) + noise(seed, x, z, 9) * 2.5;
                if (distance > CLIFF) {
                    continue;
                }
                BlockPos column = new BlockPos(x, base, z);
                boolean inArena = arena.center().distSqr(column) < (arena.radius() + 1) * (arena.radius() + 1);
                // The arrival point and the arena stay perfectly flat
                boolean flat = inArena || entry.center().distSqr(column) < (entry.radius() + 2) * (entry.radius() + 2);
                int ceiling = base + 11 + (int) (noise(seed + 1, x, z, 7) * 3) + (inArena ? 5 : 0);
                // Soil
                for (int y = base - 3; y < base; y++) {
                    level.setBlock(pos.set(x, y, z), terrain.rock().defaultBlockState(), 2);
                }
                if (distance <= 0) {
                    int bump = !flat && distance < -3 && noise(seed + 2, x, z, 5) > 0.55 ? 1 : 0;
                    for (int y = base; y <= base + bump; y++) {
                        Block ground = noise(seed + 4, x, z, 4) > 0.35 ? terrain.groundAccent() : terrain.ground();
                        level.setBlock(pos.set(x, y, z), ground.defaultBlockState(), 2);
                    }
                    if (!open) {
                        for (int y = ceiling; y <= ceiling + 1; y++) {
                            Block block = y == ceiling && random.nextFloat() < 0.025F ? theme.light : terrain.rock();
                            level.setBlock(pos.set(x, y, z), block.defaultBlockState(), 2);
                        }
                    }
                } else {
                    // Cliff, higher the further out (and up to the ceiling underground)
                    int top = open ? base + 10 + (int) (distance * 1.6) + (int) (noise(seed + 3, x, z, 6) * 5) : ceiling + 1;
                    for (int y = base; y <= top; y++) {
                        Block block = noise(seed + 5, x + y * 3, z - y * 2, 3) > 0.3 ? terrain.rockAccent() : terrain.rock();
                        if (distance < 1.5 && random.nextFloat() < 0.015F) {
                            block = theme.light;
                        }
                        level.setBlock(pos.set(x, y, z), block.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    /** Signed distance from (x, z) to the walkable area (negative inside). */
    private static double distance(List<Zone> zones, int x, int z) {
        double best = Double.MAX_VALUE;
        for (int i = 0; i < zones.size(); i++) {
            Zone zone = zones.get(i);
            double dx = x - zone.center().getX(), dz = z - zone.center().getZ();
            best = Math.min(best, Math.sqrt(dx * dx + dz * dz) - zone.radius());
            if (i > 0) {
                best = Math.min(best, segment(zones.get(i - 1).center(), zone.center(), x, z) - PATH_HALF_WIDTH);
            }
        }
        return best;
    }

    private static double segment(BlockPos a, BlockPos b, double x, double z) {
        double abx = b.getX() - a.getX(), abz = b.getZ() - a.getZ();
        double t = Mth.clamp(((x - a.getX()) * abx + (z - a.getZ()) * abz) / (abx * abx + abz * abz), 0, 1);
        double px = a.getX() + abx * t - x, pz = a.getZ() + abz * t - z;
        return Math.sqrt(px * px + pz * pz);
    }

    /** Smooth value noise in [-1, 1], with cells of {@code cell} blocks. */
    static double noise(long seed, int x, int z, int cell) {
        int cx = Math.floorDiv(x, cell), cz = Math.floorDiv(z, cell);
        double fx = (x - cx * cell) / (double) cell, fz = (z - cz * cell) / (double) cell;
        fx = fx * fx * (3 - 2 * fx);
        fz = fz * fz * (3 - 2 * fz);
        double a = corner(seed, cx, cz), b = corner(seed, cx + 1, cz), c = corner(seed, cx, cz + 1), d = corner(seed, cx + 1, cz + 1);
        return Mth.lerp(fz, Mth.lerp(fx, a, b), Mth.lerp(fx, c, d));
    }

    private static double corner(long seed, int x, int z) {
        long h = Mth.getSeed(x, (int) seed, z) ^ (seed >>> 17);
        return ((h >>> 11) & 0xFFFF) / 32767.5 - 1.0;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Zones
    // ------------------------------------------------------------------------------------------------------------

    /** The way out against the west cliff, with lanterns, and the player's arrival point facing east. */
    private static void entry(ServerLevel level, DungeonData.Dungeon dungeon, DungeonTheme theme, Zone zone) {
        BlockPos center = zone.center();
        dungeon.entry = center.above();
        GateEntity exit = new GateEntity(ModEntities.GATE.get(), level);
        exit.setup(dungeon.id, dungeon.stars, true);
        BlockPos exitPos = center.offset(-zone.radius() + 3, 1, 0);
        exit.moveTo(exitPos.getX() + 0.5, exitPos.getY(), exitPos.getZ() + 0.5, 0, 0);
        level.addFreshEntity(exit);
        for (int side : new int[]{-3, 3}) {
            BlockPos post = center.offset(-2, 0, side);
            DungeonScenes.set(level, post.above(), theme.pillar);
            DungeonScenes.set(level, post.above(2), theme.pillar);
            DungeonScenes.set(level, post.above(3), theme.light);
        }
    }

    /** A scene with its monsters and maybe a chest. Returns how many monsters must die. */
    private static int scene(ServerLevel level, DungeonData.Dungeon dungeon, DungeonTheme theme, Zone zone, RandomSource random) {
        List<BlockPos> chests = new ArrayList<>();
        DungeonScenes.build(level, zone.scene(), theme, zone.center(), zone.radius(), random, chests);
        if (!theme.open()) {
            for (int i = 0; i < 3; i++) {
                DungeonScenes.hangingLantern(level, DungeonScenes.ground(zone.center(), zone.radius() - 3, random).above(), 3,
                        theme.soulFire());
            }
        }
        int count = 3 + dungeon.stars / 2 + random.nextInt(3);
        int required = 0;
        for (int m = 0; m < count; m++) {
            if (spawnMob(level, dungeon, theme.randomMob(random), zone, random, true) != null) {
                required++;
            }
        }
        if (!chests.isEmpty() && random.nextFloat() < 0.6F) {
            BlockPos chestPos = chests.get(random.nextInt(chests.size()));
            if (level.getBlockState(chestPos).isAir()) {
                if (random.nextFloat() < MIMIC_CHANCE) {
                    // Not a chest: it doesn't count toward the objective, nobody would know it's a monster
                    DungeonMob mimic = spawnMob(level, dungeon, ModEntities.MIMIC.get(), zone, random, false);
                    if (mimic != null) {
                        mimic.moveTo(chestPos.getX() + 0.5, chestPos.getY(), chestPos.getZ() + 0.5, 180.0F, 0);
                        mimic.setYHeadRot(180.0F);
                        mimic.setYBodyRot(180.0F);
                    }
                } else {
                    chest(level, chestPos, Rewards.roomLoot(dungeon.stars, random));
                }
            }
        }
        return required;
    }

    /** A flat round arena ringed with pillars and braziers, a dais in the middle, the boss and two guards. */
    private static void arena(ServerLevel level, DungeonData.Dungeon dungeon, DungeonTheme theme, Zone zone, RandomSource random) {
        BlockPos center = zone.center();
        dungeon.bossRoom = center;
        dungeon.arenaRadius = zone.radius();
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                if (x * x + z * z <= 17) {
                    DungeonScenes.set(level, center.offset(x, 0, z), x * x + z * z <= 5 ? theme.pillar : theme.accent);
                }
            }
        }
        BlockState brazier = (theme.soulFire() ? Blocks.SOUL_CAMPFIRE : Blocks.CAMPFIRE).defaultBlockState().setValue(CampfireBlock.LIT, true);
        for (int i = 0; i < 8; i++) {
            float angle = i * Mth.TWO_PI / 8;
            BlockPos base = center.offset(Math.round(Mth.cos(angle) * (zone.radius() - 4)), 0, Math.round(Mth.sin(angle) * (zone.radius() - 4)));
            for (int y = 1; y <= 5; y++) {
                DungeonScenes.set(level, base.above(y), theme.pillar);
            }
            level.setBlock(base.above(6), brazier, 2);
        }
        spawnBoss(level, dungeon, theme, center, random);
        for (int m = 0; m < 2; m++) {
            spawnMob(level, dungeon, theme.randomMob(random), new Zone(center, 8, Kind.ARENA, null), random, false);
        }
    }

    /**
     * Closes the path into the arena with a wall of seal blocks, between the last scene and the arena. It breaks when
     * every required monster is dead (see {@link Gates#onMonsterKilled}).
     */
    private static void seal(ServerLevel level, DungeonData.Dungeon dungeon, Zone before, Zone arena) {
        double dx = arena.center().getX() - before.center().getX(), dz = arena.center().getZ() - before.center().getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        dx /= length;
        dz /= length;
        double cx = arena.center().getX() + 0.5 - dx * (arena.radius() + 4), cz = arena.center().getZ() + 0.5 - dz * (arena.radius() + 4);
        int base = arena.center().getY();
        BlockState seal = ModBlocks.DUNGEON_SEAL.get().defaultBlockState();
        int minX = Integer.MAX_VALUE, minY = base + 1, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = base + 16, maxZ = Integer.MIN_VALUE;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (double t = -8; t <= 8; t += 0.5) {
            for (double depth = 0; depth <= 1.5; depth += 0.5) {
                int x = Mth.floor(cx - dz * t + dx * depth), z = Mth.floor(cz + dx * t + dz * depth);
                for (int y = minY; y <= maxY; y++) {
                    if (level.getBlockState(pos.set(x, y, z)).isAir()) {
                        level.setBlock(pos, seal, 2);
                        minX = Math.min(minX, x);
                        maxX = Math.max(maxX, x);
                        minZ = Math.min(minZ, z);
                        maxZ = Math.max(maxZ, z);
                    }
                }
            }
        }
        dungeon.sealed = minX <= maxX;
        dungeon.sealMin = new BlockPos(minX, minY, minZ);
        dungeon.sealMax = new BlockPos(maxX, maxY, maxZ);
    }

    /** Removes the seal blocks: the boss arena is open. */
    static void breakSeal(ServerLevel level, DungeonData.Dungeon dungeon) {
        for (BlockPos pos : BlockPos.betweenClosed(dungeon.sealMin, dungeon.sealMax)) {
            if (level.getBlockState(pos).is(ModBlocks.DUNGEON_SEAL.get())) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
        }
        dungeon.sealed = false;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Monsters and chests
    // ------------------------------------------------------------------------------------------------------------

    /** A free spot on the ground of {@code zone} (not inside a house wall or a tree), or null. */
    private static BlockPos standingSpot(ServerLevel level, Zone zone, RandomSource random) {
        for (int attempt = 0; attempt < 12; attempt++) {
            BlockPos column = DungeonScenes.ground(zone.center(), zone.radius() - EDGE, random);
            for (int y = 6; y >= 0; y--) {
                BlockPos feet = column.above(y);
                if (level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir()
                        && level.getBlockState(feet.below()).isSolid()) {
                    return feet;
                }
            }
        }
        return null;
    }

    private static DungeonMob spawnMob(ServerLevel level, DungeonData.Dungeon dungeon, EntityType<? extends DungeonMob> type,
                                       Zone zone, RandomSource random, boolean required) {
        BlockPos spot = standingSpot(level, zone, random);
        DungeonMob mob = spot == null ? null : type.create(level);
        if (mob == null) {
            return null;
        }
        mob.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, random.nextFloat() * 360, 0);
        mob.setDungeonId(dungeon.id);
        mob.setRequired(required);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.STRUCTURE, null);
        MobLevels.setLevel(mob, mobLevel(dungeon.stars, random), false);
        level.addFreshEntity(mob);
        return mob;
    }

    private static void spawnBoss(ServerLevel level, DungeonData.Dungeon dungeon, DungeonTheme theme, BlockPos center, RandomSource random) {
        DungeonMob boss = theme.boss.get().create(level);
        if (boss == null) {
            return;
        }
        boss.moveTo(center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5, 270, 0);
        boss.setDungeonId(dungeon.id);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(center), MobSpawnType.STRUCTURE, null);
        MobLevels.setLevel(boss, mobLevel(dungeon.stars, random) + 3, false);
        boss.makeGateBoss(dungeon.stars);
        level.addFreshEntity(boss);
    }

    static void chest(ServerLevel level, BlockPos pos, List<ItemStack> loot) {
        level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH), 2);
        if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
            for (int i = 0; i < loot.size() && i < chest.getContainerSize(); i++) {
                chest.setItem(i * 2 % chest.getContainerSize(), loot.get(i));
            }
        }
    }
}
