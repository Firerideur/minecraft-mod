package com.firerideur.arcanequest.gametest;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.boss.AdamantiteGolem;
import com.firerideur.arcanequest.boss.CorruptedArchmage;
import com.firerideur.arcanequest.boss.SkeletonKing;
import com.firerideur.arcanequest.magic.Mana;
import com.firerideur.arcanequest.magic.Spell;
import com.firerideur.arcanequest.magic.SpellCaster;
import com.firerideur.arcanequest.magic.SpellProjectile;
import com.firerideur.arcanequest.mobs.MobLevels;
import com.firerideur.arcanequest.registry.ModEntities;
import com.firerideur.arcanequest.registry.ModItems;
import com.firerideur.arcanequest.stats.PlayerClass;
import com.firerideur.arcanequest.stats.PlayerStats;
import com.firerideur.arcanequest.stats.StatEffects;
import com.firerideur.arcanequest.stats.StatsCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.registries.ForgeRegistries;

/** Classes, magic, leveled mobs and bosses. Run with {@code ./gradlew runGameTestServer}. */
@GameTestHolder(ArcaneQuest.MODID)
@SuppressWarnings("removal") // makeMockServerPlayerInLevel is the only mock player helper in 1.21.1
public class ContentGameTests {
    private static final String EMPTY = "forge:empty3x3x3";

    @GameTest(template = EMPTY)
    public static void warriorGetsBonusHealth(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        PlayerStats stats = StatsCapability.get(player).orElseThrow();
        double base = player.getAttributeValue(Attributes.MAX_HEALTH);

        stats.setPlayerClass(PlayerClass.WARRIOR);
        StatEffects.apply(player, stats);
        helper.assertTrue(player.getAttributeValue(Attributes.MAX_HEALTH) == base + 4, "warrior should get +4 health");

        stats.setPlayerClass(PlayerClass.MAGE);
        StatEffects.apply(player, stats);
        helper.assertTrue(player.getAttributeValue(Attributes.MAX_HEALTH) == base, "warrior bonus should be removed");
        helper.assertTrue(Mana.max(player, stats) == Mana.BASE_MANA * 1.5F, "mage should have +50% mana");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void spellsCostMana(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        PlayerStats stats = StatsCapability.get(player).orElseThrow();
        stats.setMana(10);

        helper.assertTrue(SpellCaster.tryCast(player, Spell.FIRE_BOLT), "should cast with enough mana");
        helper.assertTrue(stats.getMana() == 10 - Spell.FIRE_BOLT.manaCost(), "mana should be spent");
        helper.assertTrue(!SpellCaster.tryCast(player, Spell.FIRE_BOLT), "should fail without enough mana");
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(SpellProjectile.class, player.getBoundingBox().inflate(4)).isEmpty(),
                "a bolt should have been shot");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void arcaniteArmorGivesMana(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        PlayerStats stats = StatsCapability.get(player).orElseThrow();
        helper.assertTrue(ModItems.ARCANITE_HELMET.get() instanceof ArmorItem, "arcanite helmet should be armor");
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.ARCANITE_HELMET.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.ARCANITE_BOOTS.get()));
        helper.assertTrue(Mana.arcanitePieces(player) == 2, "two arcanite pieces expected");
        helper.assertTrue(Mana.max(player, stats) == Mana.BASE_MANA + 2 * Mana.MANA_PER_ARCANITE_PIECE, "armor mana bonus");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void mobLevelsScaleStats(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        helper.assertTrue(MobLevels.getLevel(zombie) >= 1, "spawned zombie should get a level");

        MobLevels.setLevel(zombie, 11, false);
        helper.assertTrue(Math.abs(zombie.getMaxHealth() - 20 * 1.8F) < 0.01, "level 11 zombie should have 36 hp, got " + zombie.getMaxHealth());
        long normalXp = MobLevels.xpForKill(zombie);

        MobLevels.setLevel(zombie, 11, true);
        helper.assertTrue(MobLevels.isElite(zombie), "zombie should be a champion");
        helper.assertTrue(Math.abs(zombie.getMaxHealth() - 20 * 1.8F * 3) < 0.01, "champion should have x3 health");
        helper.assertTrue(zombie.hasCustomName(), "champion should be named");
        helper.assertTrue(MobLevels.xpForKill(zombie) > normalXp, "champion should give more xp");

        helper.assertTrue(MobLevels.computeLevel(helper.getLevel(), helper.getLevel().getSharedSpawnPos().atY(64),
                zombie.getRandom()) <= 3, "mobs near spawn should be low level");
        helper.assertTrue(!MobLevels.isLeveled(EntityType.WITHER.create(helper.getLevel())), "vanilla bosses keep their stats");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void dataPacksLoad(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        for (String recipe : new String[]{"mithril_sword", "adamantite_chestplate", "fire_staff", "cursed_skull",
                "mithril_ingot_from_smelting", "arcanite_helmet"}) {
            helper.assertTrue(server.getRecipeManager().byKey(ArcaneQuest.id(recipe)).isPresent(), "missing recipe " + recipe);
        }
        var lootTables = server.reloadableRegistries();
        for (String table : new String[]{"blocks/mithril_ore", "blocks/adamantite_block", "entities/skeleton_king"}) {
            var key = ResourceKey.create(Registries.LOOT_TABLE, ArcaneQuest.id(table));
            helper.assertTrue(lootTables.getLootTable(key) != LootTable.EMPTY,
                    "missing loot table " + table);
        }
        var registries = server.registryAccess();
        for (String feature : new String[]{"ore_mithril", "ore_arcanite", "ore_adamantite"}) {
            helper.assertTrue(registries.registryOrThrow(Registries.PLACED_FEATURE)
                    .containsKey(ArcaneQuest.id(feature)), "missing placed feature " + feature);
            helper.assertTrue(registries.registryOrThrow(ForgeRegistries.Keys.BIOME_MODIFIERS)
                    .containsKey(ArcaneQuest.id("add_" + feature)), "missing biome modifier " + feature);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void bossesSpawn(GameTestHelper helper) {
        SkeletonKing king = helper.spawnWithNoFreeWill(ModEntities.SKELETON_KING.get(), new BlockPos(1, 2, 1));
        CorruptedArchmage archmage = helper.spawnWithNoFreeWill(ModEntities.CORRUPTED_ARCHMAGE.get(), new BlockPos(1, 2, 1));
        AdamantiteGolem golem = helper.spawnWithNoFreeWill(ModEntities.ADAMANTITE_GOLEM.get(), new BlockPos(1, 2, 1));

        helper.assertTrue(king.getMaxHealth() == 300, "skeleton king health");
        helper.assertTrue(archmage.getMaxHealth() == 260, "archmage health");
        helper.assertTrue(golem.getMaxHealth() == 400, "golem health");
        helper.assertTrue(king.getAttributeValue(Attributes.SCALE) > 1.5, "skeleton king should be big");
        helper.assertTrue(MobLevels.getLevel(golem) == 60, "golem is level 60");
        helper.assertTrue(MobLevels.xpForKill(king) == 2500, "skeleton king xp");
        helper.assertTrue(!MobLevels.isLeveled(king), "bosses keep their fixed level");
        helper.assertTrue(king.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()
                || king.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.NETHERITE_SWORD), "king weapon");
        helper.succeed();
    }
}
