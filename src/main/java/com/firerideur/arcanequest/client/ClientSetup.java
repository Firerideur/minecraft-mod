package com.firerideur.arcanequest.client;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.client.boss.BossRenderers;
import com.firerideur.arcanequest.client.dungeon.DungeonRenderers;
import com.firerideur.arcanequest.network.ModNetwork;
import com.firerideur.arcanequest.network.OpenCharacterMenuPacket;
import com.firerideur.arcanequest.network.UseSkillPacket;
import com.firerideur.arcanequest.registry.ModEntities;
import com.firerideur.arcanequest.registry.ModMenus;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

public final class ClientSetup {
    public static final String KEY_CATEGORY = "key.categories.arcanequest";

    public static final KeyMapping OPEN_STATS = new KeyMapping("key.arcanequest.open_stats",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, KEY_CATEGORY);

    public static final KeyMapping TOGGLE_MINIMAP = new KeyMapping("key.arcanequest.toggle_minimap",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, KEY_CATEGORY);

    /** Keys of the skill slots: R, C, V, B, N by default. */
    public static final KeyMapping[] SKILL_KEYS = {
            skillKey(1, GLFW.GLFW_KEY_R), skillKey(2, GLFW.GLFW_KEY_C), skillKey(3, GLFW.GLFW_KEY_V),
            skillKey(4, GLFW.GLFW_KEY_B), skillKey(5, GLFW.GLFW_KEY_N)};

    private ClientSetup() {}

    private static KeyMapping skillKey(int number, int key) {
        return new KeyMapping("key.arcanequest.skill_" + number, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, key, KEY_CATEGORY);
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(ClientSetup::registerKeys);
        modEventBus.addListener(ClientSetup::registerOverlays);
        modEventBus.addListener(ClientSetup::registerRenderers);
        modEventBus.addListener(BossRenderers::registerLayers);
        modEventBus.addListener(BossRenderers::registerRenderers);
        modEventBus.addListener(DungeonRenderers::registerLayers);
        modEventBus.addListener(DungeonRenderers::registerRenderers);
        modEventBus.addListener(ClientSetup::clientSetup);
        MinecraftForge.EVENT_BUS.addListener(ClientSetup::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(ClientSetup::onLoggingOut);
        MinecraftForge.EVENT_BUS.addListener(BossBars::onBossBar);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_STATS);
        event.register(TOGGLE_MINIMAP);
        for (KeyMapping key : SKILL_KEYS) {
            event.register(key);
        }
    }

    private static void registerOverlays(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(ArcaneQuest.id("status_hud"), ArcaneHud::renderStatus);
        event.getLayeredDraw().add(ArcaneQuest.id("compass"), ArcaneHud::renderCompass);
        event.getLayeredDraw().add(ArcaneQuest.id("minimap"), ArcaneHud::renderMinimap);
        event.getLayeredDraw().add(ArcaneQuest.id("target_hud"), TargetHud::render);
        event.getLayeredDraw().add(ArcaneQuest.id("skill_bar"), SkillBar::render);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SPELL_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.SHADOW_SOLDIER.get(), ShadowSoldierRenderer::new);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.FUSION_FORGE.get(), FusionForgeScreen::new);
            MenuScreens.register(ModMenus.WEAPON_FORGE.get(), WeaponForgeScreen::new);
            MenuScreens.register(ModMenus.CHARACTER.get(), CharacterScreen::new);
        });
    }

    private static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        WorldMap.tick(mc);
        while (TOGGLE_MINIMAP.consumeClick()) {
            ArcaneHud.minimapVisible = !ArcaneHud.minimapVisible;
        }
        for (int slot = 0; slot < SKILL_KEYS.length; slot++) {
            while (SKILL_KEYS[slot].consumeClick()) {
                if (mc.player != null && mc.screen == null) {
                    ModNetwork.sendToServer(new UseSkillPacket(slot));
                }
            }
        }
        while (OPEN_STATS.consumeClick()) {
            if (mc.player != null && mc.screen == null) {
                ModNetwork.sendToServer(new OpenCharacterMenuPacket());
            }
        }
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientStats.get().load(new CompoundTag());
        ClientMobLevels.clear();
        BossBars.clear();
        ClientSkills.clear();
    }

    static void drawBar(GuiGraphics graphics, int x, int y, int width, int height, float progress, int color) {
        float clamped = Math.max(0.0F, Math.min(1.0F, progress));
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF000000);
        graphics.fill(x, y, x + width, y + height, 0xFF3A3A3A);
        graphics.fill(x, y, x + Math.round(width * clamped), y + height, color);
    }
}
