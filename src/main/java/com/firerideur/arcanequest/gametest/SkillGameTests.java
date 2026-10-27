package com.firerideur.arcanequest.gametest;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.magic.SpellProjectile;
import com.firerideur.arcanequest.skill.ClassSkills;
import com.firerideur.arcanequest.skill.Skill;
import com.firerideur.arcanequest.skill.SkillRuntime;
import com.firerideur.arcanequest.stats.PlayerClass;
import com.firerideur.arcanequest.stats.PlayerStats;
import com.firerideur.arcanequest.stats.StatsCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameType;
import net.minecraftforge.gametest.GameTestHolder;

@GameTestHolder(ArcaneQuest.MODID)
public class SkillGameTests {
    private static final String EMPTY = "forge:empty3x3x3";

    @GameTest(template = EMPTY)
    public static void everyClassHasItsSkills(GameTestHelper helper) {
        for (PlayerClass playerClass : PlayerClass.values()) {
            int count = ClassSkills.skillsOf(playerClass).size();
            int expected = playerClass == PlayerClass.NONE ? 0 : playerClass.isAwakened() ? ClassSkills.SLOTS : 3;
            helper.assertTrue(count == expected, playerClass + " should have " + expected + " skills, has " + count);
        }
        // Awakened classes keep the skills of their first class
        helper.assertTrue(ClassSkills.skillsOf(PlayerClass.ARCHMAGE).subList(0, 3).equals(ClassSkills.skillsOf(PlayerClass.MAGE)),
                "the archmage should keep the mage skills");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void skillsUnlockWithLevels(GameTestHelper helper) {
        PlayerStats stats = new PlayerStats();
        stats.setPlayerClass(PlayerClass.ARCHMAGE);
        stats.setLevel(29);
        helper.assertTrue(ClassSkills.unlocked(stats, 0) == Skill.FIREBALL, "slot 1 open at 20");
        helper.assertTrue(ClassSkills.unlocked(stats, 1) == null, "slot 2 locked before 30");
        stats.setLevel(75);
        helper.assertTrue(ClassSkills.unlocked(stats, 4) == Skill.TIME_STOP, "slot 5 open at 75");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void skillCostsManaAndCoolsDown(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        PlayerStats stats = StatsCapability.get(player).orElseThrow();
        stats.setLevel(20);
        stats.setPlayerClass(PlayerClass.MAGE);
        stats.setMana(100);
        helper.assertTrue(SkillRuntime.use(player, 0), "fireball should be cast");
        helper.assertTrue(stats.getMana() == 100 - Skill.FIREBALL.manaCost(), "mana should be spent");
        helper.assertTrue(!SkillRuntime.use(player, 0), "fireball should be on cooldown");
        helper.assertTrue(!SkillRuntime.use(player, 1), "frost nova is still locked at level 20");
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(SpellProjectile.class, player.getBoundingBox().inflate(4)).isEmpty(),
                "a fireball should fly");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void whirlwindHitsAround(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        PlayerStats stats = StatsCapability.get(player).orElseThrow();
        stats.setLevel(20);
        stats.setPlayerClass(PlayerClass.BERSERKER);
        stats.setMana(100);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        zombie.setNoAi(true);
        player.moveTo(zombie.getX() + 1.5, zombie.getY(), zombie.getZ());
        float health = zombie.getHealth();
        helper.assertTrue(SkillRuntime.use(player, 0), "whirlwind should be used");
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(zombie.getHealth() < health || !zombie.isAlive(), "the zombie should be hurt by the whirlwind");
            helper.succeed();
        });
    }
}
