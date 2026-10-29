package com.firerideur.arcanequest.command;

import com.firerideur.arcanequest.dungeon.Gates;
import com.firerideur.arcanequest.jobs.Job;
import com.firerideur.arcanequest.network.ModNetwork;
import com.firerideur.arcanequest.stats.PlayerClass;
import com.firerideur.arcanequest.stats.PlayerStats;
import com.firerideur.arcanequest.stats.StatEffects;
import com.firerideur.arcanequest.stats.StatsCapability;
import com.firerideur.arcanequest.stats.StatsEvents;
import com.firerideur.arcanequest.worldgen.CaveDecoratorFeature;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * /arcanequest (alias /aq) — admin and testing commands.
 * <ul>
 *     <li>/aq xp add &lt;amount&gt; [player]</li>
 *     <li>/aq level set &lt;level&gt; [player]</li>
 *     <li>/aq reset [player] — refunds every stat point</li>
 *     <li>/aq class &lt;class|none&gt; [player] — sets any class (37, see PlayerClass), ignoring level, offers and hidden conditions; none lets the player pick again</li>
 *     <li>/aq info [player]</li>
 *     <li>/aq gate [stars] — opens a gate (1 to 7 stars, random if omitted) in front of you</li>
 * </ul>
 */
public final class ArcaneQuestCommand {
    private ArcaneQuestCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = dispatcher.register(Commands.literal("arcanequest")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("xp").then(Commands.literal("add")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> addXp(ctx.getSource(), ctx.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(ctx, "amount")))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> addXp(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                                IntegerArgumentType.getInteger(ctx, "amount")))))))
                .then(Commands.literal("level").then(Commands.literal("set")
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, PlayerStats.MAX_LEVEL))
                                .executes(ctx -> setLevel(ctx.getSource(), ctx.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(ctx, "level")))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> setLevel(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                                IntegerArgumentType.getInteger(ctx, "level")))))))
                .then(Commands.literal("gate")
                        .executes(ctx -> openGate(ctx.getSource(), 0))
                        .then(Commands.argument("stars", IntegerArgumentType.integer(1, Gates.MAX_STARS))
                                .executes(ctx -> openGate(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "stars")))))
                .then(caveCommand())
                .then(jobCommand())
                .then(Commands.literal("reset")
                        .executes(ctx -> reset(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> reset(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("info")
                        .executes(ctx -> info(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> info(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))));

        var classCommand = Commands.literal("class");
        for (PlayerClass playerClass : PlayerClass.values()) {
            classCommand.then(Commands.literal(playerClass.key())
                    .executes(ctx -> setClass(ctx.getSource(), ctx.getSource().getPlayerOrException(), playerClass))
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(ctx -> setClass(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), playerClass))));
        }
        root.addChild(classCommand.build());

        dispatcher.register(Commands.literal("aq").requires(source -> source.hasPermission(2)).redirect(root));
    }

    private static int openGate(CommandSourceStack source, int stars) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int rank = stars > 0 ? stars : Gates.randomStars(player.getRandom());
        Vec3 pos = player.position().add(player.getLookAngle().multiply(1, 0, 1).normalize().scale(5.0));
        Gates.open(player.serverLevel(), pos, rank);
        source.sendSuccess(() -> Component.translatable("command.arcanequest.gate_opened", Gates.stars(rank)), true);
        return 1;
    }

    private static int addXp(CommandSourceStack source, ServerPlayer player, int amount) {
        StatsEvents.addXp(player, amount);
        source.sendSuccess(() -> Component.translatable("command.arcanequest.xp_added", amount, player.getDisplayName()), true);
        return 1;
    }

    private static int setLevel(CommandSourceStack source, ServerPlayer player, int level) {
        modify(player, stats -> stats.setLevel(level));
        source.sendSuccess(() -> Component.translatable("command.arcanequest.level_set", player.getDisplayName(), level), true);
        return 1;
    }

    private static int reset(CommandSourceStack source, ServerPlayer player) {
        modify(player, PlayerStats::resetStats);
        source.sendSuccess(() -> Component.translatable("command.arcanequest.reset", player.getDisplayName()), true);
        return 1;
    }

    private static int setClass(CommandSourceStack source, ServerPlayer player, PlayerClass playerClass) {
        modify(player, stats -> stats.setPlayerClass(playerClass));
        source.sendSuccess(() -> Component.translatable("command.arcanequest.class_set", player.getDisplayName(),
                playerClass.displayName()), true);
        return 1;
    }

    /** {@code /aq job <job> <level> [player]}: sets a job's level (for testing). */
    private static LiteralArgumentBuilder<CommandSourceStack> jobCommand() {
        var command = Commands.literal("job");
        for (Job job : Job.values()) {
            command.then(Commands.literal(job.key()).then(Commands.argument("level", IntegerArgumentType.integer(1, Job.MAX_LEVEL))
                    .executes(ctx -> setJob(ctx.getSource(), ctx.getSource().getPlayerOrException(), job,
                            IntegerArgumentType.getInteger(ctx, "level")))
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(ctx -> setJob(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), job,
                                    IntegerArgumentType.getInteger(ctx, "level"))))));
        }
        return command;
    }

    private static int setJob(CommandSourceStack source, ServerPlayer player, Job job, int level) {
        StatsCapability.get(player).ifPresent(stats -> {
            stats.setJobLevel(job, level);
            ModNetwork.sync(player);
        });
        source.sendSuccess(() -> Component.translatable("command.arcanequest.job_set", job.displayName(), level, player.getDisplayName()), true);
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> caveCommand() {
        var tp = Commands.literal("tp");
        for (CaveDecoratorFeature.Theme theme : CaveDecoratorFeature.Theme.values()) {
            if (theme != CaveDecoratorFeature.Theme.PLAIN) {
                tp.then(Commands.literal(theme.name().toLowerCase(Locale.ROOT))
                        .executes(ctx -> caveTeleport(ctx.getSource(), ctx.getSource().getPlayerOrException(), theme)));
            }
        }
        return Commands.literal("cave").executes(ctx -> caves(ctx.getSource(), ctx.getSource().getPlayerOrException())).then(tp);
    }

    /** Teleports into a decorated cave of the closest region of {@code theme} (for testing and exploring). */
    private static int caveTeleport(CommandSourceStack source, ServerPlayer player, CaveDecoratorFeature.Theme theme) {
        BlockPos pos = player.blockPosition();
        int y = theme == CaveDecoratorFeature.Theme.MAGMA ? Math.min(pos.getY(), -1) : pos.getY();
        BlockPos region = CaveDecoratorFeature.nearest(player.serverLevel().getSeed(), theme, pos.getX(), y, pos.getZ());
        BlockPos spot = region == null ? null : CaveDecoratorFeature.findCaveSpot(player.serverLevel(), theme, region);
        if (spot == null) {
            source.sendFailure(Component.translatable("command.arcanequest.cave_not_found", theme.displayName()));
            return 0;
        }
        player.teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
        source.sendSuccess(() -> Component.translatable("command.arcanequest.cave_teleported", theme.displayName(),
                spot.getX(), spot.getY(), spot.getZ()), false);
        return 1;
    }

    /** Cave region of the player's position and the closest region of every other kind on the same layer. */
    private static int caves(CommandSourceStack source, ServerPlayer player) {
        long seed = player.serverLevel().getSeed();
        BlockPos pos = player.blockPosition();
        CaveDecoratorFeature.Theme here = CaveDecoratorFeature.themeAt(seed, pos.getX(), pos.getY(), pos.getZ());
        source.sendSuccess(() -> Component.translatable("command.arcanequest.cave_here", here.displayName(),
                Component.translatable(pos.getY() < 0 ? "cave.arcanequest.layer_deep" : "cave.arcanequest.layer_upper")), false);
        for (CaveDecoratorFeature.Theme theme : CaveDecoratorFeature.Theme.values()) {
            if (theme == here || theme == CaveDecoratorFeature.Theme.PLAIN) {
                continue;
            }
            // Magma caves only exist below y = 0
            int y = theme == CaveDecoratorFeature.Theme.MAGMA ? Math.min(pos.getY(), -1) : pos.getY();
            BlockPos found = CaveDecoratorFeature.nearest(seed, theme, pos.getX(), y, pos.getZ());
            if (found != null) {
                double dx = found.getX() - pos.getX(), dz = found.getZ() - pos.getZ();
                source.sendSuccess(() -> Component.translatable("command.arcanequest.cave_nearest", theme.displayName(),
                        (int) Math.sqrt(dx * dx + dz * dz), Gates.direction(dx, dz), found.getX(), found.getZ()), false);
            }
        }
        return 1;
    }

    private static int info(CommandSourceStack source, ServerPlayer player) {
        StatsCapability.get(player).ifPresent(stats -> source.sendSuccess(() -> Component.translatable(
                "command.arcanequest.info", player.getDisplayName(), stats.getLevel(), stats.getXp(),
                PlayerStats.xpForNextLevel(stats.getLevel()), stats.getPoints()), false));
        return 1;
    }

    private static void modify(ServerPlayer player, Consumer<PlayerStats> action) {
        StatsCapability.get(player).ifPresent(stats -> {
            action.accept(stats);
            StatEffects.apply(player, stats);
            ModNetwork.sync(player);
        });
    }
}
