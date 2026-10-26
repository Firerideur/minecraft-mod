package com.firerideur.arcanequest.client;

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

/** Shows the name, level and health of the leveled mob under the crosshair. */
public final class TargetHud {
    private static final double RANGE = 24.0;
    private static final int BAR_WIDTH = 80;

    private TargetHud() {}

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) {
            return;
        }
        LivingEntity target = findTarget(mc, deltaTracker.getGameTimeDeltaPartialTick(true));
        if (target == null) {
            return;
        }
        ClientMobLevels.Info info = ClientMobLevels.get(target.getId());
        if (info == null) {
            return;
        }
        int playerLevel = ClientStats.get().getLevel();
        MutableComponent text = target.getDisplayName().copy()
                .append(Component.literal(" "))
                .append(Component.translatable("hud.arcanequest.level", info.level()).withStyle(levelColor(info.level(), playerLevel)));
        if (info.level() >= playerLevel + 10) {
            text.append(Component.literal(" ☠").withStyle(ChatFormatting.DARK_RED));
        }
        int centerX = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() / 2 + 14;
        graphics.drawCenteredString(mc.font, text, centerX, y, 0xFFFFFF);
        ClientSetup.drawBar(graphics, centerX - BAR_WIDTH / 2, y + 11, BAR_WIDTH, 2,
                target.getHealth() / target.getMaxHealth(), info.elite() ? 0xFFFFAA00 : 0xFFE03030);
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
