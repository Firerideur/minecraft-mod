package com.firerideur.arcanequest.gametest;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.dungeon.DungeonData;
import com.firerideur.arcanequest.dungeon.DungeonGenerator;
import com.firerideur.arcanequest.dungeon.DungeonMob;
import com.firerideur.arcanequest.dungeon.GateEntity;
import com.firerideur.arcanequest.dungeon.Gates;
import com.firerideur.arcanequest.mobs.MobLevels;
import com.firerideur.arcanequest.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;

import java.util.List;

@GameTestHolder(ArcaneQuest.MODID)
public class DungeonGameTests {
    private static final String EMPTY = "forge:empty3x3x3";

    @GameTest(template = EMPTY)
    public static void gateRanksAreRarerWithStars(GameTestHelper helper) {
        RandomSource random = RandomSource.create(42);
        int[] counts = new int[Gates.MAX_STARS + 1];
        for (int i = 0; i < 20000; i++) {
            counts[Gates.randomStars(random)]++;
        }
        for (int stars = 2; stars <= Gates.MAX_STARS; stars++) {
            helper.assertTrue(counts[stars] < counts[stars - 1], stars + " stars should be rarer than " + (stars - 1));
        }
        helper.assertTrue(counts[7] > 0 && counts[7] < 20000 * 0.03, "7 stars should be very rare: " + counts[7]);
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void dungeonIsBuiltAndCleared(GameTestHelper helper) {
        // The test server only has the vanilla dimensions: build the dungeon in the test world then
        ServerLevel dimension = helper.getLevel().getServer().getLevel(Gates.DUNGEON);
        ServerLevel dungeonLevel = dimension != null ? dimension : helper.getLevel();
        DungeonData data = DungeonData.get(helper.getLevel().getServer());
        DungeonData.Dungeon dungeon = data.create(3);
        DungeonGenerator.generate(dungeonLevel, dungeon, RandomSource.create(7));
        helper.assertTrue(dungeon.generated, "dungeon should be generated");
        BlockPos entry = dungeon.entry;
        helper.assertTrue(dungeonLevel.getBlockState(entry).isAir() && dungeonLevel.getBlockState(entry.above()).isAir(),
                "the entry should be free");
        helper.assertTrue(!dungeonLevel.getBlockState(entry.below()).isAir(), "the entry should have a floor");

        AABB area = new AABB(dungeon.origin()).inflate(DungeonData.SPACING / 2.0, 32, DungeonData.SPACING / 2.0);
        List<DungeonMob> mobs = dungeonLevel.getEntitiesOfClass(DungeonMob.class, area);
        List<DungeonMob> bosses = mobs.stream().filter(DungeonMob::isGateBoss).toList();
        helper.assertTrue(bosses.size() == 1, "one gate boss expected, found " + bosses.size());
        helper.assertTrue(mobs.size() >= DungeonGenerator.roomCount(3) - 2, "monsters expected in the rooms: " + mobs.size());
        int level = MobLevels.getLevel(mobs.get(0));
        helper.assertTrue(level >= 28 && level <= 36, "3-star monsters should be around level 30, got " + level);
        helper.assertTrue(dungeonLevel.getEntitiesOfClass(GateEntity.class, area).stream().anyMatch(GateEntity::isExit),
                "the first room should have an exit");

        DungeonMob boss = bosses.get(0);
        boss.kill();
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(dungeon.cleared, "killing the boss should clear the dungeon");
            helper.assertTrue(dungeonLevel.getBlockState(dungeon.bossRoom.above()).is(Blocks.CHEST), "a treasure chest should appear");
            dungeonLevel.getEntitiesOfClass(DungeonMob.class, area).forEach(DungeonMob::discard);
            dungeonLevel.getEntitiesOfClass(GateEntity.class, area).forEach(GateEntity::discard);
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void everyDungeonMobSpawns(GameTestHelper helper) {
        List<EntityType<? extends DungeonMob>> types = List.of(ModEntities.GOBLIN.get(), ModEntities.GOBLIN_SHAMAN.get(),
                ModEntities.STEELFANG_WOLF.get(), ModEntities.ICE_ELF.get(), ModEntities.ANT_SOLDIER.get(),
                ModEntities.HIGH_ORC.get(), ModEntities.DEMON_KNIGHT.get());
        for (EntityType<? extends DungeonMob> type : types) {
            DungeonMob mob = helper.spawn(type, new BlockPos(1, 1, 1));
            helper.assertTrue(mob.isAlive() && mob.getMaxHealth() > 0, type + " should spawn");
            mob.discard();
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void gateOpensWithItsRank(GameTestHelper helper) {
        GateEntity gate = Gates.open(helper.getLevel(), Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1))), 5);
        helper.assertTrue(gate.getStars() == 5 && !gate.isExit(), "a 5-star gate should open");
        helper.assertTrue(DungeonData.get(helper.getLevel().getServer()).get(gate.getDungeonId()) != null, "it needs a dungeon");
        gate.discard();
        helper.succeed();
    }
}
