package com.firerideur.arcanequest.client;

import com.firerideur.arcanequest.client.ui.BarTrail;
import com.firerideur.arcanequest.client.ui.UiKit;
import com.firerideur.arcanequest.dungeon.DungeonMob;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Name plate with level and health of the leveled mob under the crosshair. */
public final class TargetHud {
    private static final double RANGE = 24.0;
    private static final int WIDTH = 120, HEIGHT = 30;

    private static final BarTrail TRAIL = new BarTrail();
    private static int trailTarget = -1;

    private TargetHud() {}

    public static void render(GuiGraphics g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) {
            return;
        }
        LivingEntity target = findTarget(mc, deltaTracker.getGameTimeDeltaPartialTick(true));
        if (target == null) {
            return;
        }
        if (target instanceof DungeonMob mob && mob.isDormant()) {
            // Don't give away statues and mimics
            return;
        }
        ClientMobLevels.Info info = ClientMobLevels.get(target.getId());
        if (info == null) {
            return;
        }
        int playerLevel = ClientStats.get().getLevel();
        MutableComponent name = target.getDisplayName().copy();
        if (info.level() >= playerLevel + 10) {
            name.append(Component.literal(" \u2620").withStyle(ChatFormatting.DARK_RED));
        }
        // Grow with long names (Champions)
        int width = Math.max(WIDTH, mc.font.width(name) + 40);
        int x = g.guiWidth() / 2 - width / 2;
        int y = g.guiHeight() / 2 + 14;

        UiKit.panel(g, x, y, width, HEIGHT);
        if (info.elite()) {
            UiKit.frame(g, x + 1, y + 1, width - 2, HEIGHT - 2, 0xFFD9A030);
        }
        // Level badge on the left
        String level = String.valueOf(info.level());
        int badgeColor = levelColor(info.level(), playerLevel).getColor() | 0xFF000000;
        g.fill(x + 6, y + 6, x + 24, y + 18, 0xFF0A0908);
        UiKit.frame(g, x + 6, y + 6, 18, 12, badgeColor);
        g.drawCenteredString(mc.font, level, x + 15, y + 8, badgeColor);

        g.drawString(mc.font, name, x + 28, y + 7, UiKit.TEXT, false);

        float health = target.getHealth() / target.getMaxHealth();
        if (trailTarget != target.getId()) {
            TRAIL.reset(health);
            trailTarget = target.getId();
        }
        int barColor = info.elite() ? 0xFFD9A030 : 0xFFC0282D;
        UiKit.bar(g, x + 28, y + 19, width - 40, 4, health, TRAIL.update(health), barColor);
        UiKit.barText(g, mc.font, Math.round(target.getHealth()) + " / " + Math.round(target.getMaxHealth()),
                x + 28 + (width - 40) / 2, y + 19, 0.5F);
    }

    /** Like World of Warcraft: grey = trivial, green = easy, white = even, yellow/red = dangerous. */
    private static ChatFormatting levelColor(int mobLevel, int playerLevel) {
        int diff = mobLevel - playerLevel;
        if (diff <= -10) return ChatFormatting.GRAY;
        if (diff <= -3) return ChatFormatting.GREEN;
        if (diff <= 2) return ChatFormatting.WHITE;
        if (diff <= 6) return ChatFormatting.YELLOW;
        return ChatFormatting.RED;
    }

    private static LivingEntity findTarget(Minecraft mc, float partialTick) {
        Entity camera = mc.getCameraEntity();
        if (camera == null) {
            return null;
        }
        Vec3 eye = camera.getEyePosition(partialTick);
        Vec3 look = camera.getViewVector(partialTick);
        double range = RANGE;
        HitResult blockHit = camera.pick(RANGE, partialTick, false);
        if (blockHit.getType() != HitResult.Type.MISS) {
            range = blockHit.getLocation().distanceTo(eye);
        }
        Vec3 end = eye.add(look.scale(range));
        AABB area = camera.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(camera, eye, end, area,
                e -> e instanceof LivingEntity && !e.isSpectator() && e.isPickable(), range * range);
        return hit != null && hit.getEntity() instanceof LivingEntity living && living.isAlive() ? living : null;
    }
}
