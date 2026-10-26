package com.firerideur.arcanequest.command;

import com.firerideur.arcanequest.network.ModNetwork;
import com.firerideur.arcanequest.stats.PlayerStats;
import com.firerideur.arcanequest.stats.StatEffects;
import com.firerideur.arcanequest.stats.StatsCapability;
import com.firerideur.arcanequest.stats.StatsEvents;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

/**
 * /arcanequest (alias /aq) — admin and testing commands.
 * <ul>
 *     <li>/aq xp add &lt;amount&gt; [player]</li>
 *     <li>/aq level set &lt;level&gt; [player]</li>
 *     <li>/aq reset [player] — refunds every stat point</li>
 *     <li>/aq info [player]</li>
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
                .then(Commands.literal("reset")
                        .executes(ctx -> reset(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> reset(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("info")
                        .executes(ctx -> info(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> info(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))));

        dispatcher.register(Commands.literal("aq").requires(source -> source.hasPermission(2)).redirect(root));
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
