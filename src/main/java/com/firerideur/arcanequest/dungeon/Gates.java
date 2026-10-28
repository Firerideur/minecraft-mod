package com.firerideur.arcanequest.dungeon;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.registry.ModEntities;
import com.firerideur.arcanequest.stats.StatsEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Gates, Solo Leveling style: they open near players from time to time, ranked from 1 to 7 stars
 * (the higher, the rarer and the more dangerous). Inside is a dungeon in its own dimension;
 * defeating its boss clears it, gives a lot of experience and a treasure chest.
 */
public final class Gates {
    public static final ResourceKey<Level> DUNGEON = ResourceKey.create(Registries.DIMENSION, ArcaneQuest.id("dungeon"));
    public static final int MAX_STARS = 7;
    /** Chance of each rank, in percent: 1 star is common, 7 stars very rare. */
    private static final float[] RANK_WEIGHTS = {35, 25, 17, 11, 7, 3.5F, 1.5F};
    /** Every 5 minutes, a 40% chance that a gate opens near each player in the Overworld. */
    private static final int SPAWN_INTERVAL = 6000;
    private static final float SPAWN_CHANCE = 0.4F;
    private static final String RETURN_TAG = "arcanequest_gate_return";
    /** When this many monsters are left, they glow through walls so the last ones can be found. */
    private static final int REVEAL_BELOW = 3;

    /** The objective bar of each dungeon someone is in (rebuilt as needed, not saved). */
    private static final Map<Integer, ServerBossEvent> OBJECTIVES = new HashMap<>();
    /** Players who already saw the boss's title, per dungeon. */
    private static final Map<Integer, Set<UUID>> MET_BOSS = new HashMap<>();

    private Gates() {}

    public static int randomStars(RandomSource random) {
        float total = 0;
        for (float w : RANK_WEIGHTS) {
            total += w;
        }
        float roll = random.nextFloat() * total;
        for (int i = 0; i < RANK_WEIGHTS.length; i++) {
            roll -= RANK_WEIGHTS[i];
            if (roll < 0) {
                return i + 1;
            }
        }
        return 1;
    }

    /** Monster level to expect behind a gate of this rank. */
    public static int recommendedLevel(int stars) {
        return stars * 12 - 5;
    }

    /** "★★★☆☆☆☆", colored like the gate. */
    public static MutableComponent stars(int stars) {
        return Component.literal("★".repeat(stars)).withColor(GateEntity.color(stars))
                .append(Component.literal("☆".repeat(MAX_STARS - stars)).withStyle(ChatFormatting.DARK_GRAY));
    }

    static Component title(int stars, boolean exit) {
        if (exit) {
            return Component.translatable("entity.arcanequest.gate.exit").withStyle(ChatFormatting.AQUA);
        }
        return Component.translatable("entity.arcanequest.gate.rank").withStyle(ChatFormatting.WHITE).append(" ").append(stars(stars));
    }

    /** Opens a gate of rank {@code stars} at {@code pos}, with a fresh dungeon behind it. */
    public static GateEntity open(ServerLevel level, Vec3 pos, int stars) {
        DungeonData.Dungeon dungeon = DungeonData.get(level.getServer()).create(stars, DungeonTheme.random(stars, level.random));
        GateEntity gate = new GateEntity(ModEntities.GATE.get(), level);
        gate.setup(dungeon.id, stars, false);
        gate.moveTo(pos.x, pos.y, pos.z, 0, 0);
        level.addFreshEntity(gate);
        level.playSound(null, gate.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.AMBIENT, 1.0F, 0.6F + stars * 0.1F);
        return gate;
    }

    public static boolean isSealed(ServerLevel level, int dungeonId) {
        DungeonData.Dungeon dungeon = DungeonData.get(level.getServer()).get(dungeonId);
        return dungeon != null && dungeon.sealed;
    }

    public static boolean isCleared(ServerLevel level, int dungeonId) {
        DungeonData.Dungeon dungeon = DungeonData.get(level.getServer()).get(dungeonId);
        return dungeon != null && dungeon.cleared;
    }

    /** Takes {@code player} into the dungeon behind {@code gate}, building it on the first visit. */
    static void enter(GateEntity gate, ServerPlayer player) {
        ServerLevel dungeonLevel = player.server.getLevel(DUNGEON);
        DungeonData data = DungeonData.get(player.server);
        DungeonData.Dungeon dungeon = data.get(gate.getDungeonId());
        player.setPortalCooldown(60);
        if (dungeonLevel == null || dungeon == null) {
            return;
        }
        if (dungeon.cleared) {
            player.displayClientMessage(Component.translatable("message.arcanequest.gate_cleared").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (!dungeon.generated) {
            DungeonGenerator.generate(dungeonLevel, dungeon, player.getRandom());
            data.setDirty();
        }
        CompoundTag back = new CompoundTag();
        back.putString("Dimension", player.level().dimension().location().toString());
        back.putDouble("X", player.getX());
        back.putDouble("Y", player.getY());
        back.putDouble("Z", player.getZ() + 2.0);
        persisted(player).put(RETURN_TAG, back);
        BlockPos entry = dungeon.entry;
        player.teleportTo(dungeonLevel, entry.getX() + 0.5, entry.getY(), entry.getZ() + 0.5, -90.0F, 0.0F);
        player.setPortalCooldown(60);
        dungeonLevel.playSound(null, entry, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.3F, 1.2F);
        DungeonTheme theme = dungeon.theme();
        player.sendSystemMessage(Component.empty().append(stars(dungeon.stars)).append(" ")
                .append(theme.displayName().copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
        player.sendSystemMessage(Component.translatable("message.arcanequest.gate_entered", recommendedLevel(dungeon.stars))
                .withStyle(ChatFormatting.GRAY));
        title(player, theme.displayName().copy().withStyle(ChatFormatting.GOLD),
                Component.translatable("dungeon.arcanequest.enter_subtitle", stars(dungeon.stars)));
    }

    /** A big title in the middle of the screen, like the system messages of Solo Leveling. */
    private static void title(ServerPlayer player, Component title, Component subtitle) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    /** A monster of the dungeon died: when the last one falls, the seal of the boss arena breaks. */
    static void onMonsterKilled(ServerLevel level, int dungeonId) {
        DungeonData data = DungeonData.get(level.getServer());
        DungeonData.Dungeon dungeon = data.get(dungeonId);
        if (dungeon == null || dungeon.cleared) {
            return;
        }
        dungeon.killed++;
        data.setDirty();
        List<ServerPlayer> inside = playersIn(level, dungeon);
        int left = dungeon.required - dungeon.killed;
        if (left == REVEAL_BELOW) {
            for (ServerPlayer player : inside) {
                player.displayClientMessage(Component.translatable("message.arcanequest.monsters_left", left)
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
        }
        if (left <= 0 && dungeon.sealed) {
            DungeonGenerator.breakSeal(level, dungeon);
            Vec3 middle = Vec3.atCenterOf(dungeon.sealMin).add(Vec3.atCenterOf(dungeon.sealMax)).scale(0.5);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, middle.x, middle.y, middle.z, 200, 3, 4, 3, 0.1);
            level.playSound(null, BlockPos.containing(middle), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 0.7F);
            for (ServerPlayer player : inside) {
                player.sendSystemMessage(Component.translatable("message.arcanequest.seal_broken")
                        .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
                level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.6F);
            }
        }
    }

    private static AABB area(DungeonData.Dungeon dungeon) {
        return new AABB(dungeon.origin()).inflate(DungeonData.SPACING / 2.0, 64, DungeonData.SPACING / 2.0);
    }

    private static List<ServerPlayer> playersIn(ServerLevel level, DungeonData.Dungeon dungeon) {
        return level.getEntitiesOfClass(ServerPlayer.class, area(dungeon));
    }

    /**
     * Twice a second in the dungeon dimension: the objective bar of each player's dungeon, the boss's title when a
     * player steps into the arena, and the last monsters revealed.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 10 != 0) {
            return;
        }
        ServerLevel level = event.getServer().getLevel(DUNGEON);
        DungeonData data = DungeonData.get(event.getServer());
        Map<ServerPlayer, Integer> where = new HashMap<>();
        if (level != null) {
            for (ServerPlayer player : level.players()) {
                DungeonData.Dungeon dungeon = data.at(player.getX());
                if (dungeon != null) {
                    where.put(player, dungeon.id);
                    ServerBossEvent bar = OBJECTIVES.computeIfAbsent(dungeon.id, id -> new ServerBossEvent(Component.empty(),
                            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10));
                    updateObjective(bar, dungeon);
                    bar.addPlayer(player);
                    meetBoss(level, player, dungeon);
                }
            }
            if (event.getServer().getTickCount() % 100 == 0) {
                revealLastMonsters(level, data, where);
            }
        }
        // Players who left a dungeon lose its bar
        OBJECTIVES.entrySet().removeIf(entry -> {
            for (ServerPlayer player : List.copyOf(entry.getValue().getPlayers())) {
                if (!entry.getKey().equals(where.get(player))) {
                    entry.getValue().removePlayer(player);
                }
            }
            return entry.getValue().getPlayers().isEmpty();
        });
    }

    private static void updateObjective(ServerBossEvent bar, DungeonData.Dungeon dungeon) {
        if (dungeon.cleared) {
            bar.setName(Component.translatable("dungeon.arcanequest.objective_done").withStyle(ChatFormatting.GREEN));
            bar.setColor(BossEvent.BossBarColor.GREEN);
            bar.setProgress(1.0F);
        } else if (dungeon.sealed) {
            bar.setName(Component.translatable("dungeon.arcanequest.objective", Math.min(dungeon.killed, dungeon.required), dungeon.required));
            bar.setColor(BossEvent.BossBarColor.PURPLE);
            bar.setProgress(dungeon.required == 0 ? 1.0F : Mth.clamp(dungeon.killed / (float) dungeon.required, 0, 1));
        } else {
            bar.setName(Component.translatable("dungeon.arcanequest.objective_boss").withStyle(ChatFormatting.RED));
            bar.setColor(BossEvent.BossBarColor.RED);
            bar.setProgress(1.0F);
        }
    }

    /** The boss's name as a title the first time a player comes into its arena. */
    private static void meetBoss(ServerLevel level, ServerPlayer player, DungeonData.Dungeon dungeon) {
        if (dungeon.cleared || dungeon.sealed || dungeon.arenaRadius == 0
                || player.blockPosition().distSqr(dungeon.bossRoom) > (double) dungeon.arenaRadius * dungeon.arenaRadius
                || !MET_BOSS.computeIfAbsent(dungeon.id, id -> new HashSet<>()).add(player.getUUID())) {
            return;
        }
        level.getEntitiesOfClass(DungeonMob.class, new AABB(dungeon.bossRoom).inflate(dungeon.arenaRadius + 8))
                .stream().filter(DungeonMob::isGateBoss).findFirst().ifPresent(boss -> {
                    title(player, boss.getDisplayName(), Component.translatable("dungeon.arcanequest.boss_subtitle", stars(dungeon.stars)));
                    level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 0.8F, 0.8F);
                });
    }

    /** When only a few monsters are left, they glow so players can find them. */
    private static void revealLastMonsters(ServerLevel level, DungeonData data, Map<ServerPlayer, Integer> where) {
        for (int id : Set.copyOf(where.values())) {
            DungeonData.Dungeon dungeon = data.get(id);
            if (dungeon == null || !dungeon.sealed || dungeon.required - dungeon.killed > REVEAL_BELOW) {
                continue;
            }
            for (DungeonMob mob : level.getEntitiesOfClass(DungeonMob.class, area(dungeon), DungeonMob::isRequired)) {
                mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, false, false));
            }
        }
    }

    /** Back to where the player entered (or the world spawn). */
    static void exit(ServerPlayer player) {
        player.setPortalCooldown(60);
        CompoundTag back = persisted(player).getCompound(RETURN_TAG);
        ServerLevel level = null;
        if (back.contains("Dimension")) {
            ResourceLocation id = ResourceLocation.tryParse(back.getString("Dimension"));
            level = id == null ? null : player.server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
        }
        if (level == null) {
            level = player.server.overworld();
            BlockPos spawn = level.getSharedSpawnPos();
            player.teleportTo(level, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, player.getYRot(), 0.0F);
        } else {
            player.teleportTo(level, back.getDouble("X"), back.getDouble("Y"), back.getDouble("Z"), player.getYRot(), 0.0F);
        }
        player.setPortalCooldown(60);
        persisted(player).remove(RETURN_TAG);
    }

    /** The boss fell: the dungeon is cleared, everyone inside is rewarded, a chest and a way out appear. */
    static void onBossDefeated(ServerLevel level, int dungeonId, BlockPos pos) {
        DungeonData data = DungeonData.get(level.getServer());
        DungeonData.Dungeon dungeon = data.get(dungeonId);
        if (dungeon == null || dungeon.cleared) {
            return;
        }
        dungeon.cleared = true;
        data.setDirty();
        long xp = Rewards.clearXp(dungeon.stars);
        if (dungeon.sealed) {
            DungeonGenerator.breakSeal(level, dungeon);
        }
        MET_BOSS.remove(dungeonId);
        for (ServerPlayer player : playersIn(level, dungeon)) {
            player.sendSystemMessage(Component.translatable("message.arcanequest.gate_clear", stars(dungeon.stars), xp)
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            StatsEvents.addXp(player, xp);
            level.playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
            title(player, Component.translatable("dungeon.arcanequest.cleared_title").withStyle(ChatFormatting.GOLD),
                    Component.translatable("dungeon.arcanequest.cleared_subtitle", xp));
        }
        BlockPos room = dungeon.bossRoom;
        DungeonGenerator.chest(level, room.offset(0, 1, 0), Rewards.bossLoot(dungeon.stars, level.random));
        GateEntity exit = new GateEntity(ModEntities.GATE.get(), level);
        exit.setup(dungeonId, dungeon.stars, true);
        exit.moveTo(room.getX() + 0.5, room.getY() + 1, room.getZ() + 4.5, 0, 0);
        level.addFreshEntity(exit);
    }

    /** Where the return position is kept (lost on death, which is fine: dying sends you home anyway). */
    private static CompoundTag persisted(Player player) {
        return player.getPersistentData();
    }

    /** Gates open by themselves near players in the Overworld. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player instanceof ServerPlayer player) || player.tickCount % SPAWN_INTERVAL != 0
                || player.level().dimension() != Level.OVERWORLD || player.getRandom().nextFloat() >= SPAWN_CHANCE) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (!level.getEntitiesOfClass(GateEntity.class, player.getBoundingBox().inflate(96.0)).isEmpty()) {
            return;
        }
        Vec3 pos = spotNear(level, player);
        if (pos == null) {
            return;
        }
        int stars = randomStars(player.getRandom());
        open(level, pos, stars);
        double dx = pos.x - player.getX(), dz = pos.z - player.getZ();
        player.sendSystemMessage(Component.translatable("message.arcanequest.gate_opened", stars(stars),
                        (int) Math.sqrt(dx * dx + dz * dz), direction(dx, dz), recommendedLevel(stars))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** A spot on the surface 20 to 40 blocks away from the player. */
    @Nullable
    static Vec3 spotNear(ServerLevel level, Player player) {
        RandomSource random = player.getRandom();
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = 20 + random.nextDouble() * 20;
            int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (level.getBlockState(new BlockPos(x, y - 1, z)).getFluidState().isEmpty()) {
                return new Vec3(x + 0.5, y, z + 0.5);
            }
        }
        return null;
    }

    public static Component direction(double dx, double dz) {
        String[] keys = {"s", "sw", "w", "nw", "n", "ne", "e", "se"};
        double angle = Math.toDegrees(Math.atan2(-dx, dz));
        int index = Math.floorMod(Math.round(angle / 45.0), 8);
        return Component.translatable("hud.arcanequest.compass." + keys[(int) index]);
    }
}
