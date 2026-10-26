package com.firerideur.arcanequest.client;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.magic.Mana;
import com.firerideur.arcanequest.registry.ModEntities;
import com.firerideur.arcanequest.stats.PlayerStats;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.EvokerRenderer;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.WitherSkeletonRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.glfw.GLFW;

public final class ClientSetup {
    public static final String KEY_CATEGORY = "key.categories.arcanequest";

    public static final KeyMapping OPEN_STATS = new KeyMapping("key.arcanequest.open_stats",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, KEY_CATEGORY);

    private ClientSetup() {}

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(ClientSetup::registerKeys);
        modEventBus.addListener(ClientSetup::registerOverlays);
        modEventBus.addListener(ClientSetup::registerRenderers);
        MinecraftForge.EVENT_BUS.addListener(ClientSetup::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(ClientSetup::onLoggingOut);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_STATS);
    }

    private static void registerOverlays(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(ArcaneQuest.id("level_hud"), ClientSetup::renderHud);
        event.getLayeredDraw().add(ArcaneQuest.id("target_hud"), TargetHud::render);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SPELL_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.SKELETON_KING.get(), WitherSkeletonRenderer::new);
        event.registerEntityRenderer(ModEntities.CORRUPTED_ARCHMAGE.get(), EvokerRenderer::new);
        event.registerEntityRenderer(ModEntities.ADAMANTITE_GOLEM.get(), IronGolemRenderer::new);
    }

    private static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (OPEN_STATS.consumeClick()) {
            if (mc.player != null && mc.screen == null) {
                mc.setScreen(new StatsScreen());
            }
        }
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientStats.get().load(new CompoundTag());
        ClientMobLevels.clear();
    }

    /** Level, XP bar and mana bar in the top-left corner. */
    private static void renderHud(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) {
            return;
        }
        PlayerStats stats = ClientStats.get();
        int x = 4;
        int y = 4;
        int barWidth = 80;

        Component label = Component.translatable("hud.arcanequest.level", stats.getLevel());
        graphics.drawString(mc.font, label, x, y, 0xFFD700);
        if (stats.getPoints() > 0) {
            graphics.drawString(mc.font, Component.literal(" (+" + stats.getPoints() + ")"),
                    x + mc.font.width(label), y, 0x7CFC00);
        }
        drawXpBar(graphics, x, y + 11, barWidth, 3, stats);

        float maxMana = Mana.max(mc.player, stats);
        drawBar(graphics, x, y + 17, barWidth, 3, stats.getMana() / maxMana, 0xFF3080FF);
        graphics.drawString(mc.font, Component.translatable("hud.arcanequest.mana", (int) stats.getMana(), (int) maxMana),
                x + barWidth + 4, y + 14, 0x6FA8FF);
    }

    static void drawXpBar(GuiGraphics graphics, int x, int y, int width, int height, PlayerStats stats) {
        float progress = stats.isMaxLevel() ? 1.0F
                : (float) stats.getXp() / PlayerStats.xpForNextLevel(stats.getLevel());
        drawBar(graphics, x, y, width, height, progress, 0xFF7CFC00);
    }

    static void drawBar(GuiGraphics graphics, int x, int y, int width, int height, float progress, int color) {
        float clamped = Math.max(0.0F, Math.min(1.0F, progress));
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF000000);
        graphics.fill(x, y, x + width, y + height, 0xFF3A3A3A);
        graphics.fill(x, y, x + Math.round(width * clamped), y + height, color);
    }
}
