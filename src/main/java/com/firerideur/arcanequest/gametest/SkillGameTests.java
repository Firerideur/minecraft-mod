package com.firerideur.arcanequest.gametest;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.magic.Mana;
import com.firerideur.arcanequest.magic.SpellProjectile;
import com.firerideur.arcanequest.mobs.MobLevels;
import com.firerideur.arcanequest.shadow.Corpses;
import com.firerideur.arcanequest.shadow.ShadowSoldier;
import com.firerideur.arcanequest.skill.ClassSkills;
import com.firerideur.arcanequest.skill.Skill;
import com.firerideur.arcanequest.skill.SkillRuntime;
import com.firerideur.arcanequest.stats.PlayerClass;
import com.firerideur.arcanequest.stats.PlayerStats;
import com.firerideur.arcanequest.stats.StatsCapability;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@GameTestHolder(ArcaneQuest.MODID)
@SuppressWarnings("removal") // makeMockServerPlayerInLevel is the only mock player helper in 1.21.1
public class SkillGameTests {
    private static final String EMPTY = "forge:empty3x3x3";

    @GameTest(template = EMPTY)
    public static void everyClassHasItsSkills(GameTestHelper helper) {
        for (PlayerClass playerClass : PlayerClass.values()) {
            List<Skill> skills = ClassSkills.skillsOf(playerClass);
            int expected = playerClass == PlayerClass.NONE ? 0 : playerClass.isAwakened() ? ClassSkills.SLOTS : ClassSkills.FIRST_CLASS_SKILLS;
            helper.assertTrue(skills.size() == expected, playerClass + " should have " + expected + " skills, has " + skills.size());
            helper.assertTrue(new HashSet<>(skills).size() == skills.size(), playerClass + " has the same skill twice: " + skills);
        }
        // Awakened classes keep the first skills of their first class
        helper.assertTrue(ClassSkills.skillsOf(PlayerClass.ARCHMAGE).subList(0, ClassSkills.INHERITED)
                        .equals(ClassSkills.skillsOf(PlayerClass.MAGE).subList(0, ClassSkills.INHERITED)),
                "the archmage should keep the mage skills");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void everyAwakeningHasItsOwnUltimate(GameTestHelper helper) {
        Set<Skill> ultimates = new HashSet<>();
        for (PlayerClass playerClass : PlayerClass.values()) {
            Skill ultimate = ClassSkills.ultimateOf(playerClass);
            helper.assertTrue(playerClass.isAwakened() == (ultimate != null), playerClass + ": only awakened classes have an ultimate");
            if (ultimate != null) {
                helper.assertTrue(ultimates.add(ultimate), ultimate + " is the ultimate of two classes");
            }
        }
        // An ultimate belongs to its class only, and sits in the level 100 slot
        for (PlayerClass playerClass : PlayerClass.values()) {
            List<Skill> skills = ClassSkills.skillsOf(playerClass);
            for (int slot = 0; slot < skills.size(); slot++) {
                helper.assertTrue(slot == ClassSkills.ULTIMATE_SLOT || !ultimates.contains(skills.get(slot)),
                        playerClass + " uses the ultimate " + skills.get(slot) + " as a normal skill");
            }
        }
        helper.assertTrue(ClassSkills.UNLOCK_LEVELS[ClassSkills.ULTIMATE_SLOT] == PlayerStats.MAX_LEVEL, "ultimates unlock at max level");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void everyClassAndSkillIsTranslated(GameTestHelper helper) {
        for (String lang : List.of("fr_fr", "en_us")) {
            JsonObject texts;
            try (var in = SkillGameTests.class.getResourceAsStream("/assets/arcanequest/lang/" + lang + ".json")) {
                helper.assertTrue(in != null, lang + " not found");
                texts = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
            for (PlayerClass playerClass : PlayerClass.values()) {
                for (String suffix : playerClass == PlayerClass.NONE ? List.of("", ".desc") : List.of("", ".desc", ".lore")) {
                    String key = "class.arcanequest." + playerClass.key() + suffix;
                    helper.assertTrue(texts.has(key), lang + " misses " + key);
                }
            }
            for (Skill skill : Skill.values()) {
                for (String suffix : List.of("", ".desc")) {
                    String key = "skill.arcanequest." + skill.key() + suffix;
                    helper.assertTrue(texts.has(key), lang + " misses " + key);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void physicalSkillsFitWithoutIntelligence(GameTestHelper helper) {
        // Without Intelligence a player has the base mana: physical skills must stay usable
        for (Skill skill : Skill.values()) {
            helper.assertTrue(skill.isMagic() || skill.manaCost() <= Mana.BASE_MANA, skill + " costs more than the base mana");
        }
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
        helper.assertTrue(ClassSkills.unlocked(stats, 4) == Skill.ARCANE_BARRAGE, "slot 5 open at 75");
        helper.assertTrue(ClassSkills.unlocked(stats, ClassSkills.ULTIMATE_SLOT) == null, "the ultimate waits for level 100");
        stats.setLevel(100);
        helper.assertTrue(ClassSkills.unlocked(stats, ClassSkills.ULTIMATE_SLOT) == Skill.INFERNO, "the archmage ultimate opens at 100");
        // A first class has nothing in the ultimate slot
        stats.setPlayerClass(PlayerClass.MAGE);
        helper.assertTrue(ClassSkills.unlocked(stats, ClassSkills.ULTIMATE_SLOT) == null, "first classes have no ultimate");
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

    @GameTest(template = EMPTY)
    public static void ariseExtractsTheCorpses(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        PlayerStats stats = StatsCapability.get(player).orElseThrow();
        stats.setLevel(40);
        stats.setPlayerClass(PlayerClass.SHADOW_MONARCH);
        stats.setMana(100);
        int slot = ClassSkills.skillsOf(PlayerClass.SHADOW_MONARCH).indexOf(Skill.ARISE);
        helper.assertTrue(!SkillRuntime.use(player, slot), "no corpse: Arise should fail");
        helper.assertTrue(stats.getMana() == 100, "a failed Arise costs nothing");

        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        player.moveTo(zombie.getX() + 1, zombie.getY(), zombie.getZ());
        MobLevels.setLevel(zombie, 1, false);
        Corpses.add(player, zombie);
        zombie.discard();
        helper.assertTrue(SkillRuntime.use(player, slot), "Arise should extract the zombie");
        List<ShadowSoldier> army = ShadowSoldier.armyOf(player);
        // A zombie is weak: the Monarch never fails to extract it
        helper.assertTrue(army.size() == 1, "one shadow should rise, got " + army.size());
        helper.assertTrue(army.get(0).getRank() == ShadowSoldier.Rank.SOLDIER, "a zombie gives a soldier");
        helper.assertTrue(Corpses.count(player, 16) == 0, "the corpse is used");
        army.forEach(ShadowSoldier::discard);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void shadowRanksFollowTheMonster(GameTestHelper helper) {
        helper.assertTrue(ShadowSoldier.Rank.of(20, false) == ShadowSoldier.Rank.SOLDIER, "20 health: soldier");
        helper.assertTrue(ShadowSoldier.Rank.of(60, false) == ShadowSoldier.Rank.KNIGHT, "60 health: knight");
        helper.assertTrue(ShadowSoldier.Rank.of(200, true) == ShadowSoldier.Rank.COMMANDER, "boss: commander");
        helper.assertTrue(ShadowSoldier.Rank.of(500, true) == ShadowSoldier.Rank.MARSHAL, "strong boss: marshal");
        ShadowSoldier.Rank previous = null;
        for (ShadowSoldier.Rank rank : ShadowSoldier.Rank.values()) {
            helper.assertTrue(previous == null || rank.power() > previous.power(), "higher ranks must be stronger");
            previous = rank;
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void dragonsFearTerrifies(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        PlayerStats stats = StatsCapability.get(player).orElseThrow();
        stats.setLevel(75);
        stats.setPlayerClass(PlayerClass.SHADOW_MONARCH);
        stats.setMana(200);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        player.moveTo(zombie.getX() + 2, zombie.getY(), zombie.getZ());
        zombie.setTarget(player);
        helper.assertTrue(SkillRuntime.use(player, ClassSkills.skillsOf(PlayerClass.SHADOW_MONARCH).indexOf(Skill.DRAGONS_FEAR)),
                "Dragon's Fear should be used");
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(zombie.hasEffect(MobEffects.WEAKNESS) && zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                    "the zombie should be weakened and slowed");
            helper.assertTrue(zombie.getTarget() == null, "a terrified zombie forgets its target");
            zombie.discard();
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, timeoutTicks = 700)
    public static void goliathIsTemporary(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        PlayerStats stats = StatsCapability.get(player).orElseThrow();
        stats.setLevel(PlayerStats.MAX_LEVEL);
        stats.setPlayerClass(PlayerClass.FORTRESS);
        stats.setMana(100);
        // The giant must not suffocate in the ground above the test area
        for (BlockPos pos : BlockPos.betweenClosed(0, 3, 0, 2, 5, 2)) {
            helper.setBlock(pos, Blocks.AIR);
        }
        player.moveTo(helper.absoluteVec(new Vec3(1.5, 1, 1.5)));
        double scale = player.getAttributeValue(Attributes.SCALE);
        double health = player.getAttributeValue(Attributes.MAX_HEALTH);
        helper.assertTrue(SkillRuntime.use(player, ClassSkills.ULTIMATE_SLOT), "Goliath should be used");
        helper.assertTrue(player.getAttributeValue(Attributes.SCALE) > scale + 0.5, "the player should grow");
        helper.assertTrue(player.getAttributeValue(Attributes.MAX_HEALTH) == health + 20, "Goliath gives 10 hearts");
        // Level 100: power 2.6, the duration is capped at twice the base (300 ticks)
        helper.runAfterDelay(610, () -> {
            helper.assertTrue(player.getAttributeValue(Attributes.SCALE) == scale, "the player should shrink back");
            helper.assertTrue(player.getAttributeValue(Attributes.MAX_HEALTH) == health, "the bonus health should be gone");
            helper.succeed();
        });
    }
}
