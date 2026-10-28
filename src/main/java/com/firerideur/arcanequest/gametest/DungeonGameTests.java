package com.firerideur.arcanequest.gametest;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.dungeon.DungeonData;
import com.firerideur.arcanequest.dungeon.DungeonGenerator;
import com.firerideur.arcanequest.dungeon.BeastMobs;
import com.firerideur.arcanequest.dungeon.DungeonMob;
import com.firerideur.arcanequest.dungeon.DungeonTheme;
import com.firerideur.arcanequest.dungeon.FantasyMobs;
import com.firerideur.arcanequest.dungeon.GateEntity;
import com.firerideur.arcanequest.dungeon.Gates;
import com.firerideur.arcanequest.mobs.MobLevels;
import com.firerideur.arcanequest.registry.ModBlocks;
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
        DungeonData.Dungeon dungeon = data.create(3, DungeonTheme.ICE_FORTRESS);
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

    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void sealBreaksWhenEveryMonsterIsDead(GameTestHelper helper) {
        ServerLevel dimension = helper.getLevel().getServer().getLevel(Gates.DUNGEON);
        ServerLevel dungeonLevel = dimension != null ? dimension : helper.getLevel();
        DungeonData.Dungeon dungeon = DungeonData.get(helper.getLevel().getServer()).create(2, DungeonTheme.WOLF_DEN);
        DungeonGenerator.generate(dungeonLevel, dungeon, RandomSource.create(11));
        helper.assertTrue(dungeon.sealed, "the boss arena should start sealed");
        helper.assertTrue(BlockPos.betweenClosedStream(dungeon.sealMin, dungeon.sealMax)
                .anyMatch(pos -> dungeonLevel.getBlockState(pos).is(ModBlocks.DUNGEON_SEAL.get())), "seal blocks expected");

        AABB area = new AABB(dungeon.origin()).inflate(DungeonData.SPACING / 2.0, 64, DungeonData.SPACING / 2.0);
        List<DungeonMob> required = dungeonLevel.getEntitiesOfClass(DungeonMob.class, area, DungeonMob::isRequired);
        helper.assertTrue(required.size() == dungeon.required && dungeon.required > 0,
                "every required monster should be counted: " + required.size() + " vs " + dungeon.required);
        helper.assertTrue(required.stream().noneMatch(DungeonMob::isGateBoss), "the boss isn't part of the objective");
        // All but one: still sealed
        for (int i = 1; i < required.size(); i++) {
            required.get(i).kill();
        }
        helper.assertTrue(dungeon.sealed, "one monster left: the seal should hold");
        required.get(0).kill();
        helper.runAfterDelay(2, () -> {
            helper.assertFalse(dungeon.sealed, "the seal should break when the last monster dies");
            helper.assertTrue(BlockPos.betweenClosedStream(dungeon.sealMin, dungeon.sealMax)
                    .noneMatch(pos -> dungeonLevel.getBlockState(pos).is(ModBlocks.DUNGEON_SEAL.get())), "no seal block should remain");
            dungeonLevel.getEntitiesOfClass(DungeonMob.class, area).forEach(DungeonMob::discard);
            dungeonLevel.getEntitiesOfClass(GateEntity.class, area).forEach(GateEntity::discard);
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void everyDungeonMobSpawns(GameTestHelper helper) {
        List<EntityType<? extends DungeonMob>> types = List.of(ModEntities.GOBLIN.get(), ModEntities.GOBLIN_SHAMAN.get(),
                ModEntities.STEELFANG_WOLF.get(), ModEntities.ICE_ELF.get(), ModEntities.ANT_SOLDIER.get(),
                ModEntities.HIGH_ORC.get(), ModEntities.DEMON_KNIGHT.get(), ModEntities.TROLL.get(), ModEntities.MINOTAUR.get(),
                ModEntities.GARGOYLE.get(), ModEntities.LICH.get(), ModEntities.NAGA.get(), ModEntities.MIMIC.get(),
                ModEntities.TREANT.get(), ModEntities.BASILISK.get(), ModEntities.GIANT_SPIDER.get(), ModEntities.MANTIS.get(),
                ModEntities.BEETLE.get(), ModEntities.GIANT_WASP.get(), ModEntities.WEREWOLF.get(), ModEntities.RATMAN.get(),
                ModEntities.LIZARDMAN.get(), ModEntities.GHOUL.get(), ModEntities.IMP.get(), ModEntities.SUCCUBUS.get(),
                ModEntities.GREATER_DEMON.get());
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

    @GameTest(template = EMPTY)
    public static void everyRankHasThemesWithABoss(GameTestHelper helper) {
        for (int stars = 1; stars <= Gates.MAX_STARS; stars++) {
            helper.assertTrue(!DungeonTheme.forStars(stars).isEmpty(), "no theme for " + stars + " stars");
        }
        for (int stars = 2; stars <= Gates.MAX_STARS; stars++) {
            helper.assertTrue(DungeonTheme.forStars(stars).size() >= 2, stars + " stars should have several themes");
        }
        for (DungeonTheme theme : DungeonTheme.values()) {
            helper.assertTrue(theme.inhabitants().contains(theme.boss.get()), theme + " should list its boss");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void gargoyleSleepsUntilHit(GameTestHelper helper) {
        FantasyMobs.Gargoyle gargoyle = helper.spawn(ModEntities.GARGOYLE.get(), new BlockPos(1, 1, 1));
        helper.assertTrue(gargoyle.isDormant(), "a gargoyle starts as a statue");
        var player = helper.makeMockServerPlayerInLevel();
        float before = gargoyle.getHealth();
        gargoyle.hurt(player.damageSources().playerAttack(player), 10.0F);
        helper.assertTrue(!gargoyle.isDormant(), "hitting it wakes it up");
        helper.assertTrue(before - gargoyle.getHealth() <= 2.5F, "stone skin: a sleeping gargoyle takes 20% damage");
        gargoyle.discard();
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void trollRegenerates(GameTestHelper helper) {
        // A troll is taller than the test area: free the blocks above, or it suffocates in the ground of the test world
        for (BlockPos pos : BlockPos.betweenClosed(0, 3, 0, 2, 5, 2)) {
            helper.setBlock(pos, Blocks.AIR);
        }
        FantasyMobs.Troll troll = helper.spawn(ModEntities.TROLL.get(), new BlockPos(1, 1, 1));
        troll.setNoAi(true);
        troll.setHealth(troll.getMaxHealth() / 2);
        float hurt = troll.getHealth();
        // Entities may only start ticking a little after the test begins: wait for the heal
        helper.succeedWhen(() -> {
            helper.assertTrue(troll.getHealth() > hurt, "a troll should heal over time");
            troll.discard();
        });
    }

    @GameTest(template = EMPTY)
    public static void mimicLooksLikeAChest(GameTestHelper helper) {
        FantasyMobs.Mimic mimic = helper.spawn(ModEntities.MIMIC.get(), new BlockPos(1, 1, 1));
        helper.assertTrue(mimic.isDormant() && !mimic.isPushable(), "a mimic waits, still, like a chest");
        mimic.discard();
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void beetleShellProtectsTheFront(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        BeastMobs.Beetle front = helper.spawn(ModEntities.BEETLE.get(), new BlockPos(1, 1, 1));
        front.setNoAi(true);
        front.setYRot(0);
        // Facing south (+z): the player stands in front, then behind
        player.moveTo(front.getX(), front.getY(), front.getZ() + 2);
        float before = front.getHealth();
        front.hurt(player.damageSources().playerAttack(player), 10.0F);
        float frontLoss = before - front.getHealth();
        front.invulnerableTime = 0;
        player.moveTo(front.getX(), front.getY(), front.getZ() - 2);
        before = front.getHealth();
        front.hurt(player.damageSources().playerAttack(player), 10.0F);
        float backLoss = before - front.getHealth();
        helper.assertTrue(frontLoss < backLoss, "hits from the front should do less (" + frontLoss + " vs " + backLoss + ")");
        front.discard();
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void ghoulsAreUndead(GameTestHelper helper) {
        BeastMobs.Ghoul ghoul = helper.spawn(ModEntities.GHOUL.get(), new BlockPos(1, 1, 1));
        helper.assertTrue(ghoul.getType().is(net.minecraft.tags.EntityTypeTags.UNDEAD), "ghouls should be undead (holy damage)");
        ghoul.discard();
        helper.succeed();
    }
}
