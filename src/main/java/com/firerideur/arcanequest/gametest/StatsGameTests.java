package com.firerideur.arcanequest.gametest;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.stats.PlayerClass;
import com.firerideur.arcanequest.stats.PlayerStats;
import com.firerideur.arcanequest.stats.Stat;
import com.firerideur.arcanequest.stats.StatEffects;
import com.firerideur.arcanequest.stats.StatsCapability;
import com.firerideur.arcanequest.stats.StatsEvents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.gametest.GameTestHolder;

/** Run with {@code ./gradlew runGameTestServer}. */
@GameTestHolder(ArcaneQuest.MODID)
@SuppressWarnings("removal") // makeMockServerPlayerInLevel is the only mock player helper in 1.21.1
public class StatsGameTests {
    private static final String EMPTY = "forge:empty3x3x3";

    @GameTest(template = EMPTY)
    public static void levelUpGivesPoints(GameTestHelper helper) {
        PlayerStats stats = new PlayerStats();
        int gained = stats.addXp(PlayerStats.xpForNextLevel(1) + PlayerStats.xpForNextLevel(2) + 5);
        helper.assertTrue(gained == 2, "expected 2 levels, got " + gained);
        helper.assertTrue(stats.getLevel() == 3, "expected level 3, got " + stats.getLevel());
        helper.assertTrue(stats.getXp() == 5, "expected 5 leftover xp, got " + stats.getXp());
        helper.assertTrue(stats.getPoints() == 2 * PlayerStats.POINTS_PER_LEVEL, "wrong point count");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void spendAndResetPoints(GameTestHelper helper) {
        PlayerStats stats = new PlayerStats();
        helper.assertTrue(!stats.spendPoint(Stat.STRENGTH), "level 1 player should have no points");
        stats.setLevel(5);
        helper.assertTrue(stats.getPoints() == 4 * PlayerStats.POINTS_PER_LEVEL, "setLevel should grant points");
        stats.spendPoint(Stat.VITALITY);
        stats.spendPoint(Stat.VITALITY);
        helper.assertTrue(stats.get(Stat.VITALITY) == 2, "vitality should be 2");

        PlayerStats copy = new PlayerStats();
        copy.load(stats.save());
        helper.assertTrue(copy.get(Stat.VITALITY) == 2 && copy.getLevel() == 5, "save/load lost data");

        stats.resetStats();
        helper.assertTrue(stats.get(Stat.VITALITY) == 0, "reset should clear stats");
        helper.assertTrue(stats.getPoints() == 4 * PlayerStats.POINTS_PER_LEVEL, "reset should refund points");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void maxLevelIsCapped(GameTestHelper helper) {
        PlayerStats stats = new PlayerStats();
        stats.addXp(Long.MAX_VALUE / 2);
        helper.assertTrue(stats.getLevel() == PlayerStats.MAX_LEVEL, "should stop at max level");
        helper.assertTrue(stats.addXp(1000) == 0, "no levels past max");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void playerHasStatsAndBonuses(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        PlayerStats stats = StatsCapability.get(player).orElse(null);
        helper.assertTrue(stats != null, "player should have the stats capability");

        double baseHealth = player.getAttributeValue(Attributes.MAX_HEALTH);
        double baseDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);

        StatsEvents.addXp(player, PlayerStats.xpForNextLevel(1));
        helper.assertTrue(stats.getLevel() == 2, "player should be level 2");
        stats.spendPoint(Stat.VITALITY);
        stats.spendPoint(Stat.STRENGTH);
        stats.spendPoint(Stat.STRENGTH);
        StatEffects.apply(player, stats);

        double health = player.getAttributeValue(Attributes.MAX_HEALTH);
        double damage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        helper.assertTrue(health == baseHealth + StatEffects.HEALTH_PER_VITALITY, "max health bonus not applied: " + health);
        helper.assertTrue(damage == baseDamage + 2 * StatEffects.DAMAGE_PER_STRENGTH, "damage bonus not applied: " + damage);

        // Bonuses must survive a save/load of the player
        CompoundTag saved = player.saveWithoutId(new CompoundTag());
        ServerPlayer reloaded = helper.makeMockServerPlayerInLevel();
        reloaded.load(saved);
        PlayerStats reloadedStats = StatsCapability.get(reloaded).orElseThrow();
        helper.assertTrue(reloadedStats.getLevel() == 2 && reloadedStats.get(Stat.STRENGTH) == 2, "stats not saved with player");
        helper.assertTrue(reloaded.getAttributeValue(Attributes.MAX_HEALTH) == health, "health bonus not saved with player");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void commandsWork(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        var server = helper.getLevel().getServer();
        var dispatcher = server.getCommands().getDispatcher();
        helper.assertTrue(dispatcher.getRoot().getChild("arcanequest") != null, "arcanequest command not registered");
        helper.assertTrue(dispatcher.getRoot().getChild("aq") != null, "aq alias not registered");
        var source = server.createCommandSourceStack().withEntity(player).withPermission(4).withSuppressedOutput();
        server.getCommands().performPrefixedCommand(source, "/aq level set 10");
        helper.assertTrue(StatsCapability.get(player).orElseThrow().getLevel() == 10, "/aq level set should work");
        server.getCommands().performPrefixedCommand(source, "/arcanequest class mage");
        helper.assertTrue(StatsCapability.get(player).orElseThrow().getPlayerClass() == PlayerClass.MAGE, "/arcanequest class should work");
        helper.succeed();
    }
}
